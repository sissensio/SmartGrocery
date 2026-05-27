package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodel.GroceryViewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LedgerScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.ShoppingScreen
import com.example.ui.screens.StoresScreen
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.Store
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Safety crash logging to prevent silent broken IPC channels
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("CRASH_DETECTOR", "Uncaught exception in thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
        
        // Mandatory edge-to-edge flow for modern immersive display
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val viewModel: GroceryViewModel = viewModel()
                var currentTab by remember { mutableStateOf(0) }

                val isFullScreenCamera by viewModel.isFullScreenCameraOpen.collectAsState()
                LaunchedEffect(isFullScreenCamera) {
                    if (isFullScreenCamera) {
                        currentTab = 2
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (!isFullScreenCamera) {
                            NavigationBar(
                                modifier = Modifier.testTag("main_navigation_bar"),
                                tonalElevation = 8.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == 0,
                                    onClick = { currentTab = 0 },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                                            contentDescription = "Dashboard Casa",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = { Text("Casa") },
                                    modifier = Modifier.testTag("nav_tab_home")
                                )

                                NavigationBarItem(
                                    selected = currentTab == 1,
                                    onClick = { currentTab = 1 },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == 1) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart,
                                            contentDescription = "Shopping List",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = { Text("Spesa") },
                                    modifier = Modifier.testTag("nav_tab_shopping")
                                )

                                NavigationBarItem(
                                    selected = currentTab == 2,
                                    onClick = { currentTab = 2 },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == 2) Icons.Filled.CameraAlt else Icons.Outlined.CameraAlt,
                                            contentDescription = "OCR Scanner",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = { Text("Scanner") },
                                    modifier = Modifier.testTag("nav_tab_scanner")
                                )

                                NavigationBarItem(
                                    selected = currentTab == 3,
                                    onClick = { currentTab = 3 },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == 3) Icons.Filled.Balance else Icons.Outlined.Balance,
                                            contentDescription = "Contabilità Ledger",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = { Text("Ledger") },
                                    modifier = Modifier.testTag("nav_tab_ledger")
                                )

                                NavigationBarItem(
                                    selected = currentTab == 4,
                                    onClick = { currentTab = 4 },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == 4) Icons.Filled.Store else Icons.Outlined.Store,
                                            contentDescription = "Registro Negozi",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = { Text("Negozi") },
                                    modifier = Modifier.testTag("nav_tab_stores")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    val screenModifier = if (isFullScreenCamera && currentTab == 2) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier.padding(innerPadding)
                    }
                    
                    when (currentTab) {
                        0 -> HomeScreen(
                            viewModel = viewModel,
                            onNavigateToScanner = { currentTab = 2 },
                            modifier = screenModifier
                        )
                        1 -> ShoppingScreen(
                            viewModel = viewModel,
                            modifier = screenModifier
                        )
                        2 -> ScannerScreen(
                            viewModel = viewModel,
                            modifier = screenModifier
                        )
                        3 -> LedgerScreen(
                            viewModel = viewModel,
                            modifier = screenModifier
                        )
                        4 -> StoresScreen(
                            viewModel = viewModel,
                            modifier = screenModifier
                        )
                    }
                }

                // Unified Global Settings and Diagnostics Dashboard (anti-clutter)
                val showSettingsDialog by viewModel.showLocalAiSettingsDialog.collectAsState()
                if (showSettingsDialog) {
                    com.example.ui.screens.GlobalSettingsDialog(
                        viewModel = viewModel,
                        onDismiss = { viewModel.showLocalAiSettingsDialog.value = false }
                    )
                }

                val showLocalAiDownloadDialog by viewModel.showLocalAiDownloadDialog.collectAsState()
                val showLocalAiSuccessDialog by viewModel.showLocalAiSuccessDialog.collectAsState()
                
                if (showLocalAiDownloadDialog) {
                    com.example.ui.screens.LocalAiDownloadDialog(viewModel = viewModel)
                }
                if (showLocalAiSuccessDialog) {
                    com.example.ui.screens.LocalAiSuccessDialog(viewModel = viewModel)
                }
            }
        }
    }
}
