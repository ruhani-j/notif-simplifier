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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.notifsimplifier.data.AppDatabase
import com.example.notifsimplifier.data.NotifMode
import com.example.notifsimplifier.service.MyNotificationListener
import com.example.notifsimplifier.service.ReminderWorker
import com.example.notifsimplifier.ui.AppSettingsScreen
import com.example.notifsimplifier.ui.NeverRedirectScreen
import com.example.notifsimplifier.ui.NotificationListScreen
import com.example.notifsimplifier.ui.PermissionScreen
import com.example.notifsimplifier.ui.ReminderInterval
import com.example.notifsimplifier.ui.SetFilterScreen
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

        setContent {
            val hasPermission by hasListenerPermission
            val navController = rememberNavController()
            val notifications by notifDao.getAll().collectAsState(initial = emptyList())
            val pendingPrompts by MyNotificationListener.pendingPrompts.collectAsState()

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

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            LaunchedEffect(Unit) {
                // Surface any apps already in DB with mode=UNSET (e.g. after process death).
                val unset = withContext(Dispatchers.IO) { appSettingDao.getUnsetApps() }
                if (unset.isNotEmpty()) {
                    MyNotificationListener.addPendingPrompts(unset.map { it.packageName })
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

            // Navigate to set-filter prompt whenever a new pending prompt appears.
            val firstPending = pendingPrompts.firstOrNull()
            LaunchedEffect(firstPending) {
                if (firstPending != null) {
                    val current = navController.currentDestination?.route
                    if (current?.startsWith("set_filter/") != true) {
                        navController.navigate("set_filter/$firstPending") {
                            launchSingleTop = true
                        }
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
                                    lifecycleScope.launch { notifDao.clearAll() }
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
                        composable("set_filter/{packageName}") { backStack ->
                            val packageName = backStack.arguments?.getString("packageName") ?: return@composable
                            var displayName by remember { mutableStateOf(packageName) }

                            LaunchedEffect(packageName) {
                                displayName = withContext(Dispatchers.IO) {
                                    appSettingDao.getByPackage(packageName)?.displayName ?: packageName
                                }
                            }

                            SetFilterScreen(
                                displayName = displayName,
                                onChoose = { mode ->
                                    lifecycleScope.launch {
                                        val app = appSettingDao.getByPackage(packageName)
                                        if (app != null) {
                                            appSettingDao.update(app.copy(mode = mode.name))
                                        }
                                        MyNotificationListener.clearPrompt(packageName)
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }
                    }
                }
            }
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
