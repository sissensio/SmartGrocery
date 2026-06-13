package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun MuteStoreDialog(
    storeId: Int,
    storeName: String,
    notificationId: Int,
    onDismiss: () -> Unit,
    onConfirm: (reason: String, customComment: String?) -> Unit
) {
    val presets = listOf(
        "In questo posto non fanno scontrini",
        "Mi compare in continuazione",
        "Non frequento questo posto",
        "Altro motivo..."
    )
    
    var selectedIndex by remember { mutableStateOf(0) }
    var customText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Disattiva notifiche per questo posto?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Text(
                    text = "Non riceverai più promemoria di spesa per $storeName. Dicci perché:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presets.forEachIndexed { index, label ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selectedIndex == index) 
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                )
                                .clickable { selectedIndex = index }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedIndex == index,
                                onClick = { selectedIndex = index }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selectedIndex == index)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (selectedIndex == 3) {
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it },
                        label = { Text("Specifica il motivo") },
                        placeholder = { Text("Scrivi qui...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        singleLine = false
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annulla")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalReason = presets[selectedIndex]
                            val comment = if (selectedIndex == 3) customText.trim().takeIf { it.isNotEmpty() } else null
                            onConfirm(finalReason, comment)
                        }
                    ) {
                        Text("Conferma", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
