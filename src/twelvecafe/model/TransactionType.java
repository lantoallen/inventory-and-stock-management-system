package twelvecafe.model;

/** The kinds of stock movement the system records. */
public enum TransactionType {
    STOCK_IN,   // delivery (raw) or production (prepared)
    STOCK_OUT,  // used in the kitchen / served
    WASTE,      // spoiled, expired, or dropped
    ADJUSTMENT  // correction after a physical count (+ or -)
}
