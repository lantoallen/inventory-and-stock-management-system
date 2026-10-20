package twelvecafe.service;

import java.util.ArrayList;
import java.util.List;

import twelvecafe.model.InventoryItem;
import twelvecafe.model.ProductionItem;
import twelvecafe.model.RawIngredient;

/**
 * The item list from The Twelve Cafe's paper inventory forms, loaded the
 * first time the program runs (when there is no saved data yet).
 *
 * NOTE: units and reorder levels are placeholders. Confirm them with the
 * cafe; they can also be changed inside the program (Manage items).
 */
public class SeedData {

    private SeedData() {
    }

    // { name, unit, reorder level }
    private static final String[][] RAW_INGREDIENTS = {
        {"ALL PURPOSE CREAM", "box", "2"},
        {"ALL PURPOSE FLOUR", "kg", "2"},
        {"APPLE", "pc", "5"},
        {"BACON", "kg", "1"},
        {"BALSAMIC CREAM", "bottle", "1"},
        {"BBQ POWDER", "pack", "1"},
        {"BEEF CUBES", "kg", "2"},
        {"BEEF FOR STIRFRY", "kg", "2"},
        {"BLACK PEPPER", "pack", "1"},
        {"BREAD CRUMBS", "pack", "1"},
        {"BROWN SUGAR", "kg", "1"},
        {"BURGER BUNS", "pack", "3"},
        {"BUTTER", "block", "2"},
        {"CABBAGE", "kg", "1"},
        {"CALAMANSI", "kg", "0.5"},
        {"CARROTS", "kg", "1"},
        {"CHEESE POWDER", "pack", "1"},
        {"CHICKEN BREAST", "kg", "2"},
        {"CHICKEN CUBES", "kg", "2"},
        {"CHICKEN POWDER", "pack", "1"},
        {"CHICKEN THIGH", "kg", "2"},
        {"COOKING CREAM", "box", "2"},
        {"CORNSTARCH", "pack", "1"},
        {"CREAM DORY", "kg", "2"},
        {"CUCUMBER", "kg", "1"},
        {"DELMONTE KETCHUP", "bottle", "2"},
        {"DRIED PARSLEY", "pack", "1"},
        {"EDEN CHEESE", "box", "2"},
        {"EGGS", "tray", "2"},
        {"EVAP", "can", "3"},
        {"FRENCH BEANS", "kg", "1"},
        {"FRIES", "kg", "3"},
        {"GARLIC", "kg", "1"},
        {"GREEN ICE LETTUCE", "kg", "1"},
        {"GROUND BEEF", "kg", "2"},
        {"GROUND PORK", "kg", "2"},
        {"HOTSAUCE", "bottle", "1"},
        {"ICEBERG LETTUCE", "kg", "1"},
        {"KASIM", "kg", "2"},
        {"KATSU SAUCE", "bottle", "1"},
        {"KNORR SEASONING", "bottle", "1"},
        {"LEMON", "pc", "5"},
        {"LINGUINE PASTA", "pack", "2"},
        {"MAYONNAISE", "jar", "2"},
        {"MUSHROOM SLICED", "can", "3"},
        {"OIL", "L", "5"},
        {"OREGANO POWDER", "pack", "1"},
        {"OYSTER SAUCE", "bottle", "1"},
        {"PAPRIKA", "pack", "1"},
        {"PARMESAN CHEESE", "pack", "1"},
        {"PARSLEY", "bundle", "1"},
        {"PATIS", "bottle", "1"},
        {"PEANUT BUTTER", "jar", "1"},
        {"PENNE PASTA", "pack", "2"},
        {"PESTO", "jar", "1"},
        {"PICKLE RELISH", "jar", "1"},
        {"PORK CUBES", "kg", "2"},
        {"PORK STEAK", "kg", "2"},
        {"POTATO", "kg", "3"},
        {"RED BELL PEPPER", "kg", "0.5"},
        {"RED WINE", "bottle", "1"},
        {"RIB EYE STEAK", "kg", "2"},
        {"LETTUCE", "kg", "1"},
        {"SALMON FILLET", "kg", "2"},
        {"SALT", "kg", "1"},
        {"SAUSAGE", "pack", "2"},
        {"SESAME DRESSINGS", "bottle", "1"},
        {"SESAME OIL", "bottle", "1"},
        {"SESAME SEED", "pack", "1"},
        {"SHRIMP", "kg", "2"},
        {"SINGKAMAS", "kg", "1"},
        {"SOUR CREAM POWDER", "pack", "1"},
        {"SPAGHETTI PASTA", "pack", "2"},
        {"SPAGHETTI SAUCE", "pack", "2"},
        {"SPAM", "can", "6"},
        {"STRAWBERRY", "pack", "1"},
        {"SUKA", "bottle", "1"},
        {"SWEET CHILI", "bottle", "1"},
        {"TOCINO", "kg", "2"},
        {"TOMATO", "kg", "1"},
        {"TOYO", "bottle", "1"},
        {"UFC KETCHUP", "bottle", "2"},
        {"WHEAT BREAD", "loaf", "2"},
        {"WHITE ONION", "kg", "1"},
        {"WHITE PEPPER", "pack", "1"},
        {"WHITE WINE", "bottle", "1"},
        {"WORCESTERSHIRE SAUCE", "bottle", "1"},
        {"YELLOW MUSTARD", "bottle", "1"},
    };

    // SPAM and TOCINO are also raw ingredients, so the prepared versions get
    // "(PROD)" to keep every name unique for searching.
    private static final String[] PRODUCTION_ITEMS = {
        "TAPA", "SPAM (PROD)", "TOCINO (PROD)", "EGG", "BURGER PATTY", "SIDE FRIES", "FRIES 100G",
        "CARBONARA NOODLES", "CARBONARA SAUCE", "PESTO NOODLES", "PESTO SAUCE",
        "MEAT BALL PASTA", "MEAT BALL SAUCE", "CAESAR SALAD SAUCE", "CHEF'S SALAD SAUCE",
        "CROUTONS", "SHRIMP BACON WRAP", "CHICKEN FOR SALAD", "CHICKEN KATSU",
        "CHICKEN KATSU SAUCE", "BEEF STIR FRY", "SWEET&SOUR PORK", "FISH FILLET",
        "BUTTERED SHRIMP", "BINAGOONGAN", "KAREKARE MEAT", "ALA KING CHIX", "SISIG",
        "SALMON SMALL", "SALMON BIG", "RIBEYE STEAK", "STEAK SAUCE", "MASHED POTATO",
        "CHEESE SAUCE", "SOY GARLIC", "BUFFALO", "CHURROS", "BURGER SAUCE",
        "KARE KARE SAUCE", "ALAMANG", "ALAKING SAUCE", "SISIG SAUCE", "SMOKY BBQ",
        "BALSAMIC", "GARLIC MAYO DIP", "PEPPER CORN SAUCE", "BECHAMEL SAUCE", "COLESLAW",
    };

    private static final String PRODUCTION_UNIT = "portion";
    private static final double PRODUCTION_REORDER = 5;

    public static List<InventoryItem> createItems() {
        List<InventoryItem> items = new ArrayList<>();
        for (String[] row : RAW_INGREDIENTS) {
            items.add(new RawIngredient(row[0], row[1], Double.parseDouble(row[2])));
        }
        for (String name : PRODUCTION_ITEMS) {
            items.add(new ProductionItem(name, PRODUCTION_UNIT, PRODUCTION_REORDER));
        }
        return items;
    }
}
