package twelvecafe.model;

/**
 * A component prepared in the kitchen ahead of service (e.g. Tapa,
 * Carbonara Sauce, Chicken Katsu). Matches the cafe's "Production
 * Inventory" sheet: Prod / Beginning / Out / Ending.
 */
public class ProductionItem extends InventoryItem {

    public ProductionItem(String name, String unit, double reorderLevel) {
        super(name, unit, reorderLevel);
    }

    @Override
    public String getType() {
        return "PRODUCTION";
    }

    @Override
    public String getStockInLabel() {
        return "Prod";
    }

    @Override
    public String getStockOutLabel() {
        return "Out";
    }
}
