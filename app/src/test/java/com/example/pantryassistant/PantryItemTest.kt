package com.example.pantryassistant

import com.example.pantryassistant.data.Category
import com.example.pantryassistant.data.PantryItem
import com.example.pantryassistant.data.isLowStock
import com.example.pantryassistant.data.suggestedQuantityToBuy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PantryItemTest {
    private fun item(qty: Int, threshold: Int = 2, marked: Boolean = false) =
        PantryItem(name = "Milk", quantity = qty, category = Category.DAIRY, lowStockThreshold = threshold, markedLow = marked)

    @Test fun aboveThreshold_isNotLow() = assertFalse(item(qty = 5).isLowStock)

    @Test fun atThreshold_isLow() = assertTrue(item(qty = 2).isLowStock)

    @Test fun manuallyMarked_isLowEvenWhenStocked() = assertTrue(item(qty = 10, marked = true).isLowStock)

    @Test fun suggestedQuantity_refillsToDoubleThreshold() = assertEquals(3, item(qty = 1, threshold = 2).suggestedQuantityToBuy())

    @Test fun suggestedQuantity_isAtLeastOne() = assertEquals(1, item(qty = 10, threshold = 2, marked = true).suggestedQuantityToBuy())
}
