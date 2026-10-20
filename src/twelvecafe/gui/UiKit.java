package twelvecafe.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.function.IntFunction;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import twelvecafe.model.InventoryItem;
import twelvecafe.model.StockStatus;

/**
 * Shared colors, fonts and ready-made components, so every page of the
 * program looks the same.
 */
public final class UiKit {

    // ---- Colors (coffee-inspired palette) ----
    public static final Color BACKGROUND = new Color(0xF5F3F0);
    public static final Color CARD = Color.WHITE;
    public static final Color BORDER = new Color(0xE4DFD9);
    public static final Color TEXT = new Color(0x2B2420);
    public static final Color MUTED = new Color(0x7A6F67);
    public static final Color SIDEBAR = new Color(0x2D2420);
    public static final Color SIDEBAR_HOVER = new Color(0x3A2F2A);
    public static final Color SIDEBAR_ACTIVE = new Color(0x4A3B33);
    public static final Color SIDEBAR_TEXT = new Color(0xD9CFC7);
    public static final Color ACCENT = new Color(0xB86E2E);
    public static final Color ACCENT_DARK = new Color(0x9A5A22);
    public static final Color ZEBRA = new Color(0xFAF8F6);
    public static final Color SELECTION = new Color(0xF3E3D3);

    public static final Color OK = new Color(0x1E7B34);
    public static final Color OK_BG = new Color(0xE6F4EA);
    public static final Color LOW = new Color(0x9A5B00);
    public static final Color LOW_BG = new Color(0xFFF1D6);
    public static final Color OUT = new Color(0xB3261E);
    public static final Color OUT_BG = new Color(0xFCE8E6);

    private UiKit() {
    }

    // ---- Fonts ----

    public static Font font(int style, float size) {
        Font base = UIManager.getFont("Label.font");
        if (base == null) {
            base = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
        }
        return base.deriveFont(style, size);
    }

    // ---- Labels ----

