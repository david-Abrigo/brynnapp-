package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DeviceRole
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ModernMatteBlack
import com.example.ui.theme.ModernNeonLime
import com.example.ui.theme.ModernOnNeonLime
import com.example.ui.theme.SleekSuccessGreen
import com.example.ui.viewmodel.MainViewModel
import com.example.util.AppRestartHelper

@Composable
fun ChangeDeviceModeDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val storeConfig by viewModel.storeConfig.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showRestartDialog by remember { mutableStateOf(false) }
    var pendingModeName by remember { mutableStateOf("") }
    var showConfirmSwitchToOwnerDialog by remember { mutableStateOf(false) }

    if (showConfirmSwitchToOwnerDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmSwitchToOwnerDialog = false },
            containerColor = AppTheme.cardBackground,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    text = "⚠️ ¿Cambiar a Modo Dueño?",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = AppTheme.textPrimary
                )
            },
            text = {
                Text(
                    text = "Actualmente estás configurado como Receptor/Trabajador en '${storeConfig.storeName}'.\n\n" +
                            "Al cambiar a Modo Dueño, tu teléfono se desvinculará de esa tienda y cargará tu propio negocio independiente. " +
                            "No podrás administrar la tienda ni los trabajadores de '${storeConfig.storeName}'.\n\n" +
                            "¿Deseas continuar?",
                    fontSize = 13.sp,
                    color = AppTheme.textSecondary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmSwitchToOwnerDialog = false
                        viewModel.activateTrialPlan { success, errorMsg ->
                            if (success) {
                                pendingModeName = "Emisor / Dueño"
                                showRestartDialog = true
                            } else {
                                Toast.makeText(context, errorMsg ?: "Error al registrar tienda en la nube", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                        contentColor = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sí, cambiar a mi negocio", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirmSwitchToOwnerDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancelar", color = AppTheme.textSecondary, fontSize = 12.sp)
                }
            }
        )
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = {
                showRestartDialog = false
                onDismiss()
            },
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
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Reinicio necesario",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = AppTheme.textPrimary
                    )
                }
            },
            text = {
                Text(
                    text = "Has seleccionado el modo $pendingModeName. Para inicializar los módulos de este modo y adaptar la aplicación correctamente, es necesario reiniciar NotiYape.",
                    fontSize = 13.sp,
                    color = AppTheme.textSecondary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestartDialog = false
                        AppRestartHelper.restartApp(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                        contentColor = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reiniciar ahora", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showRestartDialog = false
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Más tarde", color = AppTheme.textSecondary, fontSize = 13.sp)
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row con botón cerrar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Cambiar Modo de Uso",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Elige la función de este teléfono",
                                fontSize = 11.sp,
                                color = AppTheme.textSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = AppTheme.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                val isSoloLocal = storeConfig.deviceRole == DeviceRole.LOCAL_SPEAKER
                val isSender = storeConfig.deviceRole == DeviceRole.SENDER
                val isReceiver = storeConfig.deviceRole == DeviceRole.RECEIVER

                // ── MODO 1: SOLO LOCAL ──────────────────────────────────────
                ModeSelectionCard(
                    icon = Icons.Default.PhoneAndroid,
                    iconBg = Color(0xFF64748B).copy(alpha = 0.15f),
                    iconTint = Color(0xFF475569),
                    title = "Solo Local",
                    badge = "INDIVIDUAL · 100% PRIVADO",
                    badgeColor = Color(0xFF64748B),
                    subtitle = "Los datos se guardan solo en este teléfono. Funciona de manera autónoma sin internet ni conexión a la nube.",
                    isSelected = isSoloLocal,
                    onClick = {
                        if (isSoloLocal) {
                            Toast.makeText(context, "Ya te encuentras en modo Solo Local", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.activateSoloLocal()
                            pendingModeName = "Solo Local"
                            showRestartDialog = true
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── MODO 2: EMISOR / SOY DUEÑO DE UN LOCAL ───────────────────
                ModeSelectionCard(
                    icon = Icons.Default.Store,
                    iconBg = Color(0xFF16A34A).copy(alpha = 0.15f),
                    iconTint = Color(0xFF16A34A),
                    title = "Emisor / Dueño de Tienda",
                    badge = "DUEÑO · CENTRALIZADO",
                    badgeColor = Color(0xFF16A34A),
                    subtitle = "Cobra en este teléfono, sube pagos a la nube, gestiona sucursales y comparte cobros con tus trabajadores vinculados.",
                    isSelected = isSender,
                    onClick = {
                        if (isSender) {
                            Toast.makeText(context, "Ya te encuentras en modo Emisor / Dueño", Toast.LENGTH_SHORT).show()
                        } else if (isReceiver) {
                            showConfirmSwitchToOwnerDialog = true
                        } else {
                            viewModel.activateTrialPlan { success, errorMsg ->
                                if (success) {
                                    pendingModeName = "Emisor / Dueño"
                                    showRestartDialog = true
                                } else {
                                    Toast.makeText(context, errorMsg ?: "Error al registrar tienda en la nube", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── MODO 3: RECEPTOR / TRABAJADOR ────────────────────────────
                ModeSelectionCard(
                    icon = Icons.Default.Key,
                    iconBg = Color(0xFFD97706).copy(alpha = 0.15f),
                    iconTint = Color(0xFFD97706),
                    title = "Receptor / Trabajador",
                    badge = "TRABAJADOR · MULTI-TIENDA",
                    badgeColor = Color(0xFFD97706),
                    subtitle = "Escucha y valida pagos en tiempo real desde los teléfonos de caja de tus tiendas vinculadas, sin ver configuración de sucursal.",
                    isSelected = isReceiver,
                    onClick = {
                        if (isReceiver) {
                            Toast.makeText(context, "Ya te encuentras en modo Receptor / Trabajador", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.prepareSwitchToReceiver()
                            pendingModeName = "Receptor / Trabajador"
                            showRestartDialog = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ModeSelectionCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    badge: String,
    badgeColor: Color,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AppTheme.cardBackground,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 1.8.dp else 1.dp,
            if (isSelected) SleekSuccessGreen else AppTheme.cardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = AppTheme.textSecondary,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 15.sp
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = SleekSuccessGreen,
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Activo",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
