package twelvecafe.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import twelvecafe.service.InventoryManager;

/**
 * The main window: a sidebar for navigation on the left, the current page
 * on the right (CardLayout swaps pages), a header with the working date and
 * Undo, and a status bar at the bottom.
 */
public class MainFrame extends JFrame {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy");

    private final InventoryManager manager;
    private final CardLayout cards = new CardLayout();
    private final JPanel pageArea = new JPanel(cards);
    private final Map<String, PageInfo> pages = new LinkedHashMap<>();
    private final Map<String, NavItem> navItems = new LinkedHashMap<>();

    private final JLabel pageTitle = UiKit.label("", Font.BOLD, 22f, UiKit.TEXT);
    private final JLabel pageSubtitle = UiKit.muted("");
    private final JLabel dateLabel = UiKit.label("", Font.BOLD, 13f, UiKit.TEXT);
    private final JLabel statusBar = UiKit.muted("Ready.");
    private final JButton undoButton = UiKit.secondaryButton("Undo");
    private final JPanel dateWarning = new JPanel(new BorderLayout());
    private final JLabel dateWarningText = UiKit.label("", Font.BOLD, 13f, UiKit.LOW);
    private String currentPage;
    private String lastStaff = "";

    /** Title, description and content of one page. */
    private static class PageInfo {
        final String title;
        final String subtitle;
        final Refreshable panel;

        PageInfo(String title, String subtitle, Refreshable panel) {
            this.title = title;
            this.subtitle = subtitle;
            this.panel = panel;
        }
    }

    public MainFrame(InventoryManager manager) {
        super("The Twelve Cafe - Inventory & Stock Management");
        this.manager = manager;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setSize(1280, 800);
        setLocationRelativeTo(null);

        JPanel sidebar = buildSidebar();
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(UiKit.BACKGROUND);
        JPanel top = UiKit.clear(new BorderLayout());
        top.add(buildDateWarning(), BorderLayout.NORTH);
        top.add(buildHeader(), BorderLayout.CENTER);
        main.add(top, BorderLayout.NORTH);
        pageArea.setOpaque(false);
        pageArea.setBorder(new EmptyBorder(0, 24, 0, 24));
        main.add(pageArea, BorderLayout.CENTER);
        main.add(buildStatusBar(), BorderLayout.SOUTH);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(sidebar, BorderLayout.WEST);
        getContentPane().add(main, BorderLayout.CENTER);

        showPage("dashboard");
    }

    // ============================ Pages ============================

    private void registerPages(JPanel nav) {
        addSection(nav, "OVERVIEW");
        addPage(nav, "dashboard", "Dashboard", "Dashboard",
                "Today's stock at a glance.",
                new DashboardPanel(this));
        addPage(nav, "inventory", "Inventory", "Inventory",
                "All items and stock levels. Click one for details.",
                new InventoryPanel(this));

        addSection(nav, "RECORD");
        addPage(nav, "in", "Stock In", "Stock In",
                "Record supplier deliveries and kitchen production.",
                new StockEntryPanel(this, StockEntryPanel.Mode.STOCK_IN));
        addPage(nav, "out", "Stock Out", "Stock Out",
                "Record stock used or served. The oldest batch goes first.",
                new StockEntryPanel(this, StockEntryPanel.Mode.STOCK_OUT));
        addPage(nav, "waste", "Waste", "Waste / Spoilage",
                "Record spoiled, dropped or expired stock.",
                new StockEntryPanel(this, StockEntryPanel.Mode.WASTE));
        addPage(nav, "count", "Ending Count", "Ending Count",
                "Count what is on the shelf at closing.",
                new EndingCountPanel(this));

        addSection(nav, "REPORTS");
        addPage(nav, "reports", "Reports", "Reports",
                "Restock list, expiring stock and the daily sheet.",
                new ReportsPanel(this));
        addPage(nav, "history", "History", "Transaction History",
                "Every stock movement, newest first.",
                new HistoryPanel(this));

        addSection(nav, "SETTINGS");
        addPage(nav, "items", "Manage Items", "Manage Items",
                "Add items or change units and reorder levels.",
                new ItemsPanel(this));
    }

    private void addPage(JPanel nav, String key, String navLabel, String title, String subtitle,
                         JComponent panel) {
        pages.put(key, new PageInfo(title, subtitle, (Refreshable) panel));
        pageArea.add(panel, key);
        NavItem item = new NavItem(navLabel, key);
        navItems.put(key, item);
        nav.add(item);
    }

    /** Switches to a page and reloads its data. */
    public void showPage(String key) {
        PageInfo page = pages.get(key);
        if (page == null) {
            return;
        }
        currentPage = key;
        pageTitle.setText(page.title);
        pageSubtitle.setText(page.subtitle);
        for (NavItem item : navItems.values()) {
            item.setActive(item.key.equals(key));
        }
        page.panel.refresh();
        cards.show(pageArea, key);
        updateHeader();
    }

    /**
     * Called by every page after it changes the inventory: shows the message
     * in the status bar and reloads the visible page.
     */
    public void dataChanged(String message) {
        statusBar.setText(message);
        statusBar.setForeground(UiKit.OK);
        pages.get(currentPage).panel.refresh();
        updateHeader();
    }

    public void showSaveError(IOException e) {
        UiKit.showError(this, "The change was made but could not be saved to the data folder:\n"
                + e.getMessage());
    }

    public InventoryManager getManager() {
        return manager;
    }

    /** The last staff name typed on any form, so staff do not retype it on every page. */
    public String getLastStaff() {
        return lastStaff;
    }

    public void setLastStaff(String staff) {
        this.lastStaff = staff;
    }

    // ============================ Sidebar ============================

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(UiKit.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(220, 0));

