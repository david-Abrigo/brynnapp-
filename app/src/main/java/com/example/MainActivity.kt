package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DeviceRole
import com.example.ui.MainScreen
import com.example.ui.components.CriticalPermissionsDialog
import com.example.ui.components.WorkerRevokedDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val postNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.checkPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Insistir de inmediato si es Android 13+ y no tiene permiso de notificaciones
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!com.example.util.PermissionHelper.isPostNotificationGranted(this)) {
                postNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val storeConfig by viewModel.storeConfig.collectAsStateWithLifecycle()
            val isListenerGranted by viewModel.isNotificationListenerGranted.collectAsStateWithLifecycle()
            val isBatteryIgnored by viewModel.isBatteryOptimizationIgnored.collectAsStateWithLifecycle()
            val isPostGranted by viewModel.isPostNotificationGranted.collectAsStateWithLifecycle()

            val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
            var showPermissionDialog by remember { mutableStateOf(true) }

            val isReceiver = storeConfig.deviceRole == DeviceRole.RECEIVER
            val permissionsGranted = if (isReceiver) true else (isListenerGranted && isBatteryIgnored && isPostGranted)
            val needsOnboarding = !storeConfig.isOnboarded || (isReceiver && storeConfig.storeCode.isBlank())

            MyApplicationTheme(themeMode = storeConfig.appThemeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (!currentUser.isLoggedIn) {
                        // 1) LA APP SOLO FUNCIONA SI EL USUARIO ESTÁ LOGUEADO
                        com.example.ui.screens.AuthGateScreen(viewModel = viewModel)
                    } else if (needsOnboarding) {
                        // 2) SELECCIÓN OBLIGATORIA DE MODO (Pantalla completa, botón atrás bloqueado)
                        com.example.ui.screens.ModeSelectionScreen(viewModel = viewModel)
                    } else if (!isReceiver && showPermissionDialog && !permissionsGranted) {
                        // 3) Pantalla de Permisos Críticos al abrir (Solo para Emisor / Dueño o Solo Local)
                        CriticalPermissionsDialog(
                            isListenerGranted = isListenerGranted,
                            isBatteryIgnored = isBatteryIgnored,
                            isPostGranted = isPostGranted,
                            onRequestPostPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    postNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            onDismiss = { showPermissionDialog = false }
                        )
                    } else {
                        // 4) Usuario con sesión activa y modo configurado: Acceso a MainScreen
                        MainScreen(viewModel = viewModel)
                    }

                    // 5) Ventana modal obligatoria si el trabajador fue desvinculado por el dueño
                    val isWorkerRevoked = storeConfig.workerIsRevoked
                    if (isWorkerRevoked) {
                        WorkerRevokedDialog(
                            storeName = storeConfig.storeName,
                            onExitToModeSelection = {
                                viewModel.exitRevokedWorkerToModeSelection()
                            }
                        )
                    }

                    // 6) RevenueCat Paywall y Customer Center
                    val showPaywall by viewModel.showPaywall.collectAsStateWithLifecycle()
                    val showCustomerCenter by viewModel.showCustomerCenter.collectAsStateWithLifecycle()

                    if (showPaywall) {
                        com.example.ui.components.RevenueCatPaywallDialog(
                            onDismiss = { viewModel.closePaywall() },
                            onPurchaseSuccess = { viewModel.onProSubscriptionPurchased() }
                        )
                    }

                    if (showCustomerCenter) {
                        com.example.ui.components.RevenueCatCustomerCenterDialog(
                            onDismiss = { viewModel.closeCustomerCenter() }
                        )
                    }
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermission()
    }
}
