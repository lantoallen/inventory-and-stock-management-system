package twelvecafe.model;

/** Stock level of an item, listed from most to least urgent. */
public enum StockStatus {
    OUT_OF_STOCK("OUT"),
    LOW("LOW"),
    OK("OK");

    private final String label;

    StockStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