        JPanel brand = new JPanel(new GridLayout(2, 1));
        brand.setOpaque(false);
        brand.setBorder(new EmptyBorder(24, 22, 20, 16));
        brand.add(UiKit.label("THE TWELVE CAFE", Font.BOLD, 16f, Color.WHITE));
        brand.add(UiKit.label("Inventory & Stock", Font.PLAIN, 12.5f, UiKit.SIDEBAR_TEXT));

        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        registerPages(nav);

        sidebar.add(brand, BorderLayout.NORTH);
        sidebar.add(nav, BorderLayout.CENTER);
        return sidebar;
    }

    private void addSection(JPanel nav, String text) {
        JLabel label = UiKit.label(text, Font.BOLD, 10.5f, new Color(0x9C8F86));
        label.setBorder(new EmptyBorder(14, 22, 6, 0));
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        nav.add(label);
    }

    /** One clickable sidebar entry. */
    private class NavItem extends JLabel {
        final String key;
        boolean active;

        NavItem(String text, String key) {
            super(text);
            this.key = key;
            setFont(UiKit.font(Font.PLAIN, 13.5f));
            setForeground(UiKit.SIDEBAR_TEXT);
            setOpaque(true);
            setBackground(UiKit.SIDEBAR);
            setBorder(new EmptyBorder(9, 22, 9, 10));
            setAlignmentX(LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showPage(key);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!active) {
                        setBackground(UiKit.SIDEBAR_HOVER);
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!active) {
                        setBackground(UiKit.SIDEBAR);
                    }
                }
            });
        }

        void setActive(boolean active) {
            this.active = active;
            setBackground(active ? UiKit.SIDEBAR_ACTIVE : UiKit.SIDEBAR);
            setForeground(active ? Color.WHITE : UiKit.SIDEBAR_TEXT);
            setFont(UiKit.font(active ? Font.BOLD : Font.PLAIN, 13.5f));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 4, 0, 0, active ? UiKit.ACCENT : UiKit.SIDEBAR),
                    new EmptyBorder(9, 18, 9, 10)));
        }
    }

    // ============================ Header and status bar ============================

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(22, 24, 16, 24));

        JPanel titles = UiKit.clear(new GridLayout(2, 1, 0, 2));
        titles.add(pageTitle);
        titles.add(pageSubtitle);

        JPanel right = UiKit.clear(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JPanel dateBox = UiKit.clear(new GridLayout(2, 1));
        JLabel caption = UiKit.muted("Working date");
        caption.setHorizontalAlignment(SwingConstants.RIGHT);
        dateLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        dateBox.add(caption);
        dateBox.add(dateLabel);

        JButton changeDate = UiKit.secondaryButton("Change");
        changeDate.setToolTipText("Record entries for another day (e.g. a missed paper sheet)");
        changeDate.addActionListener(e -> changeWorkingDate());
        undoButton.addActionListener(e -> undo());

        right.add(dateBox);
        right.add(Box.createHorizontalStrut(4));
        right.add(changeDate);
        right.add(undoButton);

        header.add(titles, BorderLayout.CENTER);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiKit.BORDER), new EmptyBorder(7, 24, 7, 24)));
        bar.add(statusBar, BorderLayout.WEST);
        bar.add(UiKit.muted("All changes are saved automatically."), BorderLayout.EAST);
        return bar;
    }

    /**
     * A yellow bar shown only when entries are being recorded on a date
     * other than today, so nobody records on the wrong day by accident.
     */
    private JPanel buildDateWarning() {
        dateWarning.setBackground(UiKit.LOW_BG);
        dateWarning.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xF0D9A8)), new EmptyBorder(8, 24, 8, 24)));
        JButton backToToday = UiKit.secondaryButton("Switch to today");
        backToToday.addActionListener(e -> {
            manager.setWorkingDate(LocalDate.now());
            dataChanged("Working date set back to today.");
        });
        dateWarning.add(dateWarningText, BorderLayout.CENTER);
        dateWarning.add(backToToday, BorderLayout.EAST);
        dateWarning.setVisible(false);
        return dateWarning;
    }

    private void updateHeader() {
        LocalDate date = manager.getWorkingDate();
        dateLabel.setText(date.format(DATE_FORMAT));
        boolean notToday = !date.equals(LocalDate.now());
        dateWarningText.setText("New entries are being recorded for " + date.format(DATE_FORMAT)
                + ", not today.");
        dateWarning.setVisible(notToday);

        String next = manager.peekUndo();
        undoButton.setEnabled(next != null);
        undoButton.setText(next == null ? "Undo" : "Undo: " + shorten(next, 34));
        undoButton.setToolTipText(next == null ? "Nothing to undo yet" : "Undo: " + next);
    }

    private static String shorten(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }

    private void changeWorkingDate() {
        JSpinner spinner = UiKit.dateSpinner(manager.getWorkingDate());
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("Entries will be recorded on this date:"), BorderLayout.NORTH);
        panel.add(spinner, BorderLayout.CENTER);
        int answer = JOptionPane.showOptionDialog(this, panel, "Change working date",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new Object[] {"Set date", "Today", "Cancel"}, "Set date");
        if (answer == 0 || answer == 1) {
            LocalDate date = answer == 0 ? UiKit.getDate(spinner) : LocalDate.now();
            manager.setWorkingDate(date);
            dataChanged("Working date set to " + date.format(DATE_FORMAT) + ".");
        }
    }

    private void undo() {
        String last = manager.peekUndo();
        if (last == null || !UiKit.confirm(this, "Undo the last transaction?\n\n" + last)) {
            return;
        }
        try {
            manager.undo();
            dataChanged("Undone: " + last + ".");
        } catch (IOException e) {
            showSaveError(e);
        }
    }
}
