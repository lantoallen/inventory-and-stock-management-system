package twelvecafe.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import twelvecafe.model.InventoryItem;
import twelvecafe.service.InventoryManager;

/**
 * The digital version of the paper sheet's ENDING column: staff type the
 * physical count for each item. Differences from the system's quantity are
 * recorded as adjustments, so missing stock becomes visible. On a fresh
 * install this is also where the opening stock is entered.
 */
public class EndingCountPanel extends JPanel implements Refreshable {

    private static final int COUNTED = 3;
    private static final int VARIANCE = 4;

    private final MainFrame frame;
    private final JComboBox<String> typeBox = new JComboBox<>(new String[] {"Raw ingredients", "Production items"});
    private final JTextField staffField = UiKit.textField(14, "Who is counting?");
    private final List<InventoryItem> rows = new ArrayList<>();
    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel summary = UiKit.muted(" ");
    private int shownSheet = 0;

    public EndingCountPanel(MainFrame frame) {
        super(new BorderLayout(0, 12));
        this.frame = frame;
        setOpaque(false);
        setBorder(new EmptyBorder(0, 0, 20, 0));

        model = new DefaultTableModel(new String[] {"Item", "Unit", "System qty", "Counted", "Difference"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == COUNTED;
            }
        };
        table = UiKit.style(new CountTable(model), 2, 3, 4);
        table.setSurrendersFocusOnKeystroke(true);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        UiKit.setColumnWidths(table, 260, 80, 100, 110, 110);
        table.getColumnModel().getColumn(COUNTED).setCellRenderer(new CountRenderer());
        table.getColumnModel().getColumn(VARIANCE).setCellRenderer(new VarianceRenderer());

        // One click starts typing (no double-click needed).
        JTextField editorField = new JTextField();
        editorField.setHorizontalAlignment(SwingConstants.RIGHT);
        editorField.setFont(UiKit.font(Font.BOLD, 13f));
        DefaultCellEditor editor = new DefaultCellEditor(editorField);
        editor.setClickCountToStart(1);
        table.getColumnModel().getColumn(COUNTED).setCellEditor(editor);

        model.addTableModelListener(e -> {
            if (e.getColumn() == COUNTED) {
                for (int r = e.getFirstRow(); r <= e.getLastRow() && r < rows.size(); r++) {
                    updateVariance(r);
                }
                updateSummary();
            }
        });

        typeBox.addActionListener(e -> {
            if (typeBox.getSelectedIndex() == shownSheet) {
                return;
            }
            if (countedRows() == 0 || UiKit.confirm(this, "Switch sheets? The counts you typed will be cleared.")) {
                reload();
            } else {
                typeBox.setSelectedIndex(shownSheet); // stay on the current sheet
            }
        });
        JButton save = UiKit.primaryButton("Save Counts");
        JButton clear = UiKit.secondaryButton("Clear Counts");
        save.addActionListener(e -> saveCounts());
        clear.addActionListener(e -> {
            if (countedRows() > 0 && UiKit.confirm(this, "Clear all " + countedRows() + " counts you typed?")) {
                reload();
            }
        });

        JPanel toolbar = UiKit.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.add(UiKit.muted("Sheet"));
        toolbar.add(typeBox);
        toolbar.add(UiKit.muted("    Staff name"));
        toolbar.add(staffField);
        toolbar.add(save);
        toolbar.add(clear);

        JPanel top = UiKit.clear(new BorderLayout(0, 6));
        top.add(toolbar, BorderLayout.NORTH);
        top.add(UiKit.muted("Click a cell in the COUNTED column and type the quantity on the shelf. "
                + "Press Enter to move to the next item. Leave blank to skip."), BorderLayout.SOUTH);

        JPanel card = UiKit.card();
        card.add(UiKit.scroll(table), BorderLayout.CENTER);
        card.add(summary, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(card, BorderLayout.CENTER);
    }

    /** Keeps counts already typed (e.g. after visiting another page); otherwise reloads the sheet. */
    @Override
    public void refresh() {
        if (staffField.getText().trim().isEmpty()) {
            staffField.setText(frame.getLastStaff());
        }
        if (countedRows() == 0) {
            reload();
            return;
        }
        for (int r = 0; r < rows.size(); r++) { // stock may have changed on another page
            model.setValueAt(UiKit.qty(rows.get(r).getQuantity()), r, 2);
            updateVariance(r);
        }
    }

    /** Rebuilds the sheet with empty counts. */
    private void reload() {
        if (table.isEditing()) {
            table.getCellEditor().cancelCellEditing();
        }
        shownSheet = typeBox.getSelectedIndex();
        String type = shownSheet == 0 ? "RAW" : "PRODUCTION";
        rows.clear();
        rows.addAll(frame.getManager().getItems(type, InventoryManager.BY_NAME));
        model.setRowCount(0);
        for (InventoryItem item : rows) {
            model.addRow(new Object[] {item.getName(), item.getUnit(), UiKit.qty(item.getQuantity()), "", ""});
        }
        updateSummary();
    }

    private void saveCounts() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        String staff = staffField.getText().trim();
        if (staff.isEmpty()) {
            UiKit.showError(this, "Please enter the staff name.");
            staffField.requestFocusInWindow();
            return;
        }

        // Check every entry first so nothing is saved if one is invalid.
        List<Integer> toSave = new ArrayList<>();
        for (int r = 0; r < rows.size(); r++) {
            String text = countText(r);
            if (text.isEmpty()) {
                continue;
            }
            Double value = parse(text);
            if (value == null || value < 0) {
                UiKit.showError(this, rows.get(r).getName() + ": \"" + text + "\" is not a valid count.");
                return;
            }
            toSave.add(r);
        }
        if (toSave.isEmpty()) {
            UiKit.showError(this, "No counts entered yet. Click a cell in the COUNTED column to start.");
            return;
        }

        StringBuilder report = new StringBuilder();
        int adjusted = 0;
        try {
            for (int r : toSave) {
                InventoryItem item = rows.get(r);
                double variance = frame.getManager().recordPhysicalCount(item, parse(countText(r)), staff);
                if (variance != 0) {
                    adjusted++;
                    report.append(String.format("%-24s %s %s%n", item.getName(), UiKit.signed(variance),
                            item.getUnit()));
                }
            }
        } catch (IOException e) {
            frame.showSaveError(e);
        }
        frame.setLastStaff(staff);
        reload(); // the saved counts are now the system quantities

        String message = toSave.size() + " item(s) counted, " + adjusted + " adjusted.";
        if (adjusted > 0) {
            JOptionPane.showMessageDialog(this, message + "\n\nDifferences recorded:\n" + report,
                    "Ending count saved", JOptionPane.INFORMATION_MESSAGE);
        }
        frame.dataChanged("Ending count saved: " + message);
    }

