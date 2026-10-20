package twelvecafe.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.IOException;
import java.time.LocalDate;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import twelvecafe.model.InventoryItem;
import twelvecafe.model.StockBatch;
import twelvecafe.model.StockStatus;
import twelvecafe.service.InventoryManager;

/**
 * One form used for three transactions: Stock In, Stock Out and Waste.
 * The right side shows the selected item's batch queue so staff can see
 * which batch will be used next.
 */
public class StockEntryPanel extends JPanel implements Refreshable {

    /** The transaction this form records. */
    public enum Mode { STOCK_IN, STOCK_OUT, WASTE }

    private static final String[] WASTE_REASONS = {"Spoiled", "Expired", "Dropped / damaged", "Burnt / overcooked"};

    private final MainFrame frame;
    private final Mode mode;
    private final ItemPicker picker;
    private final JLabel currentStock = UiKit.label("Choose an item", Font.BOLD, 16f, UiKit.MUTED);
    private final JLabel quantityLabel = new JLabel("Quantity");
    private final JTextField quantityField = UiKit.textField(8, null);
    private final JLabel unitLabel = UiKit.muted("");
    private final JCheckBox expiryCheck = new JCheckBox("Has expiry date");
    private final JSpinner expirySpinner = UiKit.dateSpinner(LocalDate.now().plusDays(3));
    private final JTextField staffField = UiKit.textField(16, "Who is recording this?");
    private final JLabel noteLabel = new JLabel("Note (optional)");
    private final JTextField noteField = UiKit.textField(22, null);
    private final JComboBox<String> reasonBox = new JComboBox<>(WASTE_REASONS);
    private final JLabel feedback = UiKit.feedbackLabel();

    private final JLabel itemTitle = UiKit.sectionTitle("No item selected");
    private final JLabel itemInfo = UiKit.muted("Pick an item on the left to see its batches.");
    private final DefaultTableModel batchModel = UiKit.readOnlyModel("#", "Quantity", "Received", "Expires");

    public StockEntryPanel(MainFrame frame, Mode mode) {
        super(new BorderLayout());
        this.frame = frame;
        this.mode = mode;
        // You cannot use or waste what is not there, so those forms list in-stock items only.
        this.picker = new ItemPicker(frame.getManager(), mode != Mode.STOCK_IN);
        setOpaque(false);
        setBorder(new EmptyBorder(0, 0, 20, 0));

        JPanel columns = UiKit.clear(new GridLayout(1, 2, 16, 0));
        columns.add(buildForm());
        columns.add(buildItemCard());
        add(columns, BorderLayout.CENTER);

        picker.onChange(this::itemChanged);
        itemChanged();
    }

