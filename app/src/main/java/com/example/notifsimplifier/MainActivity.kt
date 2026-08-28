package com.example.notifsimplifier

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
import com.example.notifsimplifier.service.MyNotificationListener
import com.example.notifsimplifier.ui.AppSettingsScreen
import com.example.notifsimplifier.ui.NewAppsDialog
import com.example.notifsimplifier.ui.NotificationListScreen
import com.example.notifsimplifier.ui.SettingsScreen
import com.example.notifsimplifier.ui.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getInstance(applicationContext)
        val notifDao = db.notificationDao()
        val appSettingDao = db.appSettingDao()
        val prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)

        setContent {
            val navController = rememberNavController()
            val notifications by notifDao.getAll().collectAsState(initial = emptyList())

            var newApps by remember { mutableStateOf<List<AppSettingEntity>?>(null) }

            var themeMode by remember {
                mutableStateOf(
                    ThemeMode.valueOf(
                        prefs.getString("theme", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
                    )
                )
            }

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

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

            MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavHost(navController = navController, startDestination = "list") {
                        composable("list") {
                            NotificationListScreen(
                                notifications = notifications,
                                onClearAll = {
                                    lifecycleScope.launch { notifDao.clearAll() }
                                    MyNotificationListener.pendingIntents.clear()
                                },
                                onOpenSettings = { navController.navigate("settings") },
                                onNotificationClick = { notif -> fireNotificationIntent(notif.id, notif.appName) },
                                onDismissNotification = { notif ->
                                    lifecycleScope.launch { notifDao.deleteById(notif.id) }
                                    MyNotificationListener.pendingIntents.remove(notif.id)
                                }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                themeMode = themeMode,
                                onThemeModeChange = { mode ->
                                    themeMode = mode
                                    prefs.edit().putString("theme", mode.name).apply()
                                },
                                onManageApps = { navController.navigate("manage_apps") },
                                onGrantNotificationAccess = { openNotificationAccessSettings() },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("manage_apps") {
                            AppSettingsScreen(
                                appSettingDao = appSettingDao,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }

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

    private fun fireNotificationIntent(notifId: Long, packageName: String) {
        val pi = MyNotificationListener.pendingIntents[notifId]
        if (pi != null) {
            try {
                pi.send()
                return
            } catch (_: PendingIntent.CanceledException) {
                MyNotificationListener.pendingIntents.remove(notifId)
            }
        }
        packageManager.getLaunchIntentForPackage(packageName)
            ?.let { startActivity(it) }
    }

    private fun openNotificationAccessSettings() {
        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
    }
}
