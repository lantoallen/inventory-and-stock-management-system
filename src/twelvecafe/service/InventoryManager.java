package twelvecafe.service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import twelvecafe.algo.MergeSort;
import twelvecafe.algo.Search;
import twelvecafe.ds.LinkedStack;
import twelvecafe.model.InventoryItem;
import twelvecafe.model.ProductionItem;
import twelvecafe.model.RawIngredient;
import twelvecafe.model.StockBatch;
import twelvecafe.model.Transaction;
import twelvecafe.model.TransactionType;

/**
 * The core of the system: holds every item, records stock movements,
 * produces reports, and supports undo. The console menu (Main) only
 * collects input and calls these methods.
 */
public class InventoryManager {

    /** Sort orders offered to the user. All of them are run through MergeSort. */
    public static final Comparator<InventoryItem> BY_NAME =
            (a, b) -> a.getName().compareToIgnoreCase(b.getName());
    public static final Comparator<InventoryItem> BY_QUANTITY_LOW_FIRST =
            Comparator.comparingDouble(InventoryItem::getQuantity);
    public static final Comparator<InventoryItem> BY_QUANTITY_HIGH_FIRST =
            BY_QUANTITY_LOW_FIRST.reversed();
    public static final Comparator<InventoryItem> BY_STATUS =
            Comparator.comparing(InventoryItem::getStatus).thenComparing(BY_NAME);

    /** "Expiring soon" means within this many days, everywhere in the program. */
    public static final int EXPIRY_ALERT_DAYS = 3;

    private final FileStorage storage;
    private List<InventoryItem> items = new ArrayList<>(); // always kept sorted by name
    private List<Transaction> log = new ArrayList<>();
    private final LinkedStack<UndoRecord> undoStack = new LinkedStack<>();
    private LocalDate workingDate = LocalDate.now();

    public InventoryManager(FileStorage storage) {
        this.storage = storage;
    }

    // ================= Loading and saving =================

    /** Loads saved data, or the cafe's item list on the very first run. */
    public void load() throws IOException {
        if (storage.hasSavedData()) {
            items = storage.loadItems();
            MergeSort.sort(items, BY_NAME); // binary search needs a sorted list
            storage.loadBatches(items);
            log = storage.loadTransactions();
        } else {
            items = SeedData.createItems();
            MergeSort.sort(items, BY_NAME);
            save();
        }
    }

    public void save() throws IOException {
        storage.save(items, log);
    }

    // ================= Searching =================

    /** Exact name lookup using Binary Search. Returns null if not found. */
    public InventoryItem findItem(String name) {
        int index = Search.binarySearchByName(items, name);
        return index >= 0 ? items.get(index) : null;
    }

    /** Partial name lookup using Linear Search. */
    public List<InventoryItem> searchItems(String keyword) {
        return Search.linearSearchByKeyword(items, keyword);
    }

    // ================= Stock movements =================

    /** Delivery (raw) or production (prepared): adds a new batch at the rear of the queue. */
    public void stockIn(InventoryItem item, double quantity, LocalDate expiry,
                        String staff, String note) throws IOException {
        requirePositive(quantity);
        UndoRecord undo = beginUndo(item.getStockInLabel() + " of " + qty(item, quantity), item);
        item.addBatch(new StockBatch(quantity, workingDate, expiry));
        record(TransactionType.STOCK_IN, item, quantity, staff, note);
        commit(undo);
    }

    /** Used / served: removes stock from the oldest batch first (FIFO). */
    public void stockOut(InventoryItem item, double quantity, String staff, String note) throws IOException {
        requirePositive(quantity);
        UndoRecord undo = beginUndo(item.getStockOutLabel() + " of " + qty(item, quantity), item);
        item.consume(quantity); // throws if not enough stock, before anything is recorded
        record(TransactionType.STOCK_OUT, item, quantity, staff, note);
        commit(undo);
    }

    /** Spoiled, expired or damaged stock. */
    public void recordWaste(InventoryItem item, double quantity, String staff, String reason) throws IOException {
        requirePositive(quantity);
        UndoRecord undo = beginUndo("Waste of " + qty(item, quantity), item);
        item.consume(quantity);
        record(TransactionType.WASTE, item, quantity, staff, reason);
        commit(undo);
    }

