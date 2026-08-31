package com.example.notifsimplifier.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.notifsimplifier.data.NotifMode

@Composable
private fun SmartFilterRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onInfoClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Default,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onInfoClick) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = "About $label",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(end = 12.dp)
        )
    }
}

private val TTL_OPTIONS = listOf(
    Pair(1,   "1 hour"),
    Pair(6,   "6 hours"),
    Pair(24,  "1 day"),
    Pair(72,  "3 days"),
    Pair(168, "7 days"),
    Pair(720, "30 days"),
    Pair(-1,  "Never")
)

fun ttlLabel(hours: Int): String =
    TTL_OPTIONS.firstOrNull { it.first == hours }?.second ?: "$hours hours"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    otpBypassEnabled: Boolean,
    onOtpBypassChange: (Boolean) -> Unit,
    importantBypassEnabled: Boolean,
    onImportantBypassChange: (Boolean) -> Unit,
    systemFilterEnabled: Boolean,
    onSystemFilterChange: (Boolean) -> Unit,
    ongoingFilterEnabled: Boolean,
    onOngoingFilterChange: (Boolean) -> Unit,
    marketingFilterEnabled: Boolean,
    onMarketingFilterChange: (Boolean) -> Unit,
    defaultMode: NotifMode,
    onDefaultModeChange: (NotifMode) -> Unit,
    onManageApps: () -> Unit,
    onNeverRedirect: () -> Unit,
    reminderEnabled: Boolean,
    onReminderEnabledChange: (Boolean) -> Unit,
    reminderInterval: ReminderInterval,
    onReminderIntervalChange: (ReminderInterval) -> Unit,
    reminderCustomHours: Int,
    onReminderCustomHoursChange: (Int) -> Unit,
    historyTtlHours: Int,
    onHistoryTtlChange: (Int) -> Unit,
    onViewHistory: () -> Unit,
    onGrantNotificationAccess: () -> Unit,
    onOpenAppNotificationSettings: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var showOtpInfo by remember { mutableStateOf(false) }
    var showImportantInfo by remember { mutableStateOf(false) }
    var showSystemInfo by remember { mutableStateOf(false) }
    var showInstallInfo by remember { mutableStateOf(false) }
    var showMarketingInfo by remember { mutableStateOf(false) }
    var showTtlPicker by remember { mutableStateOf(false) }

    if (showTtlPicker) {
        AlertDialog(
            onDismissRequest = { showTtlPicker = false },
            title = { Text("Keep notifications for", fontFamily = FontFamily.Default) },
            text = {
                Column {
                    TTL_OPTIONS.forEach { (hours, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onHistoryTtlChange(hours)
                                    showTtlPicker = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = historyTtlHours == hours,
                                onClick = {
                                    onHistoryTtlChange(hours)
                                    showTtlPicker = false
                                }
                            )
                            Text(
                                text = label,
                                fontFamily = FontFamily.Default,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTtlPicker = false }) {
                    Text("Cancel", fontFamily = FontFamily.Default)
                }
            }
        )
    }

    if (showOtpInfo) {
        AlertDialog(
            onDismissRequest = { showOtpInfo = false },
            title = { Text("Skip OTP notifications", fontFamily = FontFamily.Default) },
            text = {
                Text(
                    text = "When enabled, notifications that look like one-time passwords or " +
                        "verification codes are never redirected — they always appear normally " +
                        "so you don't miss them.\n\n" +
                        "Detection looks for keywords like \"OTP\", \"verification code\", " +
                        "\"security code\", \"passcode\" combined with a 4–8 digit number. " +
                        "This covers most SMS and app-based 2FA messages.",
                    fontFamily = FontFamily.Default,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = { showOtpInfo = false }) {
                    Text("Got it", fontFamily = FontFamily.Default)
                }
            }
        )
    }

    if (showImportantInfo) {
        AlertDialog(
            onDismissRequest = { showImportantInfo = false },
            title = { Text("Always show important updates", fontFamily = FontFamily.Default) },
            text = {
                Text(
                    text = "When enabled, notifications from apps set to Redirect are shown " +
                        "normally if they look time-sensitive or safety-critical.\n\n" +
                        "This covers: delivery & ride-sharing updates (driver arriving, order " +
                        "confirmed, out for delivery), financial alerts (payment, refund, " +
                        "transaction), account security alerts (new sign-in, suspicious activity), " +
                        "and appointments or bookings.\n\n" +
                        "Everything else from those apps still gets redirected to the list.",
                    fontFamily = FontFamily.Default,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = { showImportantInfo = false }) {
                    Text("Got it", fontFamily = FontFamily.Default)
                }
            }
        )
    }

    if (showSystemInfo) {
        AlertDialog(
            onDismissRequest = { showSystemInfo = false },
            title = { Text("Skip system notifications", fontFamily = FontFamily.Default) },
            text = {
                Text(
                    text = "When enabled, notifications from Android system apps — such as " +
                        "Settings, SystemUI, the phone dialer, and other built-in or OEM apps " +
                        "— are never redirected.\n\n" +
                        "Turn this off only if you specifically want to capture system-level " +
                        "notifications in the list.",
                    fontFamily = FontFamily.Default,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = { showSystemInfo = false }) {
                    Text("Got it", fontFamily = FontFamily.Default)
                }
            }
        )
    }

    if (showMarketingInfo) {
        AlertDialog(
            onDismissRequest = { showMarketingInfo = false },
            title = { Text("Redirect marketing spam", fontFamily = FontFamily.Default) },
            text = {
                Text(
                    text = "When enabled, promotional notifications from apps set to Instant are " +
                        "automatically redirected to the list instead of popping up normally.\n\n" +
                        "Detected by keywords like \"% off\", \"deal\", \"craving\", \"limited time\", " +
                        "\"exclusive\" etc. Order tracking notifications (driver arriving, order " +
                        "confirmed, etc.) are never caught — transactional content always takes " +
                        "priority.\n\n" +
                        "Only applies to apps set to Instant. Redirect apps are already captured.",
                    fontFamily = FontFamily.Default,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = { showMarketingInfo = false }) {
                    Text("Got it", fontFamily = FontFamily.Default)
                }
            }
        )
    }

    if (showInstallInfo) {
        AlertDialog(
            onDismissRequest = { showInstallInfo = false },
            title = { Text("Skip ongoing notifications", fontFamily = FontFamily.Default) },
            text = {
                Text(
                    text = "When enabled, persistent status notifications are never redirected. " +
                        "This includes music playback controls, turn-by-turn navigation, " +
                        "app downloads and installs, VPN and hotspot indicators, active calls, " +
                        "and screen recording — anything designed to stay visible continuously " +
                        "rather than as a one-time message.",
                    fontFamily = FontFamily.Default,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = { showInstallInfo = false }) {
                    Text("Got it", fontFamily = FontFamily.Default)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontFamily = FontFamily.Default) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "APPEARANCE",
                fontFamily = FontFamily.Default,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
                        label = {
                            Text(
                                text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                fontFamily = FontFamily.Default
                            )
                        }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 20.dp))

            Text(
                text = "SMART FILTERS",
                fontFamily = FontFamily.Default,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            SmartFilterRow(
                label = "Skip OTP notifications",
                checked = otpBypassEnabled,
                onCheckedChange = onOtpBypassChange,
                onInfoClick = { showOtpInfo = true }
            )
            SmartFilterRow(
                label = "Always show important updates",
                checked = importantBypassEnabled,
                onCheckedChange = onImportantBypassChange,
                onInfoClick = { showImportantInfo = true }
            )
            SmartFilterRow(
                label = "Skip system notifications",
                checked = systemFilterEnabled,
                onCheckedChange = onSystemFilterChange,
                onInfoClick = { showSystemInfo = true }
            )
            SmartFilterRow(
                label = "Skip ongoing notifications",
                checked = ongoingFilterEnabled,
                onCheckedChange = onOngoingFilterChange,
                onInfoClick = { showInstallInfo = true }
            )
            SmartFilterRow(
                label = "Redirect marketing spam",
                checked = marketingFilterEnabled,
                onCheckedChange = onMarketingFilterChange,
                onInfoClick = { showMarketingInfo = true }
            )

            HorizontalDivider()

            Text(
                text = "APPS",
                fontFamily = FontFamily.Default,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onManageApps() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Manage apps",
                    fontFamily = FontFamily.Default,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNeverRedirect() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Never redirect",
                    fontFamily = FontFamily.Default,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Default for new apps",
                    fontFamily = FontFamily.Default,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                SingleChoiceSegmentedButtonRow {
                    listOf(NotifMode.REDIRECT, NotifMode.INSTANT).forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = defaultMode == mode,
                            onClick = { onDefaultModeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                            label = {
                                Text(
                                    text = if (mode == NotifMode.REDIRECT) "Redirect" else "Instant",
                                    fontFamily = FontFamily.Default
                                )
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "REMINDERS",
                fontFamily = FontFamily.Default,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Remind me to check",
                    fontFamily = FontFamily.Default,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = onReminderEnabledChange
                )
            }

            if (reminderEnabled) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                ) {
                    ReminderInterval.entries.forEachIndexed { index, interval ->
                        SegmentedButton(
                            selected = reminderInterval == interval,
                            onClick = { onReminderIntervalChange(interval) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = ReminderInterval.entries.size),
                            label = {
                                Text(
                                    text = interval.name.lowercase().replaceFirstChar { it.uppercase() },
                                    fontFamily = FontFamily.Default
                                )
                            }
                        )
                    }
                }

                if (reminderInterval == ReminderInterval.CUSTOM) {
                    var customHoursText by remember(reminderCustomHours) {
                        mutableStateOf(reminderCustomHours.toString())
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Every",
                            fontFamily = FontFamily.Default,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        OutlinedTextField(
                            value = customHoursText,
                            onValueChange = { text ->
                                val digits = text.filter { it.isDigit() }.take(3)
                                customHoursText = digits
                                val hours = digits.toIntOrNull()?.coerceIn(1, 720)
                                if (hours != null) onReminderCustomHoursChange(hours)
                            },
                            label = { Text("Hours", fontFamily = FontFamily.Default) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.width(100.dp)
                        )
                        Text(
                            text = "hours without opening",
                            fontFamily = FontFamily.Default,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "HISTORY",
                fontFamily = FontFamily.Default,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onViewHistory() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notification history",
                    fontFamily = FontFamily.Default,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTtlPicker = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Keep notifications for",
                    fontFamily = FontFamily.Default,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = ttlLabel(historyTtlHours),
                    fontFamily = FontFamily.Default,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            Text(
                text = "PERMISSIONS",
                fontFamily = FontFamily.Default,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGrantNotificationAccess() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Grant notification access",
                    fontFamily = FontFamily.Default,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAppNotificationSettings() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "App notification settings",
                    fontFamily = FontFamily.Default,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
