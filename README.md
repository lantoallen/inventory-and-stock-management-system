# The Twelve Cafe – Inventory & Stock Management System

Datastrc Final Project · Group 2 · De La Salle Lipa, AY 2026–2027

A Java desktop application (Swing) that replaces The Twelve Cafe's paper
inventory sheets (raw-ingredient inventory and production inventory). Staff
record deliveries, production, usage, waste and end-of-day counts. The system
computes the stock, warns about low stock and expiring items, and rebuilds the
daily inventory sheet for any date.

## How to run

Requires Java 17 or newer. No other libraries.

```bash
javac -d out $(find src -name "*.java")
java -cp out twelvecafe.Main
```

On Windows (Command Prompt):

```bat
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt
java -cp out twelvecafe.Main
```

In an IDE (IntelliJ, NetBeans, VS Code), open the folder, mark `src` as the
source root, and run `twelvecafe.Main`.

On the first run the program loads the cafe's item list (88 raw ingredients,
48 production items) and creates a `data/` folder. Every change is saved
automatically. Exported reports go to `reports/`. Delete `data/` to start over.

## Pages

| Page | What it does |
|---|---|
| Dashboard | Totals, low / out-of-stock counts, what to restock first, what expires soon |
| Inventory | All items; filter by type, sort 4 ways, search; shows the selected item's FIFO batch queue and history |
| Stock In | Delivery (raw) or production (prepared), with optional expiry date |
| Stock Out | Used / served; always takes from the oldest batch first |
| Waste | Spoiled, dropped or expired stock, with a reason |
| Ending Count | Physical count at closing for a whole sheet; differences are recorded as adjustments |
| Reports | Restock list, expiring stock (with discard), daily inventory sheet; export for printing |
| History | Every stock movement, newest first, filter by item and type |
| Manage Items | Add items; edit unit and reorder level |
| Header | Working date (record a past day's sheet) and Undo |

## Data structures and algorithms

| Requirement | Where | Why |
|---|---|---|
| **Queue** (`ds/LinkedQueue`) | Each item's stock batches | FIFO: the oldest delivery is used first, which reduces spoilage |
| **Stack** (`ds/LinkedStack`) | Undo history in `InventoryManager` | LIFO: undo always reverses the most recent transaction |
| **Merge Sort** (`algo/MergeSort`) | Inventory sorting, restock list, expiry report, keeping items sorted by name | O(n log n) and stable |
| **Binary Search** (`algo/Search`) | Exact item lookup (search boxes, loading saved batches) | O(log n) on the name-sorted list |
| **Linear Search** (`algo/Search`) | Keyword / partial lookup | Finds every match, e.g. "chicken" |

All of these are written by hand. The program does not use `java.util`
queues or stacks, `Collections.sort`, or `binarySearch`, and table
auto-sorting is turned off so every list is ordered by our Merge Sort.

## Project structure

```
src/twelvecafe/
├── Main.java                 entry point: loads data, opens the window
├── model/                    InventoryItem (abstract), RawIngredient, ProductionItem,
│                             StockBatch, Transaction, TransactionType, StockStatus
├── ds/                       LinkedQueue, LinkedStack
├── algo/                     MergeSort, Search
├── service/                  InventoryManager (logic), FileStorage, SeedData,
│                             UndoRecord, ReportRow, ExpiringBatch
└── gui/                      MainFrame (sidebar + pages), one panel per page,
                              ItemPicker, UiKit (shared styles), Refreshable
```

The GUI only collects input and displays results; all inventory logic lives in
`service/InventoryManager`, so the data structures and algorithms can be
explained without reading any GUI code.

## To confirm with the cafe

- Units and reorder levels in `service/SeedData.java` are placeholders. They
  can also be changed in the program (Manage Items).
- SPAM and TOCINO appear on both sheets. The prepared versions are named
  "SPAM (PROD)" and "TOCINO (PROD)".
- Typos fixed from the original sheet: Oyster Sauce, Mayonnaise, Caesar Salad
  Sauce, Buttered Shrimp. The duplicate Ground Pork row was removed.
