package twelvecafe.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import twelvecafe.model.InventoryItem;
import twelvecafe.model.StockBatch;
import twelvecafe.model.StockStatus;
import twelvecafe.service.ExpiringBatch;
import twelvecafe.service.InventoryManager;

/** Home page: summary numbers, what to restock first, and what expires soon. */
public class DashboardPanel extends JPanel implements Refreshable {

    private static final int RESTOCK_ROWS = 15;
    private static final int ALERT_DAYS = InventoryManager.EXPIRY_ALERT_DAYS;

    private final MainFrame frame;
    private final JLabel totalValue = statValue(UiKit.TEXT);
    private final JLabel totalCaption = UiKit.muted("");
    private final JLabel lowValue = statValue(UiKit.LOW);
    private final JLabel outValue = statValue(UiKit.OUT);
    private final JLabel expiringValue = statValue(UiKit.ACCENT_DARK);
    private final JPanel welcome = buildWelcome();

    private final DefaultTableModel restockModel = UiKit.readOnlyModel("Item", "On hand", "Status");
    private final DefaultTableModel expiryModel = UiKit.readOnlyModel("Item", "Qty", "Expires", "Status");
    private final List<Long> expiryDaysLeft = new ArrayList<>();
    private final JLabel restockCaption = UiKit.muted("");

    public DashboardPanel(MainFrame frame) {
        super(new BorderLayout(0, 16));
        this.frame = frame;
        setOpaque(false);
        setBorder(new EmptyBorder(0, 0, 20, 0));

        JPanel stats = UiKit.clear(new GridLayout(1, 4, 16, 0));
        stats.add(statCard("Total items", totalValue, totalCaption));
        stats.add(statCard("Low stock", lowValue, UiKit.muted("At or below reorder level")));
        stats.add(statCard("Out of stock", outValue, UiKit.muted("Nothing left on hand")));
        stats.add(statCard("Expiring soon", expiringValue,
                UiKit.muted("Within " + ALERT_DAYS + " days, or expired")));

        JTable restockTable = UiKit.styledTable(restockModel, 1);
        UiKit.useStatusRenderer(restockTable, 2);
        UiKit.setColumnWidths(restockTable, 180, 100);
        JTable expiryTable = UiKit.styledTable(expiryModel, 1);
        UiKit.useBadgeRenderer(expiryTable, 3,
                row -> UiKit.expiryBadge(expiryDaysLeft.get(row), ALERT_DAYS));
        UiKit.setColumnWidths(expiryTable, 170, 90);
        UiKit.fixWidth(expiryTable, 2, 70);
        UiKit.fixWidth(expiryTable, 3, 90);

        JPanel lists = UiKit.clear(new GridLayout(1, 2, 16, 0));
        lists.add(tableCard("Restock first", restockCaption, restockTable));
        lists.add(tableCard("Expiring soon", UiKit.muted("Use these batches first, or record them as waste"),
                expiryTable));

        JPanel top = UiKit.clear(new BorderLayout(0, 16));
        top.add(welcome, BorderLayout.NORTH);
        top.add(stats, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(lists, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        InventoryManager manager = frame.getManager();
        welcome.setVisible(!manager.hasRecordedStock());

        int raw = manager.countItems("RAW");
        int production = manager.countItems("PRODUCTION");
        totalValue.setText(String.valueOf(raw + production));
        totalCaption.setText(raw + " raw | " + production + " production");

        List<InventoryItem> low = manager.getLowStockItems(); // most urgent first
        int outCount = 0;
        for (InventoryItem item : low) {
            if (item.getStatus() == StockStatus.OUT_OF_STOCK) {
                outCount++;
            }
        }
        lowValue.setText(String.valueOf(low.size() - outCount));
        outValue.setText(String.valueOf(outCount));

        restockModel.setRowCount(0);
        for (int i = 0; i < low.size() && i < RESTOCK_ROWS; i++) {
            InventoryItem item = low.get(i);
            restockModel.addRow(new Object[] {item.getName(),
                    UiKit.qty(item.getQuantity()) + " / " + UiKit.qty(item.getReorderLevel()) + " " + item.getUnit(),
                    item.getStatus().getLabel()});
        }
        restockCaption.setText(low.size() > RESTOCK_ROWS
                ? "On hand / reorder level. Most urgent " + RESTOCK_ROWS + " of " + low.size() + "; full list in Reports."
                : "On hand / reorder level. " + low.size() + " item(s), most urgent first.");

        List<ExpiringBatch> expiring = manager.getExpiringBatches(ALERT_DAYS);
        expiringValue.setText(String.valueOf(expiring.size()));
        expiryModel.setRowCount(0);
        expiryDaysLeft.clear();
        for (ExpiringBatch e : expiring) {
            StockBatch batch = e.getBatch();
            long left = batch.daysUntilExpiry(manager.getWorkingDate());
            expiryDaysLeft.add(left);
            expiryModel.addRow(new Object[] {e.getItem().getName(),
                    UiKit.qty(batch.getQuantity()) + " " + e.getItem().getUnit(),
                    UiKit.shortDate(batch.getExpiryDate()), UiKit.expiryText(left)});
        }
    }

    /** Shown only on a fresh install, to tell staff where to start. */
    private JPanel buildWelcome() {
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.setBackground(new Color(0xFBF3EA));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xEBD3BA)), new EmptyBorder(14, 18, 14, 18)));
        JPanel text = UiKit.clear(new GridLayout(2, 1, 0, 2));
        text.add(UiKit.label("Welcome! No stock has been recorded yet.", Font.BOLD, 14f, UiKit.TEXT));
        text.add(UiKit.muted("Start by counting what is on the shelves and entering it as your opening stock."));
        JButton start = UiKit.primaryButton("Enter opening stock");
        start.addActionListener(e -> frame.showPage("count"));
        JPanel buttonWrap = UiKit.clear(new java.awt.GridBagLayout()); // centers the button vertically
        buttonWrap.add(start);
        panel.add(text, BorderLayout.CENTER);
        panel.add(buttonWrap, BorderLayout.EAST);
        return panel;
    }

    private static JLabel statValue(Color color) {
        return UiKit.label("0", Font.BOLD, 30f, color);
    }

    private static JPanel statCard(String title, JLabel value, JLabel caption) {
        JPanel card = UiKit.card();
        card.setLayout(new GridLayout(3, 1, 0, 2));
        card.add(UiKit.label(title.toUpperCase(), Font.BOLD, 11.5f, UiKit.MUTED));
        card.add(value);
        card.add(caption);
        return card;
    }

    private static JPanel tableCard(String title, JLabel caption, JTable table) {
        JPanel card = UiKit.card();
        JPanel heading = UiKit.clear(new GridLayout(2, 1));
        heading.add(UiKit.sectionTitle(title));
        heading.add(caption);
        card.add(heading, BorderLayout.NORTH);
        card.add(UiKit.scroll(table), BorderLayout.CENTER);
        return card;
    }
}
