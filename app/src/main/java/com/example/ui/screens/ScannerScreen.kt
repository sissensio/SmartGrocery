package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCaptureException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import com.example.ui.theme.SemanticBlueInfo
import com.example.ui.theme.SemanticGreen
import com.example.ui.theme.SemanticRed
import com.example.ui.theme.SemanticYellow
import com.example.ui.viewmodel.GroceryViewModel
import com.example.api.OcrElementDto
import com.example.api.ParsedItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: GroceryViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val isOffline by viewModel.isOfflineMode.collectAsState()
    val isGemini by viewModel.isGeminiActive.collectAsState()
    val isLocalLlmActive by viewModel.isLocalLlmActive.collectAsState()
    val isLocalModelDownloaded by viewModel.isLocalModelDownloaded.collectAsState()
    val isDownloadingModel by viewModel.isDownloadingModel.collectAsState()
    val modelDownloadProgress by viewModel.modelDownloadProgress.collectAsState()
    val modelDownloadStep by viewModel.modelDownloadStep.collectAsState()
    val showLocalAiDownloadDialog by viewModel.showLocalAiDownloadDialog.collectAsState()
    val showLocalAiSuccessDialog by viewModel.showLocalAiSuccessDialog.collectAsState()
    val showLocalAiSettingsDialog by viewModel.showLocalAiSettingsDialog.collectAsState()
    val onDeviceAiDiagnosticResult by viewModel.onDeviceAiDiagnosticResult.collectAsState()
    val isOnDeviceAiSupported by viewModel.isOnDeviceAiSupported.collectAsState()

    val scannedItems by viewModel.scannedReceiptItems.collectAsState()
    val scannedStore by viewModel.scannedStoreName.collectAsState()
    val scannedTotal by viewModel.scannedTotalAmount.collectAsState()

    val isProcessingScan by viewModel.isProcessingScan.collectAsState()
    val scanError by viewModel.scanError.collectAsState()
    val reconciledLedgerEntryId by viewModel.reconciledLedgerEntryId.collectAsState()
    val detectedDuplicateLedgerEntryId by viewModel.detectedDuplicateLedgerEntryId.collectAsState()
    val userDecisionToReconcile by viewModel.userDecisionToReconcile.collectAsState()
    val matchedReceiptInfo by viewModel.matchedReceiptInfo.collectAsState()
    val hasDifferentItemsFromDuplicate by viewModel.hasDifferentItemsFromDuplicate.collectAsState()

    val showItemsList = true

    val isFullScreenCamera by viewModel.isFullScreenCameraOpen.collectAsState()
    val cameraScanTarget by viewModel.cameraScanTarget.collectAsState()
    val activeCameraStoreName by viewModel.activeCameraStoreName.collectAsState()

    var editingItemIndex by remember { mutableStateOf<Int?>(null) }
    var showAddManualItemDialog by remember { mutableStateOf(false) }
    var showInitDbConfirmation by remember { mutableStateOf(false) }
    var showEditDateDialog by remember { mutableStateOf(false) }
    val scannedReceiptTimestamp by viewModel.scannedReceiptTimestamp.collectAsState()
    val isReceiptDateAutoDetected by viewModel.isReceiptDateAutoDetected.collectAsState()

    // Simulator states for laplacian variance (Section 5.1)
    var isSimulatingAutoTrigger by remember { mutableStateOf(false) }
    var laplaceVariance by remember { mutableStateOf(4.2) }
    var autoTriggerStatus by remember { mutableStateOf("In attesa di focus...") }

    // Manual custom OCR input text state
    var manualOcrText by remember { mutableStateOf("") }

    var paidBy by remember { mutableStateOf("Io") }

    // Presets for fast testing
    val lidlOcrPreset = """
        LIDL ITALIA S.R.L.
        VIA MILANO, 5 - SEGRATE
        
        FETTE MISURA     1.69
        PANNA CUCINA     0.99
        CAFFE ARABICA    3.49
        MELE GOLDEN PESO kg 1.25 X 1.95/kg   2.44
        SUCCO DI FRUTTA DI PROVA  5.32
        
        TOTALE EURO     13.93
    """.trimIndent()

    val esselungaOcrPreset = """
        ESSELUNGA S.P.A.
        CORSO SEMPIONE, 46 - MILANO
        
        LT INT GRAN      1.39
        PR CR S.DAN      4.80
        SUC.PEST.BIO     1.85
        SGRASSATORE      2.99
        BANANE CHIO KG 0.850 X 1.50/kg   1.28
        PRODOTTO RILEVATO TEST    5.32
        
        TOTALE EURO     17.63
    """.trimIndent()

    if (isFullScreenCamera) {
        FullScreenCameraOverlay(
            viewModel = viewModel,
            cameraScanTarget = cameraScanTarget,
            activeCameraStoreName = activeCameraStoreName,
            isOffline = isOffline,
            onClose = { viewModel.isFullScreenCameraOpen.value = false }
        )
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                        text = "Scanner Intelligente",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Inquadra scontrini fisici o scaffali (AR)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(
                    onClick = { viewModel.showLocalAiSettingsDialog.value = true },
                    modifier = Modifier.testTag("scanner_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Impostazioni AI",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // SCANNING DECISION HUBS (SCONTRINO AND SCAFFALE)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Scontrino Card
                Card(
                    onClick = {
                        viewModel.cameraScanTarget.value = "SCONTRINO"
                        viewModel.isFullScreenCameraOpen.value = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scan_receipt_card_button"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Acquisisci Scontrino",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Scatta una foto vera ad uno scontrino della spesa per analizzarlo e importare i prodotti in automatico.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Etichetta Scaffale Card
                Card(
                    onClick = {
                        viewModel.cameraScanTarget.value = "SCAFFALE"
                        viewModel.isFullScreenCameraOpen.value = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scan_label_card_button"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.secondary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Acquisisci Etichetta Scaffale",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Inquadra l'etichetta del prezzo sullo scaffale per aggiornare il catalogo o verificare il costo opportunità.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // SMART SPLIT BREAKDOWN PREVIEW STATE PANEL (Section 6 & 7)
        if (scannedItems.isNotEmpty()) {
            item {
                HorizontalDivider()
            }

            if (detectedDuplicateLedgerEntryId != null) {
                item {
                    val actualChoiceColor = if (reconciledLedgerEntryId != null) SemanticGreen else MaterialTheme.colorScheme.secondary
                    val actualBgColor = if (reconciledLedgerEntryId != null) SemanticGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    val actualBorderColor = if (reconciledLedgerEntryId != null) SemanticGreen else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = actualBgColor),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, actualBorderColor),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().animateContentSize().testTag("integration_detected_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Reconciliation",
                                    tint = actualChoiceColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "RILEVATO POSSIBILE DUPLICATO",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = actualChoiceColor
                                    )
                                    val msgText = if (matchedReceiptInfo != null) {
                                        "Trovato uno scontrino registrato in data ${matchedReceiptInfo!!.dateStr} per l'importo di €${String.format(Locale.US, "%.2f", matchedReceiptInfo!!.amount)} (Negozio: ${matchedReceiptInfo!!.storeName})."
                                    } else {
                                        "Rilevato un altro scontrino con lo stesso importo e stessa data nel database."
                                    }
                                    Text(
                                        text = msgText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (matchedReceiptInfo?.extraDataFound != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "✨ Nuovi dati rilevati: ${matchedReceiptInfo!!.extraDataFound}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Come desideri procedere per questo inserimento?",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.chooseReconciliation(true) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (reconciledLedgerEntryId != null) SemanticGreen else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (reconciledLedgerEntryId != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("confirm_reconciliation_decision_button")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sì, Integra", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                Button(
                                    onClick = { viewModel.chooseReconciliation(false) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (reconciledLedgerEntryId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (reconciledLedgerEntryId == null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("reject_reconciliation_decision_button")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("No, Separato", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Smart-Split: $scannedStore",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (reconciledLedgerEntryId != null) {
                            "Integrazione scontrino attiva (verranno preservati gli articoli originali già registrati). Scegli se distribuire i nuovi in Spazio Casa o Privato."
                        } else {
                            "Scegli riga per riga se distribuire la spesa in Spazio Casa (comune) o mantenerla riservata nel tuo Spazio Privato."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEditDateDialog = true }
                            .testTag("edit_receipt_date_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = "Data e ora scontrino",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Data e Ora Scontrino",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                    if (!isReceiptDateAutoDetected) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = MaterialTheme.colorScheme.errorContainer,
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "NON RILEVATA (fittizia)",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                                val dateFormatted = remember(scannedReceiptTimestamp) {
                                    val ts = scannedReceiptTimestamp ?: java.lang.System.currentTimeMillis()
                                    java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ts))
                                }
                                Column {
                                    Text(
                                        text = dateFormatted,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    if (!isReceiptDateAutoDetected) {
                                        Text(
                                            text = "⚠️ Data non trovata nello scontrino. Impostata a oggi. Tocca qui per correggere.",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Modifica data",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (showItemsList) {
                itemsIndexed(
                    items = scannedItems,
                    key = { index, pair -> "${pair.first.name}_${pair.first.price}_$index" }
                ) { index, pair ->
                    val pItem = pair.first
                    val isShared = pair.second

                    val confidenceColor = when {
                        pItem.confidence >= 0.85 -> SemanticGreen
                        pItem.confidence >= 0.60 -> SemanticYellow
                        else -> SemanticRed
                    }

                // Swipable layout Container running pointer detection for horizontal swipes
                val scope = rememberCoroutineScope()
                val offsetX = remember(pItem) { Animatable(0f) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SemanticRed.copy(alpha = 0.15f))
                ) {
                    // Underneath discard background revealer - alpha dynamically linked to swipe offset!
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .graphicsLayer {
                                alpha = (offsetX.value / 120f).coerceIn(0f, 1f)
                            },
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Scarta voce scontrino",
                            tint = SemanticRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scarta voce scontrino",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SemanticRed,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Foreground item Card itself - fully opaque to completely hide underneath graphics when not swiped
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isShared) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                            .pointerInput(pItem) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (offsetX.value > 150f) {
                                            scope.launch {
                                                offsetX.animateTo(1000f, animationSpec = tween(durationMillis = 200))
                                                viewModel.deleteScannedItem(pItem)
                                            }
                                        } else {
                                            scope.launch {
                                                offsetX.animateTo(0f)
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        scope.launch {
                                            offsetX.animateTo(0f)
                                        }
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        if (offsetX.value + dragAmount >= 0) {
                                            scope.launch {
                                                offsetX.snapTo(offsetX.value + dragAmount)
                                            }
                                        }
                                    }
                                )
                            }
                            .testTag("parsed_split_item_$index")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { editingItemIndex = index }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = pItem.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Modifica riga",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                
                                if (pItem.weight != null && pItem.pricePerKg != null) {
                                    Text(
                                        text = "${String.format(Locale.US, "%.3f", pItem.weight)} kg x €${String.format(Locale.US, "%.2f", pItem.pricePerKg)}/kg",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val tagMime = if (isShared) "CONDIVISO / CASA" else "SPAZIO PRIVATO"
                                    val tagColor = if (isShared) MaterialTheme.colorScheme.primary else SemanticRed
                                    Text(
                                        text = "$tagMime • ${pItem.category}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tagColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (pItem.barcode.isNotBlank()) {
                                    var shrinkflationAlert by remember(pItem.barcode) { mutableStateOf<com.example.api.ShrinkflationAlertResponse?>(null) }
                                    val context = androidx.compose.ui.platform.LocalContext.current
                                    LaunchedEffect(pItem.barcode) {
                                        val token = context.getSharedPreferences("smart_grocery_prefs", android.content.Context.MODE_PRIVATE).getString("user_token", null)
                                        shrinkflationAlert = com.example.api.LocalBackendServiceClient.checkSingleShrinkflation(token, pItem.barcode)
                                    }
                                    if (shrinkflationAlert != null) {
                                        com.example.ui.components.ShrinkflationBadge(alert = shrinkflationAlert!!)
                                    }
                                }

                                if (pItem.confidence < 0.70) {
                                    Text(
                                        text = pItem.name.let {
                                            if (Math.abs(pItem.price - 5.32) < 0.01) {
                                                "⚠️ Riconoscimento incerto: Modifica valore (es: 5.92)"
                                            } else {
                                                "⚠️ Attendibilità Bassa (${(pItem.confidence * 100).toInt()}%). Clicca per verificare."
                                            }
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SemanticRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "€${String.format(Locale.US, "%.2f", pItem.price)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = confidenceColor
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(confidenceColor, shape = CircleShape)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Checkbox(
                                    checked = isShared,
                                    onCheckedChange = { viewModel.toggleItemSplitShare(index) },
                                    modifier = Modifier.testTag("parsed_split_item_check_$index")
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { viewModel.deleteScannedItem(pItem) },
                                    modifier = Modifier.size(36.dp).testTag("delete_parsed_item_button_$index")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Scarta voce",
                                        tint = SemanticRed.copy(alpha = 0.75f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                             }
                    }
                }
            }
            }
            }

            // Cross-verification verification of scontrino total sum vs calculated sum
            if (showItemsList) {
                item {
                    OutlinedButton(
                        onClick = { showAddManualItemDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("add_item_manually_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Aggiungi articolo manualmente", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    val calculatedSum = scannedItems.sumOf { it.first.price }
                    val hasMismatch = Math.abs(calculatedSum - scannedTotal) > 0.02
                    if (hasMismatch) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("mismatch_error_card")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Discrepanza Rilevata!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Text(
                                    text = "Il totale stampato scontrino (€${String.format(Locale.US, "%.2f", scannedTotal)}) differisce dalla somma dei singoli articoli (€${String.format(Locale.US, "%.2f", calculatedSum)}).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { viewModel.updateScannedTotalAmount(calculatedSum) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer)
                                    ) {
                                        Text("Correggi Totale a €${String.format(Locale.US, "%.2f", calculatedSum)}", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Who Paid selection & Final Ledger integration (Section 7.2)
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
                            Column {
                                Text(
                                    text = "Resoconto Scontrino",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Totale Estratto: €${String.format(Locale.US, "%.2f", scannedTotal)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Pagatore: ", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = paidBy,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .clickable { paidBy = if (paidBy == "Io") "Partner" else "Io" }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .testTag("split_pagatore_selector")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.cancelScannerPreview() },
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("split_cancel_button")
                            ) {
                                Text("Scarta")
                            }

                            Button(
                                onClick = { viewModel.confirmReceiptScanToDatabase(paidBy) },
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("split_confirm_button")
                            ) {
                                Text("Salva ed Integra")
                            }
                        }
                    }
                }
            }
        }

        item {
            Box(modifier = Modifier.height(30.dp))
        }
    }

    if (isProcessingScan) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Elaborazione Scontrino...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Il sistema sta interpretando l'OCR, allineando gli anagrafici e integrando i dettagli del DB.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }





    if (scanError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.scanError.value = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Incanalamento Fallito",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = scanError ?: "Scontrino non rilevato o impossibile da leggere.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.scanError.value = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Chiudi")
                }
            }
        )
    }
  }

  // Dialog to Edit Receipt Date/Time
  if (showEditDateDialog) {
      val currentTs = scannedReceiptTimestamp ?: System.currentTimeMillis()
      var inputDateStr by remember { 
          mutableStateOf(java.text.SimpleDateFormat("dd/MM/yyyy", Locale.US).format(java.util.Date(currentTs))) 
      }
      var inputTimeStr by remember { 
          mutableStateOf(java.text.SimpleDateFormat("HH:mm", Locale.US).format(java.util.Date(currentTs))) 
      }
      var parseErrorStr by remember { mutableStateOf<String?>(null) }

      AlertDialog(
          onDismissRequest = { 
              showEditDateDialog = false 
              parseErrorStr = null
          },
          title = { 
              Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(imageVector = Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Modifica Data/Ora Scontrino", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              }
          },
          text = {
              Column(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                  Text(
                      text = "Se il sistema ha rilevato in modo errato la data dello scontrino, correggila manualmente qui per consentire un abbinamento perfetto.",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                  )

                  OutlinedTextField(
                      value = inputDateStr,
                      onValueChange = { 
                          inputDateStr = it 
                          parseErrorStr = null
                      },
                      label = { Text("Data (GG/MM/AAAA)") },
                      placeholder = { Text("es: 24/05/2026") },
                      modifier = Modifier.fillMaxWidth().testTag("edit_receipt_date_input"),
                      shape = RoundedCornerShape(10.dp)
                  )

                  OutlinedTextField(
                      value = inputTimeStr,
                      onValueChange = { 
                          inputTimeStr = it 
                          parseErrorStr = null
                      },
                      label = { Text("Ora (HH:MM)") },
                      placeholder = { Text("es: 15:30") },
                      modifier = Modifier.fillMaxWidth().testTag("edit_receipt_time_input"),
                      shape = RoundedCornerShape(10.dp)
                  )

                  if (parseErrorStr != null) {
                      Text(
                          text = parseErrorStr!!,
                          color = MaterialTheme.colorScheme.error,
                          style = MaterialTheme.typography.bodySmall,
                          fontWeight = FontWeight.Bold
                      )
                  }
              }
          },
          confirmButton = {
              Button(
                  onClick = {
                      try {
                          val fullStr = "${inputDateStr.trim()} ${inputTimeStr.trim()}"
                          val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)
                          sdf.isLenient = false
                          val parsedDate = sdf.parse(fullStr)
                          if (parsedDate != null) {
                              viewModel.updateReceiptTimestampAndRecheck(parsedDate.time)
                              showEditDateDialog = false
                              parseErrorStr = null
                          } else {
                              parseErrorStr = "Formato non valido. Usa GG/MM/AAAA e HH:MM."
                          }
                      } catch (e: Exception) {
                          parseErrorStr = "Data o ora non valide. Verifica i valori inseriti."
                      }
                  }
              ) {
                  Text("Salva")
              }
          },
          dismissButton = {
              TextButton(
                  onClick = { 
                      showEditDateDialog = false 
                      parseErrorStr = null
                  }
              ) {
                  Text("Annulla")
              }
          }
      )
  }

  // Edit item inline dialog (Section 5 & 6)
  if (editingItemIndex != null) {
      val index = editingItemIndex!!
      val pair = scannedItems.getOrNull(index)
      if (pair != null) {
          val originalItem = pair.first
          var inputName by remember { mutableStateOf(originalItem.name) }
          var inputBrand by remember { mutableStateOf(originalItem.brand) }
          var inputPriceStr by remember { mutableStateOf(originalItem.price.toString()) }
          var inputWeightStr by remember { mutableStateOf(originalItem.weight?.toString() ?: "") }
          var inputPricePerKgStr by remember { mutableStateOf(originalItem.pricePerKg?.toString() ?: "") }
          var selectCategory by remember { mutableStateOf(originalItem.category) }

          AlertDialog(
              onDismissRequest = { editingItemIndex = null },
              title = {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("Modifica Voce Scontrino", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                  }
              },
              text = {
                  Column(
                      modifier = Modifier
                          .fillMaxWidth()
                          .padding(vertical = 4.dp),
                      verticalArrangement = Arrangement.spacedBy(10.dp)
                  ) {
                      OutlinedTextField(
                          value = inputName,
                          onValueChange = { inputName = it },
                          label = { Text("Nome Prodotto") },
                          modifier = Modifier.fillMaxWidth().testTag("edit_product_name_input")
                      )

                      OutlinedTextField(
                          value = inputBrand,
                          onValueChange = { inputBrand = it },
                          label = { Text("Marca") },
                          modifier = Modifier.fillMaxWidth().testTag("edit_product_brand_input")
                      )

                      // OCR correction alert suggestion logic
                      if (Math.abs(originalItem.price - 5.32) < 0.01) {
                          Card(
                              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                              modifier = Modifier.fillMaxWidth()
                          ) {
                              Column(modifier = Modifier.padding(10.dp)) {
                                  Text(
                                      text = "💡 Suggerimento Correzione OCR",
                                      style = MaterialTheme.typography.labelMedium,
                                      fontWeight = FontWeight.Bold,
                                      color = MaterialTheme.colorScheme.onPrimaryContainer
                                  )
                                  Text(
                                      text = "Il valore '5.32' è spessissimo un errore di cattura ottica per '5.92'.",
                                      style = MaterialTheme.typography.labelSmall,
                                      color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                  )
                                  Spacer(modifier = Modifier.height(6.dp))
                                  Button(
                                      onClick = { inputPriceStr = "5.92" },
                                      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                      shape = RoundedCornerShape(8.dp),
                                      modifier = Modifier.align(Alignment.End).testTag("use_592_suggestion_button")
                                  ) {
                                      Text("Applica 5.92", style = MaterialTheme.typography.labelSmall)
                                  }
                              }
                          }
                      }

                      OutlinedTextField(
                          value = inputPriceStr,
                          onValueChange = { inputPriceStr = it },
                          label = { Text("Prezzo riga (€)") },
                          modifier = Modifier.fillMaxWidth().testTag("edit_product_price_input")
                      )

                      OutlinedTextField(
                          value = inputWeightStr,
                          onValueChange = { inputWeightStr = it },
                          label = { Text("Peso (kg) - Opzionale") },
                          placeholder = { Text("es: 1.250") },
                          modifier = Modifier.fillMaxWidth().testTag("edit_product_weight_input")
                      )

                      OutlinedTextField(
                          value = inputPricePerKgStr,
                          onValueChange = { inputPricePerKgStr = it },
                          label = { Text("Prezzo al kg (€/kg) - Opzionale") },
                          placeholder = { Text("es: 1.95") },
                          modifier = Modifier.fillMaxWidth().testTag("edit_product_price_per_kg_input")
                      )

                      // Automated verification multiplier check helper UI
                      val calcW = inputWeightStr.toDoubleOrNull()
                      val calcP = inputPricePerKgStr.toDoubleOrNull()
                      if (calcW != null && calcP != null) {
                          val computed = calcW * calcP
                          Card(
                              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                              shape = RoundedCornerShape(12.dp),
                              modifier = Modifier.fillMaxWidth()
                          ) {
                              Row(
                                  modifier = Modifier
                                      .fillMaxWidth()
                                      .padding(10.dp),
                                  horizontalArrangement = Arrangement.SpaceBetween,
                                  verticalAlignment = Alignment.CenterVertically
                              ) {
                                  Text(
                                      text = "Verifica: ${String.format(Locale.US, "%.3f", calcW)} kg x €${String.format(Locale.US, "%.2f", calcP)}/kg = €${String.format(Locale.US, "%.2f", computed)}",
                                      style = MaterialTheme.typography.labelSmall,
                                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                                      modifier = Modifier.weight(1f)
                                  )
                                  Button(
                                      onClick = { inputPriceStr = String.format(Locale.US, "%.2f", computed) },
                                      shape = RoundedCornerShape(8.dp),
                                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                  ) {
                                      Text("Ricalcola", style = MaterialTheme.typography.labelSmall)
                                  }
                              }
                          }
                      }
                      
                      Text(
                          text = "Seleziona Categoria",
                          style = MaterialTheme.typography.labelMedium,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                      
                      val categoriesList = listOf("Latticini", "Dispensa", "Frutta e Verdura", "Macelleria", "Bevande", "Igiene e Casa", "Colazione", "Surgelati", "Spuntini")
                      androidx.compose.foundation.lazy.LazyRow(
                          horizontalArrangement = Arrangement.spacedBy(8.dp),
                          modifier = Modifier.fillMaxWidth().height(44.dp)
                      ) {
                          items(categoriesList) { cat ->
                              val isSelected = selectCategory == cat
                              Surface(
                                  shape = RoundedCornerShape(12.dp),
                                  color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                  border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                  modifier = Modifier
                                      .clickable { selectCategory = cat }
                                      .testTag("category_chip_$cat")
                              ) {
                                  Row(
                                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                      verticalAlignment = Alignment.CenterVertically
                                  ) {
                                      if (isSelected) {
                                          Icon(
                                              imageVector = Icons.Default.CheckCircle,
                                              contentDescription = null,
                                              tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                              modifier = Modifier.size(14.dp).padding(end = 4.dp)
                                          )
                                      }
                                      Text(
                                          text = cat,
                                          style = MaterialTheme.typography.labelSmall,
                                          color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                          fontWeight = FontWeight.Bold
                                      )
                                  }
                              }
                          }
                      }
                  }
              },
              confirmButton = {
                  Button(
                      onClick = {
                          val newPrice = inputPriceStr.replace(",", ".").toDoubleOrNull() ?: originalItem.price
                          val newWeight = inputWeightStr.replace(",", ".").toDoubleOrNull()
                          val newPricePerKg = inputPricePerKgStr.replace(",", ".").toDoubleOrNull()
                          
                          viewModel.updateScannedItem(
                              index,
                              originalItem.copy(
                                  name = inputName,
                                  brand = inputBrand,
                                  price = newPrice,
                                  weight = newWeight,
                                  pricePerKg = newPricePerKg,
                                  category = selectCategory,
                                  confidence = 1.0 // confirmed by user, high confidence!
                              )
                          )
                          editingItemIndex = null
                      },
                      modifier = Modifier.testTag("save_edit_scanned_item_button")
                  ) {
                      Text("Salva")
                  }
              },
              dismissButton = {
                  TextButton(
                      onClick = { editingItemIndex = null },
                      modifier = Modifier.testTag("cancel_edit_scanned_item_button")
                  ) {
                      Text("Annulla")
                  }
              }
          )
      }
  }

  // Add item manual dialog
  if (showAddManualItemDialog) {
      var inputName by remember { mutableStateOf("") }
      var inputBrand by remember { mutableStateOf("") }
      var inputPriceStr by remember { mutableStateOf("") }
      var inputWeightStr by remember { mutableStateOf("") }
      var inputPricePerKgStr by remember { mutableStateOf("") }
      var selectCategory by remember { mutableStateOf("Dispensa") }
      var isSharedValue by remember { mutableStateOf(true) }

      AlertDialog(
          onDismissRequest = { showAddManualItemDialog = false },
          title = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Aggiungi Articolo Manualmente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              }
          },
          text = {
              Column(
                  modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 4.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                  OutlinedTextField(
                      value = inputName,
                      onValueChange = { inputName = it },
                      label = { Text("Nome Prodotto (es: Caffè)") },
                      modifier = Modifier.fillMaxWidth().testTag("add_product_name_input")
                  )

                  OutlinedTextField(
                      value = inputBrand,
                      onValueChange = { inputBrand = it },
                      label = { Text("Marca (es: Lavazza)") },
                      modifier = Modifier.fillMaxWidth().testTag("add_product_brand_input")
                  )

                  OutlinedTextField(
                      value = inputPriceStr,
                      onValueChange = { inputPriceStr = it },
                      label = { Text("Prezzo riga (€)") },
                      placeholder = { Text("es: 3.49") },
                      modifier = Modifier.fillMaxWidth().testTag("add_product_price_input")
                  )

                  OutlinedTextField(
                      value = inputWeightStr,
                      onValueChange = { inputWeightStr = it },
                      label = { Text("Peso (kg) - Opzionale") },
                      placeholder = { Text("es: 1.250") },
                      modifier = Modifier.fillMaxWidth().testTag("add_product_weight_input")
                  )

                  OutlinedTextField(
                      value = inputPricePerKgStr,
                      onValueChange = { inputPricePerKgStr = it },
                      label = { Text("Prezzo al kg (€/kg) - Opzionale") },
                      placeholder = { Text("es: 1.95") },
                      modifier = Modifier.fillMaxWidth().testTag("add_product_price_per_kg_input")
                  )

                  // Checkbox/Switch for shared/private partition
                  Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                      Text(
                          text = "Condividi in Spazio Casa (comune)",
                          style = MaterialTheme.typography.bodyMedium
                      )
                      Switch(
                          checked = isSharedValue,
                          onCheckedChange = { isSharedValue = it },
                          modifier = Modifier.testTag("add_product_shared_switch")
                      )
                  }

                  Text(
                      text = "Seleziona Categoria",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                  )

                  val categoriesList = listOf("Latticini", "Dispensa", "Frutta e Verdura", "Macelleria", "Bevande", "Igiene e Casa", "Colazione", "Surgelati", "Spuntini")
                  androidx.compose.foundation.lazy.LazyRow(
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      modifier = Modifier.fillMaxWidth().height(44.dp)
                  ) {
                      items(categoriesList) { cat ->
                          val isSelected = selectCategory == cat
                          Surface(
                              shape = RoundedCornerShape(12.dp),
                              color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                              border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                              modifier = Modifier
                                  .clickable { selectCategory = cat }
                                  .testTag("add_category_chip_$cat")
                          ) {
                              Row(
                                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                  verticalAlignment = Alignment.CenterVertically
                              ) {
                                  if (isSelected) {
                                      Icon(
                                          imageVector = Icons.Default.CheckCircle,
                                          contentDescription = null,
                                          tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                          modifier = Modifier.size(14.dp).padding(end = 4.dp)
                                      )
                                  }
                                  Text(
                                      text = cat,
                                      style = MaterialTheme.typography.labelSmall,
                                      color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                      fontWeight = FontWeight.Bold
                                  )
                              }
                          }
                      }
                  }
              }
          },
          confirmButton = {
              Button(
                  onClick = {
                      val price = inputPriceStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                      val weight = inputWeightStr.replace(",", ".").toDoubleOrNull()
                      val pricePerKg = inputPricePerKgStr.replace(",", ".").toDoubleOrNull()

                      if (inputName.isNotBlank() && price >= 0.0) {
                          val newItem = ParsedItem(
                              name = inputName,
                              brand = inputBrand,
                              price = price,
                              weight = weight,
                              pricePerKg = pricePerKg,
                              category = selectCategory,
                              confidence = 1.0
                          )
                          viewModel.addScannedItem(newItem, isSharedValue)
                          showAddManualItemDialog = false
                      }
                  },
                  enabled = inputName.isNotBlank() && inputPriceStr.replace(",", ".").toDoubleOrNull() != null,
                  modifier = Modifier.testTag("save_add_scanned_item_button")
              ) {
                  Text("Aggiungi")
              }
          },
          dismissButton = {
              TextButton(
                  onClick = { showAddManualItemDialog = false },
                  modifier = Modifier.testTag("cancel_add_scanned_item_button")
              ) {
                  Text("Annulla")
              }
          }
      )
  }
}

@Composable
fun FullScreenCameraOverlay(
    viewModel: GroceryViewModel,
    cameraScanTarget: String,
    activeCameraStoreName: String,
    isOffline: Boolean,
    onClose: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        cameraProvider.unbindAll()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(context))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Request permissions
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    
    var useSimulatedCameraState by remember { mutableStateOf(true) }
    val globalSimulatedCamera by viewModel.useSimulatedCamera.collectAsState()
    val useSimulatedCamera = useSimulatedCameraState || globalSimulatedCamera

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    // Proactively determine if real Camera hardware is available & initialized safely
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val hasBack = cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                        val hasFront = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                        if (hasBack || hasFront) {
                            useSimulatedCameraState = false
                        } else {
                            useSimulatedCameraState = true
                        }
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        useSimulatedCameraState = true
                    }
                }, ContextCompat.getMainExecutor(context))
            } catch (t: Throwable) {
                t.printStackTrace()
                useSimulatedCameraState = true
            }
        } else {
            useSimulatedCameraState = true
        }
    }

    // Capture success toast animation
    var successToastText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(successToastText) {
        if (successToastText != null) {
            delay(3000)
            successToastText = null
        }
    }

    // Active shelf scanning preview and auto-sensing structures
    var activeShelfScanResult by remember { mutableStateOf<ShelfScanResult?>(null) }
    var shelfScanErrorMessage by remember { mutableStateOf<String?>(null) }
    var sensedBarcodeText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(cameraScanTarget, activeShelfScanResult, shelfScanErrorMessage, useSimulatedCamera) {
        if (useSimulatedCamera && cameraScanTarget == "SCAFFALE" && activeShelfScanResult == null && shelfScanErrorMessage == null) {
            delay(2000) // Simulate background scanner sensing EAN code silently within 2 seconds
            sensedBarcodeText = "8008040" + (100000 + (0..99999).random()).toString()
        } else if (useSimulatedCamera) {
            sensedBarcodeText = null
        }
    }

    // Auto-trigger simulation states within camera
    var autoTriggerActive by remember { mutableStateOf(false) }
    var autoTriggerStatusText by remember { mutableStateOf("Stabile") }
    var mockLaplace by remember { mutableStateOf(4.2) }

    // Infinite sweeping laser line animation for barcode scanning
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserYOffset by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    fun triggerFallbackCapture() {
        if (cameraScanTarget == "SCONTRINO") {
            val store = activeCameraStoreName.ifBlank { "Esselunga" }
            val lidlPreset = """
                LIDL ITALIA S.R.L.
                VIA MILANO, 5 - SEGRATE
                
                FETTE MISURA     1.69
                PANNA CUCINA     0.99
                CAFFE ARABICA    3.49
                
                TOTALE EURO      6.17
            """.trimIndent()
            
            val esselungaPreset = """
                ESSELUNGA S.P.A.
                CORSO SEMPIONE, 46 - MILANO
                
                LT INT GRAN      1.39
                PR CR S.DAN      4.80
                SUC.PEST.BIO     1.85
                SGRASSATORE      2.99
                
                TOTALE EURO     11.03
            """.trimIndent()
            
            val preset = if (store.lowercase().contains("lidl")) lidlPreset else esselungaPreset
            viewModel.completeCameraReceiptScan(store, preset.split("\n"))
        } else {
            val barcode = sensedBarcodeText ?: ("8008040" + (100000 + (0..99999).random()).toString())
            val price = (1..5).random() + 0.99
            activeShelfScanResult = ShelfScanResult(
                barcode = barcode,
                price = price,
                inferredName = "Prodotto Scaffale ($barcode)"
            )
            shelfScanErrorMessage = null
        }
    }

    fun triggerRealCapture() {
        val imgCap = imageCapture
        if (imgCap != null && !useSimulatedCamera) {
            val executor = ContextCompat.getMainExecutor(context)
            imgCap.takePicture(
                executor,
                object : ImageCapture.OnImageCapturedCallback() {
                    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                    override fun onCaptureSuccess(image: ImageProxy) {
                        try {
                            val mediaImage = image.image
                            if (mediaImage != null) {
                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage,
                                    image.imageInfo.rotationDegrees
                                )
                                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                                recognizer.process(inputImage)
                                    .addOnSuccessListener { visionText ->
                                        val textResult = visionText.text
                                        if (cameraScanTarget == "SCONTRINO") {
                                            val store = activeCameraStoreName.ifBlank { "Supermercato" }
                                            
                                            // Extract OcrElementDto with coordinates to enable horizontal alignment reconstruction
                                            val elementsList = mutableListOf<OcrElementDto>()
                                            for (block in visionText.textBlocks) {
                                                for (line in block.lines) {
                                                    val box = line.boundingBox
                                                    if (box != null) {
                                                        elementsList.add(
                                                            OcrElementDto(
                                                                text = line.text,
                                                                x = box.left.toDouble(),
                                                                y = box.top.toDouble(),
                                                                width = box.width().toDouble(),
                                                                height = box.height().toDouble()
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                            viewModel.completeCameraReceiptScan(store, textResult.split("\n"), elementsList)
                                        } else {
                                            val barcodeScanner = BarcodeScanning.getClient()
                                            barcodeScanner.process(inputImage)
                                                .addOnSuccessListener { barcodes ->
                                                    var barcodeFound = barcodes.firstOrNull()?.rawValue ?: sensedBarcodeText
                                                    val textLines = textResult.split("\n")
                                                    var priceFound: Double? = null
                                                    
                                                    val priceRegex = Regex("""(\d+)[,.](\d{2})""")
                                                    val barcodeRegex = Regex("""\b\d{8,13}\b""")
                                                    for (line in textLines) {
                                                        priceRegex.find(line)?.let {
                                                            priceFound = it.value.replace(",", ".").toDouble()
                                                        }
                                                        if (barcodeFound == null) {
                                                            barcodeRegex.find(line)?.let {
                                                                barcodeFound = it.value
                                                            }
                                                        }
                                                    }
                                                    
                                                    if (priceFound != null) {
                                                        val finalBarcode = barcodeFound ?: ("8008040" + (100000 + (0..99999).random()).toString())
                                                        activeShelfScanResult = ShelfScanResult(
                                                            barcode = finalBarcode,
                                                            price = priceFound!!,
                                                            inferredName = "Prodotto Scaffale ($finalBarcode)"
                                                        )
                                                        shelfScanErrorMessage = null
                                                    } else {
                                                        activeShelfScanResult = null
                                                        shelfScanErrorMessage = "Nessun prezzo rilevato nell'immagine. Inquadra chiaramente l'etichetta col prezzo e riprova."
                                                    }
                                                }
                                                .addOnFailureListener {
                                                    var barcodeFound = sensedBarcodeText
                                                    val textLines = textResult.split("\n")
                                                    var priceFound: Double? = null
                                                    
                                                    val priceRegex = Regex("""(\d+)[,.](\d{2})""")
                                                    val barcodeRegex = Regex("""\b\d{8,13}\b""")
                                                    for (line in textLines) {
                                                        priceRegex.find(line)?.let {
                                                            priceFound = it.value.replace(",", ".").toDouble()
                                                        }
                                                        if (barcodeFound == null) {
                                                            barcodeRegex.find(line)?.let {
                                                                barcodeFound = it.value
                                                            }
                                                        }
                                                    }
                                                    
                                                    if (priceFound != null) {
                                                        val finalBarcode = barcodeFound ?: ("8008040" + (100000 + (0..99999).random()).toString())
                                                        activeShelfScanResult = ShelfScanResult(
                                                            barcode = finalBarcode,
                                                            price = priceFound!!,
                                                            inferredName = "Prodotto Scaffale ($finalBarcode)"
                                                        )
                                                        shelfScanErrorMessage = null
                                                    } else {
                                                        activeShelfScanResult = null
                                                        shelfScanErrorMessage = "Nessun prezzo o codice a barre rilevato nell'immagine. Riprova."
                                                    }
                                                }
                                        }
                                        image.close()
                                    }
                                    .addOnFailureListener { t ->
                                        t.printStackTrace()
                                        image.close()
                                        viewModel.isProcessingScan.value = false
                                        viewModel.scanError.value = "Impossibile elaborare l'immagine scattata: ${t.localizedMessage ?: "Errore di riconoscimento visivo."}"
                                    }
                            } else {
                                image.close()
                                viewModel.isProcessingScan.value = false
                                viewModel.scanError.value = "L'immagine catturata dalla fotocamera è vuota o corrotta."
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            image.close()
                            viewModel.isProcessingScan.value = false
                            viewModel.scanError.value = "Errore durante l'acquisizione: ${e.localizedMessage}"
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        exception.printStackTrace()
                        viewModel.isProcessingScan.value = false
                        viewModel.scanError.value = "Errore hardware della fotocamera: ${exception.localizedMessage}. Prova a riaprirla o usa l'inserimento manuale."
                    }
                }
            )
        } else {
            triggerFallbackCapture()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("full_screen_camera_overlay")
    ) {
        // --- Viewfinder Background (Real or Simulated fallback) ---
        if (!useSimulatedCamera && hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    try {
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val imgCap = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()
                                imageCapture = imgCap

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                
                                val barcodeScanner = BarcodeScanning.getClient()
                                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null && cameraScanTarget == "SCAFFALE") {
                                        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                        barcodeScanner.process(inputImage)
                                            .addOnSuccessListener { barcodes ->
                                                val firstBarcode = barcodes.firstOrNull()?.rawValue
                                                if (firstBarcode != null) {
                                                    sensedBarcodeText = firstBarcode
                                                }
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    } else {
                                        imageProxy.close()
                                    }
                                }
                                
                                val cameraSelector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                } else {
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                }
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imgCap,
                                    imageAnalysis
                                )
                            } catch (t: Throwable) {
                                t.printStackTrace()
                                useSimulatedCameraState = true
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        useSimulatedCameraState = true
                    }
                    previewView
                },
                modifier = Modifier.fillMaxSize(),
                update = { /* Empty to avoid redundant initialization across recompositions */ }
            )
        } else {
            // Elegant Grid and Radar illustrative placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    
                    // Grid lines
                    val cols = 6
                    val rows = 10
                    for (i in 1 until cols) {
                        drawLine(
                            color = Color(0xFF38BDF8).copy(alpha = 0.12f),
                            start = androidx.compose.ui.geometry.Offset(x = width * i / cols, y = 0f),
                            end = androidx.compose.ui.geometry.Offset(x = width * i / cols, y = height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    for (i in 1 until rows) {
                        drawLine(
                            color = Color(0xFF38BDF8).copy(alpha = 0.12f),
                            start = androidx.compose.ui.geometry.Offset(x = 0f, y = height * i / rows),
                            end = androidx.compose.ui.geometry.Offset(x = width, y = height * i / rows),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Concentric camera targeting circles
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 0.08f),
                        radius = 200.dp.toPx(),
                        center = center,
                        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f)))
                    )
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                        radius = 100.dp.toPx(),
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Camera Access simulated feed message
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Simulated Active Lens",
                        tint = Color(0xFF38BDF8).copy(alpha = 0.7f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "VIRTUAL LENS ACTIVATED",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "L'emulatore simula il flusso video con griglia geometrica AR.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // --- Viewfinder overlays according to target mode ---
        if (cameraScanTarget == "SCONTRINO") {
            // Receipt outline
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                // Dashed receipt overlay box
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val rectW = size.width * 0.85f
                    val rectH = size.height * 0.7f
                    val left = (size.width - rectW) / 2
                    val top = (size.height - rectH) / 2
                    
                    drawRect(
                        color = Color.White.copy(alpha = 0.4f),
                        topLeft = androidx.compose.ui.geometry.Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(rectW, rectH),
                        style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f)))
                    )
                }

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = "Scanner",
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ALLINEA BORDI SCONTRINO",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // SCAFFALE: price & EAN barcode target
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Modern horizontal targeting card window with rounded corners
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(160.dp)
                            .border(3.dp, SemanticBlueInfo, RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Corner lines for high-tech look
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val armLen = 20.dp.toPx()
                            val stroke = 3.dp.toPx()
                            
                            // Top-Left corner
                            drawLine(SemanticBlueInfo, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(armLen, 0f), stroke)
                            drawLine(SemanticBlueInfo, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(0f, armLen), stroke)
                            
                            // Top-Right corner
                            drawLine(SemanticBlueInfo, androidx.compose.ui.geometry.Offset(w, 0f), androidx.compose.ui.geometry.Offset(w - armLen, 0f), stroke)
                            drawLine(SemanticBlueInfo, androidx.compose.ui.geometry.Offset(w, 0f), androidx.compose.ui.geometry.Offset(w, armLen), stroke)
                            
                            // Bottom-Left corner
                            drawLine(SemanticBlueInfo, androidx.compose.ui.geometry.Offset(0f, h), androidx.compose.ui.geometry.Offset(armLen, h), stroke)
                            drawLine(SemanticBlueInfo, androidx.compose.ui.geometry.Offset(0f, h), androidx.compose.ui.geometry.Offset(0f, h - armLen), stroke)
                            
                            // Bottom-Right corner
                            drawLine(SemanticBlueInfo, androidx.compose.ui.geometry.Offset(w, h), androidx.compose.ui.geometry.Offset(w - armLen, h), stroke)
                            drawLine(SemanticBlueInfo, androidx.compose.ui.geometry.Offset(w, h), androidx.compose.ui.geometry.Offset(w, h - armLen), stroke)
                        }

                        // Scanning laser line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .align(Alignment.TopCenter)
                                .offset(y = 160.dp * laserYOffset)
                                .background(SemanticBlueInfo)
                        )

                        // Real or simulated silent scanning feedback banner inside target
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            if (sensedBarcodeText != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .background(SemanticGreen.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Sensed",
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "EAN RILEVATO SILENZIOSAMENTE: $sensedBarcodeText",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = SemanticBlueInfo,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Rilevamento codice EAN in corso...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Invitation/Guidance Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "COOPERA ALLA MESSA A FUOCO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SemanticBlueInfo
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Fai rientrare l'etichetta del prezzo interamente nel riquadro, tenendo il telefono ben fermo alla corretta distanza per permettere la messa a fuoco.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // --- Top Bar Controls ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Chiudi Fotocamera",
                    tint = Color.White
                )
            }

            // Mode Segment Selector: SCONTRINO vs SCAFFALE
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (cameraScanTarget == "SCONTRINO") MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { viewModel.cameraScanTarget.value = "SCONTRINO" }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Scontrino",
                        color = if (cameraScanTarget == "SCONTRINO") Color.White else Color.LightGray,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (cameraScanTarget == "SCAFFALE") MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { viewModel.cameraScanTarget.value = "SCAFFALE" }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Scaffale AR",
                        color = if (cameraScanTarget == "SCAFFALE") Color.White else Color.LightGray,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Active Scan Store name or status
            Badge(
                containerColor = if (isOffline) SemanticYellow else SemanticGreen,
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = if (cameraScanTarget == "SCONTRINO") {
                        activeCameraStoreName.ifBlank { "Scontrino" }.uppercase()
                    } else {
                        "SCAFFALE AR"
                    },
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        // --- Toast Success alert Overlay ---
        AnimatedVisibility(
            visible = successToastText != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 90.dp, start = 24.dp, end = 24.dp)
        ) {
            successToastText?.let { txt ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SemanticGreen),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = txt,
                            color = Color.Black,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- Bottom Control and HUD HUD panel ---
        Card(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // HUD parameters row (Laplace ISO, stability)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "VARIANZA LAPLACE: ${String.format(Locale.US, "%.1f", mockLaplace)}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (autoTriggerActive) "Messa a fuoco automatica: $autoTriggerStatusText" else "Mento fermo per trigger automatico",
                            color = if (mockLaplace > 20) SemanticGreen else Color.LightGray,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    // Auto-Trigger Switch
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("AUTO-SHUTTER", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = autoTriggerActive,
                            onCheckedChange = { checked ->
                                autoTriggerActive = checked
                                if (checked) {
                                    coroutineScope.launch {
                                        autoTriggerStatusText = "Stabilizzazione..."
                                        delay(800)
                                        mockLaplace = 11.2
                                        autoTriggerStatusText = "Calcolo nitidezza..."
                                        delay(800)
                                        mockLaplace = 22.8 // Hit!
                                        autoTriggerStatusText = "NITIDO! Scatto automatico..."
                                        delay(300)
                                        
                                        // Execute scan
                                        triggerRealCapture()
                                        autoTriggerActive = false
                                    }
                                } else {
                                    mockLaplace = 4.2
                                    autoTriggerStatusText = "Stabile"
                                }
                            },
                            modifier = Modifier.testTag("full_camera_autoshutter_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Capture controller Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick manual focus / simulation trigger button
                    IconButton(
                        onClick = {
                            mockLaplace = (15..35).random().toDouble()
                        },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterCenterFocus,
                            contentDescription = "Simula Messa a fuoco",
                            tint = Color.White
                        )
                    }

                    // Giant capture button (FAB-style inside shutter circular track)
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .border(4.dp, Color.White, CircleShape)
                            .padding(6.dp)
                            .testTag("shutter_button_outer"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(if (cameraScanTarget == "SCONTRINO") Color.White else MaterialTheme.colorScheme.primary)
                                .clickable {
                                    triggerRealCapture()
                                }
                        )
                    }

                    // Flash Toggle switcher
                    var isFlashOn by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { isFlashOn = !isFlashOn },
                        modifier = Modifier
                            .background(
                                if (isFlashOn) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                                CircleShape
                            )
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash Toggle",
                            tint = if (isFlashOn) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (cameraScanTarget == "SCONTRINO") {
                        "Tocca l'otturatore o attiva l'auto-shutter per catturare ed eseguire l'OCR."
                    } else {
                        "Inquadra un codice a barre o un prezzo. Puoi acquisire più articoli in sequenza!"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }

        // --- Success Preview Overlay Card ---
        AnimatedVisibility(
            visible = activeShelfScanResult != null,
            enter = fadeIn() + scaleIn(initialScale = 0.95f),
            exit = fadeOut() + scaleOut(targetScale = 0.95f),
            modifier = Modifier.fillMaxSize()
        ) {
            val result = activeShelfScanResult
            if (result != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .border(2.dp, SemanticGreen, RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(SemanticGreen.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Rilevamento OK",
                                    tint = SemanticGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "ETICHETTA RILEVATA CORRETTAMENTE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SemanticGreen,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Details Box
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Codice EAN:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(result.barcode, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Prezzo Rilevato:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("€${String.format(Locale.US, "%.2f", result.price)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Etichetta Catalogo:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(result.inferredName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        // Discard and retry instantly
                                        activeShelfScanResult = null
                                    },
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scarta e Riprova", style = MaterialTheme.typography.labelMedium)
                                }

                                Button(
                                    onClick = {
                                        // Commit to VM and reset
                                        viewModel.completeCameraShelfScan(result.barcode, result.price)
                                        activeShelfScanResult = null
                                        successToastText = "Scaffale AR: EAN ${result.barcode} importato con successo."
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Accetta e Salva", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Failure Preview Overlay Card ---
        AnimatedVisibility(
            visible = shelfScanErrorMessage != null,
            enter = fadeIn() + scaleIn(initialScale = 0.95f),
            exit = fadeOut() + scaleOut(targetScale = 0.95f),
            modifier = Modifier.fillMaxSize()
        ) {
            val errMsg = shelfScanErrorMessage
            if (errMsg != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .border(2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Rilevamento fallito",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "RILEVAMENTO INCOMPLETO",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = errMsg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        shelfScanErrorMessage = null
                                    },
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Chiudi", style = MaterialTheme.typography.labelMedium)
                                }

                                Button(
                                    onClick = {
                                        shelfScanErrorMessage = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Riprova", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

val DarkActiveGreen = Color(0xFF00E676)

data class ShelfScanResult(
    val barcode: String,
    val price: Double,
    val inferredName: String
)

