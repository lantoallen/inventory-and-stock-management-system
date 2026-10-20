package twelvecafe.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import twelvecafe.model.InventoryItem;
import twelvecafe.model.StockBatch;
import twelvecafe.model.Transaction;
import twelvecafe.service.InventoryManager;

/**
 * All items in a table with filter, sort and search. Selecting an item shows
 * its batch queue (front = used next) and its recent transactions.
 */
public class InventoryPanel extends JPanel implements Refreshable {

    private static final String[] TYPES = {"All items", "Raw ingredients", "Production items"};
    private static final String[] SORTS = {"Name (A-Z)", "Quantity (lowest first)",
            "Quantity (highest first)", "Status (most urgent first)"};

    private final MainFrame frame;
    private final JComboBox<String> typeBox = new JComboBox<>(TYPES);
    private final JComboBox<String> sortBox = new JComboBox<>(SORTS);
    private final JTextField searchField = UiKit.textField(16, "Item name or keyword");
    private final JLabel resultInfo = UiKit.muted(" ");

    private final DefaultTableModel itemModel =
            UiKit.readOnlyModel("Item", "Type", "On hand", "Reorder at", "Status");
    private final JTable itemTable = UiKit.styledTable(itemModel, 2, 3);
    private final List<InventoryItem> shown = new ArrayList<>();

    private final JLabel detailTitle = UiKit.sectionTitle("Select an item");
    private final JLabel detailInfo = UiKit.muted("Click a row to see its batches and history.");
    private final DefaultTableModel batchModel =
            UiKit.readOnlyModel("#", "Quantity", "Received", "Expires");
    private final DefaultTableModel historyModel =
            UiKit.readOnlyModel("Date", "Type", "Qty", "Staff", "Note");

