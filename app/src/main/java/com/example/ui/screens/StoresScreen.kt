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

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun StoresScreen(
    viewModel: GroceryViewModel,
    modifier: Modifier = Modifier
) {
    val stores by viewModel.allStores.collectAsState()
    val ledgerEntries by viewModel.ledgerEntries.collectAsState()
    val pendingReceipts by viewModel.pendingReceipts.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    // Store detail / Edit modal state
    var selectedStoreForDetail by remember { mutableStateOf<StoreInfo?>(null) }
    var storeToEdit by remember { mutableStateOf<StoreInfo?>(null) }
    var editDisplayName by remember { mutableStateOf("") }
    var editAddress by remember { mutableStateOf("") }
    var editVat by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editLat by remember { mutableStateOf<Double?>(null) }
    var editLng by remember { mutableStateOf<Double?>(null) }

    val filteredStores = remember(stores, searchQuery, ledgerEntries, pendingReceipts) {
        val validStores = stores.filter { store ->
            val isLocal = !store.isCertified
            
            val inLedger = ledgerEntries.any { entry ->
                val desc = entry.description.lowercase(Locale.getDefault())
                desc.contains(store.name) || 
                (store.displayName != null && desc.contains(store.displayName.lowercase(Locale.getDefault())))
            }
            
            val inPending = pendingReceipts.any { pr ->
                pr.storeId == store.id || 
                pr.storeName.lowercase(Locale.getDefault()).trim() == store.name || 
                (store.displayName != null && pr.storeName.lowercase(Locale.getDefault()).trim() == store.displayName.lowercase(Locale.getDefault()).trim())
            }
            
            isLocal || inLedger || inPending
        }

        if (searchQuery.isBlank()) {
            validStores
        } else {
            validStores.filter { 
                it.name.contains(searchQuery, ignoreCase = true) || 
                (it.displayName?.contains(searchQuery, ignoreCase = true) == true) || 
                (it.vatNumber?.contains(searchQuery) == true)
            }
        }
    }
    
    val selectedComparison by viewModel.selectedComparison.collectAsState()
    var currentTab by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(selectedComparison) {
        if (selectedComparison != null) {
            currentTab = 1
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TabRow(
            selectedTabIndex = currentTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp).clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = currentTab == 0,
                onClick = { currentTab = 0 },
                text = { Text("Supermercati") },
                icon = { Icon(Icons.Default.Store, contentDescription = null) }
            )
            Tab(
                selected = currentTab == 1,
                onClick = { currentTab = 1 },
                text = { Text("Trova Prezzi") },
                icon = { Icon(Icons.Default.Search, contentDescription = null) }
            )
        }

        if (currentTab == 0) {
            val context = androidx.compose.ui.platform.LocalContext.current
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val bgLocationPermissionState = com.google.accompanist.permissions.rememberPermissionState(
                    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
                if (!bgLocationPermissionState.status.isGranted) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Geofencing richiede 'Consenti sempre'",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Per ricordarti di inserire lo scontrino quando esci da un supermercato, consenti all'app di accedere alla posizione in background in ogni momento.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                            )
                            Button(
                                onClick = {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = android.net.Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    } else {
                                        bgLocationPermissionState.launchPermissionRequest()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Attiva nei Settings")
                            }
                        }
                    }
                }
            }

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

                                if (!store.isCertified) {
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
                                } else {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Certified Supermarket",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Certificato",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
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
                                    val dateStr = if (store.lastSeen > 0L) {
                                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(store.lastSeen))
                                    } else {
                                        "Nessuna"
                                    }
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
                storeToEdit = StoreInfo(name = "", displayName = "", vatNumber = null, address = null, phone = null, lastSeen = 0L)
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
        } else {
            SmartShoppingTab(viewModel = viewModel)
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
                                val datetimeStr = if (store.lastSeen > 0L) {
                                    SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(store.lastSeen))
                                } else {
                                    "Nessuna spesa registrata"
                                }
                                Text(
                                    text = datetimeStr,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    if (!store.isCertified) {
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
                                    editLat = store.latitude
                                    editLng = store.longitude
                                    selectedStoreForDetail = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Modifica")
                            }
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Certified",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Questo supermercato è certificato ufficiale e non può essere modificato o rimosso.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
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

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            if (editLat != null && editLng != null) {
                                Text("Geofence Attivo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                Text("\${editLat.toString().take(6)}, \${editLng.toString().take(6)}", style = MaterialTheme.typography.labelSmall)
                            } else {
                                Text("No Geofence", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val locationPermissionState = com.google.accompanist.permissions.rememberMultiplePermissionsState(
                            listOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                        Button(onClick = {
                            if (locationPermissionState.allPermissionsGranted) {
                                try {
                                    val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                                    fusedLocationClient.lastLocation.addOnSuccessListener { location: android.location.Location? ->
                                        if (location != null) {
                                            editLat = location.latitude
                                            editLng = location.longitude
                                        }
                                    }
                                } catch (e: SecurityException) {
                                    // Handle
                                }
                            } else {
                                locationPermissionState.launchMultiplePermissionRequest()
                            }
                        }) {
                            Text("Imposta Qui")
                        }
                    }
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
                                latitude = editLat,
                                longitude = editLng,
                                lastSeen = 0L
                            )
                            viewModel.saveStore(newStore)
                        } else {
                            val updatedStore = originalStore.copy(
                                displayName = finalDisplayName,
                                vatNumber = cleanVat,
                                address = cleanAddress,
                                phone = cleanPhone,
                                latitude = editLat,
                                longitude = editLng,
                                lastSeen = originalStore.lastSeen
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartShoppingTab(viewModel: GroceryViewModel) {
    val searchResults by viewModel.catalogSearchResults.collectAsState()
    val comparison by viewModel.selectedComparison.collectAsState()
    var query by remember { mutableStateOf("") }
    
    val allBoughtItems by viewModel.allItems.collectAsState()
    val distinctItems = remember(allBoughtItems) {
        allBoughtItems
            .filter { it.isPurchased }
            .groupBy { it.name.trim().lowercase() }
            .map { entry -> entry.value.maxByOrNull { it.id }!! }
            .sortedBy { it.name }
    }
    
    val filteredLocalItems = remember(query, distinctItems) {
        if (query.isBlank()) distinctItems
        else distinctItems.filter { it.name.contains(query, ignoreCase = true) }
    }

    var editingItem by remember { mutableStateOf<com.example.data.GroceryItem?>(null) }
    var editingCatalogItem by remember { mutableStateOf<com.example.api.CatalogPriceComparisonItem?>(null) }
    var confirmDeleteCatalogItem by remember { mutableStateOf<com.example.api.CatalogPriceComparisonItem?>(null) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PriceCheck, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Smart Shopping",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Cerca o confronta gli articoli acquistati",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { 
                query = it
                viewModel.searchCatalog(it)
            },
            placeholder = { Text("Cerca prodotto o filtra (es. Salsa Mutti)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { 
                        query = ""
                        viewModel.searchCatalog("")
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            
            if (filteredLocalItems.isNotEmpty()) {
                item {
                    Text("I Miei Articoli (${filteredLocalItems.size})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                }
                items(filteredLocalItems) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            viewModel.fetchComparison(item.barcode.takeIf { it.isNotBlank() }, item.name)
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                if (item.brand.isNotBlank()) {
                                    Text(item.brand, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                }
                                Text("Ultimo prezzo: €${String.format(java.util.Locale.US, "%.2f", item.price)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                            IconButton(onClick = { editingItem = item }) {
                                Icon(Icons.Default.Edit, contentDescription = "Modifica", tint = MaterialTheme.colorScheme.primary)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            if (searchResults.isNotEmpty() && query.isNotBlank()) {
                item {
                    Text("Risultati dal Catalogo Globale", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                }
                items(searchResults) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            viewModel.fetchComparison(item.barcode, item.name)
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                if (item.brand != null) {
                                    Text(item.brand, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                }
                                if (item.barcode != null) {
                                    Text("Barcode: ${item.barcode}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
    
    if (editingItem != null) {
        var inputName by remember { mutableStateOf(editingItem!!.name) }
        var inputBrand by remember { mutableStateOf(editingItem!!.brand) }
        var inputPriceStr by remember { mutableStateOf(editingItem!!.price.toString()) }
        var selectCategory by remember { mutableStateOf(editingItem!!.category) }

        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Modifica Articolo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Nome Prodotto") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputBrand,
                        onValueChange = { inputBrand = it },
                        label = { Text("Marca") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputPriceStr,
                        onValueChange = { inputPriceStr = it },
                        label = { Text("Prezzo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = selectCategory,
                        onValueChange = { selectCategory = it },
                        label = { Text("Categoria") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val p = inputPriceStr.replace(",", ".").toDoubleOrNull() ?: editingItem!!.price
                    viewModel.updateGroceryItem(editingItem!!.copy(
                        name = inputName,
                        brand = inputBrand,
                        price = p,
                        category = selectCategory
                    ))
                    editingItem = null
                }) {
                    Text("Salva")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) {
                    Text("Annulla")
                }
            }
        )
    }

    if (comparison != null) {
        Dialog(onDismissRequest = { viewModel.clearComparison() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.95f).clip(RoundedCornerShape(24.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(comparison!!.productName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            if (comparison!!.brand != null) {
                                Text(comparison!!.brand!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                            }
                            
                            // Visualizzazione Categoria e Peso nella parte superiore
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                comparison!!.category?.takeIf { it.isNotBlank() }?.let { cat ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = cat,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                comparison!!.weight?.takeIf { it > 0.0 }?.let { w ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${w}g/kg",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                        IconButton(onClick = { viewModel.clearComparison() }) {
                            Icon(Icons.Default.Close, contentDescription = "Chiudi")
                        }
                    }

                    if (comparison!!.prices.isEmpty()) {
                        Text("Nessun prezzo registrato", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(comparison!!.prices.sortedBy { it.price }) { priceItem ->
                                val isBestPrice = priceItem == comparison!!.prices.minByOrNull { it.price }
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isBestPrice) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(priceItem.storeName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                                if (isBestPrice) {
                                                    Text("★ Miglior Prezzo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                }
                                                if (priceItem.discountLabel != null) {
                                                    Text(priceItem.discountLabel, style = MaterialTheme.typography.labelSmall, color = com.example.ui.theme.SemanticRed)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                val timeStr = priceItem.scannedAt.substringBefore("T") // fast format
                                                Text("Rilevato da ${priceItem.scannedBy} il $timeStr", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                            }
                                            
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                val priceStr = String.format(java.util.Locale.US, "%.2f€", priceItem.price)
                                                Text(
                                                    text = priceStr,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = if (isBestPrice) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                
                                                val itemId = priceItem.catalogItemId ?: priceItem.id
                                                if (itemId != null) {
                                                    IconButton(
                                                        onClick = { editingCatalogItem = priceItem },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Modifica",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { confirmDeleteCatalogItem = priceItem },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Elimina",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(18.dp)
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
        }
    }

    if (confirmDeleteCatalogItem != null) {
        val target = confirmDeleteCatalogItem!!
        val itemId = target.catalogItemId ?: target.id
        AlertDialog(
            onDismissRequest = { confirmDeleteCatalogItem = null },
            title = { Text("Elimina Voce Listino") },
            text = { Text("Sei sicuro di voler eliminare l'offerta di ${target.storeName} al prezzo di €${String.format(java.util.Locale.US, "%.2f", target.price)}?") },
            confirmButton = {
                Button(
                    onClick = {
                        if (itemId != null) {
                            viewModel.deleteCatalogItem(itemId, comparison?.barcode)
                        }
                        confirmDeleteCatalogItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteCatalogItem = null }) {
                    Text("Annulla")
                }
            }
        )
    }

    if (editingCatalogItem != null) {
        val target = editingCatalogItem!!
        val itemId = target.catalogItemId ?: target.id
        var editPriceStr by remember(target) { mutableStateOf(target.price.toString()) }
        var editBrand by remember(target) { mutableStateOf(comparison?.brand ?: "") }
        var editCategory by remember(target) { mutableStateOf(comparison?.category ?: "") }
        var editWeightStr by remember(target) { mutableStateOf(comparison?.weight?.toString() ?: "") }
        var editDiscountLabel by remember(target) { mutableStateOf(target.discountLabel ?: "") }

        AlertDialog(
            onDismissRequest = { editingCatalogItem = null },
            title = { Text("Modifica Voce Listino") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Negozio: ${target.storeName}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = editPriceStr,
                        onValueChange = { editPriceStr = it },
                        label = { Text("Prezzo (€)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editBrand,
                        onValueChange = { editBrand = it },
                        label = { Text("Marca") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        label = { Text("Categoria") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editWeightStr,
                        onValueChange = { editWeightStr = it },
                        label = { Text("Peso / Volume") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDiscountLabel,
                        onValueChange = { editDiscountLabel = it },
                        label = { Text("Etichetta Sconto (es. -20%)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedPrice = editPriceStr.replace(",", ".").toDoubleOrNull() ?: target.price
                        val parsedWeight = editWeightStr.replace(",", ".").toDoubleOrNull()
                        val updateDto = com.example.api.CatalogItemCreate(
                            barcode = comparison?.barcode ?: "",
                            name = comparison?.productName ?: "",
                            brand = editBrand.ifBlank { null },
                            category = editCategory.ifBlank { null },
                            price = parsedPrice,
                            unitPrice = target.unitPrice,
                            weight = parsedWeight,
                            discountLabel = editDiscountLabel.ifBlank { null },
                            storeName = target.storeName,
                            vatNumber = null
                        )
                        if (itemId != null) {
                            viewModel.updateCatalogItem(itemId, updateDto, comparison?.barcode)
                        }
                        editingCatalogItem = null
                    }
                ) {
                    Text("Salva")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCatalogItem = null }) {
                    Text("Annulla")
                }
            }
        )
    }
}
