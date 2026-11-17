package com.example.pantryassistant.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.util.Locale

/**
 * Single source of truth for the UI and the background worker.
 *
 * Rule: whenever a pantry item is low stock it gets an entry on the
 * shopping list. When it is restocked above its threshold (and not
 * manually marked low) the unpurchased entry is removed again.
 */
class PantryRepository(private val db: PantryDatabase) {
    private val pantryDao = db.pantryDao()
    private val shoppingDao = db.shoppingDao()

    val pantryItems: Flow<List<PantryItem>> = pantryDao.observeAll()
    val shoppingItems: Flow<List<ShoppingItem>> = shoppingDao.observeAll()

    suspend fun getItem(id: Long): PantryItem? = pantryDao.getById(id)

    suspend fun getLowStockItems(): List<PantryItem> = pantryDao.getLowStock()

    suspend fun addItem(item: PantryItem) = db.withTransaction {
        val id = pantryDao.insert(item)
        syncShoppingList(item.copy(id = id))
    }

    suspend fun updateItem(item: PantryItem) = db.withTransaction {
        pantryDao.update(item)
        syncShoppingList(item)
    }

    suspend fun deleteItem(item: PantryItem) {
        // Linked shopping entry is removed by the foreign-key CASCADE.
        pantryDao.delete(item)
    }

    suspend fun changeQuantity(item: PantryItem, delta: Int) {
        val newQty = (item.quantity + delta).coerceAtLeast(0)
        // Increasing stock clears a manual "low" mark.
        val markedLow = if (delta > 0) false else item.markedLow
        updateItem(item.copy(quantity = newQty, markedLow = markedLow))
    }

    suspend fun setMarkedLow(item: PantryItem, low: Boolean) =
        updateItem(item.copy(markedLow = low))

    // ---- Shopping list ----

    suspend fun addManualShoppingItem(name: String, quantity: Int) {
        shoppingDao.insert(ShoppingItem(name = name.trim(), quantityToBuy = quantity.coerceAtLeast(1)))
    }

    suspend fun togglePurchased(item: ShoppingItem) =
        shoppingDao.update(item.copy(purchased = !item.purchased))

    suspend fun removeShoppingItem(item: ShoppingItem) = shoppingDao.delete(item)

    /**
     * Moves every purchased entry back into the pantry and removes it from the list.
     *
     * - Entry linked to a pantry item → that item's quantity goes up.
     * - Extra item typed on the shopping screen (no link) → matched to a pantry
     *   item with the same name. Upper/lower case and extra spaces are ignored,
     *   so "milk", "Milk" and " MILK " all match "Milk". If none exists,
     *   a new pantry item is created in the "Other" category.
     *
     * Returns one line per entry describing what happened, e.g. "Milk: 1 → 4",
     * which the shopping screen shows in a snackbar.
     */
    suspend fun restockPurchased(): List<String> = db.withTransaction {
        val results = mutableListOf<String>()
        val purchased = shoppingDao.getPurchased()
        purchased.forEach { entry ->
            shoppingDao.delete(entry)
            val pantryItem = entry.pantryItemId?.let { pantryDao.getById(it) }
                ?: findByName(entry.name)
            if (pantryItem != null) {
                val restocked = pantryItem.copy(
                    quantity = pantryItem.quantity + entry.quantityToBuy,
                    markedLow = false,
                )
                pantryDao.update(restocked)
                syncShoppingList(restocked)
                results += "${restocked.name}: ${pantryItem.quantity} → ${restocked.quantity}"
            } else {
                val newItem = PantryItem(
                    name = entry.name.trim(),
                    quantity = entry.quantityToBuy,
                    category = Category.OTHER,
                    // Threshold 0 = only "low" when it runs out, so a freshly bought
                    // item doesn't jump straight back onto the shopping list.
                    lowStockThreshold = 0,
                )
                val id = pantryDao.insert(newItem)
                syncShoppingList(newItem.copy(id = id))
                results += "New pantry item: ${newItem.name} (${newItem.quantity})"
            }
        }
        results
    }

    /**
     * Finds the pantry item an extra shopping item refers to. Tries the SQL
     * lookup first, then compares normalized names in Kotlin, which also
     * ignores extra spaces and non-English upper/lower case.
     */
    private suspend fun findByName(name: String): PantryItem? {
        val wanted = normalize(name)
        if (wanted.isEmpty()) return null
        return pantryDao.getByName(name.trim())
            ?: pantryDao.getAll().firstOrNull { normalize(it.name) == wanted }
    }

    private fun normalize(name: String): String =
        name.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)

    private suspend fun syncShoppingList(item: PantryItem) {
        val existing = shoppingDao.getForPantryItem(item.id)
        if (item.isLowStock) {
            val needed = item.suggestedQuantityToBuy()
            if (existing == null) {
                shoppingDao.insert(ShoppingItem(name = item.name, quantityToBuy = needed, pantryItemId = item.id))
            } else if (!existing.purchased) {
                shoppingDao.update(existing.copy(name = item.name, quantityToBuy = needed))
            }
        } else if (existing != null && !existing.purchased) {
            shoppingDao.delete(existing)
        }
    }
}
