package com.example.pantryassistant.ui

import android.widget.Toast
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pantryassistant.ui.screens.AddEditItemScreen
import com.example.pantryassistant.ui.screens.PantryListScreen
import com.example.pantryassistant.ui.screens.ShoppingListScreen
import com.example.pantryassistant.worker.LowStockWorker

object Routes {
    const val PANTRY = "pantry"
    const val SHOPPING = "shopping"
    const val ITEM = "item/{itemId}"
    fun item(id: Long) = "item/$id"
}

@Composable
fun PantryAppNav(
    openShoppingRequest: Boolean,
    onShoppingRequestHandled: () -> Unit,
    viewModel: PantryViewModel = viewModel(factory = PantryViewModel.Factory),
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val pantryState by viewModel.pantryState.collectAsStateWithLifecycle()
    val shoppingItems by viewModel.shoppingItems.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute == Routes.PANTRY || currentRoute == Routes.SHOPPING

    // Opened from the low-stock notification: jump straight to the shopping list.
    LaunchedEffect(openShoppingRequest) {
        if (openShoppingRequest) {
            navController.navigate(Routes.SHOPPING) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
            }
            onShoppingRequestHandled()
        }
    }

    fun navigateTab(route: String) = navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.PANTRY,
                        onClick = { navigateTab(Routes.PANTRY) },
                        icon = { Icon(Icons.Default.Kitchen, contentDescription = null) },
                        label = { Text("Pantry") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SHOPPING,
                        onClick = { navigateTab(Routes.SHOPPING) },
                        icon = {
                            val toBuy = shoppingItems.count { !it.purchased }
                            BadgedBox(badge = { if (toBuy > 0) Badge { Text("$toBuy") } }) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null)
                            }
                        },
                        label = { Text("Shopping") },
                    )
                }
            }
        },
    ) { outerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.PANTRY,
            modifier = Modifier
                .padding(bottom = outerPadding.calculateBottomPadding())
                .consumeWindowInsets(outerPadding),
        ) {
            composable(Routes.PANTRY) {
                PantryListScreen(
                    state = pantryState,
                    onAddItem = { navController.navigate(Routes.item(0)) },
                    onEditItem = { id -> navController.navigate(Routes.item(id)) },
                    onIncrement = viewModel::increment,
                    onDecrement = viewModel::decrement,
                    onToggleLow = viewModel::toggleMarkedLow,
                    onDelete = viewModel::delete,
                    onFilterChange = viewModel::setFilter,
                    onShowLowOnlyChange = viewModel::setShowLowOnly,
                    onCheckNow = {
                        LowStockWorker.runNow(context)
                        Toast.makeText(context, "Checking for low-stock items…", Toast.LENGTH_SHORT).show()
                    },
                )
            }
            composable(
                Routes.ITEM,
                arguments = listOf(navArgument("itemId") { type = NavType.LongType }),
            ) { entry ->
                val itemId = entry.arguments?.getLong("itemId") ?: 0L
                LaunchedEffect(itemId) {
                    if (itemId == 0L) viewModel.startNewItem() else viewModel.loadItem(itemId)
                }
                AddEditItemScreen(
                    form = form,
                    onNameChange = viewModel::onNameChange,
                    onQuantityChange = viewModel::onQuantityChange,
                    onThresholdChange = viewModel::onThresholdChange,
                    onCategoryChange = viewModel::onCategoryChange,
                    onMarkedLowChange = viewModel::onMarkedLowChange,
                    onSave = { viewModel.save { navController.popBackStack() } },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SHOPPING) {
                ShoppingListScreen(
                    items = shoppingItems,
                    onTogglePurchased = viewModel::togglePurchased,
                    onRemove = viewModel::removeShoppingItem,
                    onAddManual = viewModel::addManualShoppingItem,
                    onRestock = viewModel::restockPurchased,
                )
            }
        }
    }
}
