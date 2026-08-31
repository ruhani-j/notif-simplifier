package com.example.notifsimplifier

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Parcel
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
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.notifsimplifier.data.AppDatabase
import com.example.notifsimplifier.data.AppSettingEntity
import com.example.notifsimplifier.data.NotifMode
import com.example.notifsimplifier.service.CleanupWorker
import com.example.notifsimplifier.service.MyNotificationListener
import com.example.notifsimplifier.service.ReminderWorker
import com.example.notifsimplifier.ui.AppHistoryScreen
import com.example.notifsimplifier.ui.AppSettingsScreen
import com.example.notifsimplifier.ui.NeverRedirectScreen
import com.example.notifsimplifier.ui.NotificationHistoryScreen
import com.example.notifsimplifier.ui.NotificationListScreen
import com.example.notifsimplifier.ui.PermissionScreen
import com.example.notifsimplifier.ui.ReminderInterval
import com.example.notifsimplifier.ui.SettingsScreen
import com.example.notifsimplifier.ui.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences
    private val hasListenerPermission = mutableStateOf(false)

    private fun isListenerEnabled() =
        NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)

    override fun onResume() {
        super.onResume()
        hasListenerPermission.value = isListenerEnabled()
        scheduleOrCancelReminder(
            enabled = prefs.getBoolean("reminder_enabled", false),
            interval = prefs.getString("reminder_interval", "DAILY") ?: "DAILY",
            customHours = prefs.getInt("reminder_custom_hours", 24)
        )
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED &&
            prefs.getBoolean("reminder_enabled", false)
        ) {
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 0)
        }
    }

    private fun scheduleCleanupWorker() {
        val request = PeriodicWorkRequestBuilder<CleanupWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            CleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun scheduleOrCancelReminder(enabled: Boolean, interval: String, customHours: Int) {
        val wm = WorkManager.getInstance(this)
        if (!enabled) {
            wm.cancelUniqueWork(ReminderWorker.WORK_NAME)
            return
        }
        val hours = when (interval) {
            "DAILY" -> 24L
            "WEEKLY" -> 168L
            else -> customHours.toLong().coerceIn(1, 720)
        }
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(hours, TimeUnit.HOURS)
            .build()
        wm.enqueueUniqueWork(ReminderWorker.WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getInstance(applicationContext)
        val notifDao = db.notificationDao()
        val appSettingDao = db.appSettingDao()
        prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)

        hasListenerPermission.value = isListenerEnabled()
        scheduleCleanupWorker()

        setContent {
            val hasPermission by hasListenerPermission
            val navController = rememberNavController()
            val notifications by notifDao.getAll().collectAsState(initial = emptyList())
            val collectedNotifications by notifDao.getCollected().collectAsState(initial = emptyList())

            var themeMode by remember {
                mutableStateOf(
                    ThemeMode.valueOf(
                        prefs.getString("theme", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
                    )
                )
            }

            var otpBypassEnabled by remember { mutableStateOf(prefs.getBoolean("otp_bypass", true)) }
            var systemFilterEnabled by remember { mutableStateOf(prefs.getBoolean("system_filter", true)) }
            var ongoingFilterEnabled by remember { mutableStateOf(prefs.getBoolean("ongoing_filter", true)) }
            var importantBypassEnabled by remember { mutableStateOf(prefs.getBoolean("important_bypass", true)) }
            var marketingFilterEnabled by remember { mutableStateOf(prefs.getBoolean("marketing_filter", false)) }
            var reminderEnabled by remember { mutableStateOf(prefs.getBoolean("reminder_enabled", false)) }
            var reminderInterval by remember {
                mutableStateOf(
                    runCatching {
                        ReminderInterval.valueOf(prefs.getString("reminder_interval", "DAILY") ?: "DAILY")
                    }.getOrDefault(ReminderInterval.DAILY)
                )
            }
            var reminderCustomHours by remember { mutableStateOf(prefs.getInt("reminder_custom_hours", 24)) }
            var neverRedirectPackages by remember {
                mutableStateOf(prefs.getStringSet("never_redirect_packages", emptySet()) ?: emptySet())
            }
            var defaultMode by remember {
                mutableStateOf(
                    runCatching {
                        NotifMode.valueOf(prefs.getString("new_app_default", NotifMode.REDIRECT.name) ?: NotifMode.REDIRECT.name)
                    }.getOrDefault(NotifMode.REDIRECT)
                )
            }
            var historyTtlHours by remember { mutableStateOf(prefs.getInt("history_ttl_hours", 24)) }

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            LaunchedEffect(Unit) {
                val defaultModeStr = prefs.getString("new_app_default", NotifMode.REDIRECT.name) ?: NotifMode.REDIRECT.name
                val defaultMode = runCatching { NotifMode.valueOf(defaultModeStr) }.getOrDefault(NotifMode.REDIRECT)

                withContext(Dispatchers.IO) {
                    // Fix any existing UNSET rows from older installs.
                    appSettingDao.getUnsetApps().forEach { app ->
                        appSettingDao.update(app.copy(mode = defaultMode.name))
                    }
                    // Pre-populate all installed launcher apps.
                    getInstalledUserApps().forEach { app ->
                        appSettingDao.insertIfAbsent(app.copy(mode = defaultMode.name))
                    }
                }

                // Auto-add known authenticator / 2FA apps to Never Redirect.
                val knownAuthenticators = listOf(
                    "com.google.android.apps.authenticator2", // Google Authenticator
                    "com.duosecurity.duomobile",              // Duo Mobile
                    "com.microsoft.authenticator",            // Microsoft Authenticator
                    "com.authy.authy",                        // Authy
                    "com.lastpass.authenticator",             // LastPass Authenticator
                    "org.fedorahosted.freeotp",               // FreeOTP
                    "org.fedorahosted.freeotp2",              // FreeOTP+
                    "com.aegisvault.aegis",                   // Aegis
                    "com.bitwarden.mobile",                   // Bitwarden
                    "com.yubico.yubioath",                    // Yubico Authenticator
                    "com.onelogin.mobileotp",                 // OneLogin
                    "com.okta.android.auth",                  // Okta Verify
                    "com.azure.authenticator",                // Azure Authenticator (alt pkg)
                )
                val installed = withContext(Dispatchers.IO) {
                    knownAuthenticators.filter { pkg ->
                        runCatching { packageManager.getPackageInfo(pkg, 0); true }.getOrDefault(false)
                    }
                }
                if (installed.isNotEmpty()) {
                    val current = prefs.getStringSet("never_redirect_packages", emptySet()) ?: emptySet()
                    val updated = current + installed
                    if (updated != current) {
                        prefs.edit().putStringSet("never_redirect_packages", updated).apply()
                        neverRedirectPackages = updated
                    }
                }
            }

            MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (!hasPermission) {
                        PermissionScreen(onGrantAccess = { openNotificationAccessSettings() })
                        return@Surface
                    }
                    NavHost(navController = navController, startDestination = "list") {
                        composable("list") {
                            NotificationListScreen(
                                notifications = notifications,
                                onClearAll = {
                                    lifecycleScope.launch { notifDao.clearRedirect() }
                                    MyNotificationListener.pendingIntents.clear()
                                },
                                onOpenSettings = { navController.navigate("settings") },
                                onNotificationClick = { notif -> fireNotificationIntent(notif.id, notif.appName, notif.intentBytes) },
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
                                otpBypassEnabled = otpBypassEnabled,
                                onOtpBypassChange = { enabled ->
                                    otpBypassEnabled = enabled
                                    prefs.edit().putBoolean("otp_bypass", enabled).apply()
                                },
                                importantBypassEnabled = importantBypassEnabled,
                                onImportantBypassChange = { enabled ->
                                    importantBypassEnabled = enabled
                                    prefs.edit().putBoolean("important_bypass", enabled).apply()
                                },
                                systemFilterEnabled = systemFilterEnabled,
                                onSystemFilterChange = { enabled ->
                                    systemFilterEnabled = enabled
                                    prefs.edit().putBoolean("system_filter", enabled).apply()
                                },
                                ongoingFilterEnabled = ongoingFilterEnabled,
                                onOngoingFilterChange = { enabled ->
                                    ongoingFilterEnabled = enabled
                                    prefs.edit().putBoolean("ongoing_filter", enabled).apply()
                                },
                                marketingFilterEnabled = marketingFilterEnabled,
                                onMarketingFilterChange = { enabled ->
                                    marketingFilterEnabled = enabled
                                    prefs.edit().putBoolean("marketing_filter", enabled).apply()
                                },
                                defaultMode = defaultMode,
                                onDefaultModeChange = { mode ->
                                    defaultMode = mode
                                    prefs.edit().putString("new_app_default", mode.name).apply()
                                },
                                onManageApps = { navController.navigate("manage_apps") },
                                onNeverRedirect = { navController.navigate("never_redirect") },
                                historyTtlHours = historyTtlHours,
                                onHistoryTtlChange = { hours ->
                                    historyTtlHours = hours
                                    prefs.edit().putInt("history_ttl_hours", hours).apply()
                                },
                                onViewHistory = { navController.navigate("history") },
                                onOpenSystemNotifHistory = {
                                    startActivity(Intent("android.settings.NOTIFICATION_HISTORY"))
                                },
                                onOpenHistoryApps = { navController.navigate("history_apps") },
                                reminderEnabled = reminderEnabled,
                                onReminderEnabledChange = { enabled ->
                                    reminderEnabled = enabled
                                    prefs.edit().putBoolean("reminder_enabled", enabled).apply()
                                    scheduleOrCancelReminder(enabled, reminderInterval.name, reminderCustomHours)
                                    if (enabled && Build.VERSION.SDK_INT >= 33 &&
                                        checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 0)
                                    }
                                },
                                reminderInterval = reminderInterval,
                                onReminderIntervalChange = { interval ->
                                    reminderInterval = interval
                                    prefs.edit().putString("reminder_interval", interval.name).apply()
                                    if (reminderEnabled) {
                                        scheduleOrCancelReminder(true, interval.name, reminderCustomHours)
                                    }
                                },
                                reminderCustomHours = reminderCustomHours,
                                onReminderCustomHoursChange = { hours ->
                                    reminderCustomHours = hours
                                    prefs.edit().putInt("reminder_custom_hours", hours).apply()
                                    if (reminderEnabled && reminderInterval == ReminderInterval.CUSTOM) {
                                        scheduleOrCancelReminder(true, "CUSTOM", hours)
                                    }
                                },
                                onGrantNotificationAccess = { openNotificationAccessSettings() },
                                onOpenAppNotificationSettings = { openAppNotificationSettings() },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("manage_apps") {
                            AppSettingsScreen(
                                appSettingDao = appSettingDao,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("never_redirect") {
                            NeverRedirectScreen(
                                neverRedirectPackages = neverRedirectPackages,
                                onToggle = { pkg, blocked ->
                                    val updated = if (blocked) neverRedirectPackages + pkg
                                                 else neverRedirectPackages - pkg
                                    neverRedirectPackages = updated
                                    prefs.edit().putStringSet("never_redirect_packages", updated).apply()
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("history") {
                            NotificationHistoryScreen(
                                notifications = collectedNotifications,
                                onClearAll = {
                                    lifecycleScope.launch { notifDao.clearCollected() }
                                },
                                onNavigateBack = { navController.popBackStack() },
                                onNotificationClick = { notif -> fireNotificationIntent(notif.id, notif.appName, notif.intentBytes) },
                                onDismissNotification = { notif ->
                                    lifecycleScope.launch { notifDao.deleteById(notif.id) }
                                    MyNotificationListener.pendingIntents.remove(notif.id)
                                }
                            )
                        }
                        composable("history_apps") {
                            AppHistoryScreen(
                                appSettingDao = appSettingDao,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
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

    private fun fireNotificationIntent(notifId: Long, packageName: String, intentBytes: ByteArray?) {
        val pi = MyNotificationListener.pendingIntents[notifId]
            ?: intentBytes?.let { restorePendingIntent(it) }
        if (pi != null) {
            try {
                pi.send()
                moveTaskToBack(true)
                return
            } catch (_: PendingIntent.CanceledException) {
                MyNotificationListener.pendingIntents.remove(notifId)
            }
        }
        packageManager.getLaunchIntentForPackage(packageName)
            ?.let { startActivity(it) }
        moveTaskToBack(true)
    }

    private fun restorePendingIntent(bytes: ByteArray): PendingIntent? {
        return try {
            val parcel = Parcel.obtain()
            parcel.unmarshall(bytes, 0, bytes.size)
            parcel.setDataPosition(0)
            PendingIntent.CREATOR.createFromParcel(parcel).also { parcel.recycle() }
        } catch (_: Exception) { null }
    }

    private fun openNotificationAccessSettings() {
        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
    }

    private fun openAppNotificationSettings() {
        startActivity(Intent(android.provider.Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS))
    }
}
