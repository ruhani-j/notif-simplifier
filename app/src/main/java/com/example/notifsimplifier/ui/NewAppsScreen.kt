package com.example.notifsimplifier.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.notifsimplifier.data.AppSettingEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAppsScreen(
    newApps: List<AppSettingEntity>,
    onDone: (List<AppSettingEntity>) -> Unit
) {
    val appsState = remember(newApps) { newApps.toMutableStateList() }
    val allSelected = appsState.all { it.isRedirected }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New apps detected", fontFamily = FontFamily.Monospace) }
            )
        },
        bottomBar = {
            Button(
                onClick = { onDone(appsState.toList()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Done", fontFamily = FontFamily.Monospace)
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                Text(
                    text = "Select which apps should redirect their notifications into Notif Simplifier instead of showing normally.",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select all",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = allSelected,
                        onCheckedChange = { checked ->
                            appsState.indices.forEach { i ->
                                appsState[i] = appsState[i].copy(isRedirected = checked)
                            }
                        }
                    )
                }
                HorizontalDivider()
            }

            itemsIndexed(appsState, key = { _, app -> app.packageName }) { i, app ->
                AppRow(
                    app = app,
                    onToggle = { appsState[i] = app.copy(isRedirected = it) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun AppRow(app: AppSettingEntity, onToggle: (Boolean) -> Unit) {
    val context = LocalContext.current
    val icon = remember(app.packageName) {
        runCatching {
            val d = context.packageManager.getApplicationIcon(app.packageName)
            val w = if (d.intrinsicWidth > 0) d.intrinsicWidth else 64
            val h = if (d.intrinsicHeight > 0) d.intrinsicHeight else 64
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            d.setBounds(0, 0, w, h)
            d.draw(Canvas(bmp))
            bmp.asImageBitmap()
        }.getOrNull()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Image(
                painter = BitmapPainter(icon),
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = app.displayName,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = app.isRedirected,
            onCheckedChange = onToggle
        )
    }
}