    public InventoryPanel(MainFrame frame) {
        super(new BorderLayout(0, 12));
        this.frame = frame;
        setOpaque(false);
        setBorder(new EmptyBorder(0, 0, 20, 0));

        // ---- toolbar ----
        // Results update while typing, so there is no separate Search button.
        JButton clearButton = UiKit.secondaryButton("Clear");
        searchField.setToolTipText("Exact names are found with Binary Search; partial names with Linear Search");
        sortBox.setToolTipText("Every sort order uses Merge Sort");
        UiKit.onTextChange(searchField, this::refresh);
        clearButton.addActionListener(e -> searchField.setText(""));
        typeBox.addActionListener(e -> refresh());
        sortBox.addActionListener(e -> refresh());

        JPanel toolbar = UiKit.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.add(UiKit.muted("Search"));
        toolbar.add(searchField);
        toolbar.add(clearButton);
        toolbar.add(UiKit.muted("    Show"));
        toolbar.add(typeBox);
        toolbar.add(UiKit.muted("    Sort by"));
        toolbar.add(sortBox);

        JPanel top = UiKit.clear(new BorderLayout(0, 6));
        top.add(toolbar, BorderLayout.NORTH);
        top.add(resultInfo, BorderLayout.SOUTH);

        // ---- item table ----
        UiKit.useStatusRenderer(itemTable, 4);
        UiKit.setColumnWidths(itemTable, 170, 60, 90, 100);
        itemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showDetails();
            }
        });
        JPanel tableCard = UiKit.card();
        tableCard.add(UiKit.scroll(itemTable), BorderLayout.CENTER);

        // ---- details ----
        JTable batchTable = UiKit.styledTable(batchModel, 1);
        JTable historyTable = UiKit.styledTable(historyModel, 2);
        UiKit.fixWidth(batchTable, 0, 36);
        UiKit.setColumnWidths(batchTable, 36, 95, 105, 105);
        UiKit.fixWidth(historyTable, 0, 60);
        UiKit.setColumnWidths(historyTable, 60, 80, 45, 65, 120);
        batchTable.setToolTipText("Batches are kept in a FIFO queue: first in, first out");

        JPanel detailCard = UiKit.card();
        JPanel heading = UiKit.clear(new GridLayout(2, 1));
        heading.add(detailTitle);
        heading.add(detailInfo);
        JPanel detailBody = UiKit.clear(new GridLayout(2, 1, 0, 12));
        detailBody.add(titled("Batches - #1 (the oldest) is used first", batchTable));
        detailBody.add(titled("Recent transactions", historyTable));
        detailCard.add(heading, BorderLayout.NORTH);
        detailCard.add(detailBody, BorderLayout.CENTER);

        detailCard.setPreferredSize(new Dimension(430, 0));
        JPanel body = UiKit.clear(new BorderLayout(16, 0));
        body.add(tableCard, BorderLayout.CENTER);
        body.add(detailCard, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        InventoryManager manager = frame.getManager();
        InventoryItem previouslySelected = selectedItem();
        String type = typeBox.getSelectedIndex() == 1 ? "RAW"
                : typeBox.getSelectedIndex() == 2 ? "PRODUCTION" : null;
        Comparator<InventoryItem> order;
        switch (sortBox.getSelectedIndex()) {
            case 1: order = InventoryManager.BY_QUANTITY_LOW_FIRST; break;
            case 2: order = InventoryManager.BY_QUANTITY_HIGH_FIRST; break;
            case 3: order = InventoryManager.BY_STATUS; break;
            default: order = InventoryManager.BY_NAME; break;
        }

        List<InventoryItem> items = manager.getItems(type, order); // Merge Sort
        String keyword = searchField.getText().trim();
        InventoryItem exact = null;
        if (!keyword.isEmpty()) {
            exact = manager.findItem(keyword); // Binary Search
            List<InventoryItem> filtered = new ArrayList<>();
            List<InventoryItem> matches = manager.searchItems(keyword); // Linear Search
            for (InventoryItem item : items) {
                if (matches.contains(item)) {
                    filtered.add(item);
                }
            }
            items = filtered;
        }

        shown.clear();
        shown.addAll(items);
        itemModel.setRowCount(0);
        for (InventoryItem item : items) {
            itemModel.addRow(new Object[] {item.getName(), item.getType().equals("RAW") ? "Raw" : "Prod.",
                    UiKit.qty(item.getQuantity()) + " " + item.getUnit(),
                    UiKit.qty(item.getReorderLevel()) + " " + item.getUnit(), item.getStatus().getLabel()});
        }

        if (keyword.isEmpty()) {
            resultInfo.setText(items.size() + " item(s). Prod. = production item.");
        } else if (items.isEmpty()) {
            resultInfo.setText("No item matches \"" + keyword + "\".");
        } else {
            resultInfo.setText(items.size() + " item(s) match \"" + keyword + "\".");
        }

        InventoryItem toSelect = exact != null ? exact : previouslySelected;
        int row = toSelect == null ? -1 : shown.indexOf(toSelect);
        if (row >= 0) {
            itemTable.setRowSelectionInterval(row, row);
            itemTable.scrollRectToVisible(itemTable.getCellRect(row, 0, true));
        } else if (!shown.isEmpty() && !keyword.isEmpty()) {
            itemTable.setRowSelectionInterval(0, 0);
        }
        showDetails();
    }

    private InventoryItem selectedItem() {
        int row = itemTable.getSelectedRow();
        return row >= 0 && row < shown.size() ? shown.get(row) : null;
    }

    private void showDetails() {
        InventoryItem item = selectedItem();
        batchModel.setRowCount(0);
        historyModel.setRowCount(0);
        if (item == null) {
            detailTitle.setText("Select an item");
            detailInfo.setText("Click a row to see its batches and history.");
            return;
        }
        detailTitle.setText(item.getName());
        detailInfo.setText(UiKit.qty(item.getQuantity()) + " " + item.getUnit() + " on hand  |  reorder at "
                + UiKit.qty(item.getReorderLevel()) + " " + item.getUnit() + "  |  " + item.getStatus().getLabel());

        int position = 1;
        for (StockBatch batch : item.getBatches()) { // front of the queue first
            batchModel.addRow(new Object[] {position++, UiKit.qty(batch.getQuantity()) + " " + item.getUnit(),
                    UiKit.mediumDate(batch.getDateReceived()),
                    batch.hasExpiry() ? UiKit.mediumDate(batch.getExpiryDate()) : "No expiry"});
        }
        for (Transaction t : frame.getManager().getHistory(item, 10)) {
            historyModel.addRow(new Object[] {UiKit.shortDate(t.getDate()), HistoryPanel.typeLabel(t, item),
                    HistoryPanel.amount(t), t.getStaff(), t.getNote()});
        }
    }

    private static JPanel titled(String title, JTable table) {
        JPanel panel = UiKit.clear(new BorderLayout(0, 6));
        panel.add(UiKit.label(title, Font.BOLD, 12.5f, UiKit.TEXT), BorderLayout.NORTH);
        panel.add(UiKit.scroll(table), BorderLayout.CENTER);
        return panel;
    }
}
