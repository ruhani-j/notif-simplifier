package com.example.notifsimplifier.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.notifsimplifier.data.AppSettingDao
import com.example.notifsimplifier.data.AppSettingEntity
import com.example.notifsimplifier.data.NotifMode
import kotlinx.coroutines.launch

private val BG = Color.Black
private val TEXT = Color.White
private val MUTED = Color(0xFF888888)
private val DIVIDER = Color(0xFF222222)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    appSettingDao: AppSettingDao,
    onNavigateBack: () -> Unit
) {
    val apps by appSettingDao.getAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val allRedirect = apps.isNotEmpty() && apps.all { it.mode == NotifMode.REDIRECT.name }
    val allInstant = apps.isNotEmpty() && apps.all { it.mode == NotifMode.INSTANT.name }

    Scaffold(
        containerColor = BG,
        topBar = {
            TopAppBar(
                title = {
                    Text("Manage apps", fontFamily = FontFamily.Default, color = TEXT)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TEXT)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BG)
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
                    text = "No apps yet.\nNotifications from apps will appear here as they arrive.",
                    color = MUTED,
                    fontFamily = FontFamily.Default,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                item {
                    AppModeRow(
                        name = "All apps",
                        currentMode = when {
                            allRedirect -> NotifMode.REDIRECT
                            allInstant -> NotifMode.INSTANT
                            else -> null
                        },
                        bold = true,
                        onModeSelect = { mode ->
                            scope.launch {
                                apps.forEach { appSettingDao.update(it.copy(mode = mode.name)) }
                            }
                        }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 0.5.dp)
                }
                items(apps, key = { it.packageName }) { app ->
                    AppModeRow(
                        name = app.displayName,
                        currentMode = runCatching { NotifMode.valueOf(app.mode) }.getOrDefault(NotifMode.UNSET),
                        bold = false,
                        onModeSelect = { mode ->
                            scope.launch { appSettingDao.update(app.copy(mode = mode.name)) }
                        }
                    )
                    HorizontalDivider(color = DIVIDER, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun AppModeRow(
    name: String,
    currentMode: NotifMode?,
    bold: Boolean,
    onModeSelect: (NotifMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BG)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            color = TEXT,
            fontFamily = FontFamily.Default,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeChip(
                label = "Redirect",
                selected = currentMode == NotifMode.REDIRECT,
                onClick = { onModeSelect(NotifMode.REDIRECT) }
            )
            ModeChip(
                label = "Instant",
                selected = currentMode == NotifMode.INSTANT,
                onClick = { onModeSelect(NotifMode.INSTANT) }
            )
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) Color.White else Color.Transparent)
            .border(0.5.dp, if (selected) Color.White else MUTED, shape)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else MUTED,
            fontFamily = FontFamily.Default,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
