package twelvecafe.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * One delivery or production batch of an item, e.g. "5 kg received on Oct 14,
 * expires Oct 18". Batches wait in a FIFO queue inside their InventoryItem.
 */
public class StockBatch {

    private double quantity;
    private final LocalDate dateReceived;
    private final LocalDate expiryDate; // null when the item does not expire

    public StockBatch(double quantity, LocalDate dateReceived, LocalDate expiryDate) {
        this.quantity = quantity;
        this.dateReceived = dateReceived;
        this.expiryDate = expiryDate;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public LocalDate getDateReceived() {
        return dateReceived;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public boolean hasExpiry() {
        return expiryDate != null;
    }

    /** A batch is expired once the date is past its expiry date. */
    public boolean isExpired(LocalDate today) {
        return expiryDate != null && expiryDate.isBefore(today);
    }

    /** Days left before expiry (negative when already expired). */
    public long daysUntilExpiry(LocalDate today) {
        return ChronoUnit.DAYS.between(today, expiryDate);
    }

    public StockBatch copy() {
        return new StockBatch(quantity, dateReceived, expiryDate);
    }
}
