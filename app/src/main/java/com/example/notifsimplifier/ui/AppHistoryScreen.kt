package com.example.notifsimplifier.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.notifsimplifier.data.AppSettingDao
import com.example.notifsimplifier.data.AppSettingEntity
import kotlinx.coroutines.launch

private val TTL_OPTIONS = listOf(
    Pair(1,   "1 hour"),
    Pair(6,   "6 hours"),
    Pair(24,  "1 day"),
    Pair(72,  "3 days"),
    Pair(168, "7 days"),
    Pair(720, "30 days"),
    Pair(-1,  "Never")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHistoryScreen(
    appSettingDao: AppSettingDao,
    onNavigateBack: () -> Unit
) {
    val apps by appSettingDao.getAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var dialogApp by remember { mutableStateOf<AppSettingEntity?>(null) }
    var pendingTtl by remember { mutableIntStateOf(24) }

    dialogApp?.let { app ->
        AlertDialog(
            onDismissRequest = { dialogApp = null },
            title = { Text("Keep history for", fontFamily = FontFamily.Default) },
            text = {
                Column {
                    TTL_OPTIONS.forEach { (hours, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pendingTtl = hours }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = pendingTtl == hours,
                                onClick = { pendingTtl = hours }
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
                TextButton(onClick = {
                    scope.launch {
                        appSettingDao.update(app.copy(collectHistory = true, historyTtlHours = pendingTtl))
                    }
                    dialogApp = null
                }) {
                    Text("Save", fontFamily = FontFamily.Default)
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogApp = null }) {
                    Text("Cancel", fontFamily = FontFamily.Default)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History apps", fontFamily = FontFamily.Default) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (apps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No apps yet.\nApps will appear here once notifications arrive.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Default,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                item {
                    Text(
                        text = "Choose which apps save notifications to your history log, and how long to keep them.",
                        fontFamily = FontFamily.Default,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    HorizontalDivider()
                }
                items(apps, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.displayName,
                                fontFamily = FontFamily.Default,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (app.collectHistory) {
                                Text(
                                    text = ttlLabel(app.historyTtlHours),
                                    fontFamily = FontFamily.Default,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .clickable {
                                            pendingTtl = app.historyTtlHours.takeIf { it != -2 } ?: 24
                                            dialogApp = app
                                        }
                                        .padding(top = 2.dp)
                                )
                            }
                        }
                        Switch(
                            checked = app.collectHistory,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    pendingTtl = app.historyTtlHours.takeIf { it != -2 } ?: 24
                                    dialogApp = app
                                } else {
                                    scope.launch {
                                        appSettingDao.update(app.copy(collectHistory = false))
                                    }
                                }
                            }
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
