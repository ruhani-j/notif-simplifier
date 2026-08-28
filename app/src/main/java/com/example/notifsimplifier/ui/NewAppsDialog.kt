package com.example.notifsimplifier.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.notifsimplifier.data.AppSettingEntity

@Composable
fun NewAppsDialog(
    newApps: List<AppSettingEntity>,
    onDismiss: (List<AppSettingEntity>) -> Unit
) {
    val appsState = remember(newApps) { newApps.toMutableStateList() }
    val allSelected = appsState.all { it.isRedirected }

    AlertDialog(
        onDismissRequest = { onDismiss(appsState.toList()) },
        title = {
            Text("New apps installed", fontFamily = FontFamily.Monospace)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "These apps were installed since your last visit. " +
                        "Turn on redirect to capture their notifications in the plain list instead of showing them normally.",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))

                // Select all row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select all",
                        modifier = Modifier.weight(1f),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium
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
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                appsState.forEachIndexed { i, app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = app.displayName,
                            modifier = Modifier.weight(1f),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = app.isRedirected,
                            onCheckedChange = { appsState[i] = app.copy(isRedirected = it) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss(appsState.toList()) }) {
                Text("Done", fontFamily = FontFamily.Monospace)
            }
        }
    )
}
