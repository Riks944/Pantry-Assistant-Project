package com.example.pantryassistant.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [PantryItem::class, ShoppingItem::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class PantryDatabase : RoomDatabase() {
    abstract fun pantryDao(): PantryDao
    abstract fun shoppingDao(): ShoppingDao

    companion object {
        @Volatile
        private var INSTANCE: PantryDatabase? = null

        fun getInstance(context: Context): PantryDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PantryDatabase::class.java,
                    "pantry.db",
                ).build().also { INSTANCE = it }
            }
    }
}
