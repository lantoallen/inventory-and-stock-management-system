package twelvecafe.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import twelvecafe.model.InventoryItem;
import twelvecafe.service.InventoryManager;

/**
 * One search box with a live list of matching items under it.
 *
 * Typing filters the list with Linear Search (partial match). When the text
 * is an exact item name, Binary Search finds it and selects it directly.
 * Nothing is selected until the user picks an item, so stock is never
 * recorded against the wrong item by accident.
 */
public class ItemPicker extends JPanel {

    private final InventoryManager manager;
    private final boolean inStockOnly;
    private final JTextField searchField =
            UiKit.textField(20, "Type an item name, e.g. chicken");
    private final DefaultListModel<InventoryItem> listModel = new DefaultListModel<>();
    private final JList<InventoryItem> list = new JList<>(listModel);
    private final JLabel emptyInfo = UiKit.muted(" ");
    private final List<Runnable> listeners = new ArrayList<>();
    private InventoryItem selected;
    private boolean updating;

    /** @param inStockOnly true lists only items with stock (for Stock Out and Waste) */
    public ItemPicker(InventoryManager manager, boolean inStockOnly) {
        super(new BorderLayout(0, 6));
        this.manager = manager;
        this.inStockOnly = inStockOnly;
        setOpaque(false);
        searchField.setToolTipText("Exact names are found with Binary Search; partial names with Linear Search");

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(6);
        list.setFixedCellHeight(28);
        list.setCellRenderer(new ItemRenderer());
        JScrollPane scroll = UiKit.scroll(list);
        // Keep the list visible even when empty or in a small window.
        scroll.setPreferredSize(new Dimension(0, 5 * 28 + 4));
        scroll.setMinimumSize(new Dimension(0, 3 * 28 + 4));

        add(searchField, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(emptyInfo, BorderLayout.SOUTH);

        UiKit.onTextChange(searchField, this::applyFilter);
        list.addListSelectionListener(e -> {
            if (!updating && !e.getValueIsAdjusting()) {
                setSelected(list.getSelectedValue());
            }
        });
        // Enter picks the first match; Down arrow moves into the list.
        searchField.addActionListener(e -> {
            if (!listModel.isEmpty()) {
                list.setSelectedIndex(list.getSelectedIndex() >= 0 ? list.getSelectedIndex() : 0);
            }
        });
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN && !listModel.isEmpty()) {
                    list.requestFocusInWindow();
                    list.setSelectedIndex(Math.max(0, list.getSelectedIndex()));
                }
            }
        });
        applyFilter();
    }

    public InventoryItem getSelectedItem() {
        return selected;
    }

    /** Runs the given action whenever the chosen item changes. */
    public void onChange(Runnable action) {
        listeners.add(action);
    }

    public void clear() {
        searchField.setText("");
        setSelected(null);
    }

    public void focusSearch() {
        searchField.requestFocusInWindow();
    }

    /** Reloads the list (quantities may have changed) and keeps the selection. */
    public void refresh() {
        applyFilter();
        list.repaint();
        fireChange();
    }

    private void applyFilter() {
        String text = searchField.getText().trim();
        List<InventoryItem> matches = text.isEmpty()
                ? manager.getItems(null, InventoryManager.BY_NAME)
                : manager.searchItems(text);                      // Linear Search
        InventoryItem exact = text.isEmpty() ? null : manager.findItem(text); // Binary Search

        updating = true;
        listModel.clear();
        for (InventoryItem item : matches) {
            if (!inStockOnly || item.getQuantity() > InventoryItem.EPSILON) {
                listModel.addElement(item);
            }
        }
        if (exact != null && listModel.contains(exact)) {
            selected = exact;
        } else if (selected != null && !listModel.contains(selected)) {
            selected = null;
        }
        if (selected != null) {
            list.setSelectedValue(selected, true);
        } else {
            list.clearSelection();
        }
        updating = false;

        if (!listModel.isEmpty()) {
            emptyInfo.setText(" ");
        } else if (!text.isEmpty()) {
            emptyInfo.setText("No " + (inStockOnly ? "in-stock " : "") + "item matches \"" + text + "\".");
        } else {
            emptyInfo.setText("<html>No items have stock yet.<br>Record a delivery in Stock In first.</html>");
        }
        fireChange();
    }

    private void setSelected(InventoryItem item) {
        if (item != selected) {
            selected = item;
            if (item == null) {
                list.clearSelection();
            }
            fireChange();
        }
    }

    private void fireChange() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    /** Item name on the left, stock on hand on the right. */
    private static class ItemRenderer extends JPanel implements ListCellRenderer<InventoryItem> {
        private final JLabel name = new JLabel();
        private final JLabel stock = new JLabel();

        ItemRenderer() {
            super(new BorderLayout());
            setBorder(new EmptyBorder(0, 10, 0, 10));
            name.setFont(UiKit.font(Font.PLAIN, 13f));
            stock.setFont(UiKit.font(Font.PLAIN, 12.5f));
            add(name, BorderLayout.CENTER);
            add(stock, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends InventoryItem> list, InventoryItem item,
                                                      int index, boolean selected, boolean focus) {
            name.setText(item.getName());
            stock.setText(UiKit.qty(item.getQuantity()) + " " + item.getUnit());
            setBackground(selected ? UiKit.SELECTION : (index % 2 == 0 ? Color.WHITE : UiKit.ZEBRA));
            name.setForeground(UiKit.TEXT);
            name.setFont(UiKit.font(selected ? Font.BOLD : Font.PLAIN, 13f));
            stock.setForeground(item.getQuantity() > InventoryItem.EPSILON ? UiKit.MUTED : UiKit.OUT);
            setBorder(selected
                    ? BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 3, 0, 0, UiKit.ACCENT),
                            new EmptyBorder(0, 7, 0, 10))
                    : new EmptyBorder(0, 10, 0, 10));
            return this;
        }
    }
}
