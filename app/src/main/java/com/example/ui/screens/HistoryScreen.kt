package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import android.widget.Toast
import com.example.data.model.DeviceRole
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ModernNeonLime
import com.example.ui.theme.ModernOnNeonLime
import com.example.ui.theme.ModernMatteBlack
import com.example.ui.theme.ModernBrandBlue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.YapeTransaction
import com.example.ui.components.EditProductNoteDialog
import com.example.ui.components.ExportReportDialog
import com.example.ui.components.MetricSummaryCard
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.ModernLimeVolt
import com.example.ui.theme.SleekBorderLight
import com.example.ui.theme.SleekErrorRed
import com.example.ui.theme.SleekMidnightNavy
import com.example.ui.theme.SleekOnPrimaryContainer
import com.example.ui.theme.SleekOnSurfaceLight
import com.example.ui.theme.SleekOnSurfaceVariantLight
import com.example.ui.theme.SleekPrimaryBlue
import com.example.ui.theme.SleekPrimaryContainer
import com.example.ui.theme.SleekSecondaryContainer
import com.example.ui.theme.SleekSubtleTextLight
import com.example.ui.theme.SleekSuccessGreen
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedDate by viewModel.selectedHistoryDate.collectAsStateWithLifecycle()
    val transactions by viewModel.historyTransactions.collectAsStateWithLifecycle()
    val totalAmount by viewModel.historyTotal.collectAsStateWithLifecycle()
    val storeConfig by viewModel.storeConfig.collectAsStateWithLifecycle()
    val selectedBranchFilter by viewModel.selectedBranchFilter.collectAsStateWithLifecycle()
    val storeReceivers by viewModel.storeReceivers.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var sellerFilterMode by remember { mutableStateOf("ALL") } // "ALL" (Todos los cobros) | "MINE" (Mis cobros)
    var selectedWorkerFilter by remember { mutableStateOf("ALL") } // "ALL" | worker key | "UNCLAIMED"
    var showExportDialog by remember { mutableStateOf(false) }
    var transactionToEditNote by remember { mutableStateOf<YapeTransaction?>(null) }

    LaunchedEffect(storeConfig.deviceRole) {
        if (storeConfig.deviceRole == DeviceRole.SENDER) {
            viewModel.loadStoreReceivers(silent = true)
        }
    }

    val voiceSettings by viewModel.voiceSettings.collectAsStateWithLifecycle()
    val isNotificationPermissionGranted by viewModel.isNotificationListenerGranted.collectAsStateWithLifecycle()
    val isBatteryOptimizationIgnored by viewModel.isBatteryOptimizationIgnored.collectAsStateWithLifecycle()
    val latestTransaction by viewModel.latestTransaction.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val dismissedLastPaymentId by viewModel.dismissedLastPaymentId.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val isSeller = storeConfig.deviceRole == DeviceRole.RECEIVER
    val isOwner = storeConfig.deviceRole == DeviceRole.SENDER

    val branchList = remember(storeConfig.storeBranches) {
        storeConfig.storeBranches.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    val hasMultipleBranches = branchList.size > 1

    // La card se muestra solo si no es modo dueño, hay pago Y su id no coincide con el descartado
    val showLastPayment = storeConfig.deviceRole != DeviceRole.SENDER &&
            latestTransaction != null &&
            latestTransaction!!.id != dismissedLastPaymentId

    // Conteo y total de ventas confirmadas por este vendedor
    val myTransactionsCount = remember(transactions, currentUser) {
        transactions.count { it.isConfirmedBy(currentUser.id, currentUser.displayName, currentUser.email) }
    }
    val myTransactionsTotal = remember(transactions, currentUser) {
        transactions.filter {
            it.isConfirmedBy(currentUser.id, currentUser.displayName, currentUser.email) && it.isStoreTransaction
        }.sumOf { it.amount }
    }

    // Lista de trabajadores disponibles para filtro del dueño
    val availableWorkers = remember(storeReceivers, transactions) {
        val list = mutableListOf<Pair<String, String>>()
        list.add("ALL" to "Todos")

        val fromReceivers = storeReceivers.mapNotNull { r ->
            val name = r.customName?.trim()
            if (!name.isNullOrBlank()) {
                val key = if (!r.userId.isNullOrBlank()) r.userId!! else name
                key to name
            } else null
        }

        val fromTx = transactions.mapNotNull { tx ->
            if (tx.isConfirmed && tx.claimedByName.isNotBlank()) {
                val key = tx.claimedBy.ifBlank { tx.claimedByName }
                key to tx.claimedByName
            } else null
        }

        val distinct = (fromReceivers + fromTx).distinctBy { it.second.lowercase() }
        list.addAll(distinct)

        val hasUnclaimed = transactions.any { !it.isConfirmed && it.isStoreTransaction }
        if (hasUnclaimed) {
            list.add("UNCLAIMED" to "⏳ Sin confirmar")
        }

        list
    }

    // Lista de sucursales disponibles para filtro del dueño
    val ownerBranchList = remember(storeConfig.storeBranches, transactions) {
        val list = mutableListOf("ALL")
        val fromConfig = storeConfig.storeBranches.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val fromTx = transactions.mapNotNull { it.branchName.trim().ifBlank { null } }
        list.addAll((fromConfig + fromTx).distinct())
        list.distinct()
    }

    val filteredTransactions = remember(
        transactions,
        searchQuery,
        sellerFilterMode,
        selectedWorkerFilter,
        selectedBranchFilter,
        currentUser,
        storeConfig.deviceRole
    ) {
        transactions.filter { tx ->
            // 1. Filtro Vendedor: "ALL" o "MINE"
            val matchesSeller = if (isSeller && sellerFilterMode == "MINE") {
                tx.isConfirmedBy(currentUser.id, currentUser.displayName, currentUser.email)
            } else {
                true
            }

            // 2. Filtro Dueño por Trabajador
            val matchesWorker = if (isOwner && selectedWorkerFilter != "ALL") {
                if (selectedWorkerFilter == "UNCLAIMED") {
                    !tx.isConfirmed
                } else {
                    tx.claimedBy.equals(selectedWorkerFilter, ignoreCase = true) ||
                    tx.claimedByName.equals(selectedWorkerFilter, ignoreCase = true)
                }
            } else {
                true
            }

            // 3. Filtro Dueño por Sucursal
            val matchesBranch = if (isOwner && selectedBranchFilter != "ALL") {
                tx.branchName.equals(selectedBranchFilter, ignoreCase = true)
            } else {
                true
            }

            // 4. Búsqueda por texto (Emisor, monto, nota/producto, código de seguridad, trabajador, sucursal)
            val matchesQuery = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase()
                tx.senderName.lowercase().contains(q) ||
                tx.formattedAmount.lowercase().contains(q) ||
                String.format(Locale.US, "%.2f", tx.amount).contains(q) ||
                tx.note.lowercase().contains(q) ||
                (tx.securityCode?.lowercase()?.contains(q) == true) ||
                tx.claimedByName.lowercase().contains(q) ||
                tx.branchName.lowercase().contains(q)
            }

            matchesSeller && matchesWorker && matchesBranch && matchesQuery
        }
    }

    val filteredTotalAmount = remember(filteredTransactions) {
        filteredTransactions.filter { it.isStoreTransaction }.sumOf { it.amount }
    }

    val isAnyFilterActive = remember(
        searchQuery,
        sellerFilterMode,
        selectedWorkerFilter,
        selectedBranchFilter,
        storeConfig.deviceRole
    ) {
        searchQuery.isNotBlank() ||
        (isSeller && sellerFilterMode != "ALL") ||
        (isOwner && (selectedWorkerFilter != "ALL" || selectedBranchFilter != "ALL"))
    }

    val isToday = remember(selectedDate) {
        val today = Calendar.getInstance()
        selectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                selectedDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    val formattedDateHeader = remember(selectedDate) {
        val sdf = SimpleDateFormat("EEEE, dd 'de' MMMM yyyy", Locale("es", "PE"))
        sdf.format(selectedDate.time).replaceFirstChar { it.uppercase() }
    }

    val dateShortStr = remember(selectedDate) {
        SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE")).format(selectedDate.time)
    }

    val maxAmount = remember(transactions) {
        transactions.maxOfOrNull { it.amount } ?: 0.0
    }

    val avgAmount = remember(transactions, totalAmount) {
        if (transactions.isNotEmpty()) totalAmount / transactions.size else 0.0
    }

    if (showExportDialog) {
        ExportReportDialog(
            storeName = storeConfig.storeName,
            dateString = dateShortStr,
            transactions = if (isAnyFilterActive && !isSeller) filteredTransactions else (if (searchQuery.isNotBlank()) filteredTransactions else transactions),
            totalAmount = if (isAnyFilterActive && !isSeller) filteredTotalAmount else (if (searchQuery.isNotBlank()) filteredTotalAmount else totalAmount),
            isSeller = isSeller,
            currentSellerName = currentUser.displayName.ifBlank { currentUser.email.substringBefore("@") },
            currentSellerId = currentUser.id,
            currentSellerEmail = currentUser.email,
            branchName = if (isSeller) storeConfig.workerBranchName else (if (selectedBranchFilter != "ALL") selectedBranchFilter else ""),
            initialSellerScope = if (sellerFilterMode == "MINE") "MINE" else "ALL",
            onDismiss = { showExportDialog = false }
        )
    }

    val currentTxToEdit = transactionToEditNote
    if (currentTxToEdit != null) {
        EditProductNoteDialog(
            transaction = currentTxToEdit,
            onDismiss = { transactionToEditNote = null },
            onSave = { newNote ->
                if (!currentTxToEdit.isConfirmed) {
                    viewModel.confirmTransaction(currentTxToEdit, newNote) { success, err ->
                        if (success) {
                            Toast.makeText(context, "Detalle guardado y cobro confirmado", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, err ?: "Error al guardar detalle", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    viewModel.updateTransactionNote(currentTxToEdit, newNote) { success, err ->
                        if (success) {
                            Toast.makeText(context, "Detalle de producto guardado", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, err ?: "Error al guardar detalle", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                transactionToEditNote = null
            }
        )
    }

    // DatePicker Dialog
    val datePickerDialog = remember(selectedDate) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth)
                viewModel.selectHistoryDate(cal)
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Sleek Top Header con Sincronización y Exportación
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "TERMINAL & AUDITORÍA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = if (AppTheme.isDark) Color(0xFF94A3B8) else SleekSubtleTextLight
                    )
                    Text(
                        text = "Reportes de Caja",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        color = AppTheme.textPrimary
                    )
                    Text(
                        text = storeConfig.storeName.ifBlank { "Mi Negocio" },
                        fontSize = 12.sp,
                        color = AppTheme.textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón Sincronizar
                    Surface(
                        shape = CircleShape,
                        color = AppTheme.cardBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder),
                        modifier = Modifier
                            .size(42.dp)
                            .clickable {
                                viewModel.syncNow()
                                Toast.makeText(context, "Sincronizando caja...", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = ModernNeonLime
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sincronizar",
                                    tint = AppTheme.textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Botón Exportar Reporte
                    if (transactions.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(ModernNeonLime, ModernBrandBlue)
                                    )
                                )
                                .clickable { showExportDialog = true }
                                .testTag("export_report_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = "Exportar reporte",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // ALERTA VISIBLE SI FALTA ALGÚN PERMISO CRÍTICO (Solo para Modo Caja / Emisor)
        if (storeConfig.deviceRole != DeviceRole.RECEIVER && (!isNotificationPermissionGranted || !isBatteryOptimizationIgnored)) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFFEF2F2),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF87171)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = SleekErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "¡ATENCIÓN! CAJA EN SEGUNDO PLANO EN RIESGO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                color = Color(0xFF991B1B)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (!isNotificationPermissionGranted) {
                                "La app no puede escuchar los Yapes porque falta el permiso de notificaciones."
                            } else {
                                "Android puede cerrar la caja al apagar la pantalla por ahorro de batería."
                            },
                            fontSize = 12.sp,
                            color = Color(0xFF7F1D1D)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!isNotificationPermissionGranted) {
                                Button(
                                    onClick = { com.example.util.PermissionHelper.openNotificationListenerSettings(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SleekErrorRed),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("Activar Escucha", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (!isBatteryOptimizationIgnored) {
                                Button(
                                    onClick = { com.example.util.PermissionHelper.requestIgnoreBatteryOptimization(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("Quitar Límite Batería", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. MONITOR DEL SISTEMA EN VIVO (Solo para Modo Caja / Emisor, en modo Receptor no se requiere)
        if (storeConfig.deviceRole != DeviceRole.RECEIVER) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AppTheme.cardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Estado Permiso Notificaciones (Toca para activar si está pendiente)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                if (!isNotificationPermissionGranted) {
                                    com.example.util.PermissionHelper.openNotificationListenerSettings(context)
                                } else {
                                    Toast.makeText(context, "Escucha de pagos Yape activa y funcionando", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isNotificationPermissionGranted) (if (AppTheme.isDark) ModernNeonLime else SleekSuccessGreen) else SleekErrorRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isNotificationPermissionGranted) "Escucha Yape Activa" else "Permiso Pendiente ⚠️",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isNotificationPermissionGranted) AppTheme.textPrimary else SleekErrorRed
                            )
                        }

                        // Estado Optimización de Batería (Toca para excluir si está en riesgo)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                if (!isBatteryOptimizationIgnored) {
                                    com.example.util.PermissionHelper.requestIgnoreBatteryOptimization(context)
                                } else {
                                    Toast.makeText(context, "Batería sin restricciones (No se cerrará en 2do plano)", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isBatteryOptimizationIgnored) (if (AppTheme.isDark) ModernNeonLime else SleekSuccessGreen) else Color(0xFFF59E0B))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBatteryOptimizationIgnored) "2do Plano OK" else "Batería Limita ⚠️",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.textPrimary
                            )
                        }

                        // Modo Dispositivo
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (AppTheme.isDark) Color(0xFF262934) else AppTheme.pillBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.pillBorder)
                        ) {
                            Text(
                                text = when (storeConfig.deviceRole) {
                                    DeviceRole.LOCAL_SPEAKER -> "Solo Caja"
                                    DeviceRole.SENDER -> "Caja + Nube"
                                    DeviceRole.RECEIVER -> "Receptor"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (AppTheme.isDark) ModernNeonLime else AppTheme.textPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }



        // Date Navigator Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppTheme.cardBackground
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AppTheme.pillBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.pillBorder),
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { viewModel.changeHistoryDay(-1) }
                                .testTag("prev_day_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Día anterior",
                                    tint = AppTheme.textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(AppTheme.pillBackground)
                                .border(1.dp, AppTheme.pillBorder, RoundedCornerShape(14.dp))
                                .clickable { datePickerDialog.show() }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Seleccionar fecha",
                                tint = if (AppTheme.isDark) ModernNeonLime else Color(0xFF131412),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isToday) "HOY" else "FECHA SELECCIONADA",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = if (AppTheme.isDark) {
                                        if (isToday) ModernNeonLime else AppTheme.textSecondary
                                    } else Color(0xFF131412)
                                )
                                Text(
                                    text = formattedDateHeader,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (AppTheme.isDark) AppTheme.textPrimary else Color(0xFF131412)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = AppTheme.pillBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.pillBorder),
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { viewModel.changeHistoryDay(1) }
                                .testTag("next_day_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Día siguiente",
                                    tint = AppTheme.textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (!isToday) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(ModernNeonLime, ModernBrandBlue)
                                        )
                                    )
                                    .clickable { viewModel.resetHistoryToToday() }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "← Volver a Hoy",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }


        // Daily Summary Metrics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricSummaryCard(
                    title = "Total Recaudado",
                    value = String.format(Locale("es", "PE"), "S/ %.2f", totalAmount),
                    icon = Icons.Default.Paid,
                    iconColor = if (AppTheme.isDark) ModernNeonLime else SleekPrimaryBlue,
                    iconBgColor = if (AppTheme.isDark) Color(0xFF232B1A) else SleekPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )

                MetricSummaryCard(
                    title = "Transacciones",
                    value = "${transactions.size} pagos",
                    icon = Icons.Default.ReceiptLong,
                    iconColor = if (AppTheme.isDark) Color(0xFF4ADE80) else SleekSuccessGreen,
                    iconBgColor = if (AppTheme.isDark) Color(0xFF142B1F) else Color(0xFFDCFCE7),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricSummaryCard(
                    title = "Promedio / Pago",
                    value = String.format(Locale("es", "PE"), "S/ %.2f", avgAmount),
                    icon = Icons.Default.TrendingUp,
                    iconColor = if (AppTheme.isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                    iconBgColor = if (AppTheme.isDark) Color(0xFF132838) else SleekSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )

                MetricSummaryCard(
                    title = "Mayor Pago",
                    value = String.format(Locale("es", "PE"), "S/ %.2f", maxAmount),
                    icon = Icons.Default.History,
                    iconColor = if (AppTheme.isDark) Color(0xFFF87171) else SleekErrorRed,
                    iconBgColor = if (AppTheme.isDark) Color(0xFF38181A) else Color(0xFFFFDAD6),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // CARD: ÚLTIMO PAGO RECIBIDO — visible solo si hay pago y no fue descartado
        if (showLastPayment) {
            item {
                val tx = latestTransaction!!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = AppTheme.cardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, ModernNeonLime),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (AppTheme.isDark) 0.dp else 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                    color = ModernNeonLime,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Sensors,
                                            contentDescription = null,
                                            tint = Color(0xFF131412),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ÚLTIMO PAGO RECIBIDO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp,
                                    color = AppTheme.textSecondary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Hora del pago
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = ModernNeonLime
                                ) {
                                    Text(
                                        text = tx.formattedTime,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF131412),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                // Botón cerrar ✕
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (AppTheme.isDark) Color(0xFF2E313C) else Color(0xFFE2E8F0))
                                        .border(1.dp, AppTheme.cardBorder, CircleShape)
                                        .clickable { viewModel.dismissLastPayment() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Cerrar",
                                        tint = AppTheme.textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Monto grande
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = tx.formattedAmount,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp,
                                color = AppTheme.textPrimary
                            )

                            if (tx.securityCode != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = AppTheme.pillBackground,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.pillBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Código: ",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = AppTheme.textSecondary
                                        )
                                        Text(
                                            text = tx.securityCode!!,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = AppTheme.textPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Nombre del emisor
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = tx.senderName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.textPrimary
                            )
                            Text(
                                text = if (tx.hasAsterisk) "Confirmado por Yape • Cobro registrado" else "Cobro registrado",
                                fontSize = 11.sp,
                                color = AppTheme.textSecondary
                            )
                        }

                        // ── PREGUNTA AL TRABAJADOR / ESTADO DEL COBRO ─────────────────────────
                        val isUnknown = !tx.isStoreTransaction
                        val isConfirmed = tx.isConfirmed

                        Spacer(modifier = Modifier.height(14.dp))

                        when {
                            isUnknown -> {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = SleekErrorRed.copy(alpha = 0.1f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SleekErrorRed.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = SleekErrorRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "⚠️ Yape Desconocido",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SleekErrorRed
                                                )
                                                Text(
                                                    text = "Excluido del total de ventas",
                                                    fontSize = 10.sp,
                                                    color = AppTheme.textSecondary
                                                )
                                            }
                                        }

                                        TextButton(
                                            onClick = {
                                                viewModel.markTransactionAsStore(tx)
                                                Toast.makeText(context, "Pago reintegrado", Toast.LENGTH_SHORT).show()
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Reintegrar",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SleekPrimaryBlue
                                            )
                                        }
                                    }
                                }
                            }
                            isConfirmed -> {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = SleekSuccessGreen.copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SleekSuccessGreen.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = SleekSuccessGreen,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = "✓ Cobro confirmado",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SleekSuccessGreen
                                                    )
                                                    Text(
                                                        text = "Sumado a tus ventas",
                                                        fontSize = 10.sp,
                                                        color = AppTheme.textSecondary
                                                    )
                                                }
                                            }

                                            val canUnmarkLast = tx.isClaimedByUser(
                                                userId = currentUser.id,
                                                userName = currentUser.displayName,
                                                userEmail = currentUser.email
                                            )
                                            if (canUnmarkLast) {
                                                TextButton(
                                                    onClick = {
                                                        viewModel.unconfirmTransaction(tx) { _, msg ->
                                                            Toast.makeText(context, msg ?: "Pago desmarcado", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "Desmarcar",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = AppTheme.textSecondary
                                                    )
                                                }
                                            }
                                        }

                                        // Etiquetas de atribución en la nube: Quién confirmó y Sucursal
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val whoConfirmed = tx.claimedByName.ifBlank { "Receptor" }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (AppTheme.isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else SleekSuccessGreen.copy(alpha = 0.18f),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (AppTheme.isDark) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFF16A34A).copy(alpha = 0.2f)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = if (AppTheme.isDark) Color(0xFF6EE7B7) else SleekSuccessGreen,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = whoConfirmed,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (AppTheme.isDark) Color(0xFF6EE7B7) else Color(0xFF065F46)
                                                    )
                                                }
                                            }

                                            // Sucursal: Visible con alto contraste en tema negro y claro
                                            val branchNameToDisplay = tx.branchName.ifBlank { storeConfig.workerBranchName }
                                            if (branchNameToDisplay.isNotBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (AppTheme.isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF),
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        if (AppTheme.isDark) Color(0xFF60A5FA).copy(alpha = 0.45f) else Color(0xFF3B82F6).copy(alpha = 0.25f)
                                                    )
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Storefront,
                                                            contentDescription = null,
                                                            tint = if (AppTheme.isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Text(
                                                            text = branchNameToDisplay,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = if (AppTheme.isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Detalle del producto vendido ingresado en la nube
                                        if (tx.note.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (AppTheme.isDark) Color(0xFF1E2430) else Color(0xFFF1F5F9),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (AppTheme.isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ShoppingCart,
                                                        contentDescription = null,
                                                        tint = AppTheme.textSecondary,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = "Producto: ${tx.note}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = AppTheme.textPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                var productNote by remember(tx.id) { mutableStateOf(tx.note) }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (AppTheme.isDark) Color(0xFF1E2430) else Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (AppTheme.isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFFF59E0B).copy(alpha = 0.18f),
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Help,
                                                            contentDescription = null,
                                                            tint = Color(0xFFF59E0B),
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "¿Has realizado este cobro?",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AppTheme.textPrimary
                                                )
                                            }

                                            // Etiqueta de sucursal del receptor: Visible con alto contraste en tema negro y claro
                                            val pendingBranch = storeConfig.workerBranchName.ifBlank { tx.branchName }
                                            if (pendingBranch.isNotBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (AppTheme.isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF),
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        if (AppTheme.isDark) Color(0xFF60A5FA).copy(alpha = 0.45f) else Color(0xFF3B82F6).copy(alpha = 0.25f)
                                                    )
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Storefront,
                                                            contentDescription = null,
                                                            tint = if (AppTheme.isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                            text = pendingBranch,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (AppTheme.isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Entrada opcional de producto vendido
                                        OutlinedTextField(
                                            value = productNote,
                                            onValueChange = { productNote = it },
                                            placeholder = {
                                                Text(
                                                    text = "¿Qué producto vendiste? (opcional)",
                                                    fontSize = 12.sp,
                                                    color = AppTheme.textSecondary
                                                )
                                            },
                                            singleLine = true,
                                            maxLines = 1,
                                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = AppTheme.textPrimary),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = if (AppTheme.isDark) Color(0xFF151922) else Color.White,
                                                unfocusedContainerColor = if (AppTheme.isDark) Color(0xFF151922) else Color.White,
                                                focusedBorderColor = SleekSuccessGreen,
                                                unfocusedBorderColor = if (AppTheme.isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                                            ),
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = null,
                                                    tint = AppTheme.textSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Botón SÍ -> Confirmado en la nube con nota y sucursal
                                            Button(
                                                onClick = {
                                                    viewModel.confirmTransaction(tx, productNote) { success, msg ->
                                                        Toast.makeText(
                                                            context,
                                                            msg ?: if (success) "✓ Cobro confirmado" else "Error al confirmar",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(40.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = SleekSuccessGreen,
                                                    contentColor = Color.White
                                                ),
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Sí",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            // Botón NO -> Desconocido
                                            Button(
                                                onClick = {
                                                    viewModel.markTransactionAsNonStore(tx, "Desconocido / No realizado por trabajador")
                                                    Toast.makeText(context, "⚠️ Pago marcado como desconocido", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(40.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (AppTheme.isDark) Color(0xFF2E1A1A) else Color(0xFFFFEBEE),
                                                    contentColor = SleekErrorRed
                                                ),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, SleekErrorRed.copy(alpha = 0.5f)),
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = null,
                                                    tint = SleekErrorRed,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "No",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ─── BÚSQUEDA Y FILTROS DE REPORTES (MIS COBROS / SUCURSALES / TRABAJADORES) ───
        if (transactions.isNotEmpty() || isAnyFilterActive) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Buscador Inteligente
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = when {
                                    isSeller -> "Buscar cliente, monto, producto, código..."
                                    isOwner -> "Buscar cliente, monto, producto, trabajador..."
                                    else -> "Buscar por cliente o monto..."
                                },
                                color = AppTheme.textSecondary,
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = AppTheme.textSecondary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Limpiar búsqueda",
                                        tint = AppTheme.textSecondary
                                    )
                                }
                            }
                        },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTheme.textPrimary,
                            unfocusedTextColor = AppTheme.textPrimary,
                            focusedBorderColor = ModernNeonLime,
                            unfocusedBorderColor = AppTheme.cardBorder,
                            focusedContainerColor = AppTheme.cardBackground,
                            unfocusedContainerColor = AppTheme.cardBackground,
                            cursorColor = ModernNeonLime
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_search_input"),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    // 2. Filtros para Vendedor (RECEIVER): "Todos los cobros" vs "Mis cobros"
                    if (isSeller) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Chip: Todos los cobros
                            val isAllSelected = sellerFilterMode == "ALL"
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isAllSelected) (if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack) else AppTheme.cardBackground,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isAllSelected) (if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack) else AppTheme.cardBorder
                                ),
                                modifier = Modifier
                                    .clickable { sellerFilterMode = "ALL" }
                                    .testTag("filter_seller_all")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = if (isAllSelected) (if (AppTheme.isDark) ModernOnNeonLime else Color.White) else AppTheme.textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Todos los cobros (${transactions.size})",
                                        color = if (isAllSelected) (if (AppTheme.isDark) ModernOnNeonLime else Color.White) else AppTheme.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }

                            // Chip: Mis cobros
                            val isMineSelected = sellerFilterMode == "MINE"
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isMineSelected) (if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack) else AppTheme.cardBackground,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isMineSelected) (if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack) else AppTheme.cardBorder
                                ),
                                modifier = Modifier
                                    .clickable { sellerFilterMode = "MINE" }
                                    .testTag("filter_seller_mine")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isMineSelected) (if (AppTheme.isDark) ModernOnNeonLime else Color.White) else AppTheme.textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Mis cobros ($myTransactionsCount)",
                                        color = if (isMineSelected) (if (AppTheme.isDark) ModernOnNeonLime else Color.White) else AppTheme.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isMineSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // 3. Filtros para Dueño (SENDER): Por Sucursal y Por Trabajador
                    if (isOwner) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Fila A: Filtro por Sucursal
                            if (ownerBranchList.size > 1) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Storefront,
                                            contentDescription = null,
                                            tint = if (AppTheme.isDark) ModernNeonLime else SleekPrimaryBlue,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "SUCURSAL",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AppTheme.textSecondary,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(ownerBranchList) { branch ->
                                            val isSelected = selectedBranchFilter.equals(branch, ignoreCase = true)
                                            val label = if (branch == "ALL") "Todas" else branch
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (isSelected) (if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack) else AppTheme.cardBackground,
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (isSelected) (if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack) else AppTheme.cardBorder
                                                ),
                                                modifier = Modifier.clickable {
                                                    viewModel.setBranchFilter(branch)
                                                }
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (isSelected) (if (AppTheme.isDark) ModernOnNeonLime else Color.White) else AppTheme.textPrimary,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Fila B: Filtro por Trabajador
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (AppTheme.isDark) ModernNeonLime else SleekPrimaryBlue,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "TRABAJADOR",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AppTheme.textSecondary,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(availableWorkers) { (key, label) ->
                                        val isSelected = selectedWorkerFilter.equals(key, ignoreCase = true)
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isSelected) (if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack) else AppTheme.cardBackground,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isSelected) (if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack) else AppTheme.cardBorder
                                            ),
                                            modifier = Modifier.clickable {
                                                selectedWorkerFilter = key
                                            }
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isSelected) (if (AppTheme.isDark) ModernOnNeonLime else Color.White) else AppTheme.textPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Barra de Resumen de Resultados Filtrados y Botón Limpiar
                    if (isAnyFilterActive) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (AppTheme.isDark) Color(0xFF1E2430) else Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (AppTheme.isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(ModernNeonLime)
                                    )
                                    Text(
                                        text = "Mostrando ${filteredTransactions.size} de ${transactions.size} pagos • S/ ${String.format(Locale("es", "PE"), "%.2f", filteredTotalAmount)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppTheme.textPrimary
                                    )
                                }

                                Text(
                                    text = "Limpiar filtros",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekErrorRed,
                                    modifier = Modifier
                                        .clickable {
                                            searchQuery = ""
                                            sellerFilterMode = "ALL"
                                            selectedWorkerFilter = "ALL"
                                            viewModel.setBranchFilter("ALL")
                                        }
                                        .padding(start = 6.dp, top = 2.dp, bottom = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Lista de Pagos
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.cardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Detalle de Pagos (${filteredTransactions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.textPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (filteredTransactions.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(AppTheme.pillBackground)
                                    .border(1.dp, AppTheme.pillBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = AppTheme.textSecondary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = if (isAnyFilterActive) "No se encontraron pagos coincidentes" else "Sin transacciones para este día",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.textPrimary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = if (isAnyFilterActive) "Prueba cambiando el filtro de cobros o término de búsqueda." else "Selecciona otra fecha con el calendario superior.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.textSecondary
                            )

                            if (isAnyFilterActive) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        searchQuery = ""
                                        sellerFilterMode = "ALL"
                                        selectedWorkerFilter = "ALL"
                                        viewModel.setBranchFilter("ALL")
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (AppTheme.isDark) Color(0xFF2E313C) else Color(0xFFE2E8F0),
                                        contentColor = AppTheme.textPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Restablecer filtros", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            filteredTransactions.forEach { tx ->
                                val canUnmarkTx = !isOwner && tx.isClaimedByUser(
                                    userId = currentUser.id,
                                    userName = currentUser.displayName,
                                    userEmail = currentUser.email
                                )
                                val canEditTxNote = !isOwner && (!tx.isConfirmed || canUnmarkTx)

                                TransactionItemCard(
                                    transaction = tx,
                                    canUnconfirm = canUnmarkTx,
                                    canEditNote = canEditTxNote,
                                    onEditNote = if (isOwner) null else {
                                        {
                                            if (canEditTxNote) {
                                                transactionToEditNote = tx
                                            } else {
                                                val author = tx.claimedByName.ifBlank { "el trabajador que lo confirmó" }
                                                Toast.makeText(
                                                    context,
                                                    "Solo $author puede modificar el detalle de este cobro",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    },
                                    onToggleStoreStatus = if (isOwner) null else {
                                        { isStore, reason ->
                                            viewModel.toggleStoreTransactionStatus(tx, reason)
                                        }
                                    },
                                    onConfirmPayment = if (isOwner) null else {
                                        {
                                            viewModel.confirmTransaction(tx) { success, err ->
                                                if (success) {
                                                    Toast.makeText(context, "Pago confirmado", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, err ?: "No se pudo confirmar el pago", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    },
                                    onUnconfirmPayment = if (isOwner) null else {
                                        {
                                            viewModel.unconfirmTransaction(tx) { success, err ->
                                                if (success) {
                                                    Toast.makeText(context, "Pago desmarcado", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, err ?: "Error al desmarcar", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

