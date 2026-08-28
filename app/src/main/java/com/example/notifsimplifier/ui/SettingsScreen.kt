package com.example.notifsimplifier.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.unit.dp

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
            fontFamily = FontFamily.Monospace,
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
    onManageApps: () -> Unit,
    onNeverRedirect: () -> Unit,
    onGrantNotificationAccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var showOtpInfo by remember { mutableStateOf(false) }
    var showImportantInfo by remember { mutableStateOf(false) }
    var showSystemInfo by remember { mutableStateOf(false) }
    var showInstallInfo by remember { mutableStateOf(false) }
    var showMarketingInfo by remember { mutableStateOf(false) }

    if (showOtpInfo) {
        AlertDialog(
            onDismissRequest = { showOtpInfo = false },
            title = { Text("Skip OTP notifications", fontFamily = FontFamily.Monospace) },
            text = {
                Text(
                    text = "When enabled, notifications that look like one-time passwords or " +
                        "verification codes are never redirected — they always appear normally " +
                        "so you don't miss them.\n\n" +
                        "Detection looks for keywords like \"OTP\", \"verification code\", " +
                        "\"security code\", \"passcode\" combined with a 4–8 digit number. " +
                        "This covers most SMS and app-based 2FA messages.",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = { showOtpInfo = false }) {
                    Text("Got it", fontFamily = FontFamily.Monospace)
                }
            }
        )
    }

    if (showImportantInfo) {
        AlertDialog(
            onDismissRequest = { showImportantInfo = false },
            title = { Text("Always show important updates", fontFamily = FontFamily.Monospace) },
            text = {
                Text(
                    text = "When enabled, notifications from apps set to Redirect are shown " +
                        "normally if they look time-sensitive or safety-critical.\n\n" +
                        "This covers: delivery & ride-sharing updates (driver arriving, order " +
                        "confirmed, out for delivery), financial alerts (payment, refund, " +
                        "transaction), account security alerts (new sign-in, suspicious activity), " +
                        "and appointments or bookings.\n\n" +
                        "Everything else from those apps still gets redirected to the list.",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = { showImportantInfo = false }) {
                    Text("Got it", fontFamily = FontFamily.Monospace)
                }
            }
        )
    }

    if (showSystemInfo) {
        AlertDialog(
            onDismissRequest = { showSystemInfo = false },
            title = { Text("Skip system notifications", fontFamily = FontFamily.Monospace) },
            text = {
                Text(
                    text = "When enabled, notifications from Android system apps — such as " +
                        "Settings, SystemUI, the phone dialer, and other built-in or OEM apps " +
                        "— are never redirected.\n\n" +
                        "Turn this off only if you specifically want to capture system-level " +
                        "notifications in the list.",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = { showSystemInfo = false }) {
                    Text("Got it", fontFamily = FontFamily.Monospace)
                }
            }
        )
    }

    if (showMarketingInfo) {
        AlertDialog(
            onDismissRequest = { showMarketingInfo = false },
            title = { Text("Redirect marketing spam", fontFamily = FontFamily.Monospace) },
            text = {
                Text(
                    text = "When enabled, promotional notifications from apps set to Instant are " +
                        "automatically redirected to the list instead of popping up normally.\n\n" +
                        "Detected by keywords like \"% off\", \"deal\", \"craving\", \"limited time\", " +
                        "\"exclusive\" etc. Order tracking notifications (driver arriving, order " +
                        "confirmed, etc.) are never caught — transactional content always takes " +
                        "priority.\n\n" +
                        "Only applies to apps set to Instant. Redirect apps are already captured.",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = { showMarketingInfo = false }) {
                    Text("Got it", fontFamily = FontFamily.Monospace)
                }
            }
        )
    }

    if (showInstallInfo) {
        AlertDialog(
            onDismissRequest = { showInstallInfo = false },
            title = { Text("Skip ongoing notifications", fontFamily = FontFamily.Monospace) },
            text = {
                Text(
                    text = "When enabled, persistent status notifications are never redirected. " +
                        "This includes music playback controls, turn-by-turn navigation, " +
                        "app downloads and installs, VPN and hotspot indicators, active calls, " +
                        "and screen recording — anything designed to stay visible continuously " +
                        "rather than as a one-time message.",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = { showInstallInfo = false }) {
                    Text("Got it", fontFamily = FontFamily.Monospace)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontFamily = FontFamily.Monospace) },
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
        ) {
            Text(
                text = "APPEARANCE",
                fontFamily = FontFamily.Monospace,
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
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 20.dp))

            Text(
                text = "SMART FILTERS",
                fontFamily = FontFamily.Monospace,
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
                fontFamily = FontFamily.Monospace,
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
                    fontFamily = FontFamily.Monospace,
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
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider()

            Text(
                text = "PERMISSIONS",
                fontFamily = FontFamily.Monospace,
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
                    fontFamily = FontFamily.Monospace,
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
