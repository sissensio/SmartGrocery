package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ShoppingList
import com.example.data.GroceryItem
import com.example.ui.theme.SemanticGreen
import com.example.ui.theme.SemanticRed
import com.example.ui.theme.SemanticYellow
import com.example.ui.viewmodel.GroceryViewModel
import java.util.Locale
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(
    viewModel: GroceryViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.shoppingList.collectAsState()
    val backendShoppingLists by viewModel.shoppingLists.collectAsState(initial = emptyList())
    val threshold by viewModel.indifferenceThreshold.collectAsState()

    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Dispensa") }
    var isShared by remember { mutableStateOf(true) }

    val categories = listOf("Dispensa", "Latticini", "Colazione", "Macelleria", "Bevande", "Igiene e Casa")

    // Dynamic cost opportunity simulation values
    var kmExtra by remember { mutableStateOf(4.5f) }
    var timeSaved by remember { mutableStateOf(20f) }
    
    var showShareSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var listToShare by remember { mutableStateOf<ShoppingList?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Shopping List Title & Count
        item {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "La tua Spesa",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black
                    )
                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                        Text(
                            text = "${items.size} articoli",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Text(
                    text = "Aggrega scorte stimate mancanti (Daily Need) e liste comuni",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Add Item Form
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = "Add Item",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Aggiungi Articolo Manuale",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nome (es: Latte)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("item_name_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            label = { Text("Marca (es: Granarolo)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("item_brand_input"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = priceStr,
                            onValueChange = { priceStr = it },
                            label = { Text("Prezzo (€)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("item_price_input"),
                            singleLine = true
                        )
                        
                        // Category Selector chip row
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Categoria:", style = MaterialTheme.typography.labelSmall)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        val nextIndex = (categories.indexOf(category) + 1) % categories.size
                                        category = categories[nextIndex]
                                    }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Change Cat"
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Smart-Split selector (Section 7.1)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isShared,
                                onCheckedChange = { isShared = it },
                                modifier = Modifier.testTag("item_split_checkbox")
                            )
                            Text(
                                text = if (isShared) "Condiviso / Spazio Casa" else "Privato (Invisibile)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isShared) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        }

                        Button(
                            onClick = {
                                val p = priceStr.toDoubleOrNull() ?: 0.0
                                viewModel.addItemToList(name, brand, p, category, isShared)
                                name = ""
                                brand = ""
                                priceStr = ""
                            },
                            enabled = name.isNotBlank(),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.testTag("item_add_button")
                        ) {
                            Text("Aggiungi")
                        }
                    }
                }
            }
        }

        // Active Shopping list rendering
        item {
            Text(
                text = "Articoli Consigliati & Pianificati",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (items.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Nulla da comprare",
                            tint = SemanticGreen,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Lista della spesa vuota!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Aggiungi articoli manualmente o simula una scansione scontrino per aggiornare le preferenze quotidiane.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(items) { item ->
                val color = when (item.urgencyColor.uppercase()) {
                    "RED" -> SemanticRed
                    "YELLOW" -> SemanticYellow
                    else -> SemanticGreen
                }
                
                // Shrinkflation price check simulation (Section 8.1)
                // If it's the RED latte or yellow fette, let's trigger inflation warning
                val hasInflationWarning = item.urgencyColor.uppercase() == "RED" || item.brand == "Misura"

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grocery_item_${item.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            // Urgency traffic-light semaphore (Section 6.1)
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .testTag("item_urgency_${item.id}")
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (hasInflationWarning) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.TrendingUp,
                                            contentDescription = "Inflation Alert",
                                            tint = SemanticRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "RINCARI",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SemanticRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = "${if (item.brand.isNotBlank()) "${item.brand} • " else ""}${item.category}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                if (hasInflationWarning) {
                                    Text(
                                        text = "In negozio costa il 15% in più della tua media",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SemanticRed
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "€${String.format(Locale.US, "%.2f", item.price)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = { viewModel.markItemPurchased(item) },
                                modifier = Modifier.testTag("item_purchase_check_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Mark Purchased",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.deleteItem(item) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = SemanticRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- BACKEND SHOPPING LISTS (Multi-liste) ---
        if (backendShoppingLists.isNotEmpty()) {
            item {
                Text(
                    text = "Liste Condivise (Backend)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            
            items(backendShoppingLists) { list ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = list.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Condiviso con ${list.sharedWithGroupIds.size + list.sharedWithUserIds.size} membri",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { 
                                listToShare = list
                                showShareSheet = true
                             }) {
                                Icon(Icons.Default.Share, contentDescription = "Condividi Lista", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (list.items.isEmpty()) {
                            Text("Nessun articolo per ora.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        } else {
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                list.items.take(3).forEach { pItem ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("• ${pItem.name}", style = MaterialTheme.typography.bodyMedium)
                                        Text("${pItem.quantity}x", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (list.items.size > 3) {
                                    Text(
                                        text = "Vedi altri ${list.items.size - 3}...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        // --- END BACKEND SHOPPING LISTS ---

        // TEOREMA DEL COSTO OPPORTUNITA DECISION CARD (Section 8.2)
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cost_opportunity_module")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Costo Opportunità",
                            tint = SemanticYellow,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Teorema del Costo Opportunità",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "L'app calcola la soglia di convenienza per dividere la spesa in più tappe in base al carburante e al tempo stimato.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Calculations variables sliders
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Distanza Extra: ${String.format(Locale.US, "%.1f", kmExtra)} km",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Tempo Extra: ${timeSaved.toInt()} min",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Slider(
                            value = kmExtra,
                            onValueChange = { kmExtra = it },
                            valueRange = 1f..15f,
                            modifier = Modifier.testTag("km_slider")
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Soglia Indifferenza: €${String.format(Locale.US, "%.2f", threshold)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Slider(
                            value = threshold.toFloat(),
                            onValueChange = { viewModel.indifferenceThreshold.value = it.toDouble() },
                            valueRange = 0.5f..5.0f,
                            modifier = Modifier.testTag("threshold_slider")
                        )
                    }

                    // OPPORTUNITA ANALYZER MATH DESIGN
                    val savingsGroceries = 3.60 // simulated item savings on Option A (division)
                    val fuelCostPerKm = 0.16 // estimated fuel cost model
                    val finalFuelCost = kmExtra * fuelCostPerKm
                    val netSavings = savingsGroceries - finalFuelCost
                    val decisionRecommended = netSavings > threshold

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    if (decisionRecommended) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(SemanticGreen.copy(alpha = 0.15f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = "Recommended Opt A",
                                tint = SemanticGreen,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "CONSIGLIATO: Opzione A (2 Tappe)",
                                    fontWeight = FontWeight.Bold,
                                    color = SemanticGreen,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Risparmio netto stimato di €${String.format(Locale.US, "%.2f", netSavings)} (Spesa: -€$savingsGroceries, Carburante: +€${String.format(Locale.US, "%.2f", finalFuelCost)}) supera la tua soglia di €${String.format(Locale.US, "%.2f", threshold)}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.HomeWork,
                                contentDescription = "Recommended Opt B",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "CONSIGLIATO: Opzione B (Tappa Unica)",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Il risparmio netto di €${String.format(Locale.US, "%.2f", netSavings)} NO supera i costi extra del tempo (${timeSaved.toInt()}m) e traffico rispetto alla tua soglia di €${String.format(Locale.US, "%.2f", threshold)}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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

    if (showShareSheet && listToShare != null) {
        ModalBottomSheet(
            onDismissRequest = { showShareSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Condividi '${listToShare?.name}'",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Aggiungi amici o un intero Gruppo Spesa per collaborare in tempo reale (sincronizzazione ogni 20s).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Placeholder inputs for inviting
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    label = { Text("Profile Code Amico o ID Gruppo") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { showShareSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Invia Invito", modifier = Modifier.padding(vertical = 8.dp))
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
