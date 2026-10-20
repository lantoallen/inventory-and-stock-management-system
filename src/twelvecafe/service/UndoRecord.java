package twelvecafe.service;

import java.util.ArrayList;
import java.util.List;

import twelvecafe.ds.LinkedQueue;
import twelvecafe.model.InventoryItem;
import twelvecafe.model.StockBatch;

/**
 * What the undo stack stores: the state of the affected items BEFORE a
 * change, plus how long the transaction log was. Undoing restores both.
 */
public class UndoRecord {

    private final String description;
    private final List<InventoryItem> items = new ArrayList<>();
    private final List<LinkedQueue<StockBatch>> snapshots = new ArrayList<>();
    private final int logSizeBefore;

    public UndoRecord(String description, int logSizeBefore) {
        this.description = description;
        this.logSizeBefore = logSizeBefore;
    }

    /** Saves a copy of the item's batches. Call BEFORE changing the item. */
    public void remember(InventoryItem item) {
        items.add(item);
        snapshots.add(item.snapshotBatches());
    }

    public void restore() {
        for (int i = 0; i < items.size(); i++) {
            items.get(i).restoreBatches(snapshots.get(i));
        }
    }

    public String getDescription() {
        return description;
    }

    public int getLogSizeBefore() {
        return logSizeBefore;
    }
}