    private void updateVariance(int r) {
        String text = countText(r);
        Double counted = text.isEmpty() ? null : parse(text);
        String value;
        if (text.isEmpty()) {
            value = "";
        } else if (counted == null || counted < 0) {
            value = "invalid";
        } else {
            value = UiKit.signed(counted - rows.get(r).getQuantity());
        }
        model.setValueAt(value, r, VARIANCE);
    }

    private void updateSummary() {
        summary.setText(countedRows() + " of " + rows.size() + " items counted. "
                + "A negative difference means stock is missing.");
    }

    private int countedRows() {
        int entered = 0;
        for (int r = 0; r < rows.size(); r++) {
            if (!countText(r).isEmpty()) {
                entered++;
            }
        }
        return entered;
    }

    private String countText(int r) {
        Object value = model.getValueAt(r, COUNTED);
        return value == null ? "" : value.toString().trim();
    }

    private static Double parse(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Keeps the cursor in the COUNTED column: clicking any cell of a row, or
     * pressing Enter, lands on that row's count, like filling in a paper column.
     */
    private static class CountTable extends JTable {
        CountTable(DefaultTableModel model) {
            super(model);
        }

        @Override
        public void changeSelection(int row, int column, boolean toggle, boolean extend) {
            super.changeSelection(row, COUNTED, toggle, extend);
        }
    }

    /** Highlights the editable column so staff know where to type. */
    private static class CountRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                       boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, selected, false, row, col);
            setHorizontalAlignment(SwingConstants.RIGHT);
            setBackground(selected ? new Color(0xF6DFC8) : new Color(0xFFF8EE));
            setForeground(UiKit.TEXT);
            setFont(UiKit.font(Font.BOLD, 13f));
            setBorder(new EmptyBorder(0, 10, 0, 10));
            return this;
        }
    }

    /** Red for missing stock, green for extra stock. */
    private static class VarianceRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                       boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, selected, false, row, col);
            String text = value == null ? "" : value.toString();
            setHorizontalAlignment(SwingConstants.RIGHT);
            setBackground(selected ? UiKit.SELECTION : (row % 2 == 0 ? UiKit.CARD : UiKit.ZEBRA));
            setForeground(text.startsWith("-") || text.equals("invalid") ? UiKit.OUT
                    : text.startsWith("+") ? UiKit.OK : UiKit.MUTED);
            setFont(UiKit.font(Font.BOLD, 13f));
            setBorder(new EmptyBorder(0, 10, 0, 10));
            return this;
        }
    }
}