    public static JLabel label(String text, int style, float size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(style, size));
        label.setForeground(color);
        return label;
    }

    public static JLabel sectionTitle(String text) {
        return label(text, Font.BOLD, 15f, TEXT);
    }

    public static JLabel muted(String text) {
        return label(text, Font.PLAIN, 12.5f, MUTED);
    }

    // ---- Buttons ----

    public static JButton primaryButton(String text) {
        return new FlatButton(text, ACCENT, ACCENT_DARK, Color.WHITE, null);
    }

    public static JButton secondaryButton(String text) {
        return new FlatButton(text, Color.WHITE, new Color(0xF2EEEA), TEXT, BORDER);
    }

    public static JButton dangerButton(String text) {
        return new FlatButton(text, OUT, new Color(0x8E1D17), Color.WHITE, null);
    }

    /**
     * A button that paints its own rounded background, so it looks the same
     * on Windows and macOS (the Mac look ignores button colors).
     */
    private static class FlatButton extends JButton {
        private static final Color DISABLED_FILL = new Color(0xEDE9E5);
        private static final Color DISABLED_TEXT = new Color(0xA79D95);

        private final Color normal;
        private final Color hover;
        private final Color text;
        private final Color outline;
        private boolean hovering;

        FlatButton(String label, Color normal, Color hover, Color text, Color outline) {
            super(label);
            this.normal = normal;
            this.hover = hover;
            this.text = text;
            this.outline = outline;
            setFont(font(Font.BOLD, 13f));
            setForeground(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorder(new EmptyBorder(8, 16, 8, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovering = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovering = false;
                    repaint();
                }
            });
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            setForeground(enabled ? text : DISABLED_TEXT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = !isEnabled() ? DISABLED_FILL
                    : (hovering || getModel().isPressed()) ? hover : normal;
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            if (outline != null && isEnabled()) {
                g2.setColor(outline);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            }
            g2.dispose();
            super.paintComponent(g); // draws the text
        }
    }

    // ---- Panels ----

    /** A white rounded-looking box with a thin border and padding. */
    public static JPanel card() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(CARD);
        panel.setBorder(cardBorder(16));
        return panel;
    }

    public static Border cardBorder(int padding) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(padding, padding, padding, padding));
    }

    /** A transparent panel, for grouping components inside a page. */
    public static JPanel clear(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    // ---- Tables ----

    /** A table model whose cells cannot be edited by the user. */
    public static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    /**
     * Applies the common table style. Automatic sorting is deliberately off:
     * every list is ordered by our own MergeSort before it is shown.
     */
    public static JTable styledTable(DefaultTableModel model, int... rightAlignedColumns) {
        return style(new JTable(model), rightAlignedColumns);
    }

    /** Applies the common style to a table that was already created. */
    public static JTable style(JTable table, int... rightAlignedColumns) {
        javax.swing.table.TableModel model = table.getModel();
        table.setRowHeight(30);
        table.setFont(font(Font.PLAIN, 13f));
        table.setForeground(TEXT);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(0xEFEBE7));
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(SELECTION);
        table.setSelectionForeground(TEXT);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(false);
        table.getTableHeader().setReorderingAllowed(false);

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, 32));
        header.setDefaultRenderer((t, value, sel, focus, row, col) -> {
            JLabel cell = new JLabel(value == null ? "" : value.toString().toUpperCase());
            cell.setFont(font(Font.BOLD, 11.5f));
            cell.setForeground(MUTED);
            cell.setOpaque(true);
            cell.setBackground(new Color(0xF7F4F1));
            cell.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(0, 10, 0, 10)));
            cell.setHorizontalAlignment(contains(rightAlignedColumns, col) ? SwingConstants.RIGHT : SwingConstants.LEFT);
            return cell;
        });

        for (int col = 0; col < model.getColumnCount(); col++) {
            int align = contains(rightAlignedColumns, col) ? SwingConstants.RIGHT : SwingConstants.LEFT;
            table.getColumnModel().getColumn(col).setCellRenderer(new ZebraRenderer(align));
        }
        return table;
    }

    public static JScrollPane scroll(JComponent component) {
        JScrollPane pane = new JScrollPane(component);
        pane.setBorder(BorderFactory.createLineBorder(BORDER));
        pane.getViewport().setBackground(CARD);
        pane.getVerticalScrollBar().setUnitIncrement(16);
        return pane;
    }

    /** Color of a badge: green, amber or red. */
    public enum Badge { GOOD, WARN, BAD }

    /** Shows OK / LOW / OUT as a colored badge. */
    public static void useStatusRenderer(JTable table, int column) {
        useBadgeRenderer(table, column, row -> {
            Object value = table.getModel().getValueAt(row, column);
            String text = value == null ? "" : value.toString();
            if (text.equals(StockStatus.OUT_OF_STOCK.getLabel())) {
                return Badge.BAD;
            }
            return text.equals(StockStatus.LOW.getLabel()) ? Badge.WARN : Badge.GOOD;
        });
        fixWidth(table, column, 80);
    }

    /** Shows a column as colored badges; the color for each row comes from badgeForRow. */
    public static void useBadgeRenderer(JTable table, int column, IntFunction<Badge> badgeForRow) {
        table.getColumnModel().getColumn(column).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focus, int row, int col) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(t, value, selected, false, row, col);
                cell.setHorizontalAlignment(SwingConstants.CENTER);
                cell.setFont(font(Font.BOLD, 11.5f));
                Badge badge = badgeForRow.apply(row);
                cell.setForeground(badge == Badge.BAD ? OUT : badge == Badge.WARN ? LOW : OK);
                cell.setBackground(badge == Badge.BAD ? OUT_BG : badge == Badge.WARN ? LOW_BG : OK_BG);
                cell.setBorder(BorderFactory.createMatteBorder(5, 8, 5, 8,
                        selected ? SELECTION : (row % 2 == 0 ? CARD : ZEBRA)));
                return cell;
            }
        });
    }

    /** Keeps a short column (status, dates) from being squeezed or stretched. */
    public static void fixWidth(JTable table, int column, int width) {
        table.getColumnModel().getColumn(column).setMinWidth(width);
        table.getColumnModel().getColumn(column).setMaxWidth(width + 30);
        table.getColumnModel().getColumn(column).setPreferredWidth(width);
    }

    private static class ZebraRenderer extends DefaultTableCellRenderer {
        ZebraRenderer(int alignment) {
            setHorizontalAlignment(alignment);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focus, int row, int col) {
            super.getTableCellRendererComponent(table, value, selected, false, row, col);
            if (!selected) {
                setBackground(row % 2 == 0 ? CARD : ZEBRA);
                setForeground(TEXT);
            }
            setBorder(new EmptyBorder(0, 10, 0, 10));
            return this;
        }
    }

    public static void setColumnWidths(JTable table, int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    // ---- Dates ----

    public static JSpinner dateSpinner(LocalDate initial) {
        JSpinner spinner = new JSpinner(new SpinnerDateModel());
        spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM-dd"));
        setDate(spinner, initial);
        spinner.setFont(font(Font.PLAIN, 13f));
        return spinner;
    }

    public static LocalDate getDate(JSpinner spinner) {
        Date date = (Date) spinner.getValue();
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static void setDate(JSpinner spinner, LocalDate date) {
        spinner.setValue(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("MMM d");
    private static final DateTimeFormatter MEDIUM_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy");

    /** "Oct 16" - for narrow table columns. */
    public static String shortDate(LocalDate date) {
        return date.format(SHORT_DATE);
    }

    /** "Oct 16, 2026" */
    public static String mediumDate(LocalDate date) {
        return date.format(MEDIUM_DATE);
    }

    /** "Expired", "Today", "Tomorrow" or "In 5 days". */
    public static String expiryText(long daysLeft) {
        if (daysLeft < 0) {
            return "Expired";
        }
        if (daysLeft == 0) {
            return "Today";
        }
        return daysLeft == 1 ? "Tomorrow" : "In " + daysLeft + " days";
    }

    public static Badge expiryBadge(long daysLeft, int alertDays) {
        if (daysLeft < 0) {
            return Badge.BAD;
        }
        return daysLeft <= alertDays ? Badge.WARN : Badge.GOOD;
    }

    // ---- Text fields and messages ----

    private static final Border FIELD_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xCFC6BE), 1, true), new EmptyBorder(6, 8, 6, 8));
    private static final Border FIELD_FOCUS_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT, 1, true), new EmptyBorder(6, 8, 6, 8));

    /** A text field with a visible box, padding, and an accent border while typing. */
    public static JTextField textField(int columns, String placeholder) {
        JTextField field = new JTextField(columns);
        field.setFont(font(Font.PLAIN, 13.5f));
        field.setForeground(TEXT);
        field.setBackground(Color.WHITE);
        field.setBorder(FIELD_BORDER);
        if (placeholder != null) {
            field.putClientProperty("JTextField.placeholderText", placeholder);
            field.setToolTipText(placeholder);
        }
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(FIELD_FOCUS_BORDER);
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(FIELD_BORDER);
            }
        });
        return field;
    }

    /** Runs the action every time the field's text changes. */
    public static void onTextChange(JTextField field, Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                action.run();
            }
        });
    }

    /** A colored message line shown right under a form's buttons. */
    public static JLabel feedbackLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(font(Font.BOLD, 12.5f));
        label.setOpaque(false);
        label.setBorder(new EmptyBorder(8, 12, 8, 12));
        return label;
    }

    public static void showFeedback(JLabel label, String message, boolean success) {
        label.setText("<html><div style='width:300px'>" + escape(message) + "</div></html>");
        label.setOpaque(true);
        label.setForeground(success ? OK : OUT);
        label.setBackground(success ? OK_BG : OUT_BG);
    }

    public static void clearFeedback(JLabel label) {
        label.setText(" ");
        label.setOpaque(false);
    }

    /** A muted label that wraps onto several lines in narrow windows. */
    public static JLabel wrapping(String text) {
        return muted("<html><div style='width:340px'>" + escape(text) + "</div></html>");
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ---- Formatting ----

    public static String qty(double value) {
        return InventoryItem.formatQty(value);
    }

    public static String signed(double value) {
        if (Math.abs(value) < InventoryItem.EPSILON) {
            return "0";
        }
        return (value > 0 ? "+" : "") + InventoryItem.formatQty(value);
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Cannot continue", JOptionPane.WARNING_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Please confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private static boolean contains(int[] values, int target) {
        for (int v : values) {
            if (v == target) {
                return true;
            }
        }
        return false;
    }
}
