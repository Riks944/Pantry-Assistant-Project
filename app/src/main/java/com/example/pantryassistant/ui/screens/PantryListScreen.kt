package com.example.pantryassistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.pantryassistant.data.Category
import com.example.pantryassistant.data.PantryItem
import com.example.pantryassistant.data.isLowStock
import com.example.pantryassistant.ui.PantryUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryListScreen(
    state: PantryUiState,
    onAddItem: () -> Unit,
    onEditItem: (Long) -> Unit,
    onIncrement: (PantryItem) -> Unit,
    onDecrement: (PantryItem) -> Unit,
    onToggleLow: (PantryItem) -> Unit,
    onDelete: (PantryItem) -> Unit,
    onFilterChange: (Category?) -> Unit,
    onShowLowOnlyChange: (Boolean) -> Unit,
    onCheckNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<PantryItem?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("My Pantry") },
                actions = {
                    IconButton(onClick = onCheckNow) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = "Run low-stock check now")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItem) {
                Icon(Icons.Default.Add, contentDescription = "Add item")
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.lowStockCount > 0) {
                LowStockBanner(count = state.lowStockCount)
            }
            FilterRow(state, onFilterChange, onShowLowOnlyChange)

            if (state.items.isEmpty()) {
                EmptyState(
                    text = if (state.filter == null && !state.showLowOnly)
                        "Your pantry is empty.\nTap + to add your first item."
                    else "No items match this filter.",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it.id }) { item ->
                        PantryItemCard(
                            item = item,
                            onClick = { onEditItem(item.id) },
                            onIncrement = { onIncrement(item) },
                            onDecrement = { onDecrement(item) },
                            onToggleLow = { onToggleLow(item) },
                            onDelete = { pendingDelete = item },
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${item.name}?") },
            text = { Text("It will also be removed from the shopping list.") },
            confirmButton = {
                TextButton(onClick = { onDelete(item); pendingDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun LowStockBanner(count: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (count == 1) "1 item is running low" else "$count items are running low",
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun FilterRow(
    state: PantryUiState,
    onFilterChange: (Category?) -> Unit,
    onShowLowOnlyChange: (Boolean) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = state.showLowOnly,
                onClick = { onShowLowOnlyChange(!state.showLowOnly) },
                label = { Text("Low stock") },
            )
        }
        item {
            FilterChip(
                selected = state.filter == null,
                onClick = { onFilterChange(null) },
                label = { Text("All") },
            )
        }
        items(Category.entries) { cat ->
            FilterChip(
                selected = state.filter == cat,
                onClick = { onFilterChange(if (state.filter == cat) null else cat) },
                label = { Text(cat.label) },
            )
        }
    }
}

@Composable
private fun PantryItemCard(
    item: PantryItem,
    onClick: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onToggleLow: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${item.category.label} · low at ${item.lowStockThreshold}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.isLowStock) {
                    AssistChip(
                        onClick = onToggleLow,
                        label = { Text(if (item.markedLow) "Marked low" else "Low stock") },
                        leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            labelColor = MaterialTheme.colorScheme.onErrorContainer,
                            leadingIconContentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        border = null,
                    )
                }
            }
            FilledTonalIconButton(onClick = onDecrement, enabled = item.quantity > 0) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease ${item.name}")
            }
            Text(
                item.quantity.toString(),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 36.dp),
            )
            FilledTonalIconButton(onClick = onIncrement) {
                Icon(Icons.Default.Add, contentDescription = "Increase ${item.name}")
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(if (item.markedLow) "Unmark low stock" else "Mark as low stock") },
                        onClick = { menuOpen = false; onToggleLow() },
                    )
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { menuOpen = false; onClick() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; onDelete() })
                }
            }
        }
    }
}

@Composable
fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
