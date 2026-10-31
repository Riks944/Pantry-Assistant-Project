package com.example.pantryassistant.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromCategory(category: Category): String = category.name

    @TypeConverter
    fun toCategory(value: String): Category =
        runCatching { Category.valueOf(value) }.getOrDefault(Category.OTHER)
}
