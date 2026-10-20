package twelvecafe;

import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import twelvecafe.gui.MainFrame;
import twelvecafe.service.FileStorage;
import twelvecafe.service.InventoryManager;

/**
 * The Twelve Cafe - Inventory & Stock Management System.
 *
 * Entry point: loads the saved inventory (or the cafe's item list on the
 * first run) and opens the main window.
 */
public class Main {

    public static void main(String[] args) {
        try {
            // Use the operating system's look for buttons, fields and dialogs.
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // the default Java look is fine too
        }

        InventoryManager manager = new InventoryManager(new FileStorage("data", "reports"));
        try {
            manager.load();
        } catch (IOException | RuntimeException e) {
            JOptionPane.showMessageDialog(null, "Could not load the saved inventory:\n" + e.getMessage(),
                    "The Twelve Cafe", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Swing components must be created on the Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> new MainFrame(manager).setVisible(true));
    }
}
