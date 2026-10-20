package twelvecafe.algo;

import java.util.ArrayList;
import java.util.List;

import twelvecafe.model.InventoryItem;

/**
 * Searching algorithms used to look up inventory items.
 */
public class Search {

    private Search() {
    }

    /**
     * Binary Search for an exact item name (case-insensitive).
     * The list MUST already be sorted by name (A-Z).
     *
     * Each step compares the middle item and discards half of the list,
     * so 142 items need at most 8 comparisons. Time complexity: O(log n).
     *
     * @return the index of the item, or -1 if not found
     */
    public static int binarySearchByName(List<InventoryItem> sortedItems, String name) {
        String key = name.trim();
        int low = 0;
        int high = sortedItems.size() - 1;

        while (low <= high) {
            int mid = (low + high) / 2;
            int result = sortedItems.get(mid).getName().compareToIgnoreCase(key);
            if (result == 0) {
                return mid;
            } else if (result < 0) {
                low = mid + 1;  // key is in the right half
            } else {
                high = mid - 1; // key is in the left half
            }
        }
        return -1;
    }

    /**
     * Linear Search for every item whose name contains the keyword.
     * Used for partial matches (e.g. "chicken" finds all chicken items),
     * which binary search cannot do. Time complexity: O(n).
     */
    public static List<InventoryItem> linearSearchByKeyword(List<InventoryItem> items, String keyword) {
        String key = keyword.trim().toUpperCase();
        List<InventoryItem> matches = new ArrayList<>();
        for (InventoryItem item : items) {
            if (item.getName().toUpperCase().contains(key)) {
                matches.add(item);
            }
        }
        return matches;
    }
}
