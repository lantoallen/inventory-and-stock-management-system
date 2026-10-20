package twelvecafe.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import twelvecafe.model.InventoryItem;
import twelvecafe.model.Transaction;
import twelvecafe.model.TransactionType;
import twelvecafe.service.InventoryManager;

/** Every recorded stock movement, newest first, with simple filters. */
public class HistoryPanel extends JPanel implements Refreshable {

    private static final int LIMIT = 500;
    private static final String[] TYPES = {"All types", "Stock in", "Stock out", "Waste", "Adjustment"};

    private final MainFrame frame;
    private final JTextField itemFilter = UiKit.textField(16, "Item name or keyword");
    private final JComboBox<String> typeFilter = new JComboBox<>(TYPES);
    private final DefaultTableModel model =
            UiKit.readOnlyModel("Date", "Type", "Item", "Quantity", "Staff", "Note");
    private final JLabel info = UiKit.muted(" ");

    public HistoryPanel(MainFrame frame) {
        super(new BorderLayout(0, 12));
        this.frame = frame;
        setOpaque(false);
        setBorder(new EmptyBorder(0, 0, 20, 0));

        JTable table = UiKit.styledTable(model, 3);
        UiKit.setColumnWidths(table, 110, 100, 220, 90, 110, 260);

        UiKit.onTextChange(itemFilter, this::refresh); // filters while typing
        typeFilter.addActionListener(e -> refresh());
        JButton clear = UiKit.secondaryButton("Clear");
        clear.addActionListener(e -> {
            itemFilter.setText("");
            typeFilter.setSelectedIndex(0);
            refresh();
        });

        JPanel toolbar = UiKit.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.add(UiKit.muted("Item"));
        toolbar.add(itemFilter);
        toolbar.add(UiKit.muted("   Type"));
        toolbar.add(typeFilter);
        toolbar.add(clear);

        JPanel card = UiKit.card();
        card.add(UiKit.scroll(table), BorderLayout.CENTER);
        card.add(info, BorderLayout.SOUTH);

        add(toolbar, BorderLayout.NORTH);
        add(card, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        InventoryManager manager = frame.getManager();
        String keyword = itemFilter.getText().trim().toUpperCase();
        TransactionType type = typeFilter.getSelectedIndex() == 0 ? null
                : TransactionType.values()[typeFilter.getSelectedIndex() - 1];

        List<Transaction> history = manager.getHistory(null, Integer.MAX_VALUE);
        model.setRowCount(0);
        for (Transaction t : history) {
            if (model.getRowCount() >= LIMIT) {
                break;
            }
            if (!keyword.isEmpty() && !t.getItemName().contains(keyword)) {
                continue;
            }
            if (type != null && t.getType() != type) {
                continue;
            }
            model.addRow(new Object[] {UiKit.mediumDate(t.getDate()), typeLabel(t, manager.findItem(t.getItemName())),
                    t.getItemName(), amount(t), t.getStaff(), t.getNote()});
        }
        info.setText(model.getRowCount() == 0 ? "No transactions recorded yet."
                : "Showing " + model.getRowCount() + " transaction(s), newest first"
                + (model.getRowCount() >= LIMIT ? " (latest " + LIMIT + ")." : "."));
    }

    /** Uses the cafe's own words: Delivery / Prod for stock in, Used / Out for stock out. */
    static String typeLabel(Transaction t, InventoryItem item) {
        switch (t.getType()) {
            case STOCK_IN: return item == null ? "Stock in" : item.getStockInLabel();
            case STOCK_OUT: return item == null ? "Stock out" : item.getStockOutLabel();
            case WASTE: return "Waste";
            default: return "Adjustment";
        }
    }

    static String amount(Transaction t) {
        return t.getType() == TransactionType.ADJUSTMENT ? UiKit.signed(t.getQuantity()) : UiKit.qty(t.getQuantity());
    }
}
