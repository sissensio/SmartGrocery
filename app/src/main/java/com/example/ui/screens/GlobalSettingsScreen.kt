package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.api.GeminiServiceClient
import com.example.api.LocalBackendServiceClient
import com.example.ui.theme.SemanticGreen
import com.example.ui.theme.SemanticRed
import com.example.ui.theme.SemanticYellow
import com.example.ui.theme.SemanticBlueInfo
import com.example.ui.viewmodel.GroceryViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSettingsDialog(
    viewModel: GroceryViewModel,
    onDismiss: () -> Unit
) {
    val isGemini by viewModel.isGeminiActive.collectAsState()
    val isLocalLlmActive by viewModel.isLocalLlmActive.collectAsState()
    val isLocalModelDownloaded by viewModel.isLocalModelDownloaded.collectAsState()
    val isOnDeviceAiSupported by viewModel.isOnDeviceAiSupported.collectAsState()
    val useSimulatedCamera by viewModel.useSimulatedCamera.collectAsState()
    val isAdminDeveloperMode by viewModel.isAdminDeveloperMode.collectAsState()
    val ledgerEntries by viewModel.ledgerEntries.collectAsState()

    val token by viewModel.userToken.collectAsState()
    val userEmailState by viewModel.userEmail.collectAsState()
    val isBioEnabled by viewModel.isBiometricEnabled.collectAsState()
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()
    val authError by viewModel.lastAuthError.collectAsState()
    val userRole by viewModel.userRole.collectAsState()

    var localEmailInput by remember { mutableStateOf(userEmailState ?: "sissensio@gmail.com") }
    var localPassInput by remember { mutableStateOf("") }

    var showInitDbConfirmation by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableStateOf("") }
    var showUnsupportedAlert by remember { mutableStateOf(false) }
    var manualOcrText by remember { mutableStateOf("") }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val biometricState by viewModel.biometricSignatureState.collectAsState()

    LaunchedEffect(biometricState) {
        val state = biometricState
        if (state != null) {
            val (challenge, signature, activity) = state
            try {
                val cryptoObject = androidx.biometric.BiometricPrompt.CryptoObject(signature)
                val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Accesso Biometrico")
                    .setSubtitle("Conferma identità per lo sblocco")
                    .setNegativeButtonText("Annulla")
                    .build()
                
                val prompt = androidx.biometric.BiometricPrompt(
                    activity,
                    androidx.core.content.ContextCompat.getMainExecutor(activity),
                    object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            viewModel.cancelBiometricLogin("Errore biometrico: $errString")
                        }
                        override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                            val authSig = result.cryptoObject?.signature
                            if (authSig != null) {
                                try {
                                    authSig.update(challenge.toByteArray(Charsets.UTF_8))
                                    val signed = authSig.sign()
                                    val hexSigned = signed.joinToString("") { "%02x".format(it) }
                                    viewModel.finalizeBiometricLogin(challenge, hexSigned)
                                } catch (e: Exception) {
                                    viewModel.cancelBiometricLogin("Firma rifiutata hardware.")
                                }
                            } else {
                                viewModel.cancelBiometricLogin("Firma crittografica non generata.")
                            }
                        }
                        override fun onAuthenticationFailed() {
                            viewModel.cancelBiometricLogin("Impronta non riconosciuta.")
                        }
                    }
                )
                prompt.authenticate(promptInfo, cryptoObject)
            } catch (e: Exception) {
                viewModel.cancelBiometricLogin("Errore lancio prompt.")
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Header TopBar
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Impostazioni & Diagnostica",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_settings_button")) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Chiudi")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )

                // Settings Contents Scroll frame
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // SECTION 1: GOOGLE CLOUD SERVICE (API KEY STATE)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "SERVIZIO CLOUD AI GOOGLE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Gemini Status",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isGemini) SemanticGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isGemini) "CONFIGURATO" else "OFFLINE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isGemini) SemanticGreen else MaterialTheme.colorScheme.onErrorContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 2: ON-DEVICE LOCAL AI AND COMPATIBILITY
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "AI LOCALE SUL DISPOSITIVO",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Android AICore / Galaxy AI",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Sfrutta l'NPU hardware e le librerie del sistema operativo per elaborare scontrini al 100% offline senza connessione internet.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = isLocalLlmActive,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                if (isLocalModelDownloaded && isOnDeviceAiSupported == true) {
                                                    viewModel.isLocalLlmActive.value = true
                                                } else {
                                                    showUnsupportedAlert = true
                                                }
                                            } else {
                                                viewModel.isLocalLlmActive.value = false
                                            }
                                        },
                                        modifier = Modifier.testTag("settings_local_ai_switch")
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Diagnostica di compatibilità:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (isLocalModelDownloaded) {
                                                if (isOnDeviceAiSupported == true) "Ambiente Compatibile ed Attivo" else "Dispositivo virtuale non idoneo (AICore mancante)"
                                            } else {
                                                "Nessuna diagnostica eseguita"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isLocalModelDownloaded && isOnDeviceAiSupported == true) SemanticGreen else if (isLocalModelDownloaded) SemanticRed else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Button(
                                        onClick = { viewModel.showLocalAiDownloadDialog.value = true },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("settings_run_diagnostic")
                                    ) {
                                        Text("TEST COMPATIBILITÀ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 2.5: ACCOUNT & SICUREZZA BIOMETRICA CRITTOGRAFICA
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "ACCOUNT & SICUREZZA CRITTOGRAFICA",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                if (token != null) {
                                    // User is Authenticated!
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(
                                                imageVector = Icons.Default.AccountCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = userEmailState ?: "sissensio@gmail.com",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Sessione attiva via token JWT criptato",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        
                                        Button(
                                            onClick = { viewModel.logout() },
                                            colors = ButtonDefaults.buttonColors(containerColor = SemanticRed),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("ESCI", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                                    // Biometric Enrollment section
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Impronte Digitali e Firma Keystore",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (isBioEnabled) "Configurato sul chip hardware di sicurezza" else "Genera e registra chiave pubblica RSA",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        if (isBioEnabled) {
                                            Box(
                                                modifier = Modifier
                                                    .background(SemanticGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = SemanticGreen, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "ATTIVO",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = SemanticGreen,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        } else {
                                            Button(
                                                onClick = { viewModel.enrollBiometricKeyPair() },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.testTag("enroll_biometric_button")
                                            ) {
                                                Text("ABILITA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                } else {
                                    // User needs to Authenticate!
                                    Text(
                                        text = "Collegamento Account Famiglia",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Registrati o esegui l'accesso per abilitare lo sblocco biometrico crittografico e i canali di notifica mirati dei coinquilini.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    OutlinedTextField(
                                        value = localEmailInput,
                                        onValueChange = { localEmailInput = it },
                                        label = { Text("Email Coinquilino o Amministratore") },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("auth_email_field"),
                                        leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null) }
                                    )

                                    OutlinedTextField(
                                        value = localPassInput,
                                        onValueChange = { localPassInput = it },
                                        label = { Text("Password Fiduciaria") },
                                        shape = RoundedCornerShape(12.dp),
                                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth().testTag("auth_password_field"),
                                        leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) }
                                    )

                                    if (authError != null) {
                                        Text(
                                            text = authError ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SemanticRed,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.performPasswordRegistration(localEmailInput, localPassInput) },
                                            enabled = localEmailInput.isNotBlank() && localPassInput.isNotBlank() && !isAuthLoading,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f).testTag("register_acc_button")
                                        ) {
                                            Text("REGISTRATI", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                        }

                                        Button(
                                            onClick = { viewModel.performPasswordLogin(localEmailInput, localPassInput) },
                                            enabled = localEmailInput.isNotBlank() && localPassInput.isNotBlank() && !isAuthLoading,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f).testTag("login_acc_button")
                                        ) {
                                            Text("ACCEDI", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }

                                    if (isBioEnabled) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                        
                                        Button(
                                            onClick = { 
                                                val fragmentActivity = context as? androidx.fragment.app.FragmentActivity
                                                if (fragmentActivity != null) {
                                                    viewModel.performBiometricLogin(fragmentActivity)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("biometric_login_quick_button")
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("ACCEDI CON IMPRONTE DIGITALI", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                if (isAuthLoading) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    // SECTION 3: DATABASE & SOVEREIGN GDPR EXPORT
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "GESTIONE DATI & SOVEREIGN GDPR EXPORT",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column {
                                    Text(
                                        text = "Esporta Tutto (Anti Vendor Lock-In)",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "I tuoi dati ti appartengono. Scarica lo storico in svariati formati standard (CSV / PDF) all'interno di un archivio ZIP crittografato offline.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            isExporting = true
                                            exportProgress = "Preparazione tabelle SQLite cifrate..."
                                            showExportDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        shape = RoundedCornerShape(24.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("settings_gdpr_export_button")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Sovereign GDPR Export (ZIP)")
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                                Column {
                                    Text(
                                        text = "Manutenzione",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Resetta l'app cancellando scontrini storici, spese, conguagli e anagrafica dei negozi.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { showInitDbConfirmation = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = SemanticRed),
                                        shape = RoundedCornerShape(24.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("settings_reset_db_button")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Inizializza Database (Wipe Dati)", color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 4: DEVELOPER OPTIONS & DEBUGGING SIMULATORS
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "OPZIONI SVILUPPATORE / DEBUG",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Modalità sviluppatore",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Attiva strumenti di diagnosi, simulatore di geofence e fallback camera per testare offline.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = isAdminDeveloperMode,
                                        onCheckedChange = { checked ->
                                            viewModel.isAdminDeveloperMode.value = checked
                                        },
                                        modifier = Modifier.testTag("settings_developer_mode_switch")
                                    )
                                }

                                AnimatedVisibility(
                                    visible = isAdminDeveloperMode,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.padding(top = 12.dp)
                                    ) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                                        // Diagnostics Section
                                        if (userRole == "ADMIN") {
                                            var revealKey by remember { mutableStateOf(false) }
                                            var showRevealDialog by remember { mutableStateOf(false) }

                                            if (showRevealDialog) {
                                                AlertDialog(
                                                    onDismissRequest = { showRevealDialog = false },
                                                    title = { Text("Attenzione") },
                                                    text = { Text("Sei sicuro di voler mostrare la chiave API in chiaro? Attenzione agli schermi condivisi.") },
                                                    confirmButton = {
                                                        TextButton(onClick = { revealKey = true; showRevealDialog = false }) {
                                                            Text("Rivela")
                                                        }
                                                    },
                                                    dismissButton = {
                                                        TextButton(onClick = { showRevealDialog = false }) {
                                                            Text("Annulla")
                                                        }
                                                    }
                                                )
                                            }

                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = "DIAGNOSTICA CREDENZIALI & CHIAVI",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                                
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                                                        .padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    // Backend IP
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(text = "IP Backend:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                        Text(text = com.example.BuildConfig.LOCAL_BACKEND_IP, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    
                                                    // Gemini Key
                                                    val rawKey = com.example.BuildConfig.GEMINI_API_KEY
                                                    val isKeyValid = rawKey.isNotBlank() && rawKey != "MY_GEMINI_API_KEY"
                                                    val maskedKey = if (isKeyValid && rawKey.length > 8) "${rawKey.take(9)}...${rawKey.takeLast(3)}" else rawKey
                                                    
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(text = "Chiave Gemini API:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                            if (isKeyValid) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .padding(top = 4.dp)
                                                                        .background(SemanticGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                                ) {
                                                                    Text("Configurata (${rawKey.length} chars)", style = MaterialTheme.typography.labelSmall, color = SemanticGreen, fontWeight = FontWeight.Bold)
                                                                }
                                                            } else {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .padding(top = 4.dp)
                                                                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
                                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                                ) {
                                                                    Text("Non configurata / Default", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                                                                }
                                                            }
                                                        }
                                                        
                                                        if (isKeyValid) {
                                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
                                                                Text(
                                                                    text = if (revealKey) rawKey else maskedKey,
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    modifier = Modifier.clickable(enabled = revealKey) {
                                                                        val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                                        val clip = android.content.ClipData.newPlainText("API Key", rawKey)
                                                                        clipboardManager.setPrimaryClip(clip)
                                                                    }
                                                                )
                                                                IconButton(onClick = { if (revealKey) revealKey = false else showRevealDialog = true }) {
                                                                    Icon(imageVector = if (revealKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = "Mostra Chiave", modifier = Modifier.size(20.dp))
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Camera simulation
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "SIMULATORE DI FOTOCAMERA",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Simula Messa a Fuoco / OCR",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "Disattiva per utilizzare il sensore camera del telefono reale. Attiva per simulare layout statico scontrino.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Switch(
                                                    checked = useSimulatedCamera,
                                                    onCheckedChange = { checked ->
                                                        viewModel.useSimulatedCamera.value = checked
                                                    },
                                                    modifier = Modifier.testTag("settings_simulated_camera_switch")
                                                )
                                            }
                                        }

                                        // Target di elaborazione OCR (Gemini vs Backend) - Visibile solo in modalità sviluppatore
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "TARGET DI ELABORAZIONE OCR",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = if (com.example.BuildConfig.SEND_OCR_TO_BACKEND) {
                                                            "Backend Server Locale"
                                                        } else {
                                                            "Gemini Cloud API"
                                                        },
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (com.example.BuildConfig.SEND_OCR_TO_BACKEND) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.secondary
                                                        }
                                                    )
                                                    Text(
                                                        text = if (com.example.BuildConfig.SEND_OCR_TO_BACKEND) {
                                                            "L'app invia i testi OCR o scontrini al backend per l'elaborazione (IP: ${com.example.BuildConfig.LOCAL_BACKEND_IP})."
                                                        } else {
                                                            "L'app interroga direttamente l'infrastruttura Google Cloud Gemini con connessione sicura via API Key."
                                                        },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .background(
                                                            if (com.example.BuildConfig.SEND_OCR_TO_BACKEND) SemanticBlueInfo else SemanticGreen,
                                                            CircleShape
                                                        )
                                                )
                                            }
                                        }

                                        // Geofencing simulator relocated from Home
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "SIMULATORE DI GEOFENCING PASSIVO",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = "Simula l'allontanamento fisico da un supermercato sensibile per attivare il trigger della barra di pagamento.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        viewModel.triggerSimulatedGeofenceEntrance("Esselunga")
                                                        onDismiss()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                    shape = RoundedCornerShape(20.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("settings_simulate_esselunga")
                                                ) {
                                                    Text("Esci Esselunga", style = MaterialTheme.typography.labelSmall)
                                                }
                                                Button(
                                                    onClick = {
                                                        viewModel.triggerSimulatedGeofenceEntrance("Lidl")
                                                        onDismiss()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                    shape = RoundedCornerShape(20.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("settings_simulate_lidl")
                                                ) {
                                                    Text("Esci Lidl", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }

                                        // PRESET SELECTIONS FOR TESTING FLUIDITY (Relocated under Developer Mode)
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "PRESET SCONTRINI (SIMULAZIONE OCR)",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = "Seleziona uno scontrino d'esempio per simulare l'OCR parsing e i match Jaro-Winkler.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val lidlPreset = """
                                                    LIDL ITALIA S.R.L.
                                                    VIA MILANO, 5 - SEGRATE
                                                    
                                                    FETTE MISURA     1.69
                                                    PANNA CUCINA     0.99
                                                    CAFFE ARABICA    3.49
                                                    MELE GOLDEN PESO kg 1.25 X 1.95/kg   2.44
                                                    SUCCO DI FRUTTA DI PROVA  5.32
                                                    
                                                    TOTALE EURO     13.93
                                                """.trimIndent()

                                                val esselungaPreset = """
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

                                                Button(
                                                    onClick = {
                                                        viewModel.processScanningWithGemini(lidlPreset)
                                                        onDismiss()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                    shape = RoundedCornerShape(20.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("preset_lidl")
                                                ) {
                                                    Text("Scontrino Lidl", style = MaterialTheme.typography.labelSmall)
                                                }
                                                Button(
                                                    onClick = {
                                                        viewModel.processScanningWithGemini(esselungaPreset)
                                                        onDismiss()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                    shape = RoundedCornerShape(20.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("preset_esselunga")
                                                ) {
                                                    Text("Scontrino Esselunga", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }

                                        // Custom OCR Input (Relocated under Developer Mode)
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "TESTO OCR PERSONALIZZATO",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            OutlinedTextField(
                                                value = manualOcrText,
                                                onValueChange = { manualOcrText = it },
                                                placeholder = { Text("Inserisci righe dello scontrino estrapolate qui...", style = MaterialTheme.typography.bodySmall) },
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(80.dp)
                                                    .testTag("ocr_custom_text_input")
                                            )
                                            Button(
                                                onClick = {
                                                    if (manualOcrText.isNotBlank()) {
                                                        viewModel.processScanningWithGemini(manualOcrText)
                                                        onDismiss()
                                                    }
                                                },
                                                enabled = manualOcrText.isNotBlank(),
                                                shape = RoundedCornerShape(20.dp),
                                                modifier = Modifier
                                                    .align(Alignment.End)
                                                    .testTag("submit_ocr_text_button")
                                            ) {
                                                Text("Invia ed Elabora OCR", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(110.dp))
                }
            }
        }
    }

    if (showUnsupportedAlert) {
        AlertDialog(
            onDismissRequest = { showUnsupportedAlert = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Attenzione",
                    tint = SemanticYellow,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Test di Compatibilità Richiesto",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "Non è possibile attivare l'AI Locale di sistema (AICore / Galaxy AI) senza prima aver superato con esito positivo il test di compatibilità hardware sul dispositivo.\n\nEsegui il test diagnostico per verificare l'idoneità del processore e dei moduli nativi del tuo telefono.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnsupportedAlert = false
                        viewModel.showLocalAiDownloadDialog.value = true
                    }
                ) {
                    Text("Esegui Test")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsupportedAlert = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    // INIT DB WARNING DIALOG
    if (showInitDbConfirmation) {
        AlertDialog(
            onDismissRequest = { showInitDbConfirmation = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = "Attenzione", tint = SemanticRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Inizializza Database?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Sei sicuro di voler resettare l'applicazione? Tutti i dati (spese ordinarie, scontrini scansionati, conguagli storici ed elenco dei negozi registrati) verranno cancellati in modo DEFINITIVO SIA IN LOCALE SIA SUL SERVER DI BACKEND, e non potranno quindi essere più ripristinati in nessun modo.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.initializeDatabase()
                        showInitDbConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SemanticRed),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.testTag("confirm_reset_db")
                ) {
                    Text("Sì, Inizializza", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showInitDbConfirmation = false },
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Annulla")
                }
            }
        )
    }

    // EXPORT PROCESS DIALOG (Relocated from Ledger)
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { if (!isExporting) showExportDialog = false },
            title = { Text("Anti Vendor Lock-In Export") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = exportProgress,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    if (isExporting) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        
                        LaunchedEffect(Unit) {
                            delay(1000)
                            exportProgress = "Estrapolazione scontrini e contanti privati..."
                            delay(1000)
                            exportProgress = "Generazione file 'smart_grocery_ledger_export.csv'..."
                            delay(1000)
                            exportProgress = "Cucitura PDF compendio finale..."
                            delay(800)
                            isExporting = false
                            exportProgress = "Archivio ZIP generato con successo! I tuoi dati sono liberi ed offline."
                        }
                    }
                }
            },
            confirmButton = {
                val context = androidx.compose.ui.platform.LocalContext.current
                Button(
                    onClick = {
                        showExportDialog = false
                        exportAndShareData(context, ledgerEntries)
                    },
                    enabled = !isExporting,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.testTag("settings_close_export_dialog")
                ) {
                    Text("Scarica / Condividi")
                }
            }
        )
    }
}

fun exportAndShareData(context: android.content.Context, databaseEntries: List<com.example.data.LedgerEntry>) {
    try {
        val cacheDir = context.cacheDir
        val exportFile = java.io.File(cacheDir, "smart_grocery_ledger_export.csv")
        exportFile.bufferedWriter().use { writer ->
            writer.write("ID,Descrizione,Importo,PagatoDa,Data,Stato,ArticoliJson\n")
            databaseEntries.forEach { entry ->
                val status = if (entry.isSettled) "Saldato" else "In sospeso"
                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(entry.timestamp))
                val sanitizedDesc = entry.description.replace("\"", "\"\"")
                val sanitizedItems = (entry.receiptItemsJson ?: "").replace("\"", "\"\"")
                writer.write("${entry.id},\"$sanitizedDesc\",${entry.amount},\"${entry.paidBy}\",\"$dateStr\",\"$status\",\"$sanitizedItems\"\n")
            }
        }
        
        val authority = "${context.packageName}.fileprovider"
        val fileUri = androidx.core.content.FileProvider.getUriForFile(context, authority, exportFile)
        
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "SmartGrocery Ledger Export")
            putExtra(android.content.Intent.EXTRA_TEXT, "Ecco lo storico scontrini ed export contabilità di SmartGrocery Manager.")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooserIntent = android.content.Intent.createChooser(intent, "Salva o Invia Archivio GDPR")
        chooserIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "Errore nell'esportazione: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
    }
}

@Composable
fun LocalAiDownloadDialog(viewModel: GroceryViewModel) {
    val showLocalAiDownloadDialog by viewModel.showLocalAiDownloadDialog.collectAsState()
    val isDownloadingModel by viewModel.isDownloadingModel.collectAsState()
    val modelDownloadProgress by viewModel.modelDownloadProgress.collectAsState()
    val modelDownloadStep by viewModel.modelDownloadStep.collectAsState()

    if (showLocalAiDownloadDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isDownloadingModel) {
                    viewModel.showLocalAiDownloadDialog.value = false
                    viewModel.isLocalLlmActive.value = false
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (isDownloadingModel) "Analisi in corso..." else "Verifica Coprocessore AI Locale",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isDownloadingModel) {
                        Text(
                            text = "Interrogazione coprocessori hardware SoC (NPU), verifica integrazione Android AICore e servizi Samsung/Xiaomi...",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        
                        LinearProgressIndicator(
                            progress = modelDownloadProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = modelDownloadStep,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(modelDownloadProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
                            text = "La scansione offline al 100% richiede la presenza di un coprocessore neurale fisico (NPU) o di servizi AI forniti dal sistema operativo del tuo telefono (Android AICore di Google, Samsung Galaxy AI, o moduli equivalenti Xiaomi/Oppo).\n\nL'app eseguirà una diagnostica hardware e software per verificare l'effettiva compatibilità ed evitare simulazioni ingannevoli.\n\nVuoi procedere con il test di compatibilità?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                if (!isDownloadingModel) {
                    Button(
                        onClick = { viewModel.startLocalLlmDownload() }
                    ) {
                        Text("Esegui Diagnostica")
                    }
                }
            },
            dismissButton = {
                if (!isDownloadingModel) {
                    TextButton(
                        onClick = {
                            viewModel.showLocalAiDownloadDialog.value = false
                            viewModel.isLocalLlmActive.value = false
                        }
                    ) {
                        Text("Annulla")
                    }
                }
            }
        )
    }
}

@Composable
fun LocalAiSuccessDialog(viewModel: GroceryViewModel) {
    val showLocalAiSuccessDialog by viewModel.showLocalAiSuccessDialog.collectAsState()
    val isOnDeviceAiSupported by viewModel.isOnDeviceAiSupported.collectAsState()
    val onDeviceAiDiagnosticResult by viewModel.onDeviceAiDiagnosticResult.collectAsState()
    val scrollState = rememberScrollState()

    if (showLocalAiSuccessDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showLocalAiSuccessDialog.value = false },
            icon = {
                Icon(
                    imageVector = if (isOnDeviceAiSupported == true) Icons.Default.CheckCircle else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (isOnDeviceAiSupported == true) SemanticGreen else if (isOnDeviceAiSupported == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = if (isOnDeviceAiSupported == true) "AI Locale Attivata!" else "Risultato Diagnostica Locale",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isOnDeviceAiSupported == true) {
                            "Congratulazioni! Il tuo dispositivo supporta l'accelerazione neurale e l'elaborazione offline."
                        } else if (isOnDeviceAiSupported == false) {
                            "Ambiente non compatibile con l'elaborazione neurale offline."
                        } else {
                            "Diagnostica completata."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isOnDeviceAiSupported == true) SemanticGreen else if (isOnDeviceAiSupported == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        ) {
                            Text(
                                text = onDeviceAiDiagnosticResult,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.showLocalAiSuccessDialog.value = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOnDeviceAiSupported == true) SemanticGreen else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(if (isOnDeviceAiSupported == true) "Attiva offline" else "Ho Capito")
                }
            }
        )
    }
}
