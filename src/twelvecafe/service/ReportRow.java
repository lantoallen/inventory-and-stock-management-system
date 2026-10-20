package twelvecafe.service;

import twelvecafe.model.InventoryItem;

/**
 * One line of the daily inventory report, the digital version of a row on
 * the cafe's paper form: Beginning + In - Out - Waste +/- Adjust = Ending.
 */
public class ReportRow {

    private final InventoryItem item;
    private final double beginning;
    private final double stockIn;
    private final double stockOut;
    private final double waste;
    private final double adjustment;

    public ReportRow(InventoryItem item, double beginning, double stockIn,
                     double stockOut, double waste, double adjustment) {
        this.item = item;
        this.beginning = beginning;
        this.stockIn = stockIn;
        this.stockOut = stockOut;
        this.waste = waste;
        this.adjustment = adjustment;
    }

    public double getEnding() {
        return beginning + stockIn - stockOut - waste + adjustment;
    }

    /** True when the item had no stock and no movement that day. */
    public boolean isEmpty() {
        double eps = InventoryItem.EPSILON;
        return Math.abs(beginning) < eps && Math.abs(stockIn) < eps && Math.abs(stockOut) < eps
                && Math.abs(waste) < eps && Math.abs(adjustment) < eps;
    }

    public InventoryItem getItem() {
        return item;
    }

    public double getBeginning() {
        return beginning;
    }

    public double getStockIn() {
        return stockIn;
    }

    public double getStockOut() {
        return stockOut;
    }

    public double getWaste() {
        return waste;
    }

    public double getAdjustment() {
        return adjustment;
    }
}