    private JPanel buildForm() {
        JPanel card = UiKit.card();
        card.add(UiKit.sectionTitle(formTitle()), BorderLayout.NORTH);

        JPanel form = UiKit.clear(new GridBagLayout());
        int row = 0;
        addRow(form, row++, new JLabel("Item"), picker);
        addRow(form, row++, new JLabel("Current stock"), currentStock);

        JPanel qtyRow = UiKit.clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
        qtyRow.add(quantityField);
        qtyRow.add(Box.createHorizontalStrut(8));
        qtyRow.add(unitLabel);
        addRow(form, row++, quantityLabel, qtyRow);

        if (mode == Mode.STOCK_IN) {
            expiryCheck.setOpaque(false);
            expirySpinner.setEnabled(false);
            expiryCheck.addActionListener(e -> expirySpinner.setEnabled(expiryCheck.isSelected()));
            JPanel expiryRow = UiKit.clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
            expiryRow.add(expiryCheck);
            expiryRow.add(Box.createHorizontalStrut(8));
            expiryRow.add(expirySpinner);
            addRow(form, row++, new JLabel("Expiry"), expiryRow);
        }

        addRow(form, row++, new JLabel("Staff name"), staffField);
        if (mode == Mode.WASTE) {
            reasonBox.setEditable(true); // pick a reason or type another one
            noteLabel.setText("Reason");
            addRow(form, row++, noteLabel, reasonBox);
        } else {
            addRow(form, row++, noteLabel, noteField);
        }

        JButton record = UiKit.primaryButton(buttonText());
        JButton clear = UiKit.secondaryButton("Clear");
        record.addActionListener(e -> record());
        quantityField.addActionListener(e -> record());
        clear.addActionListener(e -> clearForm());
        JPanel buttons = UiKit.clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.add(record);
        buttons.add(Box.createHorizontalStrut(8));
        buttons.add(clear);
        addRow(form, row++, new JLabel(""), buttons);
        addRow(form, row++, new JLabel(""), feedback);

        // push everything to the top
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = row;
        filler.weighty = 1;
        form.add(new JLabel(), filler);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildItemCard() {
        JPanel card = UiKit.card();
        JPanel heading = UiKit.clear(new GridLayout(2, 1));
        heading.add(itemTitle);
        heading.add(itemInfo);
        JTable batchTable = UiKit.styledTable(batchModel, 1);
        UiKit.fixWidth(batchTable, 0, 36);
        batchTable.setToolTipText("Batches are kept in a FIFO queue: first in, first out");
        JPanel body = UiKit.clear(new BorderLayout(0, 6));
        body.add(UiKit.label("Batches - #1 (the oldest) is used first", Font.BOLD, 12.5f, UiKit.TEXT),
                BorderLayout.NORTH);
        body.add(UiKit.scroll(batchTable), BorderLayout.CENTER);
        body.add(UiKit.wrapping(helpText()), BorderLayout.SOUTH);
        card.add(heading, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    @Override
    public void refresh() {
        if (!expiryCheck.isSelected()) {
            UiKit.setDate(expirySpinner, frame.getManager().getWorkingDate().plusDays(3));
        }
        if (staffField.getText().trim().isEmpty()) {
            staffField.setText(frame.getLastStaff());
        }
        picker.refresh();
    }

    // ---- actions ----

    private void record() {
        InventoryItem item = picker.getSelectedItem();
        if (item == null) {
            UiKit.showFeedback(feedback, "Choose an item from the list first.", false);
            picker.focusSearch();
            return;
        }
        double quantity;
        try {
            quantity = Double.parseDouble(quantityField.getText().trim());
        } catch (NumberFormatException e) {
            UiKit.showFeedback(feedback, "Enter the quantity as a number, e.g. 5 or 2.5.", false);
            quantityField.requestFocusInWindow();
            return;
        }
        if (quantity <= 0) {
            UiKit.showFeedback(feedback, "Quantity must be greater than 0.", false);
            quantityField.requestFocusInWindow();
            return;
        }
        String staff = staffField.getText().trim();
        if (staff.isEmpty()) {
            UiKit.showFeedback(feedback, "Enter the staff name.", false);
            staffField.requestFocusInWindow();
            return;
        }
        String note = mode == Mode.WASTE
                ? String.valueOf(reasonBox.getSelectedItem()).trim()
                : noteField.getText().trim();

        InventoryManager manager = frame.getManager();
        try {
            String done;
            if (mode == Mode.STOCK_IN) {
                LocalDate expiry = expiryCheck.isSelected() ? UiKit.getDate(expirySpinner) : null;
                if (expiry != null && expiry.isBefore(manager.getWorkingDate())
                        && !UiKit.confirm(this, "The expiry date has already passed. Record anyway?")) {
                    return;
                }
                manager.stockIn(item, quantity, expiry, staff, note);
                done = "Recorded " + item.getStockInLabel().toLowerCase();
            } else if (mode == Mode.STOCK_OUT) {
                manager.stockOut(item, quantity, staff, note);
                done = "Recorded " + item.getStockOutLabel().toLowerCase();
            } else {
                manager.recordWaste(item, quantity, staff, note);
                done = "Recorded waste";
            }
            frame.setLastStaff(staff);
            String message = done + " of " + UiKit.qty(quantity) + " " + item.getUnit() + " " + item.getName()
                    + ". Now " + UiKit.qty(item.getQuantity()) + " " + item.getUnit() + " on hand.";
            if (item.getStatus() != StockStatus.OK) {
                message += " Status: " + item.getStatus().getLabel() + ", restock soon.";
            }
            quantityField.setText("");
            noteField.setText("");
            frame.dataChanged(message);
            UiKit.showFeedback(feedback, message, true);
        } catch (IllegalArgumentException e) {
            UiKit.showFeedback(feedback, e.getMessage(), false);
        } catch (IOException e) {
            frame.showSaveError(e);
        }
    }

    private void clearForm() {
        quantityField.setText("");
        noteField.setText("");
        expiryCheck.setSelected(false);
        expirySpinner.setEnabled(false);
        UiKit.clearFeedback(feedback);
        picker.clear();
        picker.focusSearch();
    }

    /** Updates labels and the batch table when another item is chosen. */
    private void itemChanged() {
        InventoryItem item = picker.getSelectedItem();
        batchModel.setRowCount(0);
        if (item == null) {
            currentStock.setText("Choose an item");
            currentStock.setFont(UiKit.font(Font.PLAIN, 14f));
            currentStock.setForeground(UiKit.MUTED);
            unitLabel.setText("");
            quantityLabel.setText("Quantity");
            itemTitle.setText("No item selected");
            itemInfo.setText("Pick an item on the left to see its batches.");
            return;
        }
        currentStock.setText(UiKit.qty(item.getQuantity()) + " " + item.getUnit());
        currentStock.setFont(UiKit.font(Font.BOLD, 20f));
        currentStock.setForeground(item.getStatus() == StockStatus.OK ? UiKit.TEXT
                : item.getStatus() == StockStatus.LOW ? UiKit.LOW : UiKit.OUT);
        unitLabel.setText(item.getUnit());
        boolean raw = item.getType().equals("RAW");
        switch (mode) {
            case STOCK_IN:
                quantityLabel.setText(raw ? "Delivery quantity" : "Production quantity");
                noteLabel.setText(raw ? "Supplier / note" : "Note (optional)");
                break;
            case STOCK_OUT:
                quantityLabel.setText(raw ? "Quantity used" : "Quantity out");
                break;
            default:
                quantityLabel.setText("Quantity wasted");
                break;
        }
        itemTitle.setText(item.getName());
        itemInfo.setText((raw ? "Raw ingredient" : "Production item") + "  |  reorder at "
                + UiKit.qty(item.getReorderLevel()) + " " + item.getUnit() + "  |  " + item.getStatus().getLabel());
        int position = 1;
        for (StockBatch batch : item.getBatches()) {
            batchModel.addRow(new Object[] {position++, UiKit.qty(batch.getQuantity()) + " " + item.getUnit(),
                    UiKit.mediumDate(batch.getDateReceived()),
                    batch.hasExpiry() ? UiKit.mediumDate(batch.getExpiryDate()) : "No expiry"});
        }
    }

    // ---- text per mode ----

    private String formTitle() {
        switch (mode) {
            case STOCK_IN: return "Record delivery or production";
            case STOCK_OUT: return "Record stock used";
            default: return "Record waste";
        }
    }

    private String buttonText() {
        switch (mode) {
            case STOCK_IN: return "Record Stock In";
            case STOCK_OUT: return "Record Stock Out";
            default: return "Record Waste";
        }
    }

    private String helpText() {
        switch (mode) {
            case STOCK_IN: return "New stock is added as a new batch at the end of the line.";
            case STOCK_OUT: return "Stock is taken from batch #1 first. When it runs out, the next batch is used.";
            default: return "Waste is taken from the oldest batch first and shows in the daily sheet.";
        }
    }

    private static void addRow(JPanel form, int row, JLabel label, JComponent field) {
        label.setFont(UiKit.font(Font.BOLD, 12.5f));
        label.setForeground(UiKit.MUTED);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(16, 0, 0, 16);
        form.add(label, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = row;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(10, 0, 0, 0);
        form.add(field, c);
    }
}
