package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.supabase.SupabaseReceiverDto
import com.example.ui.components.ManageBranchesDialog
import com.example.ui.components.WorkerScheduleDialog
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
fun WorkersScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
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
    var workerToEditBranch by remember { mutableStateOf<SupabaseReceiverDto?>(null) }
    var workerToKick by remember { mutableStateOf<SupabaseReceiverDto?>(null) }
    var showManageBranchesDialog by remember { mutableStateOf(false) }

    // Carga inicial de trabajadores al abrir la pantalla.
    // Las actualizaciones en tiempo real llegan via Supabase Realtime (WebSocket).
    LaunchedEffect(storeConfig.storeCode) {
        if (storeConfig.storeCode.isNotBlank()) {
            viewModel.loadStoreReceivers(silent = false)
        }
    }

    val activeWorkers = remember(receivers) {
        receivers.filter { !it.status.equals("REVOKED", ignoreCase = true) }
    }

    val workersInShiftCount = remember(activeWorkers) {
        activeWorkers.count {
            WorkerScheduleHelper.isWithinSchedule(
                scheduleEnabled = it.scheduleEnabled,
                startTime = it.scheduleStartTime,
                endTime = it.scheduleEndTime,
                days = it.scheduleDays
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.canvasBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 1. HEADER DE LA PANTALLA ─────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GESTIÓN DE EQUIPO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = if (AppTheme.isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    Text(
                        text = "Trabajadores & Receptores",
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        letterSpacing = (-0.5).sp,
                        color = AppTheme.textPrimary
                    )
                    Text(
                        text = "Tienda: ${storeConfig.storeName.ifBlank { "Mi Negocio" }} · Código: ${storeConfig.storeCode}",
                        fontSize = 12.sp,
                        color = AppTheme.textSecondary
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (AppTheme.isDark) Color(0xFF1E2028) else Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder),
                    modifier = Modifier.size(42.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.loadStoreReceivers() },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Recargar",
                                tint = AppTheme.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── 2. STATS RÁPIDOS ────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatPill(
                    label = "Total Vinculados",
                    value = activeWorkers.size.toString(),
                    icon = Icons.Default.People,
                    color = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    label = "En Turno Ahora",
                    value = workersInShiftCount.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = SleekSuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    label = "Con Horario",
                    value = activeWorkers.count { it.scheduleEnabled }.toString(),
                    icon = Icons.Default.Schedule,
                    color = Color(0xFF0284C7),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── 3. TARJETA: GENERAR CÓDIGO DE VINCULACIÓN (AMPLIA Y DESTACADA) ───
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (AppTheme.isDark) Color(0xFF18221B) else Color(0xFFF2FBF4)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.2.dp,
                    if (AppTheme.isDark) ModernNeonLime.copy(alpha = 0.5f) else ModernMatteBlack.copy(alpha = 0.15f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = ModernNeonLime,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        tint = ModernMatteBlack,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Vincular Nuevo Trabajador",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = AppTheme.textPrimary
                                )
                                Text(
                                    text = "Genera un código para que otro teléfono escuche los cobros",
                                    fontSize = 11.sp,
                                    color = AppTheme.textSecondary
                                )
                            }
                        }
                    }

                    if (generatedCode != null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (AppTheme.isDark) ModernNeonLime.copy(alpha = 0.12f) else ModernMatteBlack.copy(alpha = 0.06f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "CÓDIGO DE ACCESO (VÁLIDO POR 24 HORAS)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = AppTheme.textSecondary
                                        )
                                        Text(
                                            text = generatedCode ?: "",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 3.sp,
                                            color = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Código Vinculación", generatedCode))
                                            Toast.makeText(context, "¡Código copiado!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                                            contentColor = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Copiar Código", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val shareText = "¡Hola! Únete a la tienda '${storeConfig.storeName}' en NotiYape usando este código de vinculación: $generatedCode"
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                                type = "text/plain"
                                            }
                                            val shareIntent = Intent.createChooser(sendIntent, "Compartir código de vinculación")
                                            context.startActivity(shareIntent)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Compartir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
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
                                    Toast.makeText(context, "¡Código generado exitosamente!", Toast.LENGTH_SHORT).show()
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
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        if (isGeneratingCode) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime
                            )
                        } else {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (generatedCode == null) "Generar Código de Vinculación" else "Generar Otro Código",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // ── GESTIÓN DE SUCURSALES (MULTI-LOCAL) ─────────────────────────────
        item {
            val branchesList = remember(storeConfig.storeBranches) {
                val list = storeConfig.storeBranches.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .toMutableList()
                if (!list.contains("Principal")) list.add(0, "Principal")
                list
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.cardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF3B82F6).copy(alpha = 0.15f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = Color(0xFF3B82F6),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Sucursales y Puntos de Venta",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = AppTheme.textPrimary
                                )
                                Text(
                                    text = "${branchesList.size} puntos configurados • Notificaciones filtradas",
                                    fontSize = 11.sp,
                                    color = AppTheme.textSecondary
                                )
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

                    // Chips interactivos de sucursales con conteo de trabajadores
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        branchesList.forEach { branch ->
                            val isPrincipal = branch.equals("Principal", ignoreCase = true)
                            val count = activeWorkers.count {
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
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = if (count > 0) SleekSuccessGreen.copy(alpha = 0.2f) else AppTheme.cardBorder
                                    ) {
                                        Text(
                                            text = "$count",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (count > 0) SleekSuccessGreen else AppTheme.textSecondary,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Chip para añadir rápido
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
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = AppTheme.textSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Nueva",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 4. TÍTULO DE LISTA DE TRABAJADORES ───────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trabajadores Conectados (${activeWorkers.size})",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = AppTheme.textPrimary
                )
                Text(
                    text = if (isLoading) "Sincronizando..." else "En tiempo real",
                    fontSize = 11.sp,
                    color = AppTheme.textSecondary
                )
            }
        }

        // ── 5. LISTA DE TRABAJADORES O ESTADO VACÍO ─────────────────────────
        if (activeWorkers.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AppTheme.cardBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (AppTheme.isDark) Color(0xFF242731) else Color(0xFFF1F5F9),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = AppTheme.textSecondary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Aún no hay trabajadores vinculados",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = AppTheme.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Presiona 'Generar Código' arriba y dale el código a tu empleado o colócalo en el teléfono de caja secundaria.",
                            fontSize = 13.sp,
                            color = AppTheme.textSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            items(activeWorkers, key = { it.id ?: it.userId ?: it.hashCode().toString() }) { worker ->
                SpaciousWorkerCard(
                    worker = worker,
                    onEditName = { workerToEditName = worker },
                    onEditSchedule = { workerToEditSchedule = worker },
                    onEditBranch = { workerToEditBranch = worker },
                    onKick = { workerToKick = worker }
                )
            }
        }
    }

    // ─── DIÁLOGOS MODULARES DE ACCIÓN (EDITAR NOMBRE, HORARIOS, EXPULSIÓN) ───

    // Modal 1: Personalizar Nombre
    if (workerToEditName != null) {
        val worker = workerToEditName!!
        var newNameInput by remember { mutableStateOf(worker.customName) }
        var isSaving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSaving) workerToEditName = null },
            containerColor = AppTheme.cardBackground,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = AppTheme.textPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Personalizar Nombre", fontWeight = FontWeight.Black, fontSize = 17.sp, color = AppTheme.textPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Asigna un alias o nombre fácil de reconocer para este trabajador:",
                        fontSize = 12.sp,
                        color = AppTheme.textSecondary,
                        lineHeight = 16.sp
                    )
                    OutlinedTextField(
                        value = newNameInput,
                        onValueChange = { newNameInput = it },
                        label = { Text("Nombre / Alias del Trabajador") },
                        placeholder = { Text("Ej: Carlos - Mozo Turno Mañana") },
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
                                Toast.makeText(context, "Nombre actualizado correctamente", Toast.LENGTH_SHORT).show()
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
                    enabled = !isSaving && newNameInput.isNotBlank()
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (!isSaving) {
                    TextButton(onClick = { workerToEditName = null }) {
                        Text("Cancelar", color = AppTheme.textSecondary)
                    }
                }
            }
        )
    }

    // Modal 2: Configurar Horario
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

    // Modal 3: Botar / Expulsar Trabajador
    if (workerToKick != null) {
        val worker = workerToKick!!
        var isKicking by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isKicking) workerToKick = null },
            containerColor = AppTheme.cardBackground,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Surface(shape = CircleShape, color = SleekErrorRed.copy(alpha = 0.15f), modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = SleekErrorRed, modifier = Modifier.size(24.dp))
                    }
                }
            },
            title = {
                Text("¿Expulsar Trabajador?", fontWeight = FontWeight.Black, fontSize = 18.sp, color = AppTheme.textPrimary)
            },
            text = {
                Text(
                    text = "¿Confirmas que deseas botar a '${worker.customName}' (${worker.userEmail ?: "Sin correo"})?\n\nEste dispositivo dejará de recibir alertas de ventas de inmediato.",
                    fontSize = 13.sp,
                    color = AppTheme.textSecondary,
                    lineHeight = 18.sp
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
                                Toast.makeText(context, "Trabajador desvinculado con éxito", Toast.LENGTH_SHORT).show()
                                workerToKick = null
                            } else {
                                Toast.makeText(context, err ?: "Error al expulsar trabajador", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekErrorRed, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isKicking
                ) {
                    if (isKicking) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Sí, Botar del Negocio", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (!isKicking) {
                    OutlinedButton(
                        onClick = { workerToKick = null },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancelar", color = AppTheme.textSecondary)
                    }
                }
            }
        )
    }

    // Modal 4: Asignar Sucursal a Trabajador
    if (workerToEditBranch != null) {
        val worker = workerToEditBranch!!
        val branchesList = remember(storeConfig.storeBranches) {
            val list = storeConfig.storeBranches.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
            if (!list.contains("Principal")) list.add(0, "Principal")
            list
        }
        var selectedBranch by remember {
            val initial = worker.branchName.ifBlank { "Principal" }
            mutableStateOf(if (branchesList.contains(initial)) initial else "Principal")
        }
        var isSavingBranch by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSavingBranch) workerToEditBranch = null },
            containerColor = AppTheme.cardBackground,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Store, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Asignar Sucursal / Local", fontWeight = FontWeight.Black, fontSize = 17.sp, color = AppTheme.textPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Selecciona la sucursal o caja a la que pertenece '${worker.customName}':",
                        fontSize = 12.sp,
                        color = AppTheme.textSecondary,
                        lineHeight = 16.sp
                    )

                    Text("Sucursales creadas:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppTheme.textPrimary)

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        branchesList.forEach { branch ->
                            val isSelected = selectedBranch.equals(branch, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedBranch = branch },
                                label = {
                                    Text(
                                        text = branch,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF3B82F6),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val branch = selectedBranch.trim()
                        if (branch.isBlank()) return@Button
                        val id = worker.id ?: return@Button
                        isSavingBranch = true
                        viewModel.updateReceiverBranch(id, branch) { success, err ->
                            isSavingBranch = false
                            if (success) {
                                Toast.makeText(context, "Sucursal asignada con éxito", Toast.LENGTH_SHORT).show()
                                workerToEditBranch = null
                            } else {
                                Toast.makeText(context, err ?: "Error al asignar sucursal", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isSavingBranch && selectedBranch.isNotBlank()
                ) {
                    if (isSavingBranch) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Guardar Asignación", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (!isSavingBranch) {
                    TextButton(onClick = { workerToEditBranch = null }) {
                        Text("Cancelar", color = AppTheme.textSecondary)
                    }
                }
            }
        )
    }

    // Modal 5: Administrar Sucursales de la Tienda
    if (showManageBranchesDialog) {
        ManageBranchesDialog(
            currentBranchesString = if (storeConfig.storeBranches.isNotBlank()) storeConfig.storeBranches else "Principal,Sucursal 2",
            storeReceivers = receivers,
            onDismiss = { showManageBranchesDialog = false },
            onSaveBranches = { newBranches, reassignments ->
                val joined = newBranches.joinToString(",")
                viewModel.updateStoreBranches(joined)
                // Reasignar trabajadores modificados / migrados
                reassignments.forEach { (workerId, newBranch) ->
                    viewModel.updateReceiverBranch(workerId, newBranch) { _, _ -> }
                }
                Toast.makeText(context, "Sucursales guardadas con éxito", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/**
 * Tarjeta espaciosa e individual para cada trabajador en la lista
 */
@Composable
private fun SpaciousWorkerCard(
    worker: SupabaseReceiverDto,
    onEditName: () -> Unit,
    onEditSchedule: () -> Unit,
    onEditBranch: () -> Unit,
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
        shape = RoundedCornerShape(18.dp),
        color = AppTheme.cardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Fila Superior: Avatar + Nombre + Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (AppTheme.isDark) Color(0xFF2E313C) else Color(0xFFE2E8F0),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = worker.customName.firstOrNull()?.uppercase() ?: "T",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = worker.customName,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = AppTheme.textPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = onEditName, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Editar nombre",
                                    tint = AppTheme.textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = worker.userEmail ?: "Dispositivo receptor",
                            fontSize = 12.sp,
                            color = AppTheme.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (worker.branchName.isNotBlank()) Color(0xFF3B82F6).copy(alpha = 0.15f) else AppTheme.pillBackground
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Store,
                                    contentDescription = null,
                                    tint = if (worker.branchName.isNotBlank()) Color(0xFF3B82F6) else AppTheme.textSecondary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (worker.branchName.isNotBlank()) "Sucursal: ${worker.branchName}" else "Sin sucursal asignada",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (worker.branchName.isNotBlank()) Color(0xFF3B82F6) else AppTheme.textSecondary
                                )
                            }
                        }
                    }
                }

                // Badge de estado
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isRevoked -> SleekErrorRed.copy(alpha = 0.15f)
                        isInSchedule -> SleekSuccessGreen.copy(alpha = 0.15f)
                        else -> Color(0xFFD97706).copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = when {
                            isRevoked -> "REVOCADO"
                            isInSchedule -> "EN TURNO"
                            else -> "FUERA TURNO"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = when {
                            isRevoked -> SleekErrorRed
                            isInSchedule -> SleekSuccessGreen
                            else -> Color(0xFFD97706)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = AppTheme.cardBorder, thickness = 0.8.dp)

            // Fila Media: Información de Horario y Sucursal
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AppTheme.pillBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = AppTheme.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Horario de Notificación:", fontSize = 10.sp, color = AppTheme.textSecondary)
                            Text(
                                text = if (worker.scheduleEnabled) {
                                    "${worker.scheduleStartTime} - ${worker.scheduleEndTime} (${if (worker.scheduleDays == "ALL") "Todos los días" else worker.scheduleDays})"
                                } else {
                                    "24/7 (Sin restricción)"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.textPrimary
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = onEditBranch,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (AppTheme.isDark) Color(0xFF2E313C) else Color(0xFFE2E8F0),
                                contentColor = AppTheme.textPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sucursal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onEditSchedule,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (AppTheme.isDark) Color(0xFF2E313C) else Color(0xFFE2E8F0),
                                contentColor = AppTheme.textPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Horario", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Fila Inferior: Acciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ID: ${worker.id?.take(8) ?: "Local"}",
                    fontSize = 10.sp,
                    color = AppTheme.textSecondary.copy(alpha = 0.6f)
                )

                TextButton(
                    onClick = onKick,
                    colors = ButtonDefaults.textButtonColors(contentColor = SleekErrorRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Botar / Desvincular", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Píldora de estadística rápida
 */
@Composable
private fun StatPill(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AppTheme.cardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppTheme.textSecondary,
                    maxLines = 1
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}
