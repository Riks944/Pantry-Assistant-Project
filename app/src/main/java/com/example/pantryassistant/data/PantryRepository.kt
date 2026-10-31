package com.example.pantryassistant.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

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
     * Moves every purchased entry back into the pantry: linked items get their
     * quantity increased and their low mark cleared; the entries are removed.
     */
    suspend fun restockPurchased(): Int = db.withTransaction {
        val purchased = shoppingDao.getPurchased()
        purchased.forEach { entry ->
            val pantryId = entry.pantryItemId
            shoppingDao.delete(entry)
            if (pantryId != null) {
                pantryDao.getById(pantryId)?.let { p ->
                    val restocked = p.copy(quantity = p.quantity + entry.quantityToBuy, markedLow = false)
                    pantryDao.update(restocked)
                    syncShoppingList(restocked)
                }
            }
        }
        purchased.size
    }

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
