package com.example.pantryassistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.pantryassistant.data.ShoppingItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    items: List<ShoppingItem>,
    onTogglePurchased: (ShoppingItem) -> Unit,
    onRemove: (ShoppingItem) -> Unit,
    onAddManual: (String, Int) -> Unit,
    onRestock: (onDone: (List<String>) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val purchasedCount = items.count { it.purchased }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Shopping List") }) },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            AddManualRow(onAddManual)

            if (items.isEmpty()) {
                Column(Modifier.weight(1f)) {
                    EmptyState("Nothing to buy.\nLow-stock pantry items appear here automatically.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items, key = { it.id }) { item ->
                        ShoppingRow(item, { onTogglePurchased(item) }, { onRemove(item) })
                    }
                }
            }

            Button(
                onClick = {
                    onRestock { results ->
                        // Shows what happened to each item, e.g. "Milk: 1 → 4".
                        scope.launch { snackbarHost.showSnackbar(results.joinToString("\n")) }
                    }
                },
                enabled = purchasedCount > 0,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text(if (purchasedCount > 0) "Move $purchasedCount bought item(s) to pantry" else "Check items as you buy them")
            }
        }
    }
}

@Composable
private fun AddManualRow(onAdd: (String, Int) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var qty by rememberSaveable { mutableStateOf("1") }
    val submit = {
        if (name.isNotBlank()) {
            onAdd(name, qty.toIntOrNull() ?: 1)
            name = ""
            qty = "1"
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Add extra item") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = qty,
            onValueChange = { qty = it.filter(Char::isDigit) },
            label = { Text("Qty") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.width(72.dp).padding(start = 8.dp),
        )
        IconButton(onClick = submit, enabled = name.isNotBlank()) {
            Icon(Icons.Default.Add, contentDescription = "Add to shopping list")
        }
    }
}

@Composable
private fun ShoppingRow(item: ShoppingItem, onToggle: () -> Unit, onRemove: () -> Unit) {
    Card(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = item.purchased, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (item.purchased) TextDecoration.LineThrough else null,
                )
                Text(
                    "Buy ${item.quantityToBuy}" + if (item.pantryItemId == null) " · extra item" else " · from pantry",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove ${item.name}")
            }
        }
    }
}
