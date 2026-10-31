package com.example.pantryassistant.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryDao {
    @Query("SELECT * FROM pantry_items ORDER BY category, name COLLATE NOCASE")
    fun observeAll(): Flow<List<PantryItem>>

    @Query("SELECT * FROM pantry_items WHERE id = :id")
    suspend fun getById(id: Long): PantryItem?

    @Query("SELECT * FROM pantry_items WHERE markedLow = 1 OR quantity <= lowStockThreshold ORDER BY name COLLATE NOCASE")
    suspend fun getLowStock(): List<PantryItem>

    @Insert
    suspend fun insert(item: PantryItem): Long

    @Update
    suspend fun update(item: PantryItem)

    @Delete
    suspend fun delete(item: PantryItem)
}

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_items ORDER BY purchased, name COLLATE NOCASE")
    fun observeAll(): Flow<List<ShoppingItem>>

    @Query("SELECT * FROM shopping_items WHERE pantryItemId = :pantryItemId LIMIT 1")
    suspend fun getForPantryItem(pantryItemId: Long): ShoppingItem?

    @Query("SELECT * FROM shopping_items WHERE purchased = 1")
    suspend fun getPurchased(): List<ShoppingItem>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: ShoppingItem): Long

    @Update
    suspend fun update(item: ShoppingItem)

    @Delete
    suspend fun delete(item: ShoppingItem)

    @Query("DELETE FROM shopping_items WHERE pantryItemId = :pantryItemId")
    suspend fun deleteForPantryItem(pantryItemId: Long)
}
