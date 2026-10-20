package twelvecafe.gui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import twelvecafe.model.InventoryItem;
import twelvecafe.model.StockBatch;
import twelvecafe.service.ExpiringBatch;
import twelvecafe.service.InventoryManager;
import twelvecafe.service.ReportRow;

/** Restock list, expiring stock, and the daily inventory sheet. */
public class ReportsPanel extends JPanel implements Refreshable {

    private final MainFrame frame;
    private final JTabbedPane tabs = new JTabbedPane();

    // Restock list
    private final DefaultTableModel restockModel =
            UiKit.readOnlyModel("No.", "Item", "Type", "On hand", "Reorder at", "Unit", "Status");
    private final JLabel restockInfo = UiKit.muted(" ");

    // Expiring stock
    private final JSpinner daysSpinner =
            new JSpinner(new SpinnerNumberModel(InventoryManager.EXPIRY_ALERT_DAYS, 0, 365, 1));
    private final DefaultTableModel expiryModel =
            UiKit.readOnlyModel("Expires", "Item", "Quantity", "Received", "Status");
    private final List<Long> expiryDaysLeft = new ArrayList<>();
    private final JLabel expiryInfo = UiKit.muted(" ");
    private final JButton discardButton = UiKit.dangerButton("Discard expired stock");

    // Daily sheet
    private final JSpinner dateSpinner = UiKit.dateSpinner(LocalDate.now());
    private final JComboBox<String> sheetType = new JComboBox<>(new String[] {"Raw ingredients", "Production items"});
    private final JCheckBox hideEmpty = new JCheckBox("Hide items with no stock and no movement", true);
    private final DefaultTableModel dailyModel = UiKit.readOnlyModel("Item", "Beginning", "Delivery", "Used",
            "Waste", "Adjust", "Ending", "Unit");
    private final JTable dailyTable = UiKit.styledTable(dailyModel, 1, 2, 3, 4, 5, 6);
    private final JLabel dailyInfo = UiKit.muted(" ");
    private boolean dateInitialized;

