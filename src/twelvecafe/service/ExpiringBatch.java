package twelvecafe.service;

import twelvecafe.model.InventoryItem;
import twelvecafe.model.StockBatch;

/** A batch paired with the item it belongs to, for the expiry report. */
public class ExpiringBatch {

    private final InventoryItem item;
    private final StockBatch batch;

    public ExpiringBatch(InventoryItem item, StockBatch batch) {
        this.item = item;
        this.batch = batch;
    }

    public InventoryItem getItem() {
        return item;
    }

    public StockBatch getBatch() {
        return batch;
    }
}
