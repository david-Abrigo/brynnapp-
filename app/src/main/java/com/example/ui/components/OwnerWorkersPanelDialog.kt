package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.supabase.SupabaseReceiverDto
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ModernMatteBlack
import com.example.ui.theme.ModernNeonLime
import com.example.ui.theme.ModernOnNeonLime
import com.example.ui.theme.SleekErrorRed
import com.example.ui.theme.SleekSuccessGreen
import com.example.ui.viewmodel.MainViewModel
import com.example.util.WorkerScheduleHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OwnerWorkersPanelDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val storeConfig by viewModel.storeConfig.collectAsStateWithLifecycle()
    val receivers by viewModel.storeReceivers.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingReceivers.collectAsStateWithLifecycle()

    var generatedCode by remember { mutableStateOf<String?>(null) }
    var isGeneratingCode by remember { mutableStateOf(false) }

    // Diálogos de acción
    var workerToEditName by remember { mutableStateOf<SupabaseReceiverDto?>(null) }
    var workerToEditSchedule by remember { mutableStateOf<SupabaseReceiverDto?>(null) }
    var workerToKick by remember { mutableStateOf<SupabaseReceiverDto?>(null) }

    // Carga inicial al abrir el dialogo.
    // Las actualizaciones en tiempo real llegan via Supabase Realtime (WebSocket).
    LaunchedEffect(storeConfig.storeCode) {
        if (storeConfig.storeCode.isNotBlank()) {
            viewModel.loadStoreReceivers(silent = false)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Panel de Trabajadores",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = AppTheme.textPrimary
                            )
                            Text(
                                text = "Tienda: ${storeConfig.storeName} (${storeConfig.storeCode})",
                                fontSize = 11.sp,
                                color = AppTheme.textSecondary
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.loadStoreReceivers() },
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Recargar", tint = AppTheme.textPrimary)
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = AppTheme.textPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SECCIÓN: GENERAR CÓDIGO LARGO PARA TRABAJADORES
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (AppTheme.isDark) Color(0xFF191F1A) else Color(0xFFF2FBF4)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (AppTheme.isDark) ModernNeonLime.copy(alpha = 0.5f) else ModernMatteBlack.copy(alpha = 0.15f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Código para que se unan",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = AppTheme.textPrimary
                                )
                            }

                            Button(
                                onClick = {
                                    isGeneratingCode = true
                                    viewModel.generatePairingCode(storeConfig.storeCode) { success, result ->
                                        isGeneratingCode = false
                                        if (success && result != null) {
                                            generatedCode = result
                                            Toast.makeText(context, "¡Código generado con éxito!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, result ?: "Error al generar código", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                enabled = !isGeneratingCode,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                                    contentColor = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                if (isGeneratingCode) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                                } else {
                                    Text(if (generatedCode == null) "Generar Código" else "Nuevo Código", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (generatedCode != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (AppTheme.isDark) ModernNeonLime.copy(alpha = 0.12f) else ModernMatteBlack.copy(alpha = 0.05f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("CÓDIGO DE VINCULACIÓN (24H)", fontSize = 9.sp, fontWeight = FontWeight.Black, color = AppTheme.textSecondary)
                                        Text(
                                            text = generatedCode ?: "",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 2.sp,
                                            color = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack
                                        )
                                    }
                                    IconButton(onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Código Vinculación", generatedCode))
                                        Toast.makeText(context, "Código copiado al portapapeles", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = AppTheme.textPrimary)
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Genera un código seguro para que tus trabajadores se vinculen y reciban los cobros.",
                                fontSize = 11.sp,
                                color = AppTheme.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Resumen de receptores
                val activeWorkers = receivers.filter { !it.status.equals("REVOKED", ignoreCase = true) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Trabajadores Vinculados (${activeWorkers.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = AppTheme.textPrimary
                    )
                    Text(
                        text = if (isLoading) "Actualizando..." else "En tiempo real",
                        fontSize = 10.sp,
                        color = AppTheme.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // LISTA DE TRABAJADORES
                if (activeWorkers.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = AppTheme.cardBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = AppTheme.textSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Aún no hay trabajadores vinculados",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = AppTheme.textPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Genera un código arriba y compártelo con tus empleados para que se unan a esta tienda.",
                                fontSize = 12.sp,
                                color = AppTheme.textSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(activeWorkers, key = { it.id ?: it.userId ?: it.hashCode().toString() }) { worker ->
                            WorkerCard(
                                worker = worker,
                                onEditName = { workerToEditName = worker },
                                onEditSchedule = { workerToEditSchedule = worker },
                                onKick = { workerToKick = worker }
                            )
                        }
                    }
                }
            }
        }
    }

    // DIÁLOGO 1: PERSONALIZAR NOMBRE
    if (workerToEditName != null) {
        val worker = workerToEditName!!
        var newNameInput by remember { mutableStateOf(worker.customName) }
        var isSaving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSaving) workerToEditName = null },
            title = {
                Text("Personalizar Nombre", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Asigna un nombre o alias para identificar a este trabajador en tu negocio:",
                        fontSize = 12.sp,
                        color = AppTheme.textSecondary
                    )
                    OutlinedTextField(
                        value = newNameInput,
                        onValueChange = { newNameInput = it },
                        label = { Text("Nombre / Alias") },
                        placeholder = { Text("Ej: Juan - Caja 1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newNameInput.trim()
                        if (name.isBlank()) return@Button
                        val id = worker.id ?: return@Button
                        isSaving = true
                        viewModel.updateReceiverName(id, name) { success, err ->
                            isSaving = false
                            if (success) {
                                Toast.makeText(context, "Nombre actualizado", Toast.LENGTH_SHORT).show()
                                workerToEditName = null
                            } else {
                                Toast.makeText(context, err ?: "Error al actualizar", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                        contentColor = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime
                    ),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isSaving
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (!isSaving) {
                    TextButton(onClick = { workerToEditName = null }) {
                        Text("Cancelar")
                    }
                }
            }
        )
    }

    // DIÁLOGO 2: CONFIGURAR HORARIO
    if (workerToEditSchedule != null) {
        val worker = workerToEditSchedule!!
        WorkerScheduleDialog(
            worker = worker,
            onDismiss = { workerToEditSchedule = null },
            onSave = { scheduleEnabled, startTime, endTime, days, onComplete ->
                val id = worker.id
                if (id == null) {
                    onComplete(false, "ID no válido")
                    return@WorkerScheduleDialog
                }
                viewModel.updateReceiverSchedule(
                    receiverId = id,
                    scheduleEnabled = scheduleEnabled,
                    startTime = startTime,
                    endTime = endTime,
                    days = days
                ) { success, err ->
                    onComplete(success, err)
                    if (success) {
                        Toast.makeText(context, "Horario guardado correctamente", Toast.LENGTH_SHORT).show()
                        workerToEditSchedule = null
                    } else {
                        Toast.makeText(context, err ?: "Error al guardar horario", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // DIÁLOGO 3: BOTAR / EXPULSAR TRABAJADOR
    if (workerToKick != null) {
        val worker = workerToKick!!
        var isKicking by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isKicking) workerToKick = null },
            icon = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = SleekErrorRed, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("¿Expulsar Trabajador?", fontWeight = FontWeight.Black, fontSize = 16.sp)
            },
            text = {
                Text(
                    text = "¿Estás seguro de botar a '${worker.customName}' (${worker.userEmail ?: "Sin correo"})? Ya no recibirá los cobros ni notificaciones de esta tienda.",
                    fontSize = 13.sp,
                    color = AppTheme.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = worker.id ?: return@Button
                        isKicking = true
                        viewModel.kickReceiver(worker) { success, err ->
                            isKicking = false
                            if (success) {
                                Toast.makeText(context, "Trabajador desvinculado", Toast.LENGTH_SHORT).show()
                                workerToKick = null
                            } else {
                                Toast.makeText(context, err ?: "Error al expulsar", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekErrorRed, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isKicking
                ) {
                    Text("Sí, Expulsar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (!isKicking) {
                    OutlinedButton(
                        onClick = { workerToKick = null },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        )
    }
}

@Composable
private fun WorkerCard(
    worker: SupabaseReceiverDto,
    onEditName: () -> Unit,
    onEditSchedule: () -> Unit,
    onKick: () -> Unit
) {
    val isRevoked = worker.status.equals("REVOKED", ignoreCase = true)
    val isInSchedule = WorkerScheduleHelper.isWithinSchedule(
        scheduleEnabled = worker.scheduleEnabled,
        startTime = worker.scheduleStartTime,
        endTime = worker.scheduleEndTime,
        days = worker.scheduleDays
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AppTheme.cardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (AppTheme.isDark) Color(0xFF2E313C) else Color(0xFFE2E8F0),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = worker.customName.firstOrNull()?.uppercase() ?: "T",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = AppTheme.textPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = worker.customName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = AppTheme.textPrimary
                            )
                            IconButton(onClick = onEditName, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar nombre", tint = AppTheme.textSecondary, modifier = Modifier.size(14.dp))
                            }
                        }
                        Text(
                            text = worker.userEmail ?: "Sin correo registrado",
                            fontSize = 11.sp,
                            color = AppTheme.textSecondary
                        )
                    }
                }

                // Badge de estado
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        isRevoked -> SleekErrorRed.copy(alpha = 0.15f)
                        isInSchedule -> SleekSuccessGreen.copy(alpha = 0.15f)
                        else -> Color(0xFFD97706).copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = when {
                            isRevoked -> "DESVINCULADO"
                            isInSchedule -> "EN TURNO"
                            else -> "FUERA HORARIO"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = when {
                            isRevoked -> SleekErrorRed
                            isInSchedule -> SleekSuccessGreen
                            else -> Color(0xFFD97706)
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Horario info
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AppTheme.pillBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = AppTheme.textSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (worker.scheduleEnabled) {
                                "${worker.scheduleStartTime} - ${worker.scheduleEndTime} (${if (worker.scheduleDays == "ALL") "Todos los días" else worker.scheduleDays})"
                            } else {
                                "24/7 (Sin límite de horario)"
                            },
                            fontSize = 11.sp,
                            color = AppTheme.textSecondary
                        )
                    }

                    TextButton(onClick = onEditSchedule, modifier = Modifier.height(28.dp)) {
                        Text("Horario", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Acciones inferiores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onKick,
                    colors = ButtonDefaults.textButtonColors(contentColor = SleekErrorRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Botar / Expulsar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
