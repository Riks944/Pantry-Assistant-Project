package com.example.pantryassistant.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Fixed set of categories used by the pantry form and list grouping. */
enum class Category(val label: String) {
    PRODUCE("Produce"),
    DAIRY("Dairy"),
    MEAT("Meat & Fish"),
    GRAINS("Grains & Pasta"),
    CANNED("Canned Goods"),
    SNACKS("Snacks"),
    BEVERAGES("Beverages"),
    SPICES("Spices & Condiments"),
    CLEANING("Cleaning"),
    TOILETRIES("Toiletries"),
    OTHER("Other");
}

/**
 * A household item kept in the pantry.
 *
 * An item counts as low stock when the user marks it manually ([markedLow])
 * or when its [quantity] drops to or below [lowStockThreshold].
 */
@Entity(tableName = "pantry_items")
data class PantryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: Int,
    val category: Category,
    val lowStockThreshold: Int = 1,
    val markedLow: Boolean = false,
)

/** Kept outside the entity so Room never tries to map it to a column. */
val PantryItem.isLowStock: Boolean
    get() = markedLow || quantity <= lowStockThreshold

/** How many to buy: enough to get back to twice the threshold (at least 1). */
fun PantryItem.suggestedQuantityToBuy(): Int =
    (lowStockThreshold * 2 - quantity).coerceAtLeast(1)
