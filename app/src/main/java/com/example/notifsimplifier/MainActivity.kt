package com.example.notifsimplifier

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.notifsimplifier.data.AppDatabase
import com.example.notifsimplifier.data.AppSettingEntity
import com.example.notifsimplifier.ui.AppSettingsScreen
import com.example.notifsimplifier.ui.NewAppsDialog
import com.example.notifsimplifier.ui.NotificationListScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getInstance(applicationContext)
        val notifDao = db.notificationDao()
        val appSettingDao = db.appSettingDao()

        setContent {
            val navController = rememberNavController()
            val notifications by notifDao.getAll().collectAsState(initial = emptyList())

            // Non-null list triggers the new-apps dialog; null means no dialog.
            var newApps by remember { mutableStateOf<List<AppSettingEntity>?>(null) }

            // Off-main-thread diff of installed apps vs known apps on each launch.
            LaunchedEffect(Unit) {
                val fresh = withContext(Dispatchers.IO) {
                    val installed = getInstalledUserApps()
                    val known = appSettingDao.getAllKnownPackageNames().toSet()
                    val diff = installed.filter { it.packageName !in known }
                    diff.forEach { appSettingDao.insertIfAbsent(it) }
                    diff
                }
                if (fresh.isNotEmpty()) newApps = fresh
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavHost(navController = navController, startDestination = "list") {
                        composable("list") {
                            NotificationListScreen(
                                notifications = notifications,
                                onOpenNotificationAccessSettings = { openNotificationAccessSettings() },
                                onClearAll = { lifecycleScope.launch { notifDao.clearAll() } },
                                onOpenSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("settings") {
                            AppSettingsScreen(
                                appSettingDao = appSettingDao,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }

                    // Overlays the nav host — AlertDialog renders above all content.
                    newApps?.let { apps ->
                        NewAppsDialog(
                            newApps = apps,
                            onDismiss = { updated ->
                                lifecycleScope.launch {
                                    updated.forEach { appSettingDao.update(it) }
                                }
                                newApps = null
                            }
                        )
                    }
                }
            }
        }
    }

    private fun getInstalledUserApps(): List<AppSettingEntity> {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }
        return resolveInfos
            .map { it.activityInfo }
            .filter { it.packageName != "com.example.notifsimplifier" }
            .distinctBy { it.packageName }
            .mapNotNull { info ->
                runCatching {
                    AppSettingEntity(
                        packageName = info.packageName,
                        displayName = packageManager
                            .getApplicationLabel(packageManager.getApplicationInfo(info.packageName, 0))
                            .toString()
                    )
                }.getOrNull()
            }
    }

    private fun openNotificationAccessSettings() {
        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
    }
}
