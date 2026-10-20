package twelvecafe.model;

import java.time.LocalDate;

/**
 * One recorded stock movement. The full list of transactions is the cafe's
 * history, and the daily report is computed from it.
 */
public class Transaction {

    private final LocalDate date;
    private final TransactionType type;
    private final String itemName;
    private final double quantity; // positive, except ADJUSTMENT which can be negative
    private final String staff;
    private final String note;

    public Transaction(LocalDate date, TransactionType type, String itemName,
                       double quantity, String staff, String note) {
        this.date = date;
        this.type = type;
        this.itemName = itemName;
        this.quantity = quantity;
        this.staff = staff;
        this.note = note;
    }

    /** The effect on stock: + for stock in, - for stock out and waste. */
    public double getSignedQuantity() {
        switch (type) {
            case STOCK_IN:
                return quantity;
            case STOCK_OUT:
            case WASTE:
                return -quantity;
            default:
                return quantity; // ADJUSTMENT already carries its sign
        }
    }

    public LocalDate getDate() {
        return date;
    }

    public TransactionType getType() {
        return type;
    }

    public String getItemName() {
        return itemName;
    }

    public double getQuantity() {
        return quantity;
    }

    public String getStaff() {
        return staff;
    }

    public String getNote() {
        return note;
    }
}
