# Pantry Assistant (Android, Kotlin)

Final project for **Topic 4: Home Pantry and Shopping Assistant**: an app for keeping track of household items so you don't buy things you already have.

## How to open and run
1. Unzip the folder and open it in Android Studio with **File ▸ Open ▸ PantryAssistant**. Use Android Studio Ladybug or newer.
2. Let Gradle sync finish. It downloads Gradle 8.9 and all dependencies.
3. Run the app on an emulator or phone running Android 8.0 (API 26) or newer.
4. On Android 13+, tap **Allow** when the app asks for notification permission.

## How the brief's requirements are covered

| Requirement | Where it is implemented |
|---|---|
| Create pantry items with quantity and category | `AddEditItemScreen` form (name, category dropdown, quantity, low-stock threshold) with validation in `PantryViewModel.save()` |
| Mark an item as low stock and add it to a shopping list | ⋮ menu → *Mark as low stock*, or the switch on the form. `PantryRepository.syncShoppingList()` adds the item to the shopping list automatically. An item also counts as low stock when its quantity drops to the threshold or below. |
| Persist pantry and shopping list locally | Room database (`PantryDatabase`, `PantryDao`, `ShoppingDao`), with a foreign key and cascade delete |
| One reminder / background check for low stock | `LowStockWorker` (WorkManager) runs every 12 hours and posts a notification listing the low items. Tapping the notification opens the shopping list. The 🔔 button runs the check immediately for a demo. |
| Midterm feature: add items, change quantity, see the resulting shopping list | The +/– buttons on each pantry card update Room, and the Shopping tab (with a badge count) updates right away |

**Technical emphasis covered:** forms, lists (`LazyColumn`), ViewModel state (`StateFlow`), Room, Navigation Compose (bottom bar plus an add/edit route with an argument), notifications and WorkManager.

**Scope boundary respected:** no barcode scanning, syncing, price comparison or online ordering.

## Extra features
- Filter chips for category and for low stock only
- Shopping list: check items off as you buy them, add extra items by hand, and use **Move bought items to pantry**, which adds the bought quantity back into the pantry and clears the low-stock flag
- Suggested amount to buy = enough to get back to 2× the threshold
- Material 3, dark mode, dynamic color

## Project structure
```
app/src/main/java/com/example/pantryassistant/
├── PantryApp.kt              Application: creates the repository, notification channel, schedules the worker
├── MainActivity.kt           Compose host, notification permission, deep link from the notification
├── data/
│   ├── PantryItem.kt         @Entity + Category enum + low-stock rules
│   ├── ShoppingItem.kt       @Entity (FK → PantryItem)
│   ├── PantryDao.kt          PantryDao + ShoppingDao
│   ├── Converters.kt         Category <-> String
│   ├── PantryDatabase.kt     Room database singleton
│   └── PantryRepository.kt   Business logic (low stock ⇒ shopping list, restock)
├── ui/
│   ├── PantryViewModel.kt    UI state + form state + actions
│   ├── AppNavigation.kt      NavHost, bottom navigation
│   ├── screens/              PantryListScreen, AddEditItemScreen, ShoppingListScreen
│   └── theme/Theme.kt
└── worker/
    ├── LowStockWorker.kt     WorkManager background check
    └── NotificationHelper.kt Channel + notification
app/src/test/…/PantryItemTest.kt   Unit tests for the low-stock rules
```

## Demo script for presentation
1. Add "Milk", Dairy, quantity 3, low at 1.
2. Tap **–** twice. Milk turns *Low stock* and appears on the Shopping tab with a badge.
3. On "Rice", choose ⋮ → *Mark as low stock*. It is added to the shopping list.
4. Tap 🔔. A notification lists the low items, and tapping it opens the shopping list.
5. Check Milk on the shopping list, then tap **Move bought items to pantry**. Milk's quantity goes back up.
6. Close and reopen the app. Everything is still there (Room).
