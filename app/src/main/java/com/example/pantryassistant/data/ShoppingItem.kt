package com.example.pantryassistant.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An entry on the shopping list. It is usually linked to a pantry item
 * ([pantryItemId]) but can also be a standalone item the user typed in.
 */
@Entity(
    tableName = "shopping_items",
    foreignKeys = [
        ForeignKey(
            entity = PantryItem::class,
            parentColumns = ["id"],
            childColumns = ["pantryItemId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["pantryItemId"], unique = true)],
)
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantityToBuy: Int = 1,
    val pantryItemId: Long? = null,
    val purchased: Boolean = false,
)