    public ReportsPanel(MainFrame frame) {
        super(new BorderLayout());
        this.frame = frame;
        setOpaque(false);
        setBorder(new EmptyBorder(0, 0, 20, 0));

        tabs.setFont(UiKit.font(Font.BOLD, 13f));
        tabs.addTab("Restock List", buildRestockTab());
        tabs.addTab("Expiring Stock", buildExpiryTab());
        tabs.addTab("Daily Inventory Sheet", buildDailyTab());
        tabs.addChangeListener(e -> refresh());
        add(tabs, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        if (!dateInitialized) {
            UiKit.setDate(dateSpinner, frame.getManager().getWorkingDate());
            dateInitialized = true;
        }
        refreshRestock();
        refreshExpiry();
        refreshDaily();
    }

    // ============================ Restock list ============================

    private JComponent buildRestockTab() {
        JTable table = UiKit.styledTable(restockModel, 3, 4);
        UiKit.useStatusRenderer(table, 6);
        UiKit.fixWidth(table, 0, 44);
        UiKit.setColumnWidths(table, 44, 240, 100, 80, 90, 70);
        table.setToolTipText("Ordered with Merge Sort: lowest stock compared to the reorder level first");
        JButton export = UiKit.secondaryButton("Export for printing");
        export.addActionListener(e -> export("restock-" + frame.getManager().getWorkingDate() + ".txt",
                "THE TWELVE CAFE - RESTOCK LIST (" + frame.getManager().getWorkingDate() + ")", restockModel));

        JPanel toolbar = UiKit.clear(new BorderLayout());
        toolbar.add(UiKit.muted("Items at or below their reorder level, most urgent first. "
                + "Use it as the next supplier order."), BorderLayout.CENTER);
        toolbar.add(export, BorderLayout.EAST);
        return tab(toolbar, table, restockInfo);
    }

    private void refreshRestock() {
        List<InventoryItem> low = frame.getManager().getLowStockItems();
        restockModel.setRowCount(0);
        int no = 1;
        for (InventoryItem item : low) {
            restockModel.addRow(new Object[] {no++, item.getName(),
                    item.getType().equals("RAW") ? "Raw" : "Production", UiKit.qty(item.getQuantity()),
                    UiKit.qty(item.getReorderLevel()), item.getUnit(), item.getStatus().getLabel()});
        }
        restockInfo.setText(low.size() + " item(s) need restocking.");
    }

    // ============================ Expiring stock ============================

    private JComponent buildExpiryTab() {
        JTable table = UiKit.styledTable(expiryModel, 2);
        UiKit.useBadgeRenderer(table, 4,
                row -> UiKit.expiryBadge(expiryDaysLeft.get(row), InventoryManager.EXPIRY_ALERT_DAYS));
        UiKit.fixWidth(table, 4, 110);
        UiKit.setColumnWidths(table, 120, 260, 110, 120, 110);
        table.setToolTipText("Ordered with Merge Sort by expiry date, soonest first");
        daysSpinner.addChangeListener(e -> refreshExpiry());
        discardButton.addActionListener(e -> discardExpired());

        JPanel toolbar = UiKit.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.add(UiKit.muted("Show batches expiring within"));
        toolbar.add(daysSpinner);
        toolbar.add(UiKit.muted("days, soonest first.      "));
        toolbar.add(discardButton);
        return tab(toolbar, table, expiryInfo);
    }

    private void refreshExpiry() {
        InventoryManager manager = frame.getManager();
        List<ExpiringBatch> list = manager.getExpiringBatches((Integer) daysSpinner.getValue());
        expiryModel.setRowCount(0);
        expiryDaysLeft.clear();
        int expired = 0;
        for (ExpiringBatch e : list) {
            StockBatch batch = e.getBatch();
            long left = batch.daysUntilExpiry(manager.getWorkingDate());
            if (left < 0) {
                expired++;
            }
            expiryDaysLeft.add(left);
            expiryModel.addRow(new Object[] {UiKit.mediumDate(batch.getExpiryDate()), e.getItem().getName(),
                    UiKit.qty(batch.getQuantity()) + " " + e.getItem().getUnit(),
                    UiKit.mediumDate(batch.getDateReceived()), UiKit.expiryText(left)});
        }
        expiryInfo.setText(list.isEmpty() ? "Nothing expires in this period."
                : list.size() + " batch(es) listed, " + expired + " already expired.");
        discardButton.setEnabled(expired > 0);
        discardButton.setToolTipText(expired > 0 ? "Remove expired batches and record them as waste"
                : "Nothing has expired");
    }

    private void discardExpired() {
        String staff = (String) JOptionPane.showInputDialog(this, "All expired batches will be removed and "
                + "recorded as waste.\nStaff name:", "Discard expired stock", JOptionPane.QUESTION_MESSAGE,
                null, null, frame.getLastStaff());
        if (staff == null || staff.trim().isEmpty()) {
            return;
        }
        try {
            List<String> removed = frame.getManager().discardExpired(staff.trim());
            frame.setLastStaff(staff.trim());
            frame.dataChanged("Discarded expired stock: " + String.join(", ", removed) + ".");
        } catch (IOException e) {
            frame.showSaveError(e);
        }
    }

    // ============================ Daily inventory sheet ============================

    private JComponent buildDailyTab() {
        UiKit.setColumnWidths(dailyTable, 220, 80, 80, 80, 70, 70, 80, 70);
        dateSpinner.addChangeListener(e -> refreshDaily());
        sheetType.addActionListener(e -> refreshDaily());
        hideEmpty.setOpaque(false);
        hideEmpty.addActionListener(e -> refreshDaily());
        JButton export = UiKit.secondaryButton("Export for printing");
        export.addActionListener(e -> {
            LocalDate date = UiKit.getDate(dateSpinner);
            boolean raw = sheetType.getSelectedIndex() == 0;
            export("daily-" + (raw ? "raw" : "production") + "-" + date + ".txt",
                    "THE TWELVE CAFE - " + (raw ? "RAW INGREDIENT" : "PRODUCTION") + " INVENTORY  |  "
                            + date + " (" + date.getDayOfWeek() + ")", dailyModel);
        });

        JPanel toolbar = UiKit.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.add(UiKit.muted("Date"));
        toolbar.add(dateSpinner);
        toolbar.add(sheetType);
        toolbar.add(hideEmpty);
        toolbar.add(export);
        return tab(toolbar, dailyTable, dailyInfo);
    }

    private void refreshDaily() {
        LocalDate date = UiKit.getDate(dateSpinner);
        boolean raw = sheetType.getSelectedIndex() == 0;
        // Column names follow the cafe's paper sheets.
        dailyTable.getColumnModel().getColumn(2).setHeaderValue(raw ? "Delivery" : "Prod");
        dailyTable.getColumnModel().getColumn(3).setHeaderValue(raw ? "Used" : "Out");
        dailyTable.getTableHeader().repaint();

        List<ReportRow> rows = frame.getManager().getDailyReport(date, raw ? "RAW" : "PRODUCTION");
        dailyModel.setRowCount(0);
        int hidden = 0;
        for (ReportRow r : rows) {
            if (hideEmpty.isSelected() && r.isEmpty()) {
                hidden++;
                continue;
            }
            dailyModel.addRow(new Object[] {r.getItem().getName(), UiKit.qty(r.getBeginning()),
                    UiKit.qty(r.getStockIn()), UiKit.qty(r.getStockOut()), UiKit.qty(r.getWaste()),
                    UiKit.signed(r.getAdjustment()), UiKit.qty(r.getEnding()), r.getItem().getUnit()});
        }
        dailyInfo.setText("Ending = Beginning + " + (raw ? "Delivery - Used" : "Prod - Out")
                + " - Waste + Adjust.   " + (hidden > 0 ? hidden + " empty item(s) hidden." : ""));
    }

    // ============================ Helpers ============================

    private static JComponent tab(JComponent toolbar, JTable table, JLabel footer) {
        JPanel panel = UiKit.card();
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(UiKit.scroll(table), BorderLayout.CENTER);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    /** Writes the table as aligned plain text into the reports folder. */
    private void export(String fileName, String title, DefaultTableModel model) {
        int cols = model.getColumnCount();
        int[] widths = new int[cols];
        for (int c = 0; c < cols; c++) {
            widths[c] = model.getColumnName(c).length();
            for (int r = 0; r < model.getRowCount(); r++) {
                widths[c] = Math.max(widths[c], String.valueOf(model.getValueAt(r, c)).length());
            }
        }
        List<String> lines = new ArrayList<>();
        lines.add(title);
        StringBuilder header = new StringBuilder();
        for (int c = 0; c < cols; c++) {
            header.append(String.format("%-" + (widths[c] + 3) + "s", model.getColumnName(c).toUpperCase()));
        }
        lines.add(header.toString().trim());
        lines.add("-".repeat(header.toString().trim().length()));
        for (int r = 0; r < model.getRowCount(); r++) {
            StringBuilder line = new StringBuilder();
            for (int c = 0; c < cols; c++) {
                line.append(String.format("%-" + (widths[c] + 3) + "s", model.getValueAt(r, c)));
            }
            lines.add(line.toString().trim());
        }
        try {
            Path file = frame.getManager().exportReport(fileName, lines).toAbsolutePath();
            frame.dataChanged("Report saved to " + file);
            if (Desktop.isDesktopSupported() && UiKit.confirm(this, "Report saved to:\n" + file + "\n\nOpen it now?")) {
                Desktop.getDesktop().open(file.toFile());
            }
        } catch (IOException | UnsupportedOperationException e) {
            UiKit.showError(this, "Could not save or open the report: " + e.getMessage());
        }
    }
}
