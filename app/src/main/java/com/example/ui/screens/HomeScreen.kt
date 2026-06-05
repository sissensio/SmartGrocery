package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import com.example.data.PendingReceipt
import com.example.ui.theme.SemanticBlueInfo
import com.example.ui.theme.SemanticRed
import com.example.ui.theme.SemanticYellow
import com.example.ui.viewmodel.GroceryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: GroceryViewModel,
    onNavigateToScanner: () -> Unit,
    onNavigateToReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pendingBacklog by viewModel.pendingReceipts.collectAsState()
    val webSocketMessage by viewModel.webSocketSyncMessage.collectAsState()
    val geofenceTriggerStore by viewModel.activeGeofenceNotification.collectAsState()
    val isOffline by viewModel.isOfflineMode.collectAsState()
    val isGemini by viewModel.isGeminiActive.collectAsState()

    val notifications by viewModel.notificationsList.collectAsState()
    val allNotifications by viewModel.allBackendNotifications.collectAsState()
    val unreadCount by viewModel.unreadNotificationsCount.collectAsState()
    var showNotificationCenter by remember { mutableStateOf(false) }

    val limitAlert by viewModel.transactionLimitAlertMessage.collectAsState()
    val activeSessions by viewModel.activeShoppingSessions.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startPollingActiveSessions()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPollingActiveSessions()
        }
    }

    var microStoreName by remember { mutableStateOf("") }
    var microAmount by remember { mutableStateOf("") }
    var microPaidBy by remember { mutableStateOf("Io") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // WebSocket sync banner (Section 7)
        item {
            AnimatedVisibility(
                visible = webSocketMessage != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                webSocketMessage?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("websocket_sync_banner")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "WebSocket LiveSync",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearSyncBanner() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Banner",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }


        // Active Sessions Banner
        if (activeSessions.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeSessions.forEach { session ->
                        ActiveSessionBanner(session)
                    }
                }
            }
        }

        // Title Welcome Block
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SmartGrocery",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "L'Assistente Silenzioso a Frizione Zero",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                // Notification Bell
                Box(modifier = Modifier.padding(end = 8.dp)) {
                    IconButton(
                        onClick = { showNotificationCenter = true },
                        modifier = Modifier.testTag("home_notification_bell")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        Text(unreadCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifiche",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { viewModel.showLocalAiSettingsDialog.value = true },
                    modifier = Modifier.testTag("home_settings_gear")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Impostazioni",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Online / Offline & System Configurations Bar
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isOffline) "Stato: OFFLINE (CRDT Attivo)" else "Stato: ONLINE (Sync Real-time)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isOffline) SemanticYellow else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isOffline) "Scontrini e modifiche salvate in locale" else "Server WebSocket connesso",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = !isOffline,
                        onCheckedChange = { viewModel.toggleOfflineMode() },
                        thumbContent = {
                            Icon(
                                imageVector = if (isOffline) Icons.Default.WifiOff else Icons.Default.Wifi,
                                contentDescription = "Toggle Network Status",
                                modifier = Modifier.size(SwitchDefaults.IconSize)
                            )
                        },
                        modifier = Modifier.testTag("network_toggle")
                    )
                }
            }
        }

        // report quick-link banner/card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.40f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToReport() }
                    .testTag("home_report_link_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = "Report",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Analisi Nutrizionale & NOVA",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Distribuzione dei nutrienti e processamento spesa del gruppo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Vai",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // "Scontrini in Sospeso" back-queue section (Section 4.2)
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "Pending Receipts",
                                tint = SemanticBlueInfo,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Scontrini in Sospeso",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Badge(containerColor = if (pendingBacklog.isNotEmpty()) SemanticRed else MaterialTheme.colorScheme.secondary) {
                            Text(
                                text = "${pendingBacklog.size}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Hai le mani occupate post-spesa? I tuoi scontrini geolocalizzati rimasti in coda per un caricamento cumulativo sequentially.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (pendingBacklog.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nessuno scontrino in attesa. Ottimo!",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            pendingBacklog.forEach { r ->
                                val dateStr = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(r.timestamp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable {
                                            viewModel.startProcessingPendingReceipt(r)
                                            onNavigateToScanner()
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Storefront,
                                            contentDescription = "Store",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = r.storeName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${r.location} • $dateStr",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                viewModel.startProcessingPendingReceipt(r)
                                                onNavigateToScanner()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = "Scan Pending Rec",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.removePendingReceipt(r) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete Pending Rec",
                                                tint = SemanticRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }



        // Small Local Retailers (No-Scan fast totalizer) - Section 8.3
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = "Small Retailer",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Spesa Veloce Al Fornaio / Rione",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Spesa dal fornaio, fruttivendolo, macelleria locale senza codici EAN o scontrini stampati. Inserimento totale rapido d'un tocco.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = microStoreName,
                        onValueChange = { microStoreName = it },
                        label = { Text("Nome Esercente (es: Fornaio)") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("micro_store_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = microAmount,
                        onValueChange = { microAmount = it },
                        label = { Text("Totale Importo (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("micro_amount_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Pagatore: ", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = microPaidBy,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { microPaidBy = if (microPaidBy == "Io") "Partner" else "Io" }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("micro_paid_by_selector")
                            )
                        }

                        Button(
                            onClick = {
                                val amt = microAmount.toDoubleOrNull()
                                if (microStoreName.isNotBlank() && amt != null) {
                                    viewModel.addMicroRetailerSpesa(microStoreName, amt, microPaidBy)
                                    microStoreName = ""
                                    microAmount = ""
                                }
                            },
                            enabled = microStoreName.isNotBlank() && microAmount.toDoubleOrNull() != null,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.testTag("micro_retailer_submit")
                        ) {
                            Text("Registra Spesa")
                        }
                    }
                }
            }
        }

        item {
            Box(modifier = Modifier.height(30.dp))
        }
    }

    if (limitAlert != null) {
        AlertDialog(
            onDismissRequest = { viewModel.transactionLimitAlertMessage.value = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Gavel,
                    contentDescription = "Blocco Limite Superato",
                    tint = SemanticRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Limite Transazione Superato!",
                    fontWeight = FontWeight.Bold,
                    color = SemanticRed,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = limitAlert ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.transactionLimitAlertMessage.value = null },
                    colors = ButtonDefaults.buttonColors(containerColor = SemanticRed),
                    modifier = Modifier.testTag("dismiss_limit_dialog_btn")
                ) {
                    Text("OK, Ho Capito", color = Color.White)
                }
            }
        )
    }

    if (showNotificationCenter) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationCenter = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier.fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text("Notifiche", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    if (allNotifications.isNotEmpty()) {
                        TextButton(onClick = { viewModel.deleteAllBackendNotifications() }) {
                            Text("Svuota", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                if (allNotifications.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nessuna notifica.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp)
                    ) {
                        items(allNotifications, key = { it.id }) { notif ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = {
                                    when (it) {
                                        SwipeToDismissBoxValue.EndToStart -> {
                                            viewModel.deleteBackendNotification(notif.id)
                                            true
                                        }
                                        SwipeToDismissBoxValue.StartToEnd -> {
                                            if (!notif.isRead) {
                                                viewModel.acknowledgeNotification(notif.id)
                                            }
                                            false
                                        }
                                        else -> false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                modifier = Modifier.padding(vertical = 8.dp).clip(RoundedCornerShape(20.dp)),
                                backgroundContent = {
                                    val direction = dismissState.dismissDirection
                                    if (direction == SwipeToDismissBoxValue.EndToStart) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.errorContainer)
                                                .padding(horizontal = 24.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error)
                                        }
                                    } else if (direction == SwipeToDismissBoxValue.StartToEnd) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFF4CAF50).copy(alpha = 0.2f))
                                                .padding(horizontal = 24.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Segna come letta", tint = Color(0xFF4CAF50))
                                        }
                                    }
                                }
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().alpha(if (notif.isRead) 0.5f else 1f),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (notif.isRead) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = if (notif.isRead) 0.dp else 2.dp),
                                    onClick = { viewModel.acknowledgeNotification(notif.id) }
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        if (!notif.isRead) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .background(Color(0xFF4CAF50), RoundedCornerShape(50))
                                            )
                                            Spacer(Modifier.width(16.dp))
                                        } else {
                                            Spacer(Modifier.width(8.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = notif.title, 
                                                style = MaterialTheme.typography.titleMedium, 
                                                fontWeight = if (notif.isRead) FontWeight.Normal else FontWeight.Bold,
                                                color = if (notif.isRead) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = notif.body, 
                                                style = MaterialTheme.typography.bodyMedium, 
                                                color = if (notif.isRead) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveSessionBanner(session: com.example.api.ActiveShoppingSessionResponse) {
    var localDwellTime by remember(session.startedAt) { mutableLongStateOf(session.dwellTimeSeconds.toLong()) }

    LaunchedEffect(session.startedAt) {
        while(isActive) {
            delay(1000)
            localDwellTime++
        }
    }

    val minutes = localDwellTime / 60
    val seconds = localDwellTime % 60
    val timeString = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().testTag("active_session_${session.userId}")
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_scale"
            )

            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = "Spesa in corso",
                modifier = Modifier.size(24.dp).scale(scale),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("${session.userName} è da ${session.storeName}!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                Text("Spesa in corso da: $timeString", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
    }
}
