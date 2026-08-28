package com.example.notifsimplifier.ui

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class AppInfo(val packageName: String, val displayName: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeverRedirectScreen(
    neverRedirectPackages: Set<String>,
    onToggle: (packageName: String, blocked: Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { loadInstalledApps(context) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Never redirect", fontFamily = FontFamily.Monospace) },
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
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading apps…", fontFamily = FontFamily.Monospace)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(apps, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(packageName = app.packageName, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = app.displayName,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = app.packageName in neverRedirectPackages,
                            onCheckedChange = { blocked -> onToggle(app.packageName, blocked) }
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(packageName) {
        runCatching {
            val d = context.packageManager.getApplicationIcon(packageName)
            val w = if (d.intrinsicWidth > 0) d.intrinsicWidth else 64
            val h = if (d.intrinsicHeight > 0) d.intrinsicHeight else 64
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            d.setBounds(0, 0, w, h); d.draw(Canvas(bmp))
            bmp.asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(BitmapPainter(bitmap), contentDescription = null, modifier = modifier)
    } else {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

private fun loadInstalledApps(context: Context): List<AppInfo> {
    val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
        addCategory(android.content.Intent.CATEGORY_LAUNCHER)
    }
    val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.queryIntentActivities(intent, 0)
    }
    return resolveInfos
        .map { it.activityInfo }
        .filter { it.packageName != context.packageName }
        .distinctBy { it.packageName }
        .mapNotNull { info ->
            runCatching {
                AppInfo(
                    packageName = info.packageName,
                    displayName = context.packageManager
                        .getApplicationLabel(context.packageManager.getApplicationInfo(info.packageName, 0))
                        .toString()
                )
            }.getOrNull()
        }
        .sortedBy { it.displayName.lowercase() }
}
