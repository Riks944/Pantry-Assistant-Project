package com.example.pantryassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.pantryassistant.ui.PantryAppNav
import com.example.pantryassistant.ui.theme.PantryTheme
import com.example.pantryassistant.worker.NotificationHelper

class MainActivity : ComponentActivity() {

    private var openShoppingRequest by mutableStateOf(false)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        askNotificationPermission()

        setContent {
            PantryTheme {
                PantryAppNav(
                    openShoppingRequest = openShoppingRequest,
                    onShoppingRequestHandled = { openShoppingRequest = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(NotificationHelper.EXTRA_OPEN_SHOPPING, false) == true) {
            openShoppingRequest = true
        }
    }

    /** Android 13+ needs runtime permission to show the low-stock notification. */
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
