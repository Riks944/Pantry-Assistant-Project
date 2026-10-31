package com.example.pantryassistant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pantryassistant.PantryApp
import com.example.pantryassistant.data.Category
import com.example.pantryassistant.data.PantryItem
import com.example.pantryassistant.data.PantryRepository
import com.example.pantryassistant.data.ShoppingItem
import com.example.pantryassistant.data.isLowStock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the pantry list screen needs. */
data class PantryUiState(
    val items: List<PantryItem> = emptyList(),
    val filter: Category? = null,
    val showLowOnly: Boolean = false,
    val lowStockCount: Int = 0,
)

/** State of the add/edit form. Fields are Strings so the user can type freely. */
data class ItemFormState(
    val id: Long = 0,
    val name: String = "",
    val quantity: String = "1",
    val threshold: String = "1",
    val category: Category = Category.OTHER,
    val markedLow: Boolean = false,
    val nameError: String? = null,
    val quantityError: String? = null,
    val thresholdError: String? = null,
) {
    val isEditing: Boolean get() = id != 0L
}

class PantryViewModel(private val repository: PantryRepository) : ViewModel() {

    private val filter = MutableStateFlow<Category?>(null)
    private val lowOnly = MutableStateFlow(false)

    val pantryState: StateFlow<PantryUiState> =
        combine(repository.pantryItems, filter, lowOnly) { items, cat, low ->
            PantryUiState(
                items = items.filter { (cat == null || it.category == cat) && (!low || it.isLowStock) },
                filter = cat,
                showLowOnly = low,
                lowStockCount = items.count { it.isLowStock },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PantryUiState())

    val shoppingItems: StateFlow<List<ShoppingItem>> =
        repository.shoppingItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _form = MutableStateFlow(ItemFormState())
    val form: StateFlow<ItemFormState> = _form.asStateFlow()

    // ---- Pantry list actions ----

    fun setFilter(category: Category?) { filter.value = category }
    fun setShowLowOnly(value: Boolean) { lowOnly.value = value }

    fun increment(item: PantryItem) = viewModelScope.launch { repository.changeQuantity(item, +1) }
    fun decrement(item: PantryItem) = viewModelScope.launch { repository.changeQuantity(item, -1) }
    fun toggleMarkedLow(item: PantryItem) = viewModelScope.launch { repository.setMarkedLow(item, !item.markedLow) }
    fun delete(item: PantryItem) = viewModelScope.launch { repository.deleteItem(item) }

    // ---- Form ----

    fun startNewItem() { _form.value = ItemFormState() }

    fun loadItem(id: Long) = viewModelScope.launch {
        repository.getItem(id)?.let { item ->
            _form.value = ItemFormState(
                id = item.id,
                name = item.name,
                quantity = item.quantity.toString(),
                threshold = item.lowStockThreshold.toString(),
                category = item.category,
                markedLow = item.markedLow,
            )
        }
    }

    fun onNameChange(v: String) = _form.update { it.copy(name = v, nameError = null) }
    fun onQuantityChange(v: String) = _form.update { it.copy(quantity = v.filter(Char::isDigit), quantityError = null) }
    fun onThresholdChange(v: String) = _form.update { it.copy(threshold = v.filter(Char::isDigit), thresholdError = null) }
    fun onCategoryChange(v: Category) = _form.update { it.copy(category = v) }
    fun onMarkedLowChange(v: Boolean) = _form.update { it.copy(markedLow = v) }

    /** Validates and saves. Calls [onSaved] only when the form is valid. */
    fun save(onSaved: () -> Unit) {
        val f = _form.value
        val qty = f.quantity.toIntOrNull()
        val threshold = f.threshold.toIntOrNull()
        val checked = f.copy(
            nameError = if (f.name.isBlank()) "Name is required" else null,
            quantityError = if (qty == null) "Enter a whole number" else null,
            thresholdError = if (threshold == null) "Enter a whole number" else null,
        )
        if (checked.nameError != null || checked.quantityError != null || checked.thresholdError != null) {
            _form.value = checked
            return
        }
        val item = PantryItem(
            id = f.id,
            name = f.name.trim(),
            quantity = qty!!,
            category = f.category,
            lowStockThreshold = threshold!!,
            markedLow = f.markedLow,
        )
        viewModelScope.launch {
            if (f.isEditing) repository.updateItem(item) else repository.addItem(item)
            onSaved()
        }
    }

    // ---- Shopping list actions ----

    fun togglePurchased(item: ShoppingItem) = viewModelScope.launch { repository.togglePurchased(item) }
    fun removeShoppingItem(item: ShoppingItem) = viewModelScope.launch { repository.removeShoppingItem(item) }
    fun addManualShoppingItem(name: String, qty: Int) = viewModelScope.launch {
        if (name.isNotBlank()) repository.addManualShoppingItem(name, qty)
    }
    fun restockPurchased(onDone: (Int) -> Unit) = viewModelScope.launch {
        onDone(repository.restockPurchased())
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PantryApp
                PantryViewModel(app.repository)
            }
        }
    }
}
