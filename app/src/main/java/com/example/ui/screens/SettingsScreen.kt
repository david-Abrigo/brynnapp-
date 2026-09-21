package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsPhone
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DeviceRole
import com.example.service.sound.NotificationSoundType
import com.example.service.sound.SoundAlertMode
import com.example.ui.components.AuthDialog
import com.example.ui.components.LinkedStoresDialog
import com.example.ui.components.ManageBranchesDialog
import com.example.ui.components.OwnerWorkersPanelDialog
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ModernMatteBlack
import com.example.ui.theme.ModernNeonLime
import com.example.ui.theme.ModernOnNeonLime
import com.example.ui.theme.SleekErrorRed
import com.example.ui.theme.SleekSubtleTextLight
import com.example.ui.theme.SleekSuccessGreen
import com.example.ui.viewmodel.MainViewModel
import java.util.Locale

/**
 * Estilo unificado de Switch de alto contraste:
 * - Modo Oscuro: Track Verde Lima con pulgar negro
 * - Modo Claro: Track Negro Mate con pulgar blanco (elegante y ultra-contrastado)
 */
@Composable
private fun appSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = if (AppTheme.isDark) ModernMatteBlack else Color.White,
    checkedTrackColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
    uncheckedThumbColor = AppTheme.textSecondary,
    uncheckedTrackColor = AppTheme.pillBackground
)

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToWorkers: (() -> Unit)? = null
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val storeConfig by viewModel.storeConfig.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ─── Estados de Diálogos Modulares (Cada opción abre SOLO su contenido) ───
    var showAuthDialog by remember { mutableStateOf(false) }
    var showAccountDetailDialog by remember { mutableStateOf(false) }
    var showBusinessIdentityDialog by remember { mutableStateOf(false) }
    var showWalletsDialog by remember { mutableStateOf(false) }
    var showDeviceModeDialog by remember { mutableStateOf(false) }
    var showLinkedStoresDialog by remember { mutableStateOf(false) }
    var showVoiceAudioDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showSystemHealthDialog by remember { mutableStateOf(false) }
    var showCloudServerDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showWorkersPanelDialog by remember { mutableStateOf(false) }
    var showChangeModeConfirm by remember { mutableStateOf(false) }

    // ─── Render de Diálogos Modulares ───────────────────────────────────────

    if (showChangeModeConfirm) {
        AlertDialog(
            onDismissRequest = { showChangeModeConfirm = false },
            containerColor = AppTheme.cardBackground,
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = ModernNeonLime.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Cambiar Modo de Uso",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = AppTheme.textPrimary
                    )
                }
            },
            text = {
                Text(
                    text = "Serás redirigido a la pantalla principal de selección de modo (la misma que aparece tras iniciar sesión) para elegir entre Solo Local, Dueño o Receptor.\n\n¿Deseas continuar?",
                    fontSize = 13.sp,
                    color = AppTheme.textSecondary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showChangeModeConfirm = false
                        viewModel.resetToModeSelection()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                        contentColor = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sí, cambiar modo", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showChangeModeConfirm = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancelar", color = AppTheme.textSecondary, fontSize = 13.sp)
                }
            }
        )
    }

    if (showLinkedStoresDialog) {
        LinkedStoresDialog(
            viewModel = viewModel,
            onDismiss = { showLinkedStoresDialog = false }
        )
    }

    if (showWorkersPanelDialog) {
        OwnerWorkersPanelDialog(
            viewModel = viewModel,
            onDismiss = { showWorkersPanelDialog = false }
        )
    }

    if (showAuthDialog) {
        AuthDialog(viewModel = viewModel, onDismiss = { showAuthDialog = false })
    }

    if (showBusinessIdentityDialog) {
        SectionDialog(title = "Identidad del Negocio", onDismiss = { showBusinessIdentityDialog = false }) {
            BusinessIdentitySection(viewModel = viewModel)
        }
    }

    if (showWalletsDialog) {
        SectionDialog(title = "Billeteras de Cobro", onDismiss = { showWalletsDialog = false }) {
            WalletsFilterSection(viewModel = viewModel)
        }
    }

    if (showDeviceModeDialog) {
        SectionDialog(title = "Modo del Teléfono & Vinculación", onDismiss = { showDeviceModeDialog = false }) {
            DeviceModeSection(viewModel = viewModel)
        }
    }

    if (showVoiceAudioDialog) {
        SectionDialog(title = "Sonido & Locución de Voz", onDismiss = { showVoiceAudioDialog = false }) {
            VoiceAudioSection(viewModel = viewModel)
        }
    }

    if (showAppearanceDialog) {
        SectionDialog(title = "Apariencia y Tema", onDismiss = { showAppearanceDialog = false }) {
            ThemeAppearanceSection(viewModel = viewModel)
        }
    }

    if (showSystemHealthDialog && storeConfig.deviceRole != DeviceRole.RECEIVER) {
        SectionDialog(title = "Permisos y Segundo Plano", onDismiss = { showSystemHealthDialog = false }) {
            SystemHealthSection(viewModel = viewModel)
        }
    }

    if (showCloudServerDialog) {
        SectionDialog(title = "Servidor en la Nube (Avanzado)", onDismiss = { showCloudServerDialog = false }) {
            CloudSyncSection(viewModel = viewModel)
        }
    }

    if (showHelpDialog) {
        SectionDialog(title = "Ayuda y Soporte", onDismiss = { showHelpDialog = false }) {
            HelpSupportSection()
        }
    }

    // Modal de Detalle de Cuenta
    if (showAccountDetailDialog && currentUser.isLoggedIn) {
        AlertDialog(
            onDismissRequest = { showAccountDetailDialog = false },
            containerColor = AppTheme.cardBackground,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = ModernNeonLime, modifier = Modifier.size(36.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(currentUser.initial, fontWeight = FontWeight.Black, fontSize = 16.sp, color = ModernMatteBlack)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Mi Cuenta", fontWeight = FontWeight.Black, fontSize = 18.sp, color = AppTheme.textPrimary)
                        Text(currentUser.email, fontSize = 12.sp, color = AppTheme.textSecondary)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = RoundedCornerShape(14.dp), color = AppTheme.pillBackground, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Plan Actual", fontSize = 12.sp, color = AppTheme.textSecondary)
                                Surface(shape = RoundedCornerShape(6.dp), color = if (currentUser.plan == "FREE") Color(0xFF16A34A).copy(alpha = 0.2f) else Color(0xFFD97706).copy(alpha = 0.2f)) {
                                    Text(currentUser.plan.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (currentUser.plan == "FREE") Color(0xFF16A34A) else Color(0xFFD97706), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                }
                            }
                            if (currentUser.whatsapp.isNotBlank()) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("WhatsApp", fontSize = 12.sp, color = AppTheme.textSecondary)
                                    Text(currentUser.whatsapp, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekSuccessGreen)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Estado", fontSize = 12.sp, color = AppTheme.textSecondary)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(SleekSuccessGreen))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Conectado", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekSuccessGreen)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { viewModel.signOut(); showAccountDetailDialog = false }, shape = RoundedCornerShape(12.dp)) {
                    Text("Cerrar Sesión", color = SleekErrorRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                Button(onClick = { showAccountDetailDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack, contentColor = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime), shape = RoundedCornerShape(12.dp)) {
                    Text("Cerrar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        )
    }

    // Modal de Confirmación de Salida
    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            containerColor = AppTheme.cardBackground,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Cerrar Sesión", fontWeight = FontWeight.Black, color = AppTheme.textPrimary) },
            text = { Text("¿Estás seguro de que deseas salir de tu cuenta de Brynn?", color = AppTheme.textSecondary) },
            confirmButton = {
                Button(onClick = { viewModel.signOut(); showSignOutConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = SleekErrorRed), shape = RoundedCornerShape(12.dp)) {
                    Text("Salir", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSignOutConfirm = false }, shape = RoundedCornerShape(12.dp)) {
                    Text("Cancelar", color = AppTheme.textSecondary)
                }
            }
        )
    }

    // ─── UI Principal con Jerarquía Ordenada ─────────────────────────────────

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.canvasBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // ── 1. HEADER DE PERFIL (Estilo Bancario / Terminal) ───────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Surface(
                    shape = CircleShape,
                    color = if (currentUser.isLoggedIn) (if (AppTheme.isDark) Color(0xFF1E293B) else Color(0xFF1E3A6E)) else AppTheme.pillBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.pillBorder),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (currentUser.isLoggedIn) {
                            Text(
                                text = currentUser.initial,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = if (AppTheme.isDark) ModernNeonLime else Color.White
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, tint = AppTheme.textSecondary, modifier = Modifier.size(26.dp))
                        }
                    }
                }
                if (currentUser.isLoggedIn) {
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(ModernNeonLime)
                            .align(Alignment.BottomEnd)
                            .border(2.dp, AppTheme.canvasBackground, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AJUSTES & CUENTA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = if (AppTheme.isDark) Color(0xFF94A3B8) else SleekSubtleTextLight
                )
                Text(
                    text = if (currentUser.isLoggedIn)
                        "¡Hola, ${currentUser.displayName.ifBlank { currentUser.email.substringBefore("@") }}!"
                    else "¡Hola, Comerciante!",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    letterSpacing = (-0.5).sp,
                    color = AppTheme.textPrimary
                )
                Text(
                    text = if (currentUser.isLoggedIn) currentUser.email else "Sin cuenta vinculada",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppTheme.textSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            if (!currentUser.isLoggedIn) {
                Button(
                    onClick = { showAuthDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                        contentColor = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Ingresar", fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // ── 2. SECCIÓN: INFORMACIÓN PERSONAL ────────────────────────────────
        SettingsSectionCard(title = "Información personal") {
            SettingsRow(
                icon = Icons.Default.Person,
                label = "Datos personales",
                subtitle = if (currentUser.isLoggedIn) "Plan ${currentUser.plan} · Conectado" else "Toca para sincronizar y respaldar pagos",
                onClick = {
                    if (currentUser.isLoggedIn) showAccountDetailDialog = true
                    else showAuthDialog = true
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 3. SECCIÓN: CONFIGURACIÓN DEL NEGOCIO ───────────────────────────
        SettingsSectionCard(title = "Configuración del negocio") {
            if (storeConfig.deviceRole != DeviceRole.RECEIVER) {
                SettingsRow(
                    icon = Icons.Default.Store,
                    label = "Datos del negocio",
                    subtitle = storeConfig.storeName.ifBlank { "Nombre comercial y voz" },
                    onClick = { showBusinessIdentityDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = AppTheme.cardBorder, thickness = 0.8.dp)
                val activeWalletsCount = listOf(storeConfig.captureYape, storeConfig.captureBcp, storeConfig.capturePlin).count { it }
                SettingsRow(
                    icon = Icons.Default.Security,
                    label = "Billeteras de cobro",
                    subtitle = "$activeWalletsCount activas (Yape · Plin · BCP)",
                    onClick = { showWalletsDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = AppTheme.cardBorder, thickness = 0.8.dp)
            }
            val roleName = when (storeConfig.deviceRole) {
                DeviceRole.SENDER -> "Emisor / Dueño"
                DeviceRole.LOCAL_SPEAKER -> "Solo Local"
                DeviceRole.RECEIVER -> "Receptor / Trabajador"
            }
            SettingsRow(
                icon = Icons.Default.PhoneAndroid,
                label = "Modo del teléfono",
                subtitle = "$roleName (Modo activo)",
                onClick = null
            )
            if (storeConfig.deviceRole == DeviceRole.SENDER) {
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = AppTheme.cardBorder, thickness = 0.8.dp)
                SettingsRow(
                    icon = Icons.Default.People,
                    label = "Panel de Trabajadores",
                    subtitle = "Vincular, horarios, sucursales y control",
                    onClick = {
                        if (onNavigateToWorkers != null) {
                            onNavigateToWorkers()
                        } else {
                            showWorkersPanelDialog = true
                        }
                    }
                )
            }
            if (storeConfig.deviceRole == DeviceRole.RECEIVER) {
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = AppTheme.cardBorder, thickness = 0.8.dp)
                SettingsRow(
                    icon = Icons.Default.Business,
                    label = "Mis Tiendas / Cambiar Negocio",
                    subtitle = "Tienda activa: ${storeConfig.storeName.ifBlank { "Sin tienda" }}",
                    onClick = { showLinkedStoresDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 4. SECCIÓN: AUDIO Y APARIENCIA ──────────────────────────────────
        SettingsSectionCard(title = "Audio y Apariencia") {
            SettingsRow(
                icon = Icons.Default.RecordVoiceOver,
                label = "Sonido & Voz",
                subtitle = "Locutor de montos, plantillas y tonos de cobro",
                onClick = { showVoiceAudioDialog = true }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = AppTheme.cardBorder, thickness = 0.8.dp)
            val themeName = when (storeConfig.appThemeMode.uppercase()) {
                "LIGHT" -> "Claro"
                "DARK" -> "Oscuro"
                else -> "Sistema"
            }
            SettingsRow(
                icon = Icons.Default.Tune,
                label = "Apariencia de la app",
                subtitle = "Tema actual: $themeName",
                onClick = { showAppearanceDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 5. SECCIÓN: SISTEMA Y SOPORTE ────────────────────────────────────
        SettingsSectionCard(title = "Sistema y Soporte") {
            if (storeConfig.deviceRole != DeviceRole.RECEIVER) {
                SettingsRow(
                    icon = Icons.Default.Notifications,
                    label = "Permisos y batería",
                    subtitle = "Segundo plano, inicio automático y fijar en RAM",
                    onClick = { showSystemHealthDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = AppTheme.cardBorder, thickness = 0.8.dp)
            }
            if (storeConfig.deviceRole == DeviceRole.SENDER) {
                SettingsRow(
                    icon = Icons.Default.CloudSync,
                    label = "Servidor en la nube",
                    subtitle = "Configuración técnica de Supabase y FCM",
                    onClick = { showCloudServerDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = AppTheme.cardBorder, thickness = 0.8.dp)
            }
            SettingsRow(
                icon = Icons.Default.Help,
                label = "Ayuda y sugerencias",
                subtitle = "Preguntas frecuentes y soporte de Brynn",
                onClick = { showHelpDialog = true }
            )
        }

        // ── 5.5 SECCIÓN: SUSCRIPCIÓN Y FACTURACIÓN (REVENUECAT) ─────────
        val isPro by viewModel.isProSubscribed.collectAsStateWithLifecycle()
        SettingsSectionCard(title = "Suscripción y Facturación") {
            if (isPro) {
                SettingsRow(
                    icon = Icons.Default.CheckCircle,
                    label = "Suscripción Premium Activa",
                    subtitle = "Gestionar plan, renovaciones o cancelaciones",
                    onClick = { viewModel.openCustomerCenter() }
                )
            } else {
                SettingsRow(
                    icon = Icons.Default.Bolt,
                    label = "Actualizar a Premium",
                    subtitle = "Suscripción mensual o anual para negocios sin límites",
                    onClick = { viewModel.openPaywall() }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── 6. SECCIÓN: MODO DE USO Y SESIÓN ─────────────────────────────
        val bottomRoleName = when (storeConfig.deviceRole) {
            DeviceRole.SENDER -> "Emisor / Dueño"
            DeviceRole.LOCAL_SPEAKER -> "Solo Local"
            DeviceRole.RECEIVER -> "Receptor / Trabajador"
        }
        SettingsSectionCard(title = "Modo de Uso y Sesión") {
            SettingsRow(
                icon = Icons.Default.Bolt,
                label = "Cambiar modo de uso",
                subtitle = "Redirigir a selección de modo ($bottomRoleName)",
                onClick = { showChangeModeConfirm = true }
            )
            if (currentUser.isLoggedIn) {
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = AppTheme.cardBorder, thickness = 0.8.dp)
                SettingsRow(
                    icon = Icons.Default.ExitToApp,
                    label = "Cerrar sesión",
                    subtitle = "Salir de la cuenta (${currentUser.email})",
                    onClick = { showSignOutConfirm = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(90.dp))
    }
}

// ─── COMPOSABLES DE SECCIÓN MODULARES E INDEPENDIENTES ─────────────────────────

/**
 * 1. Modal Exclusivo: Identidad del Negocio
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BusinessIdentitySection(viewModel: MainViewModel) {
    val storeConfig by viewModel.storeConfig.collectAsStateWithLifecycle()
    val receivers by viewModel.storeReceivers.collectAsStateWithLifecycle()
    var storeNameInput by remember(storeConfig.storeName) { mutableStateOf(storeConfig.storeName) }
    var showManageBranchesDialog by remember { mutableStateOf(false) }
    val isReceiver = storeConfig.deviceRole == DeviceRole.RECEIVER

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = ModernNeonLime,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Store, contentDescription = null, tint = ModernMatteBlack, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Nombre de tu Comercio", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AppTheme.textPrimary)
                    Text(if (isReceiver) "Fijado por el Dueño (Sólo Lectura)" else "Nombre comercial y mención en voz", fontSize = 11.sp, color = AppTheme.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isReceiver) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Modo Receptor / Trabajador",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFB45309)
                            )
                            Text(
                                "El nombre del negocio está definido por el Dueño. No se puede modificar desde este dispositivo.",
                                fontSize = 11.sp,
                                color = AppTheme.textSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Text("Este nombre se utiliza en las locuciones de voz (\"Cobro exitoso en [Nombre]\") y en los encabezados de los reportes.", fontSize = 12.sp, color = AppTheme.textSecondary, lineHeight = 16.sp)
                Spacer(modifier = Modifier.height(14.dp))
            }

            OutlinedTextField(
                value = storeNameInput,
                onValueChange = {
                    if (!isReceiver) {
                        storeNameInput = it
                        viewModel.updateStoreConfig(storeConfig.copy(storeName = it))
                    }
                },
                enabled = !isReceiver,
                label = { Text("Nombre del Negocio") },
                placeholder = { Text("Ej: Mi Tienda") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                trailingIcon = {
                    if (isReceiver) {
                        Icon(Icons.Default.Lock, contentDescription = "Bloqueado", tint = AppTheme.textSecondary, modifier = Modifier.size(18.dp))
                    } else if (storeNameInput.trim() == storeConfig.storeName.trim() && storeNameInput.isNotBlank()) {
                        Icon(Icons.Default.Check, contentDescription = "Guardado", tint = SleekSuccessGreen, modifier = Modifier.size(18.dp))
                    }
                }
            )

            if (storeConfig.deviceRole == DeviceRole.SENDER) {
                val branchesList = remember(storeConfig.storeBranches) {
                    val list = storeConfig.storeBranches.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .toMutableList()
                    if (!list.contains("Principal")) list.add(0, "Principal")
                    list
                }

                if (showManageBranchesDialog) {
                    ManageBranchesDialog(
                        currentBranchesString = if (storeConfig.storeBranches.isNotBlank()) storeConfig.storeBranches else "Principal,Sucursal 2",
                        storeReceivers = receivers,
                        onDismiss = { showManageBranchesDialog = false },
                        onSaveBranches = { newBranches, reassignments ->
                            val joined = newBranches.joinToString(",")
                            viewModel.updateStoreBranches(joined)
                            reassignments.forEach { (workerId, newBranch) ->
                                viewModel.updateReceiverBranch(workerId, newBranch) { _, _ -> }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = AppTheme.cardBorder, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Sucursales y Puntos de Venta", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppTheme.textPrimary)
                            Text("${branchesList.size} punto(s) de cobro configurados", fontSize = 11.sp, color = AppTheme.textSecondary)
                        }
                    }
                    Button(
                        onClick = { showManageBranchesDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (AppTheme.isDark) Color(0xFF2E313C) else Color(0xFFE2E8F0),
                            contentColor = AppTheme.textPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gestionar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    branchesList.forEach { branch ->
                        val isPrincipal = branch.equals("Principal", ignoreCase = true)
                        val count = receivers.count {
                            val b = it.branchName.ifBlank { "Principal" }
                            b.equals(branch, ignoreCase = true)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPrincipal) Color(0xFF3B82F6).copy(alpha = 0.12f) else AppTheme.pillBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isPrincipal) Color(0xFF3B82F6).copy(alpha = 0.35f) else AppTheme.cardBorder
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showManageBranchesDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isPrincipal) Icons.Default.Store else Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = if (isPrincipal) Color(0xFF3B82F6) else AppTheme.textSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = branch,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPrincipal) Color(0xFF3B82F6) else AppTheme.textPrimary
                                )
                                if (count > 0) {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = SleekSuccessGreen.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "$count",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = SleekSuccessGreen,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AppTheme.pillBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showManageBranchesDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = AppTheme.textSecondary, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Añadir", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppTheme.textSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = AppTheme.cardBorder, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.padding(end = 8.dp)) {
                            Text(
                                text = "Permitir Ver Ventas Anteriores a Receptores",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = AppTheme.textPrimary
                            )
                            Text(
                                text = if (storeConfig.allowWorkerHistory)
                                    "Los trabajadores pueden ver el balance e historial de días pasados de la tienda."
                                else
                                    "Los trabajadores solo verán los cobros en tiempo real de su turno de hoy.",
                                fontSize = 11.sp,
                                color = AppTheme.textSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                    Switch(
                        checked = storeConfig.allowWorkerHistory,
                        onCheckedChange = { enabled ->
                            viewModel.updateAllowWorkerHistory(enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = com.example.ui.theme.ModernNeonLime,
                            checkedTrackColor = com.example.ui.theme.ModernMatteBlack,
                            uncheckedThumbColor = AppTheme.textSecondary,
                            uncheckedTrackColor = AppTheme.cardBorder
                        )
                    )
                }
            }
        }
    }
}

/**
 * 2. Modal Exclusivo: Billeteras que Cobran
 */
@Composable
private fun WalletsFilterSection(viewModel: MainViewModel) {
    val storeConfig by viewModel.storeConfig.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Selecciona las billeteras activas para captura y lectura de pagos:", fontSize = 12.sp, color = AppTheme.textSecondary)

        // Yape
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (storeConfig.captureYape) (if (AppTheme.isDark) ModernNeonLime.copy(alpha = 0.12f) else ModernMatteBlack.copy(alpha = 0.05f)) else AppTheme.pillBackground,
            border = androidx.compose.foundation.BorderStroke(
                if (storeConfig.captureYape) 1.5.dp else 1.dp,
                if (storeConfig.captureYape) (if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack) else Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.updateStoreConfig(storeConfig.copy(captureYape = !storeConfig.captureYape)) }
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color(0xFF732283), modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("Y", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp) }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Yape Oficial", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppTheme.textPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(6.dp), color = ModernNeonLime) {
                            Text("Principal", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ModernMatteBlack, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Text("Notificaciones de com.bcp.innovacxion.yapeapp", fontSize = 10.sp, color = AppTheme.textSecondary)
                }
                Switch(
                    checked = storeConfig.captureYape,
                    onCheckedChange = { viewModel.updateStoreConfig(storeConfig.copy(captureYape = it)) },
                    colors = appSwitchColors()
                )
            }
        }

        // BCP Banca Móvil
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (storeConfig.captureBcp) (if (AppTheme.isDark) ModernNeonLime.copy(alpha = 0.12f) else ModernMatteBlack.copy(alpha = 0.05f)) else AppTheme.pillBackground,
            border = androidx.compose.foundation.BorderStroke(
                if (storeConfig.captureBcp) 1.5.dp else 1.dp,
                if (storeConfig.captureBcp) (if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack) else Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.updateStoreConfig(storeConfig.copy(captureBcp = !storeConfig.captureBcp)) }
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color(0xFF002A8F), modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("BCP", color = Color(0xFFFF7800), fontWeight = FontWeight.Black, fontSize = 10.sp) }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("BCP Banca Móvil", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppTheme.textPrimary)
                    Text("Transferencias y avisos de com.bcp.bank.bcp", fontSize = 10.sp, color = AppTheme.textSecondary)
                }
                Switch(
                    checked = storeConfig.captureBcp,
                    onCheckedChange = { viewModel.updateStoreConfig(storeConfig.copy(captureBcp = it)) },
                    colors = appSwitchColors()
                )
            }
        }

        // Plin
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (storeConfig.capturePlin) (if (AppTheme.isDark) ModernNeonLime.copy(alpha = 0.12f) else ModernMatteBlack.copy(alpha = 0.05f)) else AppTheme.pillBackground,
            border = androidx.compose.foundation.BorderStroke(
                if (storeConfig.capturePlin) 1.5.dp else 1.dp,
                if (storeConfig.capturePlin) (if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack) else Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.updateStoreConfig(storeConfig.copy(capturePlin = !storeConfig.capturePlin)) }
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color(0xFF00C3DE), modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("P", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp) }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Plin (Interoperable)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppTheme.textPrimary)
                    Text("Cobros desde Interbank, BBVA y Scotiabank", fontSize = 10.sp, color = AppTheme.textSecondary)
                }
                Switch(
                    checked = storeConfig.capturePlin,
                    onCheckedChange = { viewModel.updateStoreConfig(storeConfig.copy(capturePlin = it)) },
                    colors = appSwitchColors()
                )
            }
        }
    }
}

/**
 * 3. Modal Exclusivo: Selector de Tema Visual
 */
@Composable
private fun ThemeAppearanceSection(viewModel: MainViewModel) {
    val storeConfig by viewModel.storeConfig.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Tema de la Interfaz", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AppTheme.textPrimary)
            Text("Selecciona cómo se visualizará Brynn en tu dispositivo", fontSize = 12.sp, color = AppTheme.textSecondary)

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val currentMode = storeConfig.appThemeMode.uppercase()
                val options = listOf(
                    Triple("SYSTEM", "Sistema", Icons.Default.PhoneAndroid),
                    Triple("LIGHT", "Claro", Icons.Default.LightMode),
                    Triple("DARK", "Oscuro", Icons.Default.DarkMode)
                )

                options.forEach { (modeKey, modeTitle, iconVector) ->
                    val isSelected = currentMode == modeKey
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack
                        } else AppTheme.pillBackground,
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) Color.Transparent else AppTheme.cardBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                viewModel.updateStoreConfig(storeConfig.copy(appThemeMode = modeKey))
                                Toast.makeText(context, "Tema cambiado a: $modeTitle", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 14.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = if (isSelected) {
                                    if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime
                                } else AppTheme.textSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = modeTitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) {
                                    if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime
                                } else AppTheme.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4. Modal Exclusivo: Modo del Teléfono & Vinculación
 */
@Composable
private fun DeviceModeSection(viewModel: MainViewModel) {
    val storeConfig by viewModel.storeConfig.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showWorkersPanel by remember { mutableStateOf(false) }
    var showLinkedStoresDialog by remember { mutableStateOf(false) }
    var showPairingDialog by remember { mutableStateOf(false) }
    var pairingCodeInput by remember { mutableStateOf("") }
    var isPairingLoading by remember { mutableStateOf(false) }
    var pairingErrorMessage by remember { mutableStateOf<String?>(null) }

    var generatedCode by remember { mutableStateOf<String?>(null) }
    var isGeneratingCode by remember { mutableStateOf(false) }

    val isSender = storeConfig.deviceRole == DeviceRole.SENDER || storeConfig.deviceRole == DeviceRole.LOCAL_SPEAKER
    val isReceiver = storeConfig.deviceRole == DeviceRole.RECEIVER

    if (showLinkedStoresDialog) {
        LinkedStoresDialog(
            viewModel = viewModel,
            onDismiss = { showLinkedStoresDialog = false }
        )
    }

    if (showWorkersPanel) {
        OwnerWorkersPanelDialog(
            viewModel = viewModel,
            onDismiss = { showWorkersPanel = false }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Opción 1: Caja Principal
        RoleCard(
            title = "Caja Principal (Cobra en este teléfono)",
            description = "Captura las notificaciones de Yape/BCP/Plin en este dispositivo y las canta de inmediato.",
            icon = Icons.Default.Store,
            isSelected = isSender,
            onClick = {
                if (storeConfig.deviceRole != DeviceRole.SENDER) {
                    viewModel.updateStoreConfig(storeConfig.copy(deviceRole = DeviceRole.SENDER))
                    Toast.makeText(context, "Modo cambiado a Caja Principal", Toast.LENGTH_SHORT).show()
                }
            }
        )

        if (isSender) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (AppTheme.isDark) Color(0xFF191F1A) else Color(0xFFF2FBF4)),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (AppTheme.isDark) ModernNeonLime.copy(alpha = 0.5f) else ModernMatteBlack.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = ModernNeonLime, modifier = Modifier.size(28.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.SettingsPhone, contentDescription = null, tint = ModernMatteBlack, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vincular otro teléfono o parlante", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppTheme.textPrimary)
                    }

                    Text("Genera un código seguro para que tus trabajadores o un parlante secundario escuchen los cobros a distancia.", fontSize = 11.sp, color = AppTheme.textSecondary, lineHeight = 15.sp)

                    if (generatedCode != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (AppTheme.isDark) ModernNeonLime.copy(alpha = 0.15f) else ModernMatteBlack.copy(alpha = 0.05f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("CÓDIGO DE VINCULACIÓN (Válido 24h)", fontSize = 9.sp, fontWeight = FontWeight.Black, color = AppTheme.textSecondary)
                                    Text(text = generatedCode ?: "", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack)
                                }
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Código Receptor", generatedCode))
                                    Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = AppTheme.textPrimary)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            isGeneratingCode = true
                            viewModel.generatePairingCode(storeConfig.storeCode) { success, result ->
                                isGeneratingCode = false
                                if (success && result != null) {
                                    generatedCode = result
                                    Toast.makeText(context, "¡Código generado!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, result ?: "Error al generar código", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isGeneratingCode,
                        colors = ButtonDefaults.buttonColors(containerColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack, contentColor = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        if (isGeneratingCode) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AppTheme.textPrimary)
                        } else {
                            Text(if (generatedCode == null) "Generar Código de Vinculación" else "Generar Nuevo Código", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = { showWorkersPanel = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (AppTheme.isDark) ModernNeonLime.copy(alpha = 0.15f) else ModernMatteBlack.copy(alpha = 0.08f),
                            contentColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gestionar Trabajadores Vinculados", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Opción 2: Parlante Remoto
        RoleCard(
            title = "Parlante / Pantalla Remota",
            description = "No cobra directamente; recibe las alertas en tiempo real desde el teléfono de caja.",
            icon = Icons.Default.PhoneAndroid,
            isSelected = isReceiver,
            onClick = {
                showLinkedStoresDialog = true
            }
        )

        if (isReceiver) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0284C7).copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tienda Vinculada", fontSize = 11.sp, color = AppTheme.textSecondary)
                            Text(storeConfig.storeName.ifBlank { "Vinculado" }, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppTheme.textPrimary)
                        }
                        Button(
                            onClick = {
                                showLinkedStoresDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7), contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Ver Mis Tiendas", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal de Vinculación con Código (Receptor)
    if (showPairingDialog) {
        AlertDialog(
            onDismissRequest = { if (!isPairingLoading) showPairingDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = ModernNeonLime, modifier = Modifier.size(32.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.SettingsPhone, contentDescription = null, tint = ModernMatteBlack, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Vincular a Tienda", fontWeight = FontWeight.Black, fontSize = 16.sp, color = AppTheme.textPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Ingresa el código seguro generado por el dueño de la tienda en su teléfono de Caja Principal para sincronizar las ventas a distancia.", fontSize = 12.sp, color = AppTheme.textSecondary, lineHeight = 16.sp)

                    OutlinedTextField(
                        value = pairingCodeInput,
                        onValueChange = { input ->
                            if (input.length <= 16) {
                                pairingCodeInput = input.filter { it.isLetterOrDigit() || it == '-' }
                            }
                        },
                        label = { Text("Código de Vinculación") },
                        placeholder = { Text("Ej: k7m2p") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (pairingErrorMessage != null) {
                        Surface(shape = RoundedCornerShape(8.dp), color = SleekErrorRed.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth()) {
                            Text(text = pairingErrorMessage ?: "", color = SleekErrorRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pairingCodeInput.isBlank()) {
                            pairingErrorMessage = "Ingresa el código de vinculación"
                            return@Button
                        }
                        isPairingLoading = true
                        pairingErrorMessage = null
                        viewModel.redeemPairingCode(pairingCodeInput.trim()) { success, message ->
                            isPairingLoading = false
                            if (success) {
                                showPairingDialog = false
                                Toast.makeText(context, message ?: "¡Vinculado con éxito!", Toast.LENGTH_LONG).show()
                            } else {
                                pairingErrorMessage = message ?: "Error al vincular el código"
                            }
                        }
                    },
                    enabled = !isPairingLoading && pairingCodeInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack, contentColor = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isPairingLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Vincular y Activar", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            },
            dismissButton = {
                if (!isPairingLoading) {
                    OutlinedButton(onClick = { showPairingDialog = false }, shape = RoundedCornerShape(12.dp)) {
                        Text("Cancelar", color = AppTheme.textSecondary, fontSize = 12.sp)
                    }
                }
            },
            containerColor = AppTheme.cardBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

/**
 * 5. Modal Exclusivo: Permisos y Salud del Sistema
 */
@Composable
private fun SystemHealthSection(viewModel: MainViewModel) {
    val context = LocalContext.current
    val isListenerGranted by viewModel.isNotificationListenerGranted.collectAsStateWithLifecycle()
    val isBatteryIgnored by viewModel.isBatteryOptimizationIgnored.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.cardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Lectura de Notificaciones
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Lectura de Notificaciones", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppTheme.textPrimary)
                        Text(if (isListenerGranted) "Concedido • Interceptando pagos" else "Falta permiso • Toca para activar", fontSize = 11.sp, color = if (isListenerGranted) SleekSuccessGreen else SleekErrorRed)
                    }
                    Button(
                        onClick = { com.example.util.PermissionHelper.openNotificationListenerSettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isListenerGranted) Color(0xFFDCFCE7) else SleekErrorRed, contentColor = if (isListenerGranted) SleekSuccessGreen else Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(if (isListenerGranted) "Activo" else "Habilitar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = AppTheme.cardBorder, thickness = 0.8.dp)

                // Exclusión de Batería
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Exclusión de Batería", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppTheme.textPrimary)
                        Text(if (isBatteryIgnored) "Sin restricciones de energía" else "Android puede pausar la app en reposo", fontSize = 11.sp, color = if (isBatteryIgnored) SleekSuccessGreen else Color(0xFFD97706))
                    }
                    Button(
                        onClick = { com.example.util.PermissionHelper.requestIgnoreBatteryOptimization(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isBatteryIgnored) Color(0xFFDCFCE7) else Color(0xFFD97706), contentColor = if (isBatteryIgnored) SleekSuccessGreen else Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(if (isBatteryIgnored) "Excluido" else "Excluir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = AppTheme.cardBorder, thickness = 0.8.dp)

                // Inicio Automático
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Inicio Automático (${com.example.util.PermissionHelper.getDeviceManufacturer()})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppTheme.textPrimary)
                        Text("Permite arrancar tras reiniciar el teléfono", fontSize = 11.sp, color = AppTheme.textSecondary)
                    }
                    Button(
                        onClick = {
                            val opened = com.example.util.PermissionHelper.openAutoStartSettings(context)
                            if (opened) {
                                Toast.makeText(context, "Activa 'Inicio Automático' para Brynn", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack, contentColor = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Configurar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Guía de Bloqueo en RAM
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = if (AppTheme.isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = ModernNeonLime, modifier = Modifier.size(24.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = ModernMatteBlack, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bloquear Brynn en Memoria RAM", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppTheme.textPrimary)
                }
                Text("Abre las Apps Recientes, mantén presionada la ventana de Brynn y activa el Candado para evitar que el sistema la cierre.", fontSize = 11.sp, lineHeight = 15.sp, color = AppTheme.textSecondary)
            }
        }
    }
}

/**
 * 6. Modal Exclusivo: Servidor en la Nube (Avanzado)
 */
@Composable
private fun CloudSyncSection(viewModel: MainViewModel) {
    val storeConfig by viewModel.storeConfig.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var supabaseUrlInput by remember(storeConfig.supabaseUrl) { mutableStateOf(storeConfig.supabaseUrl) }
    var supabaseKeyInput by remember(storeConfig.supabaseAnonKey) { mutableStateOf(storeConfig.supabaseAnonKey) }
    var isKeyVisible by remember { mutableStateOf(false) }
    val testConnResult by viewModel.testConnectionResult.collectAsStateWithLifecycle()
    val fcmTestResult by viewModel.fcmTestResult.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Credenciales de backend para sincronización multi-dispositivo y alertas FCM:", fontSize = 12.sp, color = AppTheme.textSecondary)

        OutlinedTextField(
            value = supabaseUrlInput,
            onValueChange = { supabaseUrlInput = it },
            label = { Text("URL de Supabase") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = supabaseKeyInput,
            onValueChange = { supabaseKeyInput = it },
            label = { Text("Supabase Anon Key") },
            singleLine = true,
            visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                    Icon(imageVector = if (isKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = AppTheme.textSecondary)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    viewModel.updateStoreConfig(storeConfig.copy(supabaseUrl = supabaseUrlInput.trim(), supabaseAnonKey = supabaseKeyInput.trim()))
                    Toast.makeText(context, "Credenciales guardadas", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack, contentColor = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).height(40.dp)
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = { viewModel.testSupabaseCredentials(supabaseUrlInput.trim(), supabaseKeyInput.trim()) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).height(40.dp)
            ) {
                Text("Probar Conexión", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        if (testConnResult != null) {
            Surface(shape = RoundedCornerShape(8.dp), color = AppTheme.pillBackground, modifier = Modifier.fillMaxWidth()) {
                Text(text = testConnResult ?: "", fontSize = 11.sp, modifier = Modifier.padding(8.dp), color = AppTheme.textPrimary)
            }
        }

        OutlinedButton(
            onClick = { viewModel.testFcmPush() },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(40.dp)
        ) {
            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Enviar Push de Prueba (FCM)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        if (fcmTestResult != null) {
            Surface(shape = RoundedCornerShape(8.dp), color = AppTheme.pillBackground, modifier = Modifier.fillMaxWidth()) {
                Text(text = fcmTestResult ?: "", fontSize = 11.sp, modifier = Modifier.padding(8.dp), color = AppTheme.textPrimary)
            }
        }
    }
}

/**
 * 7. Modal Exclusivo: Ayuda y Soporte
 */
@Composable
private fun HelpSupportSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.cardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = ModernNeonLime, modifier = Modifier.size(24.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = ModernMatteBlack, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("¿Cómo funciona Brynn?", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppTheme.textPrimary)
                }
                Text("Brynn intercepta de forma segura las notificaciones de cobro emitidas por Yape, BCP y Plin, y las canta por el altavoz o parlante Bluetooth al instante sin necesidad de revisar la pantalla.", fontSize = 11.sp, lineHeight = 15.sp, color = AppTheme.textSecondary)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.cardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = ModernNeonLime, modifier = Modifier.size(24.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = ModernMatteBlack, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("¿Por qué no canta un cobro?", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppTheme.textPrimary)
                }
                Text("Asegúrate de que el permiso de notificaciones esté ACTIVO y que el volumen multimedia de tu teléfono no esté en silencio o en modo no molestar.", fontSize = 11.sp, lineHeight = 15.sp, color = AppTheme.textSecondary)
            }
        }

        Surface(shape = RoundedCornerShape(12.dp), color = AppTheme.pillBackground, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Versión de la app", fontSize = 12.sp, color = AppTheme.textSecondary)
                Text("Brynn v1.0.0", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppTheme.textPrimary)
            }
        }
    }
}

// ─── COMPOSABLES DE SOPORTE DE UI ──────────────────────────────────────────────

@Composable
private fun SettingsSectionCard(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
            color = if (AppTheme.isDark) Color(0xFF94A3B8) else Color(0xFF6F7274),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = AppTheme.cardBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ModernNeonLime),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = ModernMatteBlack, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
                color = AppTheme.textPrimary
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = AppTheme.textSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
        if (onClick != null) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (AppTheme.isDark) Color(0xFF64748B) else Color(0xFFCBD5E1),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SectionDialog(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.cardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = (-0.3).sp,
                        color = AppTheme.textPrimary
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AppTheme.pillBackground)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Cerrar", tint = AppTheme.textSecondary, modifier = Modifier.size(16.dp))
                    }
                }
                HorizontalDivider(color = AppTheme.cardBorder, thickness = 0.8.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    content()
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
fun RoleCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AppTheme.cardBackground,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) (if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack) else AppTheme.cardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) ModernNeonLime else AppTheme.pillBackground,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) ModernMatteBlack else AppTheme.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppTheme.textPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = description, fontSize = 11.sp, lineHeight = 14.sp, color = AppTheme.textSecondary)
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (AppTheme.isDark) ModernMatteBlack else ModernNeonLime,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ─── SECCIÓN: VOZ & AUDIO ───────────────────────────────────────────────────────
@Composable
fun VoiceAudioSection(viewModel: MainViewModel) {
    val voiceSettings by viewModel.voiceSettings.collectAsStateWithLifecycle()
    val storeConfig by viewModel.storeConfig.collectAsStateWithLifecycle()
    var templateText by remember(voiceSettings.messageTemplate) { mutableStateOf(voiceSettings.messageTemplate) }

    val presetTemplates = listOf(
        "¡Recibiste {monto} de {emisor}!",
        "Yape recibido de {emisor} por {monto}",
        "Pago confirmado: {emisor} te envió {monto}",
        "¡Cobro exitoso en {negocio}! {monto} de {emisor}",
        "Nuevo pago: {monto} de {emisor}"
    )

    val simulatedPreviewText = remember(templateText, storeConfig.storeName) {
        val businessName = storeConfig.storeName.ifBlank { "Mi Negocio" }
        val sampleAmountWords = com.example.service.tts.YapeTtsManager.formatAmountForSpeech(1.50)
        var preview = templateText
            .replace("{monto} soles", sampleAmountWords, ignoreCase = true)
            .replace("{monto} sol", sampleAmountWords, ignoreCase = true)
            .replace("{monto}", sampleAmountWords)
            .replace("{emisor}", "Carlos Quispe")
            .replace("{hora}", "03:45 PM")
            .replace("{negocio}", businessName, ignoreCase = true)
            .replace("{tienda}", businessName, ignoreCase = true)
        if (!templateText.contains("{monto}") && !templateText.contains("{emisor}")) {
            preview = "$templateText. $sampleAmountWords de Carlos Quispe"
        }
        preview
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Master switch locutor (con icono verde lima e icono negro)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.cardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = if (voiceSettings.voiceEnabled) ModernNeonLime else AppTheme.pillBackground,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (voiceSettings.voiceEnabled) Icons.Default.RecordVoiceOver else Icons.Default.VolumeMute,
                                contentDescription = null,
                                tint = if (voiceSettings.voiceEnabled) ModernMatteBlack else AppTheme.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Locutor de Pagos", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppTheme.textPrimary)
                        Text(if (voiceSettings.voiceEnabled) "Canta el monto de cada pago recibido" else "Voz desactivada (silencioso)", fontSize = 11.sp, color = AppTheme.textSecondary)
                    }
                }
                Switch(
                    checked = voiceSettings.voiceEnabled,
                    onCheckedChange = { viewModel.updateVoiceSettings(voiceSettings.copy(voiceEnabled = it)) },
                    colors = appSwitchColors()
                )
            }
        }

        // Plantilla del mensaje
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.cardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (AppTheme.isDark) ModernNeonLime.copy(alpha = 0.4f) else AppTheme.cardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Frase Cantada en Voz Alta", fontWeight = FontWeight.Black, fontSize = 14.sp, color = AppTheme.textPrimary)
                Text("Plantillas sugeridas:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = AppTheme.textPrimary)

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    presetTemplates.forEach { preset ->
                        val isSelected = templateText == preset
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) (if (AppTheme.isDark) ModernNeonLime.copy(alpha = 0.2f) else ModernMatteBlack.copy(alpha = 0.06f)) else AppTheme.pillBackground,
                            border = androidx.compose.foundation.BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) (if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack) else Color.Transparent),
                            modifier = Modifier.fillMaxWidth().clickable {
                                templateText = preset
                                viewModel.updateVoiceSettings(voiceSettings.copy(messageTemplate = preset))
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = preset, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = AppTheme.textPrimary, modifier = Modifier.weight(1f))
                                if (isSelected) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = templateText,
                    onValueChange = {
                        templateText = it
                        viewModel.updateVoiceSettings(voiceSettings.copy(messageTemplate = it))
                    },
                    placeholder = { Text("Ej: ¡Atención! {monto} de {emisor}") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (templateText.isNotEmpty()) {
                            IconButton(onClick = {
                                templateText = ""
                                viewModel.updateVoiceSettings(voiceSettings.copy(messageTemplate = ""))
                            }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = null, tint = AppTheme.textSecondary)
                            }
                        }
                    }
                )

                // Chips de variables
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Variables:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppTheme.textSecondary)
                    listOf("{monto}", "{emisor}", "{hora}", "{negocio}").forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AppTheme.pillBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder),
                            modifier = Modifier.clickable {
                                if (!templateText.contains(tag)) {
                                    val newText = if (templateText.isBlank()) tag else "$templateText $tag"
                                    templateText = newText
                                    viewModel.updateVoiceSettings(voiceSettings.copy(messageTemplate = newText))
                                }
                            }
                        ) {
                            Text(text = tag, color = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                        }
                    }
                }

                // Cuadro de previsualización
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (AppTheme.isDark) Color(0xFF1B1D25) else Color(0xFFF1F3F7),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "Ejemplo en caja:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppTheme.textSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "\"$simulatedPreviewText\"", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppTheme.textPrimary, lineHeight = 16.sp)
                    }
                }

                Button(
                    onClick = {
                        viewModel.updateVoiceSettings(voiceSettings.copy(messageTemplate = templateText))
                        viewModel.testTts(simulatedPreviewText)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack, contentColor = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Probar Locución en Vivo", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }

        // Parámetros de pronunciación
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.cardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Ajustes de Pronunciación", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppTheme.textPrimary)

                // Volume
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Volumen de Voz", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppTheme.textPrimary)
                    Text("${(voiceSettings.volume * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppTheme.textPrimary)
                }
                Slider(
                    value = voiceSettings.volume,
                    onValueChange = { viewModel.updateVoiceSettings(voiceSettings.copy(volume = it)) },
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                        activeTrackColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                        inactiveTrackColor = AppTheme.pillBackground
                    )
                )

                // Rate
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Velocidad de Habla", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppTheme.textPrimary)
                    Text("${String.format(Locale.US, "%.2f", voiceSettings.speechRate)}x", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppTheme.textPrimary)
                }
                Slider(
                    value = voiceSettings.speechRate,
                    onValueChange = { viewModel.updateVoiceSettings(voiceSettings.copy(speechRate = it)) },
                    valueRange = 0.6f..1.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                        activeTrackColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                        inactiveTrackColor = AppTheme.pillBackground
                    )
                )

                // Pitch
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tono de Voz (Pitch)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppTheme.textPrimary)
                    Text(if (voiceSettings.pitch < 0.9f) "Grave" else if (voiceSettings.pitch > 1.1f) "Agudo" else "Normal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppTheme.textPrimary)
                }
                Slider(
                    value = voiceSettings.pitch,
                    onValueChange = { viewModel.updateVoiceSettings(voiceSettings.copy(pitch = it)) },
                    valueRange = 0.7f..1.4f,
                    colors = SliderDefaults.colors(
                        thumbColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                        activeTrackColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                        inactiveTrackColor = AppTheme.pillBackground
                    )
                )
            }
        }

        // Tonos de cobro y efectos
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.cardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = ModernNeonLime, modifier = Modifier.size(36.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = ModernMatteBlack, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Tonos de Cobro (Efectos Auditivos)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppTheme.textPrimary)
                        Text("Sonidos de campana, monedas y caja registradora", fontSize = 11.sp, color = AppTheme.textSecondary)
                    }
                }

                // Alert Mode
                Text("¿Cuándo reproducir el tono?", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = AppTheme.textPrimary)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    SoundAlertMode.entries.forEach { mode ->
                        val isSelected = voiceSettings.soundAlertMode.equals(mode.key, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) (if (AppTheme.isDark) ModernNeonLime.copy(alpha = 0.2f) else ModernMatteBlack.copy(alpha = 0.06f)) else AppTheme.pillBackground,
                            border = androidx.compose.foundation.BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) (if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack) else Color.Transparent),
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.updateVoiceSettings(voiceSettings.copy(soundAlertMode = mode.key)) }
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(16.dp).clip(CircleShape).background(if (isSelected) (if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack) else Color.Transparent).border(2.dp, if (isSelected) (if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack) else AppTheme.textSecondary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(if (AppTheme.isDark) ModernOnNeonLime else Color.White))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(mode.title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = AppTheme.textPrimary)
                                    Text(mode.description, fontSize = 9.sp, color = AppTheme.textSecondary)
                                }
                            }
                        }
                    }
                }

                // Tono
                Text("Selecciona el tono:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = AppTheme.textPrimary)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    NotificationSoundType.entries.forEach { sound ->
                        val isSelected = voiceSettings.notificationSound.equals(sound.key, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) (if (AppTheme.isDark) Color(0xFF242630) else Color.White) else AppTheme.pillBackground,
                            border = androidx.compose.foundation.BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) (if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack) else AppTheme.cardBorder),
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.updateVoiceSettings(voiceSettings.copy(notificationSound = sound.key))
                                viewModel.playNotificationSoundPreview(sound.key)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) (if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack) else AppTheme.pillBackground,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (sound == NotificationSoundType.MUTE) Icons.Default.VolumeMute else Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = if (isSelected) (if (AppTheme.isDark) ModernMatteBlack else ModernNeonLime) else AppTheme.textSecondary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(sound.displayName, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = AppTheme.textPrimary)
                                        Text(sound.description, fontSize = 9.sp, color = AppTheme.textSecondary)
                                    }
                                }
                                if (sound != NotificationSoundType.MUTE) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                                        modifier = Modifier.clickable { viewModel.playNotificationSoundPreview(sound.key) }
                                    ) {
                                        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime, modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("Escuchar", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Volumen del tono
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Volumen del tono", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppTheme.textPrimary)
                    Text("${(voiceSettings.soundVolume * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppTheme.textPrimary)
                }
                Slider(
                    value = voiceSettings.soundVolume,
                    onValueChange = { viewModel.updateVoiceSettings(voiceSettings.copy(soundVolume = it)) },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                        activeTrackColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                        inactiveTrackColor = AppTheme.pillBackground
                    )
                )

                // Vibración háptica
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(AppTheme.pillBackground).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = ModernNeonLime, modifier = Modifier.size(24.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.Vibration, contentDescription = null, tint = ModernMatteBlack, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vibración háptica al recibir cobro", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppTheme.textPrimary)
                    }
                    Switch(
                        checked = voiceSettings.vibrateWithSound,
                        onCheckedChange = { viewModel.updateVoiceSettings(voiceSettings.copy(vibrateWithSound = it)) },
                        colors = appSwitchColors()
                    )
                }
            }
        }
    }
}
