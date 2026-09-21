package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DeviceRole
import com.example.data.supabase.SupabaseLinkedStoreDto
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ModernBrandBlue
import com.example.ui.theme.ModernMatteBlack
import com.example.ui.theme.ModernNeonLime
import com.example.ui.theme.ModernOnNeonLime
import com.example.ui.theme.SleekErrorRed
import com.example.ui.theme.SleekSuccessGreen
import com.example.ui.viewmodel.MainViewModel

/**
 * Diálogo interactivo para que un Trabajador / Receptor pueda ver todos los negocios y tiendas
 * a las que pertenece (incluso de diferentes dueños), cambiar su tienda activa o unirse a una nueva con código.
 */
@Composable
fun LinkedStoresDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onStoreSelected: ((SupabaseLinkedStoreDto) -> Unit)? = null
) {
    val context = LocalContext.current
    val linkedStores by viewModel.linkedStores.collectAsStateWithLifecycle()
    val storeConfig by viewModel.storeConfig.collectAsStateWithLifecycle()

    var isRefreshing by remember { mutableStateOf(false) }
    var showPairingInput by remember { mutableStateOf(false) }
    var pairingCodeInput by remember { mutableStateOf("") }
    var isPairingLoading by remember { mutableStateOf(false) }
    var pairingErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isRefreshing = true
        viewModel.loadMyLinkedStores {
            isRefreshing = false
        }
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
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFD97706).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "MODO RECEPTOR · TRABAJADOR",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFD97706),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFFD97706)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            IconButton(
                                onClick = {
                                    isRefreshing = true
                                    viewModel.loadMyLinkedStores { isRefreshing = false }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = AppTheme.textSecondary)
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = AppTheme.textSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Tus Negocios & Tiendas",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Selecciona el negocio en el que trabajarás hoy para escuchar y validar cobros.",
                    fontSize = 12.sp,
                    color = AppTheme.textSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Filtrar únicamente tiendas activas no revocadas ni presentes en blacklist
                val activeLinkedStores = linkedStores.filter { 
                    !it.status.equals("REVOKED", ignoreCase = true) && !viewModel.isStoreRevoked(it.storeCode)
                }

                // Estado: Tiene tiendas vinculadas
                if (activeLinkedStores.isNotEmpty()) {
                    Text(
                        text = "TIENDAS DONDE ESTÁS REGISTRADO (${activeLinkedStores.size})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = AppTheme.textSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        activeLinkedStores.forEach { store ->
                            val isActive = storeConfig.deviceRole == DeviceRole.RECEIVER &&
                                    storeConfig.storeCode.trim().equals(store.storeCode.trim(), ignoreCase = true)

                            LinkedStoreCard(
                                store = store,
                                isActive = isActive,
                                onSelect = {
                                    viewModel.selectActiveStore(
                                        storeCode = store.storeCode,
                                        storeName = store.storeName,
                                        branchName = store.branchName
                                    ) { success, errMsg ->
                                        if (success) {
                                            Toast.makeText(
                                                context,
                                                "Conectado a ${store.storeName}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onStoreSelected?.invoke(store)
                                            onDismiss()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                errMsg ?: "No se pudo conectar a la tienda",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            viewModel.loadMyLinkedStores()
                                        }
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = AppTheme.cardBorder, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Botón para agregar una nueva tienda mediante código
                    Button(
                        onClick = {
                            pairingCodeInput = ""
                            pairingErrorMessage = null
                            showPairingInput = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (AppTheme.isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFD97706))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Vincular a Otro Negocio con Código",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                } else if (!isRefreshing) {
                    // Estado vacío: Sin tiendas vinculadas aún
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = AppTheme.pillBackground,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD97706).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(24.dp))
                            }

                            Text(
                                text = "Aún no estás vinculado a ningún negocio",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Pide al dueño de la tienda que genere un código de vinculación de 8 caracteres desde su Panel de Trabajadores.",
                                fontSize = 12.sp,
                                color = AppTheme.textSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 16.sp
                            )

                            Button(
                                onClick = {
                                    pairingCodeInput = ""
                                    pairingErrorMessage = null
                                    showPairingInput = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD97706),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ingresar Código de Vinculación", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal para ingresar el código de vinculación
    if (showPairingInput) {
        AlertDialog(
            onDismissRequest = { if (!isPairingLoading) showPairingInput = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD97706).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(24.dp))
                }
            },
            title = {
                Text("Vincular Nuevo Negocio", fontWeight = FontWeight.Black, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Ingresa el código seguro proporcionado por el dueño del local:",
                        fontSize = 13.sp,
                        color = AppTheme.textSecondary
                    )

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
                        shape = RoundedCornerShape(14.dp)
                    )

                    if (pairingErrorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SleekErrorRed.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = pairingErrorMessage ?: "",
                                color = SleekErrorRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp)
                            )
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
                                showPairingInput = false
                                Toast.makeText(context, "¡Vinculado exitosamente!", Toast.LENGTH_SHORT).show()
                                val currentCode = viewModel.storeConfig.value.storeCode
                                val latest = viewModel.linkedStores.value.firstOrNull {
                                    it.storeCode.equals(currentCode, ignoreCase = true)
                                } ?: SupabaseLinkedStoreDto(
                                    storeCode = currentCode,
                                    storeName = viewModel.storeConfig.value.storeName,
                                    branchName = viewModel.storeConfig.value.workerBranchName
                                )
                                onStoreSelected?.invoke(latest)
                                onDismiss()
                            } else {
                                pairingErrorMessage = message ?: "Código no válido o expirado"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                        contentColor = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isPairingLoading
                ) {
                    if (isPairingLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Unirme al Negocio", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (!isPairingLoading) {
                    OutlinedButton(
                        onClick = { showPairingInput = false },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar", color = AppTheme.textSecondary)
                    }
                }
            }
        )
    }
}

@Composable
private fun LinkedStoreCard(
    store: SupabaseLinkedStoreDto,
    isActive: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (isActive) SleekSuccessGreen else AppTheme.cardBorder
    val bgColor = if (isActive) SleekSuccessGreen.copy(alpha = 0.08f) else AppTheme.cardBackground

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(if (isActive) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isActive) SleekSuccessGreen.copy(alpha = 0.18f) else Color(0xFFD97706).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Store,
                    contentDescription = null,
                    tint = if (isActive) SleekSuccessGreen else Color(0xFFD97706),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = store.storeName.ifBlank { "Negocio" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SleekSuccessGreen.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "ACTIVA AHORA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = SleekSuccessGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Código de Tienda
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (AppTheme.isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = "#${store.storeCode}",
                            fontSize = 10.sp,
                            color = AppTheme.textSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (store.customName.isNotBlank() && !store.customName.equals("Trabajador", ignoreCase = true)) {
                    Text(
                        text = "Registrado como: ${store.customName}",
                        fontSize = 11.sp,
                        color = AppTheme.textSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onSelect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) SleekSuccessGreen else (if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack),
                    contentColor = if (isActive) Color.White else (if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime)
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                if (isActive) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Activo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("Entrar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
