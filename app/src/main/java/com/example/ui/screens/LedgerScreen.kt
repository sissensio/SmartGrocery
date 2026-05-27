package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.LedgerEntry
import com.example.ui.theme.SemanticGreen
import com.example.ui.theme.SemanticYellow
import com.example.ui.viewmodel.GroceryViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.example.api.ParsedItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    viewModel: GroceryViewModel,
    modifier: Modifier = Modifier
) {
    val ledgerEntries by viewModel.ledgerEntries.collectAsState()

    var selectedEntryForDetails by remember { mutableStateOf<LedgerEntry?>(null) }
    var editingLedgerItemIndex by remember { mutableStateOf<Int?>(null) }
    var editingLedgerItemByEntry by remember { mutableStateOf<LedgerEntry?>(null) }



    // Algorithmic Resolver for Net Balance Debt - Section 7.2
    val outstandingEntries = ledgerEntries.filter { !it.isSettled }
    val totalPaidByIo = outstandingEntries.filter { it.paidBy == "Io" }.sumOf { it.amount }
    val totalPaidByPartner = outstandingEntries.filter { it.paidBy == "Partner" }.sumOf { it.amount }

    val diff = totalPaidByIo - totalPaidByPartner
    val netOwedAmount = Math.abs(diff) / 2.0
    val netDebtor = if (diff > 0) "Partner" else "Io"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "Contabilità di Casa",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Risolvi debiti, crediti e spese condivise a fine mese",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // OVERALL SETTLEMENT BALANCER (Section 7.2 Ledger Formula)
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ledger_balancer_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Balance,
                            contentDescription = "Bilancio Conguaglio",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Bilancio di Conguaglio Netto",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Totale Speso Io:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("€${String.format(Locale.US, "%.2f", totalPaidByIo)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Totale Speso Partner:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("€${String.format(Locale.US, "%.2f", totalPaidByPartner)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    if (outstandingEntries.isEmpty() || netOwedAmount == 0.0) {
                        Text(
                            text = "Conti in perfetto pareggio. Ottimo lavoro!",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = SemanticGreen,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (netDebtor == "Partner") "Il Partner ti deve:" else "Tu devi al Partner:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "€${String.format(Locale.US, "%.2f", netOwedAmount)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("net_owed_amount_label")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { viewModel.settleLedger() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ledger_settle_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.CurrencyExchange, contentDescription = "Settle")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Salda Tutti i Sospesi")
                                }
                            }
                        }
                    }
                }
            }
        }



        // LIST OF HISTORICAL LEDGER ENTRIES
        item {
            Text(
                text = "Cronologia Spese Condivise",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (ledgerEntries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nessuna transazione registrata nel Ledger comune.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(ledgerEntries) { entry ->
                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(entry.timestamp))
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (entry.isSettled) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { selectedEntryForDetails = entry }
                        .testTag("ledger_entry_${entry.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (entry.isSettled) Icons.Default.Check else Icons.Default.CurrencyBitcoin,
                                contentDescription = "Settle Status",
                                tint = if (entry.isSettled) SemanticGreen else SemanticYellow,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = entry.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (entry.isSettled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (entry.receiptItemsJson != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.ReceiptLong,
                                            contentDescription = "Scontrino Scansionato",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Pagato da ${entry.paidBy} • $dateStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                if (entry.isSettled) {
                                    Text(
                                        text = "REGOLATO",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SemanticGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else if (!entry.is_synced) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CloudUpload,
                                            contentDescription = "In attesa di sync",
                                            tint = SemanticYellow,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "IN ATTESA DI SYNC",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SemanticYellow,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "€${String.format(Locale.US, "%.2f", entry.amount)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (entry.isSettled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { viewModel.deleteLedgerEntry(entry) },
                                modifier = Modifier.size(36.dp).testTag("delete_ledger_entry_button_${entry.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Elimina Scontrino",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Vedi Dettagli",
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Box(modifier = Modifier.height(30.dp))
        }
    }



    // Receipt Inspector Detail Dialog (Consult receipt detail from Ledger)
    if (selectedEntryForDetails != null) {
        val liveEntry = ledgerEntries.find { it.id == selectedEntryForDetails!!.id } ?: selectedEntryForDetails!!
        val entry = liveEntry
        val parsedItems = remember(entry.receiptItemsJson) {
            if (entry.receiptItemsJson != null) {
                try {
                    val moshiObj = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                    val listType = Types.newParameterizedType(List::class.java, ParsedItem::class.java)
                    val jsonAdapter = moshiObj.adapter<List<ParsedItem>>(listType)
                    jsonAdapter.fromJson(entry.receiptItemsJson) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
        Dialog(
            onDismissRequest = { selectedEntryForDetails = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header Row with Title & Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (entry.receiptItemsJson != null) "Dettaglio Scontrino" else "Dettaglio Transazione",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(
                            onClick = { selectedEntryForDetails = null },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Chiudi",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Summary Card (Scontrino Info Table)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = entry.description,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Pagato da:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = entry.paidBy,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Data transazione:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(entry.timestamp)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (entry.receiptItemsJson != null) "Porzione Comune:" else "Totale Transazione:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "€${String.format(Locale.US, "%.2f", entry.amount)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (parsedItems.isNotEmpty()) {
                        Text(
                            text = "Articoli nello Scontrino (${parsedItems.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f) // Fills remaining space, allowing scroll inside!
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(parsedItems) { pIndex, pItem ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.clickable {
                                            editingLedgerItemIndex = pIndex
                                            editingLedgerItemByEntry = entry
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Left details column
                                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                // Item title with wrapped spacing, multi-line support and edit icon indicator
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = pItem.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.weight(1f, fill = false),
                                                        maxLines = 3
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Modifica articolo",
                                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }

                                                // Brand & Category subtitle row
                                                if (pItem.brand.isNotBlank() || pItem.category.isNotBlank()) {
                                                    val subtitleText = buildString {
                                                        if (pItem.brand.isNotBlank()) append(pItem.brand)
                                                        if (pItem.brand.isNotBlank() && pItem.category.isNotBlank()) append(" • ")
                                                        if (pItem.category.isNotBlank()) append(pItem.category)
                                                    }
                                                    Text(
                                                        text = subtitleText,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }

                                                // Quantity & price per kg (if available) - mathematically verified in scanner!
                                                if (pItem.weight != null && pItem.pricePerKg != null && pItem.weight > 0 && pItem.pricePerKg > 0) {
                                                    Text(
                                                        text = "${String.format(Locale.US, "%.3f", pItem.weight)} kg x €${String.format(Locale.US, "%.2f", pItem.pricePerKg)}/kg",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                // Shared splitting tag indicators
                                                if (pItem.isShared) {
                                                    Card(
                                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "CONDIVISO 50%",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                } else {
                                                    Card(
                                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "PERSONALE",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                            color = MaterialTheme.colorScheme.secondary,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }

                                            // Price tag column
                                            Text(
                                                text = "€${String.format(Locale.US, "%.2f", pItem.price)}",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Questa transazione è stata inserita manualmente o non contiene elenchi articoli scorporati.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dialog Actions (Close & Delete option)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.deleteLedgerEntry(entry)
                                selectedEntryForDetails = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(25.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("delete_receipt_detail_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Elimina", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Elimina")
                            }
                        }

                        Button(
                            onClick = { selectedEntryForDetails = null },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("close_receipt_details_button"),
                            shape = RoundedCornerShape(25.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chiudi", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingLedgerItemIndex != null && editingLedgerItemByEntry != null) {
        val entry = ledgerEntries.find { it.id == editingLedgerItemByEntry!!.id } ?: editingLedgerItemByEntry!!
        val index = editingLedgerItemIndex!!
        
        val parsedItems = remember(entry.receiptItemsJson) {
            if (entry.receiptItemsJson != null) {
                try {
                    val moshiObj = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                    val listType = Types.newParameterizedType(List::class.java, ParsedItem::class.java)
                    val jsonAdapter = moshiObj.adapter<List<ParsedItem>>(listType)
                    jsonAdapter.fromJson(entry.receiptItemsJson) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
        
        if (index < parsedItems.size) {
            val originalItem = parsedItems[index]
            
            var inputName by remember { mutableStateOf(originalItem.name) }
            var inputBrand by remember { mutableStateOf(originalItem.brand) }
            var inputPriceStr by remember { mutableStateOf(String.format(Locale.US, "%.2f", originalItem.price)) }
            var inputWeightStr by remember { mutableStateOf(originalItem.weight?.let { String.format(Locale.US, "%.3f", it) } ?: "") }
            var inputPricePerKgStr by remember { mutableStateOf(originalItem.pricePerKg?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
            var inputIsShared by remember { mutableStateOf(originalItem.isShared) }
            var selectCategory by remember { mutableStateOf(originalItem.category) }
            
            AlertDialog(
                onDismissRequest = { 
                    editingLedgerItemIndex = null
                    editingLedgerItemByEntry = null
                },
                title = { Text("Modifica Voce Scontrino Registrato", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                text = {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = inputName,
                                onValueChange = { inputName = it },
                                label = { Text("Nome Prodotto") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = inputBrand,
                                onValueChange = { inputBrand = it },
                                label = { Text("Marca") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = inputPriceStr,
                                onValueChange = { inputPriceStr = it },
                                label = { Text("Prezzo (€)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = inputWeightStr,
                                onValueChange = { inputWeightStr = it },
                                label = { Text("Peso / Quantità (kg) (opzionale)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = inputPricePerKgStr,
                                onValueChange = { inputPricePerKgStr = it },
                                label = { Text("Prezzo al kg (€/kg) (opzionale)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = inputIsShared,
                                    onCheckedChange = { inputIsShared = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Spesa Condivisa al 50%", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        
                        item {
                            Text(
                                text = "Seleziona Categoria",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
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
                                            .testTag("ledger_category_chip_$cat")
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
                        
                        // Weighted helper check
                        val calcW = inputWeightStr.toDoubleOrNull()
                        val calcP = inputPricePerKgStr.toDoubleOrNull()
                        if (calcW != null && calcP != null) {
                            val computed = calcW * calcP
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
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
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val newPrice = inputPriceStr.replace(",", ".").toDoubleOrNull() ?: originalItem.price
                            val newWeight = inputWeightStr.replace(",", ".").toDoubleOrNull()
                            val newPricePerKg = inputPricePerKgStr.replace(",", ".").toDoubleOrNull()
                            
                            val updatedItem = originalItem.copy(
                                name = inputName,
                                brand = inputBrand,
                                price = newPrice,
                                weight = newWeight,
                                pricePerKg = newPricePerKg,
                                isShared = inputIsShared,
                                category = selectCategory,
                                confidence = 1.0
                            )
                            viewModel.updateLedgerEntryItem(entry, index, updatedItem)
                            
                            editingLedgerItemIndex = null
                            editingLedgerItemByEntry = null
                        }
                    ) {
                        Text("Salva")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                viewModel.deleteLedgerEntryItem(entry, index)
                                editingLedgerItemIndex = null
                                editingLedgerItemByEntry = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Elimina Articolo")
                        }
                        
                        TextButton(
                            onClick = {
                                editingLedgerItemIndex = null
                                editingLedgerItemByEntry = null
                            }
                        ) {
                            Text("Annulla")
                        }
                    }
                }
            )
        }
    }
}
