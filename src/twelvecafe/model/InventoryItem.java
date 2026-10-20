package twelvecafe.model;

import java.time.LocalDate;

import twelvecafe.ds.LinkedQueue;

/**
 * Base class for everything the cafe keeps in stock.
 *
 * Abstract: the cafe never has a "plain" item, only a RawIngredient
 * (delivered by suppliers) or a ProductionItem (prepared in the kitchen).
 * The stock itself is a FIFO queue of batches, so the oldest stock is
 * always used first.
 */
public abstract class InventoryItem {

    /** Tolerance for comparing decimal quantities like 0.1 + 0.2. */
    public static final double EPSILON = 0.0001;

    private final String name;
    private String unit;
    private double reorderLevel;
    private LinkedQueue<StockBatch> batches = new LinkedQueue<>();

    protected InventoryItem(String name, String unit, double reorderLevel) {
        this.name = name.trim().toUpperCase();
        this.unit = unit;
        this.reorderLevel = reorderLevel;
    }

    // ---- Abstract methods: each subclass answers differently (polymorphism) ----

    /** "RAW" or "PRODUCTION"; also used in the save file. */
    public abstract String getType();

    /** Name of the stock-in column on the cafe's paper form ("Delivery" / "Prod"). */
    public abstract String getStockInLabel();

    /** Name of the stock-out column ("Used" / "Out"). */
    public abstract String getStockOutLabel();

    // ---- Stock operations ----

    /** Total stock on hand: the sum of all batches in the queue. */
    public double getQuantity() {
        double total = 0;
        for (StockBatch batch : batches) {
            total += batch.getQuantity();
        }
        return total;
    }

    /** New stock goes to the rear of the queue. */
    public void addBatch(StockBatch batch) {
        batches.enqueue(batch);
    }

    /**
     * Removes stock FIFO: takes from the oldest batch first and moves to the
     * next batch only when the oldest one is used up.
     */
    public void consume(double amount) {
        if (amount > getQuantity() + EPSILON) {
            throw new IllegalArgumentException("Not enough stock of " + name + ". On hand: "
                    + formatQty(getQuantity()) + " " + unit);
        }
        double remaining = amount;
        while (remaining > EPSILON && !batches.isEmpty()) {
            StockBatch oldest = batches.peek();
            if (oldest.getQuantity() <= remaining + EPSILON) {
                remaining -= oldest.getQuantity();
                batches.dequeue(); // batch fully used
            } else {
                oldest.setQuantity(oldest.getQuantity() - remaining);
                remaining = 0;
            }
        }
    }

    /** Removes every expired batch and returns the total quantity removed. */
    public double removeExpired(LocalDate today) {
        LinkedQueue<StockBatch> kept = new LinkedQueue<>();
        double removed = 0;
        while (!batches.isEmpty()) {
            StockBatch batch = batches.dequeue();
            if (batch.isExpired(today)) {
                removed += batch.getQuantity();
            } else {
                kept.enqueue(batch); // order is preserved
            }
        }
        batches = kept;
        return removed;
    }

    public boolean hasExpiredBatch(LocalDate today) {
        for (StockBatch batch : batches) {
            if (batch.isExpired(today)) {
                return true;
            }
        }
        return false;
    }

    /** Deep copy of the batch queue, saved before a change so it can be undone. */
    public LinkedQueue<StockBatch> snapshotBatches() {
        LinkedQueue<StockBatch> copy = new LinkedQueue<>();
        for (StockBatch batch : batches) {
            copy.enqueue(batch.copy());
        }
        return copy;
    }

    public void restoreBatches(LinkedQueue<StockBatch> snapshot) {
        this.batches = snapshot;
    }

    public LinkedQueue<StockBatch> getBatches() {
        return batches;
    }

    // ---- Status ----

    public StockStatus getStatus() {
        double qty = getQuantity();
        if (qty <= EPSILON) {
            return StockStatus.OUT_OF_STOCK;
        }
        if (qty <= reorderLevel + EPSILON) {
            return StockStatus.LOW;
        }
        return StockStatus.OK;
    }

    /** Stock as a fraction of the reorder level; lower = more urgent to restock. */
    public double getStockRatio() {
        if (reorderLevel <= 0) {
            return getQuantity() > 0 ? Double.MAX_VALUE : 0;
        }
        return getQuantity() / reorderLevel;
    }

    // ---- Getters / setters (encapsulation) ----

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public double getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(double reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    /** Shows 2 instead of 2.0 and 0.25 instead of 0.25000001. */
    public static String formatQty(double value) {
        double rounded = Math.round(value * 100) / 100.0;
        if (rounded == Math.rint(rounded)) {
            return String.valueOf((long) rounded);
        }
        return String.valueOf(rounded);
    }
}
