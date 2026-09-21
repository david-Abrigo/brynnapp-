package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.example.data.model.DeviceRole
import com.example.ui.components.LinkedStoresDialog
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ModernBrandBlue
import com.example.ui.theme.ModernMatteBlack
import com.example.ui.theme.ModernNeonLime
import com.example.ui.theme.ModernOnNeonLime
import com.example.ui.viewmodel.MainViewModel

/**
 * Pantalla Completa de Selección de Modo de Uso.
 * Aparece obligatoriamente tras iniciar sesión si el dispositivo no ha sido configurado.
 * 
 * Reglas clave:
 * 1) Ocupa toda la pantalla (fillMaxSize).
 * 2) El botón Atrás de Android está estrictamente bloqueado con BackHandler.
 * 3) Una vez seleccionado un modo, exige reiniciar la aplicación para cargar
 *    únicamente los módulos y configuraciones necesarias de ese modo.
 */
@Composable
fun ModeSelectionScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Bloqueo estricto del botón de atrás de Android en toda la pantalla
    BackHandler(enabled = true) {
        Toast.makeText(
            context,
            "Debes elegir un modo de uso para comenzar",
            Toast.LENGTH_SHORT
        ).show()
    }

    var showOwnerModal by remember { mutableStateOf(false) }
    var showLinkedStoresDialog by remember { mutableStateOf(false) }

    // ─── PANTALLA PRINCIPAL DE SELECCIÓN (OCUPA TODO EL DISPOSITIVO) ───────────
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.canvasBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Badge de Marca
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (AppTheme.isDark) ModernNeonLime.copy(alpha = 0.18f) else ModernMatteBlack.copy(alpha = 0.06f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (AppTheme.isDark) ModernNeonLime.copy(alpha = 0.4f) else ModernMatteBlack.copy(alpha = 0.12f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CONFIGURACIÓN DE DISPOSITIVO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Elige tu Modo de Uso",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = AppTheme.textPrimary,
            letterSpacing = (-0.5).sp
        )

        Text(
            text = "Cada modo carga únicamente las pantallas, funciones y módulos que necesitas en este teléfono.",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AppTheme.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(26.dp))

        // ── 1. MODO: SOLO LOCAL (CAJA AUTÓNOMA) ──────────────────────────────
        FullRoleCard(
            icon = Icons.Default.PhoneAndroid,
            iconTint = Color(0xFF64748B),
            title = "Solo Locutor Local (Caja)",
            badge = "100% PRIVADO · SIN NUBE",
            badgeColor = Color(0xFF64748B),
            description = "Lee en voz alta cada Yape recibido en este teléfono. No requiere internet ni sincronización en la nube. 3 pestañas limpias.",
            buttonText = "Elegir Solo Local",
            onClick = {
                viewModel.activateSoloLocal()
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── 2. MODO: EMISOR / DUEÑO DE TIENDA ────────────────────────────────
        FullRoleCard(
            icon = Icons.Default.Store,
            iconTint = Color(0xFF16A34A),
            title = "Emisor / Soy Dueño de Tienda",
            badge = "DUEÑO · CENTRALIZADO",
            badgeColor = Color(0xFF16A34A),
            description = "Cobra en este teléfono, sube pagos a la nube, gestiona sucursales y comparte alertas en vivo con tus trabajadores. 4 pestañas (incluye Equipo).",
            buttonText = "Elegir Modo Dueño",
            onClick = {
                showOwnerModal = true
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── 3. MODO: RECEPTOR / TRABAJADOR ───────────────────────────────────
        FullRoleCard(
            icon = Icons.Default.Key,
            iconTint = Color(0xFFD97706),
            title = "Receptor / Trabajador",
            badge = "TRABAJADOR · MULTI-TIENDA",
            badgeColor = Color(0xFFD97706),
            description = "Escucha y valida pagos en tiempo real desde la caja. No captura tus Yapes personales. Oculta configuración de sucursales.",
            buttonText = "Elegir Modo Trabajador",
            onClick = {
                showLinkedStoresDialog = true
            }
        )

        Spacer(modifier = Modifier.height(28.dp))
    }

    // ─── MODAL: BENEFICIOS / DEMO 30 DÍAS PARA DUEÑO ──────────────────────────
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

                    BenefitLine(
                        icon = Icons.Default.Bolt,
                        title = "Alertas en Tiempo Real",
                        description = "Tus trabajadores escuchan y ven los Yapes al instante en sus teléfonos."
                    )
                    BenefitLine(
                        icon = Icons.Default.Business,
                        title = "Multi-Sucursal & Exclusión Mutua",
                        description = "Etiqueta trabajadores por sucursal. Un pago validado no puede repetirse en otra tienda."
                    )
                    BenefitLine(
                        icon = Icons.Default.Star,
                        title = "Control y Auditoría Total",
                        description = "Filtra reportes por sucursal, gestiona accesos y revoca personal cuando quieras."
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            viewModel.activateTrialPlan { success, errorMsg ->
                                if (success) {
                                    showOwnerModal = false
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

                    OutlinedButton(
                        onClick = {
                            viewModel.activateTrialPlan { success, errorMsg ->
                                if (success) {
                                    showOwnerModal = false
                                } else {
                                    Toast.makeText(context, errorMsg ?: "Error al registrar tienda en la nube", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = "Activar Modo Emisor (Dueño)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            showOwnerModal = false
                            viewModel.openPaywall()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = "👑 Ver Planes Premium (Sin límites)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack
                        )
                    }
                }
            }
        }
    }

    // ─── MODAL: MIS TIENDAS & VINCULACIÓN (MODO RECEPTOR) ──────────────────────
    if (showLinkedStoresDialog) {
        LinkedStoresDialog(
            viewModel = viewModel,
            onDismiss = {
                showLinkedStoresDialog = false
            },
            onStoreSelected = {
                showLinkedStoresDialog = false
            }
        )
    }
}

@Composable
private fun FullRoleCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    badge: String,
    badgeColor: Color,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconTint.copy(alpha = 0.14f),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = AppTheme.textPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                fontSize = 12.sp,
                color = AppTheme.textSecondary,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                    contentColor = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(
                    text = buttonText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun BenefitLine(
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
