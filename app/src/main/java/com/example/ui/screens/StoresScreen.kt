package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.StoreInfo
import com.example.ui.viewmodel.GroceryViewModel
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoresScreen(
    viewModel: GroceryViewModel,
    modifier: Modifier = Modifier
) {
    val stores by viewModel.allStores.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    // Store detail / Edit modal state
    var selectedStoreForDetail by remember { mutableStateOf<StoreInfo?>(null) }
    var storeToEdit by remember { mutableStateOf<StoreInfo?>(null) }
    var editDisplayName by remember { mutableStateOf("") }
    var editAddress by remember { mutableStateOf("") }
    var editVat by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }

    val filteredStores = remember(stores, searchQuery) {
        if (searchQuery.isBlank()) {
            stores
        } else {
            stores.filter { 
                it.name.contains(searchQuery, ignoreCase = true) || 
                (it.displayName?.contains(searchQuery, ignoreCase = true) == true) || 
                (it.vatNumber?.contains(searchQuery) == true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Upper Title & Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Store,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Registro Negozi",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Negozi normalizzati e registrati dall'app",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cerca per nome o Partita IVA...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Pulisci")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("store_search_input")
        )

        if (filteredStores.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "Nessun negozio registrato nel database localmente." else "Nessun risultato trovato.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "Scansiona uno scontrino per registrare e normalizzare in automatico l'attività commerciale." else "Modifica i criteri di ricerca o inseriscine uno manualmente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredStores) { store ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStoreForDetail = store }
                            .testTag("store_card_${store.name.lowercase(Locale.getDefault())}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = store.displayName ?: store.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (store.displayName != null && store.displayName != store.name) {
                                        Text(
                                            text = "Ragione Sociale: ${store.name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            storeToEdit = store
                                            editDisplayName = store.displayName ?: store.name
                                            editAddress = store.address ?: ""
                                            editVat = store.vatNumber ?: ""
                                            editPhone = store.phone ?: ""
                                        },
                                        modifier = Modifier.size(36.dp).testTag("edit_store_button_${store.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Modifica Informazioni Negozio",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteStore(store)
                                        },
                                        modifier = Modifier.size(36.dp).testTag("delete_store_button_${store.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Elimina Negozio",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Details badges
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!store.vatNumber.isNullOrBlank()) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "P.IVA: ${store.vatNumber}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "P.IVA NON ACQUISITA",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(store.lastSeen))
                                    Text(
                                        text = "Ultima spesa: $dateStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (!store.address.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = store.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Button to add a Store manually
        Button(
            onClick = {
                storeToEdit = StoreInfo(name = "", displayName = "", vatNumber = null, address = null, phone = null, lastSeen = System.currentTimeMillis())
                editDisplayName = ""
                editAddress = ""
                editVat = ""
                editPhone = ""
            },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .height(48.dp)
                .testTag("add_store_manually_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Registra Negozio Manualmente", fontWeight = FontWeight.Bold)
            }
        }
    }

    // --- DIALOG FOR STORE DETAILS ---
    if (selectedStoreForDetail != null) {
        val store = selectedStoreForDetail!!
        Dialog(
            onDismissRequest = { selectedStoreForDetail = null },
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
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Dettagli Dell'Attività",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        IconButton(onClick = { selectedStoreForDetail = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Chiudi")
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Nome Standardizzabile:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = store.displayName ?: store.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column {
                                Text(
                                    text = "Ragione Sociale Unica:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = store.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Column {
                                Text(
                                    text = "Codice Partita IVA d'Impresa:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = store.vatNumber ?: "Non rilevata nello scontrino",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (store.vatNumber != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                )
                            }

                            Column {
                                Text(
                                    text = "Numero di Telefono:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = store.phone ?: "Non specificato",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (store.phone != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.secondary
                                )
                            }

                            Column {
                                Text(
                                    text = "Indirizzo Acquisito Sede:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = store.address ?: "In attesa di scansione geolocalizzata",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Column {
                                Text(
                                    text = "Registrazione ultimo passaggio:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                val datetimeStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(store.lastSeen))
                                Text(
                                    text = datetimeStr,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.deleteStore(store)
                                selectedStoreForDetail = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rimuovi Registrazione")
                        }

                        Button(
                            onClick = {
                                storeToEdit = store
                                editDisplayName = store.displayName ?: store.name
                                editAddress = store.address ?: ""
                                editVat = store.vatNumber ?: ""
                                editPhone = store.phone ?: ""
                                selectedStoreForDetail = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Modifica")
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG FOR EDIT / CREATE STORE ---
    if (storeToEdit != null) {
        val originalStore = storeToEdit!!
        AlertDialog(
            onDismissRequest = { storeToEdit = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (originalStore.id == 0) "Nuova Registrazione Negozio" else "Modifica Anagrafica Negozio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (originalStore.id == 0) {
                        var tempRawName by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = tempRawName,
                            onValueChange = { 
                                tempRawName = it
                                editDisplayName = it
                            },
                            label = { Text("Nome della Ragione Sociale Unica") },
                            placeholder = { Text("Es. Esselunga Milano, Conad City...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column {
                            Text("Ragione Sociale Originale:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            Text(originalStore.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedTextField(
                        value = editDisplayName,
                        onValueChange = { editDisplayName = it },
                        label = { Text("Nome Visualizzato Normalizzato") },
                        placeholder = { Text("Es: Esselunga, Coop, Lidl") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editVat,
                        onValueChange = { editVat = it },
                        label = { Text("Codice Partita IVA (11 cifre)") },
                        placeholder = { Text("Partita IVA, es: 01234567890") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editAddress,
                        onValueChange = { editAddress = it },
                        label = { Text("Indirizzo Sede / Negozio") },
                        placeholder = { Text("Es: Via Monte Rosa 4, Milano") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Numero di Telefono") },
                        placeholder = { Text("Es: +39 02 1234567") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanVat = editVat.trim().takeIf { it.isNotBlank() }
                        val cleanAddress = editAddress.trim().takeIf { it.isNotBlank() }
                        val cleanPhone = editPhone.trim().takeIf { it.isNotBlank() }
                        val finalDisplayName = editDisplayName.trim().ifBlank { originalStore.name }

                        if (originalStore.id == 0) {
                            val cleanName = originalStore.name.ifBlank { finalDisplayName }
                            val newStore = StoreInfo(
                                name = cleanName,
                                displayName = finalDisplayName,
                                vatNumber = cleanVat,
                                address = cleanAddress,
                                phone = cleanPhone,
                                lastSeen = System.currentTimeMillis()
                            )
                            viewModel.saveStore(newStore)
                        } else {
                            val updatedStore = originalStore.copy(
                                displayName = finalDisplayName,
                                vatNumber = cleanVat,
                                address = cleanAddress,
                                phone = cleanPhone,
                                lastSeen = System.currentTimeMillis()
                            )
                            viewModel.saveStore(updatedStore)
                        }
                        storeToEdit = null
                    }
                ) {
                    Text("Salva Anagrafica")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { storeToEdit = null }) {
                    Text("Annulla")
                }
            }
        )
    }
}
