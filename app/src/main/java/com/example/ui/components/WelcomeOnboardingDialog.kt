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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ModernBrandBlue
import com.example.ui.theme.ModernMatteBlack
import com.example.ui.theme.ModernNeonLime
import com.example.ui.theme.ModernOnNeonLime
import com.example.ui.theme.SleekErrorRed
import com.example.ui.theme.SleekSuccessGreen
import com.example.ui.viewmodel.MainViewModel

/**
 * Pantalla / Modal de Bienvenida (Onboarding inicial tras login o cambio de modo).
 * Ofrece 3 modos de uso claros:
 * 1) "Solo Local": Datos solo en este dispositivo, al salir de sesión se limpian.
 * 2) "Emisor / Soy Dueño": Conecta trabajadores a sucursales, con ventana de Actualizar a Premium o Demo 30 Días.
 * 3) "Unirme a un Negocio": Ingresa código de vinculación para actuar como receptor.
 */
@Composable
fun WelcomeOnboardingDialog(
    viewModel: MainViewModel,
    onOpenAuthDialog: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showOwnerModal by remember { mutableStateOf(false) }
    var showLinkedStoresDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(28.dp),
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
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "¡Bienvenido a Brynn!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "¿Cómo vas a usar la aplicación en este teléfono?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppTheme.textSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // MODO 1: SOLO LOCAL
                OnboardingRoleOption(
                    icon = Icons.Default.PhoneAndroid,
                    iconBg = Color(0xFF64748B).copy(alpha = 0.15f),
                    iconTint = Color(0xFF475569),
                    title = "Solo Local",
                    badge = "INDIVIDUAL",
                    badgeColor = Color(0xFF64748B),
                    subtitle = "Los datos se guardan solo en este teléfono. Al cerrar sesión tus datos locales se limpiarán por privacidad.",
                    onClick = {
                        viewModel.activateSoloLocal()
                        Toast.makeText(context, "Modo Solo Local activado", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // MODO 2: EMISOR / SOY DUEÑO DE UN LOCAL
                OnboardingRoleOption(
                    icon = Icons.Default.Store,
                    iconBg = Color(0xFF16A34A).copy(alpha = 0.15f),
                    iconTint = Color(0xFF16A34A),
                    title = "Emisor / Soy Dueño de un Local",
                    badge = "DUEÑO · PRO",
                    badgeColor = Color(0xFF16A34A),
                    subtitle = "Envía confirmaciones a tus trabajadores, asigna sucursales con exclusión mutua y gestiona tu caja.",
                    onClick = {
                        showOwnerModal = true
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // MODO 3: RECEPTOR / TRABAJADOR DE NEGOCIO(S)
                OnboardingRoleOption(
                    icon = Icons.Default.Key,
                    iconBg = Color(0xFFD97706).copy(alpha = 0.15f),
                    iconTint = Color(0xFFD97706),
                    title = "Receptor / Trabajador",
                    badge = "TRABAJADOR",
                    badgeColor = Color(0xFFD97706),
                    subtitle = "Escucha y valida pagos en tu sucursal. Conéctate a tus tiendas registradas o únete a una nueva.",
                    onClick = {
                        showLinkedStoresDialog = true
                    }
                )
            }
        }
    }

    // ─── MODAL: ACTUALIZAR A PREMIUM O PROBAR DEMO 30 DÍAS (MODO EMISOR) ────
    if (showOwnerModal) {
        Dialog(
            onDismissRequest = { showOwnerModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ModernNeonLime.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "PLAN DUEÑO / EMISOR",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = { showOwnerModal = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = AppTheme.textSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Conecta tu Negocio y Trabajadores",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Activa el modo Emisor para centralizar cobros y compartirlos a distancia con tu personal.",
                        fontSize = 12.sp,
                        color = AppTheme.textSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Lista de Beneficios
                    BenefitItem(
                        icon = Icons.Default.Bolt,
                        title = "Alertas en Tiempo Real",
                        description = "Tus trabajadores escuchan y ven los Yapes al instante en sus teléfonos."
                    )
                    BenefitItem(
                        icon = Icons.Default.Business,
                        title = "Multi-Sucursal & Exclusión Mutua",
                        description = "Etiqueta trabajadores por sucursal. Un pago validado no puede repetirse en otra tienda."
                    )
                    BenefitItem(
                        icon = Icons.Default.Star,
                        title = "Control y Auditoría Total",
                        description = "Filtra reportes por sucursal, personaliza nombres, horarios o revoca accesos cuando quieras."
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // BOTÓN 1: PROBAR DEMO 30 DÍAS
                    Button(
                        onClick = {
                            viewModel.activateTrialPlan { success, errorMsg ->
                                if (success) {
                                    Toast.makeText(context, "🎉 ¡Demo de 30 Días activada!", Toast.LENGTH_SHORT).show()
                                    showOwnerModal = false
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, errorMsg ?: "Error al registrar tienda en la nube", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                            contentColor = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "🎁 Probar Demo 30 Días Gratis",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // BOTÓN 2: ACTUALIZAR A PREMIUM (REVENUECAT PAYWALL)
                    OutlinedButton(
                        onClick = {
                            showOwnerModal = false
                            onDismiss()
                            viewModel.openPaywall()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = "Actualizar a Premium (Sin límites)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack
                        )
                    }
                }
            }
        }
    }

    // ─── MODAL: MIS TIENDAS & VINCULACIÓN (MODO RECEPTOR) ───────────────────
    if (showLinkedStoresDialog) {
        LinkedStoresDialog(
            viewModel = viewModel,
            onDismiss = { showLinkedStoresDialog = false },
            onStoreSelected = {
                showLinkedStoresDialog = false
                onDismiss()
            }
        )
    }
}

@Composable
private fun BenefitItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (AppTheme.isDark) ModernNeonLime.copy(alpha = 0.15f) else ModernBrandBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (AppTheme.isDark) ModernNeonLime else ModernBrandBlue,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = AppTheme.textSecondary,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun OnboardingRoleOption(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    badge: String,
    badgeColor: Color,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AppTheme.cardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder),
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
                            fontSize = 9.sp,
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

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = AppTheme.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