    /**
     * The "ENDING" column of the paper form: staff count what is physically
     * on the shelf. Any difference from the system's quantity is recorded as
     * an adjustment so the books match reality.
     *
     * @return the variance (counted - expected); 0 means the count matched
     */
    public double recordPhysicalCount(InventoryItem item, double counted, String staff) throws IOException {
        if (counted < 0) {
            throw new IllegalArgumentException("Count cannot be negative.");
        }
        double expected = item.getQuantity();
        double variance = counted - expected;
        if (Math.abs(variance) < InventoryItem.EPSILON) {
            return 0;
        }
        UndoRecord undo = beginUndo("Count adjustment of " + item.getName(), item);
        if (variance < 0) {
            item.consume(-variance);
        } else {
            item.addBatch(new StockBatch(variance, workingDate, null));
        }
        record(TransactionType.ADJUSTMENT, item, variance, staff,
                "Counted " + InventoryItem.formatQty(counted) + ", system had "
                        + InventoryItem.formatQty(expected));
        commit(undo);
        return variance;
    }

    /** Removes every expired batch from every item and records it as waste. */
    public List<String> discardExpired(String staff) throws IOException {
        List<String> summary = new ArrayList<>();
        UndoRecord undo = new UndoRecord("Discard of expired stock", log.size());
        for (InventoryItem item : items) {
            if (item.hasExpiredBatch(workingDate)) {
                undo.remember(item);
                double removed = item.removeExpired(workingDate);
                record(TransactionType.WASTE, item, removed, staff, "Expired");
                summary.add(qty(item, removed));
            }
        }
        if (!summary.isEmpty()) {
            commit(undo);
        }
        return summary;
    }

    /**
     * Undoes the most recent stock movement (LIFO), restoring the items and
     * removing its transactions from the log.
     *
     * @return what was undone, or null if there is nothing to undo
     */
    public String undo() throws IOException {
        if (undoStack.isEmpty()) {
            return null;
        }
        UndoRecord last = undoStack.pop();
        last.restore();
        while (log.size() > last.getLogSizeBefore()) {
            log.remove(log.size() - 1);
        }
        save();
        return last.getDescription();
    }

    /** Description of what undo would reverse, or null if the stack is empty. */
    public String peekUndo() {
        return undoStack.isEmpty() ? null : undoStack.peek().getDescription();
    }

    // ================= Item management =================

