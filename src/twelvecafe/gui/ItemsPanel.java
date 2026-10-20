package twelvecafe.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import twelvecafe.model.InventoryItem;
import twelvecafe.service.InventoryManager;

/** Add new items, or change the unit and reorder level of existing ones. */
public class ItemsPanel extends JPanel implements Refreshable {

    private final MainFrame frame;
    private final DefaultTableModel model = UiKit.readOnlyModel("Item", "Type", "Unit", "Reorder at");
    private final JTable table = UiKit.styledTable(model, 3);
    private final List<InventoryItem> rows = new ArrayList<>();

    private final JComboBox<String> newType = new JComboBox<>(new String[] {"Raw ingredient", "Production item"});
    private final JTextField newName = UiKit.textField(18, "e.g. Garlic Rice");
    private final JTextField newUnit = UiKit.textField(10, "kg, pack, bottle, portion...");
    private final JTextField newReorder = UiKit.textField(6, "e.g. 5");

    private final JLabel editTitle = UiKit.sectionTitle("Edit selected item");
    private final JTextField editUnit = UiKit.textField(10, null);
    private final JTextField editReorder = UiKit.textField(6, null);
    private final JButton saveEdit = UiKit.primaryButton("Save Changes");
    private final CardLayout editCards = new CardLayout();
    private final JPanel editArea = UiKit.clear(editCards);

    public ItemsPanel(MainFrame frame) {
        super(new GridLayout(1, 2, 16, 0));
        this.frame = frame;
        setOpaque(false);
        setBorder(new EmptyBorder(0, 0, 20, 0));

        UiKit.setColumnWidths(table, 190, 90, 70, 100);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelected();
            }
        });
        JPanel listCard = UiKit.card();
        listCard.add(UiKit.sectionTitle("All items (A-Z)"), BorderLayout.NORTH);
        listCard.add(UiKit.scroll(table), BorderLayout.CENTER);

        JPanel right = UiKit.clear(new GridLayout(2, 1, 0, 16));
        right.add(buildEditCard());
        right.add(buildAddCard());

        add(listCard);
        add(right);
    }

    private JPanel buildEditCard() {
        JPanel card = UiKit.card();
        card.add(editTitle, BorderLayout.NORTH);
        JPanel form = UiKit.clear(new GridBagLayout());
        addRow(form, 0, "Unit", editUnit);
        addRow(form, 1, "Reorder level", editReorder);
        addRow(form, 2, "", saveEdit);
        filler(form, 3);
        saveEdit.addActionListener(e -> saveEdit());
        editReorder.addActionListener(e -> saveEdit());

        // Until an item is chosen, show a hint instead of empty, disabled fields.
        JPanel hint = UiKit.clear(new GridBagLayout());
        hint.add(UiKit.muted("Select an item in the list on the left to edit its unit and reorder level."));
        editArea.add(hint, "hint");
        editArea.add(form, "form");
        card.add(editArea, BorderLayout.CENTER);
        card.add(UiKit.muted("An item is LOW when its stock reaches the reorder level."), BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildAddCard() {
        JPanel card = UiKit.card();
        card.add(UiKit.sectionTitle("Add new item"), BorderLayout.NORTH);
        JPanel form = UiKit.clear(new GridBagLayout());
        addRow(form, 0, "Type", newType);
        addRow(form, 1, "Item name", newName);
        addRow(form, 2, "Unit", newUnit);
        addRow(form, 3, "Reorder level", newReorder);
        JButton add = UiKit.primaryButton("Add Item");
        add.addActionListener(e -> addItem());
        newReorder.addActionListener(e -> addItem());
        addRow(form, 4, "", add);
        filler(form, 5);
        card.add(form, BorderLayout.CENTER);
        return card;
    }

    @Override
    public void refresh() {
        InventoryItem selected = selected();
        rows.clear();
        rows.addAll(frame.getManager().getItems(null, InventoryManager.BY_NAME));
        model.setRowCount(0);
        for (InventoryItem item : rows) {
            model.addRow(new Object[] {item.getName(), item.getType().equals("RAW") ? "Raw" : "Prod.",
                    item.getUnit(), UiKit.qty(item.getReorderLevel())});
        }
        int index = selected == null ? -1 : rows.indexOf(selected);
        if (index >= 0) {
            table.setRowSelectionInterval(index, index);
        }
        loadSelected();
    }

    private InventoryItem selected() {
        int row = table.getSelectedRow();
        return row >= 0 && row < rows.size() ? rows.get(row) : null;
    }

    private void loadSelected() {
        InventoryItem item = selected();
        editCards.show(editArea, item == null ? "hint" : "form");
        if (item == null) {
            editTitle.setText("Edit selected item");
            return;
        }
        editTitle.setText("Edit " + item.getName());
        editUnit.setText(item.getUnit());
        editReorder.setText(UiKit.qty(item.getReorderLevel()));
    }

    private void saveEdit() {
        InventoryItem item = selected();
        if (item == null) {
            return;
        }
        String unit = editUnit.getText().trim();
        Double reorder = parseNonNegative(editReorder.getText());
        if (unit.isEmpty() || reorder == null) {
            UiKit.showError(this, "Enter a unit and a reorder level of 0 or more.");
            return;
        }
        try {
            frame.getManager().updateItemSettings(item, unit, reorder);
            frame.dataChanged("Updated " + item.getName() + ": unit " + unit + ", reorder at "
                    + UiKit.qty(reorder) + ".");
        } catch (IOException e) {
            frame.showSaveError(e);
        }
    }

    private void addItem() {
        String name = newName.getText().trim();
        String unit = newUnit.getText().trim();
        Double reorder = parseNonNegative(newReorder.getText());
        if (name.isEmpty() || unit.isEmpty() || reorder == null) {
            UiKit.showError(this, "Enter the item name, unit, and a reorder level of 0 or more.");
            return;
        }
        try {
            InventoryItem item = frame.getManager().addItem(newType.getSelectedIndex() == 0, name, unit, reorder);
            newName.setText("");
            newUnit.setText("");
            newReorder.setText("");
            frame.dataChanged("Added " + item.getName() + ".");
            int index = rows.indexOf(item);
            if (index >= 0) {
                table.setRowSelectionInterval(index, index);
                table.scrollRectToVisible(table.getCellRect(index, 0, true));
            }
        } catch (IllegalArgumentException e) {
            UiKit.showError(this, e.getMessage());
        } catch (IOException e) {
            frame.showSaveError(e);
        }
    }

    private static Double parseNonNegative(String text) {
        try {
            double value = Double.parseDouble(text.trim());
            return value >= 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void addRow(JPanel form, int row, String label, JComponent field) {
        JLabel l = UiKit.label(label, Font.BOLD, 12.5f, UiKit.MUTED);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(10, 0, 0, 16);
        form.add(l, c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = row;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = field instanceof JButton ? GridBagConstraints.NONE : GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(10, 0, 0, 0);
        form.add(field, c);
    }

    private static void filler(JPanel form, int row) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.weighty = 1;
        form.add(new JLabel(), c);
    }
}
