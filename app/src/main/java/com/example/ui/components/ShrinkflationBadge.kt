package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.api.ShrinkflationAlertResponse
import com.example.ui.theme.SemanticRed
import com.example.ui.theme.SemanticYellow

@Composable
fun ShrinkflationBadge(alert: ShrinkflationAlertResponse) {
    var showDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_badge")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_badge_scale"
    )

    Card(
        modifier = Modifier
            .padding(top = 8.dp)
            .clickable { showDialog = true }
            .scale(scale),
        colors = CardDefaults.cardColors(containerColor = SemanticRed.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = SemanticRed,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "📉 Shrinkflation Alert",
                style = MaterialTheme.typography.labelSmall,
                color = SemanticRed,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showDialog) {
        ShrinkflationDiagnosticCard(alert = alert, onDismiss = { showDialog = false })
    }
}

@Composable
fun ShrinkflationDiagnosticCard(alert: ShrinkflationAlertResponse, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = SemanticRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Analisi Sgrammatura",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi")
                    }
                }

                Text(
                    text = "${alert.brand} - ${alert.itemName}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "⚖️ Variazione Peso (Prodotto Nascosto)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Confezione ridotta da ${String.format(java.util.Locale.US, "%.3f", alert.originalWeight)}kg a ${String.format(java.util.Locale.US, "%.3f", alert.newWeight)}kg (Taglio netto del -${String.format(java.util.Locale.US, "%.1f", alert.weightReductionPercent)}% di prodotto 🚨)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SemanticRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "💰 Variazione Prezzo (Rincaro Effettivo)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        val rincaroStr = if (alert.priceIncreasePercent > 0) "+${String.format(java.util.Locale.US, "%.1f", alert.priceIncreasePercent)}%" else "Invariato"
                        Text(
                            text = "Vecchio Prezzo: €${String.format(java.util.Locale.US, "%.2f", alert.originalPrice)}  →  Nuovo Prezzo: €${String.format(java.util.Locale.US, "%.2f", alert.newPrice)}\n(Rincaro del prezzo al kg del $rincaroStr)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Text(
                    text = "Pratica commerciale scorretta rilevata nell'ultimo periodo a difesa della tua spesa condivisa.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
