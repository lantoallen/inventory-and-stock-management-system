package twelvecafe.model;

/**
 * An ingredient bought from a supplier (e.g. Chicken Breast, Eggs, Oil).
 * Matches the cafe's "INVENTORY" sheet: Delivery / Beginning / Ending.
 */
public class RawIngredient extends InventoryItem {

    public RawIngredient(String name, String unit, double reorderLevel) {
        super(name, unit, reorderLevel);
    }

    @Override
    public String getType() {
        return "RAW";
    }

    @Override
    public String getStockInLabel() {
        return "Delivery";
    }

    @Override
    public String getStockOutLabel() {
        return "Used";
    }
}
