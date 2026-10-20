package twelvecafe.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import twelvecafe.algo.Search;
import twelvecafe.model.InventoryItem;
import twelvecafe.model.ProductionItem;
import twelvecafe.model.RawIngredient;
import twelvecafe.model.StockBatch;
import twelvecafe.model.Transaction;
import twelvecafe.model.TransactionType;

/**
 * Saves and loads the inventory as plain text files so stock is kept
 * between runs. Fields are separated by "|".
 *
 *   data/items.txt         TYPE|NAME|UNIT|REORDER_LEVEL
 *   data/batches.txt       NAME|QUANTITY|DATE_RECEIVED|EXPIRY (or -)
 *   data/transactions.txt  DATE|TYPE|NAME|QUANTITY|STAFF|NOTE
 */
public class FileStorage {

    private static final String SEPARATOR = "|";
    private static final String SPLIT_REGEX = "\\|";
    private static final String NO_DATE = "-";

    private final Path dataDir;
    private final Path reportsDir;

    public FileStorage(String dataDir, String reportsDir) {
        this.dataDir = Paths.get(dataDir);
        this.reportsDir = Paths.get(reportsDir);
    }

    public boolean hasSavedData() {
        return Files.exists(dataDir.resolve("items.txt"));
    }

    // ---------------- Save ----------------

    public void save(List<InventoryItem> items, List<Transaction> log) throws IOException {
        Files.createDirectories(dataDir);

        try (BufferedWriter out = writer("items.txt")) {
            for (InventoryItem item : items) {
                out.write(join(item.getType(), item.getName(), item.getUnit(),
                        String.valueOf(item.getReorderLevel())));
                out.newLine();
            }
        }

        try (BufferedWriter out = writer("batches.txt")) {
            for (InventoryItem item : items) {
                for (StockBatch batch : item.getBatches()) { // front to rear keeps FIFO order
                    out.write(join(item.getName(), String.valueOf(batch.getQuantity()),
                            batch.getDateReceived().toString(),
                            batch.hasExpiry() ? batch.getExpiryDate().toString() : NO_DATE));
                    out.newLine();
                }
            }
        }

        try (BufferedWriter out = writer("transactions.txt")) {
            for (Transaction t : log) {
                out.write(join(t.getDate().toString(), t.getType().name(), t.getItemName(),
                        String.valueOf(t.getQuantity()), t.getStaff(), t.getNote()));
                out.newLine();
            }
        }
    }

    // ---------------- Load ----------------

    public List<InventoryItem> loadItems() throws IOException {
        List<InventoryItem> items = new ArrayList<>();
        for (String[] f : readRows("items.txt", 4)) {
            String type = f[0];
            String name = f[1];
            String unit = f[2];
            double reorder = Double.parseDouble(f[3]);
            if (type.equals("PRODUCTION")) {
                items.add(new ProductionItem(name, unit, reorder));
            } else {
                items.add(new RawIngredient(name, unit, reorder));
            }
        }
        return items;
    }

    /** Puts each saved batch back into its item's queue. Items must be sorted by name. */
    public void loadBatches(List<InventoryItem> sortedItems) throws IOException {
        for (String[] f : readRows("batches.txt", 4)) {
            int index = Search.binarySearchByName(sortedItems, f[0]);
            if (index < 0) {
                continue; // item was removed from items.txt
            }
            LocalDate expiry = f[3].equals(NO_DATE) ? null : LocalDate.parse(f[3]);
            sortedItems.get(index).addBatch(
                    new StockBatch(Double.parseDouble(f[1]), LocalDate.parse(f[2]), expiry));
        }
    }

    public List<Transaction> loadTransactions() throws IOException {
        List<Transaction> log = new ArrayList<>();
        for (String[] f : readRows("transactions.txt", 6)) {
            log.add(new Transaction(LocalDate.parse(f[0]), TransactionType.valueOf(f[1]), f[2],
                    Double.parseDouble(f[3]), f[4], f[5]));
        }
        return log;
    }

    /** Writes a printable report to the reports folder and returns its path. */
    public Path saveReport(String fileName, List<String> lines) throws IOException {
        Files.createDirectories(reportsDir);
        Path file = reportsDir.resolve(fileName);
        Files.write(file, lines, StandardCharsets.UTF_8);
        return file;
    }

    // ---------------- Helpers ----------------

    private BufferedWriter writer(String fileName) throws IOException {
        return Files.newBufferedWriter(dataDir.resolve(fileName), StandardCharsets.UTF_8);
    }

    private List<String[]> readRows(String fileName, int fieldCount) throws IOException {
        List<String[]> rows = new ArrayList<>();
        Path file = dataDir.resolve(fileName);
        if (!Files.exists(file)) {
            return rows;
        }
        try (BufferedReader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] fields = line.split(SPLIT_REGEX, -1);
                if (fields.length == fieldCount) {
                    rows.add(fields);
                }
            }
        }
        return rows;
    }

    private static String join(String... fields) {
        return String.join(SEPARATOR, fields);
    }
}
