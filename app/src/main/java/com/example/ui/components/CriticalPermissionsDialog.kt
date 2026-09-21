package com.example.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ModernMatteBlack
import com.example.ui.theme.ModernNeonLime
import com.example.ui.theme.ModernOnNeonLime
import com.example.ui.theme.SleekErrorRed
import com.example.ui.theme.SleekSuccessGreen
import com.example.util.PermissionHelper

/**
 * Modal dialog that insists on granting critical permissions required for Yape background voice announcements.
 * Styled consistently with Brynn's modern dark/light design system.
 */
@Composable
fun CriticalPermissionsDialog(
    isListenerGranted: Boolean,
    isBatteryIgnored: Boolean,
    isPostGranted: Boolean,
    onRequestPostPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allGranted = isListenerGranted && isBatteryIgnored && isPostGranted

    if (!allGranted) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 24.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                if (AppTheme.isDark) Color(0xFFEF4444).copy(alpha = 0.2f)
                                else Color(0xFFFEE2E2)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = if (AppTheme.isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Permisos Requeridos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Brynn necesita estos permisos para cantar los cobros en voz alta y seguir activo en segundo plano",
                        fontSize = 12.sp,
                        color = AppTheme.textSecondary,
                        lineHeight = 16.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Permiso de Notificaciones Yape (Listener)
                        PermissionActionRow(
                            title = "1. Acceso a Notificaciones",
                            description = "Detecta y canta los pagos de Yape / Plin / BCP",
                            isGranted = isListenerGranted,
                            icon = Icons.Default.NotificationsActive,
                            buttonText = "Permitir",
                            onClick = { PermissionHelper.openNotificationListenerSettings(context) }
                        )

                        // Pasos numerados de activación (solo si aún no está concedido)
                        if (!isListenerGranted) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (AppTheme.isDark) Color(0xFF1E293B) else Color(0xFFF0F9FF),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (AppTheme.isDark) Color(0xFF334155) else Color(0xFFBAE6FD)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "¿Cómo activarlo?",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (AppTheme.isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                                    )
                                    listOf(
                                        "Toca el botón \"Permitir\" de arriba",
                                        "Busca \"Brynn\" en la lista de apps",
                                        "Activa el interruptor ✅"
                                    ).forEachIndexed { index, step ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (AppTheme.isDark) Color(0xFF0284C7) else Color(0xFF0EA5E9),
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "${index + 1}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = step,
                                                fontSize = 11.sp,
                                                color = AppTheme.textSecondary,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Exclusión de Batería
                        PermissionActionRow(
                            title = "2. Sin Ahorro de Batería",
                            description = "Evita que Android cierre la app al bloquear pantalla",
                            isGranted = isBatteryIgnored,
                            icon = Icons.Default.BatteryAlert,
                            buttonText = "Excluir",
                            onClick = { PermissionHelper.requestIgnoreBatteryOptimization(context) }
                        )

                        // 3. Mostrar Notificaciones (Android 13+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isPostGranted) {
                            PermissionActionRow(
                                title = "3. Mostrar Avisos en Pantalla",
                                description = "Alertas visuales de cobros y sincronización",
                                isGranted = isPostGranted,
                                icon = Icons.Default.Notifications,
                                buttonText = "Permitir",
                                onClick = onRequestPostPermission
                            )
                        }

                        // 4. Inicio Automático (Auto-start)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = AppTheme.cardBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { PermissionHelper.openAutoStartSettings(context) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (AppTheme.isDark) Color(0xFF2563EB).copy(alpha = 0.25f)
                                            else Color(0xFFDBEAFE)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = if (AppTheme.isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Inicio Automático (${PermissionHelper.getDeviceManufacturer()})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AppTheme.textPrimary
                                    )
                                    Text(
                                        text = "Toca para abrir ajustes e iniciar con el celular",
                                        fontSize = 11.sp,
                                        color = AppTheme.textSecondary
                                    )
                                }
                            }
                        }

                        // 5. Bloquear en Memoria (Instrucción rápida)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = AppTheme.pillBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.pillBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (AppTheme.isDark) Color(0xFF374151)
                                            else Color(0xFFE5E7EB)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = AppTheme.textPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Bloquear en Memoria RAM",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AppTheme.textPrimary
                                    )
                                    Text(
                                        text = "En Apps Recientes, ponle candado (🔒) a Brynn",
                                        fontSize = 11.sp,
                                        color = AppTheme.textSecondary
                                    )
                                }
                            }
                        }
                    }


                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Button
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isListenerGranted) {
                                if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack
                            } else {
                                if (AppTheme.isDark) Color(0xFFEF4444) else Color(0xFFDC2626)
                            },
                            contentColor = if (isListenerGranted) {
                                if (AppTheme.isDark) ModernOnNeonLime else Color.White
                            } else {
                                Color.White
                            }
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (isListenerGranted) "Continuar" else "Configurar Después",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionActionRow(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: ImageVector,
    buttonText: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AppTheme.cardBackground,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isGranted) {
                if (AppTheme.isDark) Color(0xFF166534) else Color(0xFFBBF7D0)
            } else {
                if (AppTheme.isDark) Color(0xFF991B1B) else Color(0xFFFECACA)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isGranted) {
                                if (AppTheme.isDark) Color(0xFF14532D).copy(alpha = 0.5f)
                                else Color(0xFFDCFCE7)
                            } else {
                                if (AppTheme.isDark) Color(0xFF7F1D1D).copy(alpha = 0.5f)
                                else Color(0xFFFEE2E2)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) {
                            if (AppTheme.isDark) Color(0xFF4ADE80) else SleekSuccessGreen
                        } else {
                            if (AppTheme.isDark) Color(0xFFF87171) else SleekErrorRed
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.textPrimary
                    )
                    Text(
                        text = description,
                        fontSize = 10.sp,
                        color = AppTheme.textSecondary,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isGranted) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (AppTheme.isDark) Color(0xFF14532D).copy(alpha = 0.6f) else Color(0xFFDCFCE7)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (AppTheme.isDark) Color(0xFF4ADE80) else SleekSuccessGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ACTIVO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (AppTheme.isDark) Color(0xFF4ADE80) else SleekSuccessGreen
                        )
                    }
                }
            } else {
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                        contentColor = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = buttonText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
