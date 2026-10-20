package twelvecafe.algo;

import java.util.Comparator;
import java.util.List;

/**
 * Merge Sort: a divide-and-conquer sorting algorithm.
 *
 * 1. Divide the list into two halves.
 * 2. Recursively sort each half.
 * 3. Merge the two sorted halves into one sorted list.
 *
 * Time complexity: O(n log n) in all cases. Space: O(n).
 * It is stable: items that compare equal keep their original order.
 *
 * The Comparator decides the sort order, so the same algorithm sorts items
 * by name, by quantity, by status, or batches by expiry date.
 */
public class MergeSort {

    private MergeSort() {
    }

    public static <T> void sort(List<T> list, Comparator<? super T> comparator) {
        int n = list.size();
        if (n < 2) {
            return;
        }
        Object[] items = list.toArray();
        Object[] temp = new Object[n];
        mergeSort(items, temp, 0, n - 1, comparator);
        for (int i = 0; i < n; i++) {
            @SuppressWarnings("unchecked")
            T item = (T) items[i];
            list.set(i, item);
        }
    }

    private static <T> void mergeSort(Object[] items, Object[] temp, int low, int high,
                                      Comparator<? super T> comparator) {
        if (low >= high) {
            return; // a list of 0 or 1 element is already sorted
        }
        int mid = (low + high) / 2;
        mergeSort(items, temp, low, mid, comparator);
        mergeSort(items, temp, mid + 1, high, comparator);
        merge(items, temp, low, mid, high, comparator);
    }

    @SuppressWarnings("unchecked")
    private static <T> void merge(Object[] items, Object[] temp, int low, int mid, int high,
                                  Comparator<? super T> comparator) {
        int left = low;
        int right = mid + 1;
        int k = low;

        while (left <= mid && right <= high) {
            // "<=" keeps the sort stable
            if (comparator.compare((T) items[left], (T) items[right]) <= 0) {
                temp[k++] = items[left++];
            } else {
                temp[k++] = items[right++];
            }
        }
        while (left <= mid) {
            temp[k++] = items[left++];
        }
        while (right <= high) {
            temp[k++] = items[right++];
        }
        for (int i = low; i <= high; i++) {
            items[i] = temp[i];
        }
    }
}