    public InventoryItem addItem(boolean raw, String name, String unit, double reorderLevel) throws IOException {
        name = clean(name);
        unit = clean(unit);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Item name is required.");
        }
        if (findItem(name) != null) {
            throw new IllegalArgumentException(name.toUpperCase() + " already exists.");
        }
        InventoryItem item = raw
                ? new RawIngredient(name, unit, reorderLevel)
                : new ProductionItem(name, unit, reorderLevel);
        items.add(item);
        MergeSort.sort(items, BY_NAME); // keep the list sorted for binary search
        save();
        return item;
    }

    public void updateItemSettings(InventoryItem item, String unit, double reorderLevel) throws IOException {
        item.setUnit(clean(unit));
        item.setReorderLevel(reorderLevel);
        save();
    }

    // ================= Reports =================

    /** Items of one type (or all when type is null), sorted with the given order. */
    public List<InventoryItem> getItems(String type, Comparator<InventoryItem> order) {
        List<InventoryItem> result = new ArrayList<>();
        for (InventoryItem item : items) {
            if (type == null || item.getType().equals(type)) {
                result.add(item);
            }
        }
        MergeSort.sort(result, order);
        return result;
    }

    /**
     * Items at or below their reorder level, most urgent first: items the
     * cafe actually uses (has recorded movement for) come before items that
     * were never stocked; within each group, the lowest stock compared to the
     * reorder level comes first.
     */
    public List<InventoryItem> getLowStockItems() {
        Set<String> used = new HashSet<>();
        for (Transaction t : log) {
            used.add(t.getItemName());
        }
        List<InventoryItem> result = new ArrayList<>();
        for (InventoryItem item : items) {
            if (item.getQuantity() <= item.getReorderLevel() + InventoryItem.EPSILON) {
                result.add(item);
            }
        }
        Comparator<InventoryItem> urgency =
                Comparator.comparingInt((InventoryItem item) -> used.contains(item.getName()) ? 0 : 1)
                        .thenComparingDouble(InventoryItem::getStockRatio)
                        .thenComparing(BY_NAME);
        MergeSort.sort(result, urgency);
        return result;
    }

    /** False on a fresh install, before any stock has been recorded. */
    public boolean hasRecordedStock() {
        return !log.isEmpty();
    }

    /** Batches that expire within the given number of days (or already have), soonest first. */
    public List<ExpiringBatch> getExpiringBatches(int withinDays) {
        LocalDate limit = workingDate.plusDays(withinDays);
        List<ExpiringBatch> result = new ArrayList<>();
        for (InventoryItem item : items) {
            for (StockBatch batch : item.getBatches()) {
                if (batch.hasExpiry() && !batch.getExpiryDate().isAfter(limit)) {
                    result.add(new ExpiringBatch(item, batch));
                }
            }
        }
        MergeSort.sort(result, Comparator.comparing((ExpiringBatch e) -> e.getBatch().getExpiryDate())
                .thenComparing(e -> e.getItem().getName()));
        return result;
    }

    /**
     * Rebuilds the cafe's paper form for any date from the transaction log:
     * Beginning = all movement before the date; In/Out/Waste/Adjust = that day.
     */
    public List<ReportRow> getDailyReport(LocalDate date, String type) {
        // itemName -> {beginning, in, out, waste, adjust}
        Map<String, double[]> totals = new HashMap<>();
        for (Transaction t : log) {
            if (t.getDate().isAfter(date)) {
                continue;
            }
            double[] row = totals.computeIfAbsent(t.getItemName(), k -> new double[5]);
            if (t.getDate().isBefore(date)) {
                row[0] += t.getSignedQuantity();
            } else {
                switch (t.getType()) {
                    case STOCK_IN:
                        row[1] += t.getQuantity();
                        break;
                    case STOCK_OUT:
                        row[2] += t.getQuantity();
                        break;
                    case WASTE:
                        row[3] += t.getQuantity();
                        break;
                    case ADJUSTMENT:
                        row[4] += t.getQuantity();
                        break;
                }
            }
        }
        List<ReportRow> report = new ArrayList<>();
        for (InventoryItem item : getItems(type, BY_NAME)) {
            double[] r = totals.getOrDefault(item.getName(), new double[5]);
            report.add(new ReportRow(item, r[0], r[1], r[2], r[3], r[4]));
        }
        return report;
    }

    /** Most recent transactions first, optionally for one item only. */
    public List<Transaction> getHistory(InventoryItem item, int limit) {
        List<Transaction> result = new ArrayList<>();
        for (int i = log.size() - 1; i >= 0 && result.size() < limit; i--) {
            Transaction t = log.get(i);
            if (item == null || t.getItemName().equals(item.getName())) {
                result.add(t);
            }
        }
        return result;
    }

    public java.nio.file.Path exportReport(String fileName, List<String> lines) throws IOException {
        return storage.saveReport(fileName, lines);
    }

    // ================= Working date =================

    public LocalDate getWorkingDate() {
        return workingDate;
    }

    /** Lets staff record a past day's sheet, or demo the system across several days. */
    public void setWorkingDate(LocalDate workingDate) {
        this.workingDate = workingDate;
    }

    public int countItems(String type) {
        int count = 0;
        for (InventoryItem item : items) {
            if (item.getType().equals(type)) {
                count++;
            }
        }
        return count;
    }

    // ================= Helpers =================

    private UndoRecord beginUndo(String description, InventoryItem item) {
        UndoRecord undo = new UndoRecord(description, log.size());
        undo.remember(item);
        return undo;
    }

    private void commit(UndoRecord undo) throws IOException {
        undoStack.push(undo);
        save();
    }

    private void record(TransactionType type, InventoryItem item, double quantity, String staff, String note) {
        log.add(new Transaction(workingDate, type, item.getName(), quantity, clean(staff), clean(note)));
    }

    private static void requirePositive(double quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0.");
        }
    }

    /** e.g. "2 kg CHICKEN BREAST", used in undo descriptions. */
    private static String qty(InventoryItem item, double quantity) {
        return InventoryItem.formatQty(quantity) + " " + item.getUnit() + " " + item.getName();
    }

    /** "|" separates fields in the save files, so it cannot appear in text. */
    private static String clean(String text) {
        return text == null ? "" : text.replace("|", "/").trim();
    }
}
