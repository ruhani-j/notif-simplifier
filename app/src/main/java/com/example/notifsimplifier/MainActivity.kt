package com.example.notifsimplifier

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.lifecycleScope
import com.example.notifsimplifier.data.AppDatabase
import com.example.notifsimplifier.ui.NotificationListScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dao = AppDatabase.getInstance(applicationContext).notificationDao()

        setContent {
            val notifications by dao.getAll().collectAsState(initial = emptyList())

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NotificationListScreen(
                        notifications = notifications,
                        onOpenNotificationAccessSettings = { openNotificationAccessSettings() },
                        onClearAll = { lifecycleScope.launch { dao.clearAll() } }
                    )
                }
            }
        }
    }

    private fun openNotificationAccessSettings() {
        // Android requires the user to manually flip this on — it can't be
        // requested as a normal runtime permission.
        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
    }
}
