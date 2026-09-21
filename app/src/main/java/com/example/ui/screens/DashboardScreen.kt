package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.data.model.DeviceRole
import com.example.ui.components.OwnerWorkersPanelDialog

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ModernBrandBlue
import com.example.ui.theme.ModernBrandBlueSecondary
import com.example.ui.theme.ModernDeepCharcoal
import com.example.ui.theme.ModernHeroSubtext
import com.example.ui.theme.ModernMatteBlack
import com.example.ui.theme.ModernNeonLime
import com.example.ui.theme.ModernOnNeonLime
import com.example.ui.viewmodel.MainViewModel
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToWorkers: (() -> Unit)? = null
) {
    val todayTransactions by viewModel.todayTransactions.collectAsStateWithLifecycle()
    val weeklyTransactions by viewModel.weeklyTransactions.collectAsStateWithLifecycle()
    val latestTransaction by viewModel.latestTransaction.collectAsStateWithLifecycle()
    val storeConfig by viewModel.storeConfig.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val selectedBranchFilter by viewModel.selectedBranchFilter.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val receivers by viewModel.storeReceivers.collectAsStateWithLifecycle()
    var showWorkersPanel by remember { mutableStateOf(false) }

    LaunchedEffect(storeConfig.storeCode) {
        if (storeConfig.deviceRole == DeviceRole.SENDER) {
            viewModel.loadStoreReceivers()
        }
    }

    var dateFilterMenuExpanded by remember { mutableStateOf(false) }
    var selectedDateRangeLabel by remember { mutableStateOf("Hoy") }
    var heroMenuExpanded by remember { mutableStateOf(false) }
    var ticketMenuExpanded by remember { mutableStateOf(false) }
    var verifiedMenuExpanded by remember { mutableStateOf(false) }
    var scheduleMenuExpanded by remember { mutableStateOf(false) }
    var selectedScheduleRange by remember { mutableStateOf("Jornada") }


    // Transacciones filtradas según selección de rango ("Hoy", "Ayer", "Esta Semana")
    val activeTransactions = remember(selectedDateRangeLabel, todayTransactions, weeklyTransactions) {
        val cal = Calendar.getInstance()
        when (selectedDateRangeLabel) {
            "Ayer" -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                val yesterdayYear = cal.get(Calendar.YEAR)
                val yesterdayDay = cal.get(Calendar.DAY_OF_YEAR)
                weeklyTransactions.filter { tx ->
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                    txCal.get(Calendar.YEAR) == yesterdayYear && txCal.get(Calendar.DAY_OF_YEAR) == yesterdayDay
                }
            }
            "Esta Semana" -> weeklyTransactions
            else -> todayTransactions
        }
    }

    val activeStoreTransactions = remember(activeTransactions) {
        activeTransactions.filter { it.isStoreTransaction }
    }

    val displayTotal = remember(activeStoreTransactions) {
        activeStoreTransactions.sumOf { it.amount }
    }

    val displayCount = remember(activeStoreTransactions) {
        activeStoreTransactions.size
    }

    val averageTicket = if (displayCount > 0) displayTotal / displayCount else 0.0

    // Métricas avanzadas adicionales
    val maxTicket = remember(activeStoreTransactions) {
        activeStoreTransactions.maxOfOrNull { it.amount } ?: 0.0
    }
    val minTicket = remember(activeStoreTransactions) {
        activeStoreTransactions.minOfOrNull { it.amount } ?: 0.0
    }

    val todayFullDate = remember(selectedDateRangeLabel) {
        val cal = Calendar.getInstance()
        when (selectedDateRangeLabel) {
            "Ayer" -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                SimpleDateFormat("d MMMM, yyyy", Locale("es", "PE")).format(cal.time) + " (Ayer)"
            }
            "Esta Semana" -> "Últimos 7 días"
            else -> SimpleDateFormat("d MMMM, yyyy", Locale("es", "PE")).format(Date())
        }
    }

    // Cálculo dinámico de distribución por franjas horarias
    val hourlyCounts = remember(activeStoreTransactions) {
        val slots = listOf("8am", "10am", "12pm", "2pm", "4pm", "6pm", "8pm")
        val counts = IntArray(7) { 0 }
        val cal = Calendar.getInstance()

        activeStoreTransactions.forEach { tx ->
            cal.timeInMillis = tx.timestamp
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            when (hour) {
                in 6..8 -> counts[0]++
                in 9..10 -> counts[1]++
                in 11..12 -> counts[2]++
                in 13..14 -> counts[3]++
                in 15..16 -> counts[4]++
                in 17..18 -> counts[5]++
                in 19..23 -> counts[6]++
            }
        }
        slots.zip(counts.toList())
    }
    val maxHourlyCount = hourlyCounts.maxOfOrNull { it.second } ?: 1
    val peakHourSlot = hourlyCounts.maxByOrNull { it.second }

    // Cálculo de distribución por rangos de monto
    val tierSmallCount = activeStoreTransactions.count { it.amount <= 15.0 }
    val tierMediumCount = activeStoreTransactions.count { it.amount > 15.0 && it.amount <= 50.0 }
    val tierLargeCount = activeStoreTransactions.count { it.amount > 50.0 }

    // Cálculo diario de los últimos 7 días para el nuevo gráfico de barras semanal
    val last7DaysData = remember(weeklyTransactions) {
        val cal = Calendar.getInstance()
        val daysFormat = SimpleDateFormat("EEE", Locale("es", "PE"))
        val dayNumFormat = SimpleDateFormat("dd", Locale.getDefault())
        val result = mutableListOf<Triple<String, Double, Int>>() // Label, TotalAmount, Count

        for (i in 6 downTo 0) {
            val targetCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val targetYear = targetCal.get(Calendar.YEAR)
            val targetDay = targetCal.get(Calendar.DAY_OF_YEAR)

            val dayTxs = weeklyTransactions.filter { tx ->
                val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                txCal.get(Calendar.YEAR) == targetYear && txCal.get(Calendar.DAY_OF_YEAR) == targetDay && tx.isStoreTransaction
            }
            val dayTotal = dayTxs.sumOf { it.amount }
            val label = if (i == 0) "Hoy" else daysFormat.format(targetCal.time).replace(".", "").replaceFirstChar { it.uppercase() }
            result.add(Triple(label, dayTotal, dayTxs.size))
        }
        result
    }
    val maxDayTotal = last7DaysData.maxOfOrNull { it.second } ?: 1.0

    if (showWorkersPanel) {
        OwnerWorkersPanelDialog(
            viewModel = viewModel,
            onDismiss = { showWorkersPanel = false }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.canvasBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP USER BAR & STATUS
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val cleanDisplayName = remember(storeConfig.storeName, storeConfig.storeCode) {
                    val rawName = storeConfig.storeName.trim()
                    when {
                        rawName.isBlank() -> "Mi Negocio"
                        rawName.equals(storeConfig.storeCode, ignoreCase = true) -> "Mi Negocio"
                        rawName.startsWith("str_", ignoreCase = true) -> "Mi Negocio"
                        rawName.contains("str_", ignoreCase = true) -> rawName.replace(Regex("""\(?str_[a-fA-F0-9]+\)?"""), "").trim().ifBlank { "Mi Negocio" }
                        else -> rawName
                    }
                }

                // Store Info Profile Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AppTheme.pillBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.pillBorder),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = cleanDisplayName.take(1).uppercase().ifBlank { "M" },
                                color = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = cleanDisplayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.textPrimary
                            )
                        }
                        Text(
                            text = when (storeConfig.deviceRole) {
                                com.example.data.model.DeviceRole.SENDER -> "📱 Caja Principal activa"
                                com.example.data.model.DeviceRole.RECEIVER -> "📢 Parlante / Receptor activo"
                                com.example.data.model.DeviceRole.LOCAL_SPEAKER -> "🔊 Modo autónomo local"
                            },
                            fontSize = 11.sp,
                            color = AppTheme.textSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Date Pill & Refresh Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = AppTheme.cardBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder),
                            modifier = Modifier.clickable { dateFilterMenuExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedDateRangeLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.textPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = AppTheme.textPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = dateFilterMenuExpanded,
                            onDismissRequest = { dateFilterMenuExpanded = false }
                        ) {
                            listOf("Hoy", "Ayer", "Esta Semana").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedDateRangeLabel = option
                                        dateFilterMenuExpanded = false
                                        Toast.makeText(context, "Filtro: $option", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = AppTheme.cardBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder),
                        modifier = Modifier
                            .size(38.dp)
                            .clickable {
                                viewModel.syncNow()
                                Toast.makeText(context, "Sincronizando...", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = AppTheme.accentGreen
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
                }
            }
        }

        // PANEL DE TRABAJADORES (SÓLO EMISOR / DUEÑO)
        if (storeConfig.deviceRole == DeviceRole.SENDER) {
            item {
                val activeCount = receivers.count { !it.status.equals("REVOKED", ignoreCase = true) }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AppTheme.cardBackground,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (activeCount > 0) (if (AppTheme.isDark) ModernNeonLime.copy(alpha = 0.4f) else ModernMatteBlack.copy(alpha = 0.15f)) else AppTheme.cardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (onNavigateToWorkers != null) {
                                onNavigateToWorkers()
                            } else {
                                showWorkersPanel = true
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.People,
                                        contentDescription = null,
                                        tint = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Panel de Trabajadores",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = AppTheme.textPrimary
                                )
                                Text(
                                    text = if (activeCount > 0) "$activeCount receptor(es) enlazado(s)" else "Toca para vincular a tu personal",
                                    fontSize = 11.sp,
                                    color = AppTheme.textSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (AppTheme.isDark) ModernNeonLime.copy(alpha = 0.15f) else ModernMatteBlack.copy(alpha = 0.08f)
                        ) {
                            Text(
                                text = "Gestionar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // PAGE TITLE
        item {

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Resumen de Cobros",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        color = AppTheme.textPrimary
                    )
                    Text(
                        text = todayFullDate,
                        fontSize = 12.sp,
                        color = AppTheme.textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Control financiero y métricas de pagos en tiempo real",
                    fontSize = 13.sp,
                    color = AppTheme.textSecondary
                )
            }
        }

        // FILTRO DE SUCURSAL (DUEÑO) O BADGE INFORMATIVO DE SUCURSAL (RECEPTOR)
        if (storeConfig.deviceRole == DeviceRole.SENDER) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = AppTheme.accentGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FILTRAR POR SUCURSAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.textSecondary,
                            letterSpacing = 1.sp
                        )
                    }

                    val branchList = remember(storeConfig.storeBranches) {
                        val list = mutableListOf("ALL")
                        list.addAll(storeConfig.storeBranches.split(",").map { it.trim() }.filter { it.isNotBlank() })
                        list.distinct()
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(branchList) { branch ->
                            val isSelected = selectedBranchFilter.equals(branch, ignoreCase = true)
                            val label = if (branch == "ALL") "Todas las sucursales" else branch
                            Surface(
                                shape = RoundedCornerShape(20.dp),
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
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // HERO CARD: TOTAL RECAUDADO
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_dashboard_total_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.cardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Header inside card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = AppTheme.pillBackground,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Total Recaudado",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.textPrimary
                            )
                        }

                        Box {
                            IconButton(onClick = { heroMenuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Opciones",
                                    tint = AppTheme.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = heroMenuExpanded,
                                onDismissRequest = { heroMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Cargar datos de prueba de hoy") },
                                    onClick = {
                                        heroMenuExpanded = false
                                        viewModel.insertMockTransactionsForToday()
                                        Toast.makeText(context, "Insertando pagos de prueba...", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sincronizar caja ahora") },
                                    onClick = {
                                        heroMenuExpanded = false
                                        viewModel.syncNow()
                                        Toast.makeText(context, "Sincronizando caja...", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Ver en Reportes detallados") },
                                    onClick = {
                                        heroMenuExpanded = false
                                        Toast.makeText(context, "Navega a la pestaña Reportes para ver filtros avanzados", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Big Amount with +Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format(Locale("es", "PE"), "S/ %.2f", displayTotal),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp,
                            color = AppTheme.textPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ModernNeonLime
                        ) {
                            Text(
                                text = "+${displayCount} pagos",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = ModernOnNeonLime,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // OVERLAPPING CIRCLE VISUALIZER
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Large Brand Blue Circle (Main Total Indicator)
                        Box(
                            modifier = Modifier
                                .size(116.dp)
                                .offset(x = (-38).dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            ModernBrandBlue,
                                            Color(0xFF254580)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format(Locale("es", "PE"), "%.0f", displayTotal),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = "soles",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        // Average Ticket Indicator Circle
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .offset(x = 42.dp, y = (-12).dp)
                                .clip(CircleShape)
                                .background(if (AppTheme.isDark) Color(0xFF2E313C) else Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format(Locale("es", "PE"), "%.1f", averageTicket),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = AppTheme.textPrimary
                                )
                                Text(
                                    text = "promedio",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AppTheme.textSecondary
                                )
                            }
                        }

                        // Bright Neon Lime Circle (Payment Count Indicator)
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .offset(x = 14.dp, y = 30.dp)
                                .clip(CircleShape)
                                .background(ModernNeonLime),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$displayCount",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ModernOnNeonLime
                                )
                                Text(
                                    text = "pagos",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ModernOnNeonLime.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Progress breakdown indicators
                    val mainProgress = if (displayTotal > 0) 0.65f else 0.05f
                    val secondaryProgress = if (displayCount > 0) 0.35f else 0.05f
                    val tertiaryProgress = if (averageTicket > 0) 0.25f else 0.05f
                    // Color del círculo gris (promedio) para coincidir con la leyenda
                    val grayCircleColor = if (AppTheme.isDark) Color(0xFF2E313C) else Color(0xFFE2E8F0)

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Progress 1: Brand Blue (Cobros Confirmados)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${(mainProgress * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.textPrimary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            LinearProgressIndicator(
                                progress = { mainProgress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = AppTheme.brandBlue,
                                trackColor = AppTheme.pillBackground,
                                strokeCap = StrokeCap.Round
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Cobros Confirmados",
                                    fontSize = 11.sp,
                                    color = AppTheme.textSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AppTheme.brandBlue))
                            }
                        }

                        // Progress 2: Gris (coincide con el círculo gris del promedio)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${(secondaryProgress * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.textPrimary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            LinearProgressIndicator(
                                progress = { secondaryProgress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = grayCircleColor,
                                trackColor = AppTheme.pillBackground,
                                strokeCap = StrokeCap.Round
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Rendimiento Ticket",
                                    fontSize = 11.sp,
                                    color = AppTheme.textSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(grayCircleColor))
                            }
                        }

                        // Progress 3: Verde lima (coincide con el círculo verde lima de pagos)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${(tertiaryProgress * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.textPrimary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            LinearProgressIndicator(
                                progress = { tertiaryProgress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = ModernNeonLime,
                                trackColor = AppTheme.pillBackground,
                                strokeCap = StrokeCap.Round
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Frecuencia Hoy",
                                    fontSize = 11.sp,
                                    color = AppTheme.textSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ModernNeonLime))
                            }
                        }
                    }
                }
            }
        }

        // DUAL STAT CARDS ROW
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Left Card: Ticket Promedio
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = AppTheme.cardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ticket Prom.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.textPrimary
                                )
                            }

                            Box {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = null,
                                    tint = AppTheme.textSecondary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { ticketMenuExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = ticketMenuExpanded,
                                    onDismissRequest = { ticketMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Total: S/ ${String.format(Locale.US, "%.2f", displayTotal)}") },
                                        onClick = { ticketMenuExpanded = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Operaciones: $displayCount") },
                                        onClick = { ticketMenuExpanded = false }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "S/ ${String.format(Locale.US, "%.2f", averageTicket)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.textPrimary
                                )
                                Text(
                                    text = "x transacción",
                                    fontSize = 11.sp,
                                    color = AppTheme.textSecondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (AppTheme.isDark) AppTheme.pillBackground else Color(0xFFDCFCE7))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "+4.2%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.accentLimeText
                                )
                            }
                        }
                    }
                }

                // Right Card: Verificados
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = AppTheme.cardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Percent,
                                    contentDescription = null,
                                    tint = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Verificados",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.textPrimary
                                )
                            }

                            Box {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = null,
                                    tint = AppTheme.textSecondary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { verifiedMenuExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = verifiedMenuExpanded,
                                    onDismissRequest = { verifiedMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Estatus: 100% Verificado") },
                                        onClick = { verifiedMenuExpanded = false }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "100%",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.textPrimary
                                )
                                Text(
                                    text = "Vía Notificaciones",
                                    fontSize = 11.sp,
                                    color = AppTheme.textSecondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (AppTheme.isDark) AppTheme.pillBackground else Color(0xFFDCFCE7))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "OK",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.accentLimeText
                                )
                            }
                        }
                    }
                }
            }
        }

        // METRICS STRIP: COBRO MAYOR, MENOR, HORA PICO
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.cardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cobro Máximo
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = AppTheme.accentGreen
                            )
                            Text(
                                text = "MAYOR",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.textSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "S/ ${String.format(Locale.US, "%.2f", maxTicket)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = AppTheme.textPrimary
                        )
                    }

                    // Cobro Mínimo
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFFF6B6B)
                            )
                            Text(
                                text = "MENOR",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.textSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "S/ ${String.format(Locale.US, "%.2f", minTicket)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = AppTheme.textPrimary
                        )
                    }

                    // Hora Pico
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = AppTheme.brandBlue
                            )
                            Text(
                                text = "HORA PICO",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.textSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = peakHourSlot?.first ?: "N/A",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = AppTheme.textPrimary
                        )
                    }
                }
            }
        }

        // 7-DAY WEEKLY TREND BAR CHART
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.cardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = AppTheme.brandBlue
                            )
                            Text(
                                text = "TENDENCIA ÚLTIMOS 7 DÍAS",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.textSecondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }

                        val weeklySum = last7DaysData.sumOf { it.second }
                        Text(
                            text = "Total S/ ${String.format(Locale.US, "%.2f", weeklySum)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = AppTheme.brandBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    val maxAmount = last7DaysData.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        last7DaysData.forEach { (dayLabel, amount, count) ->
                            val heightFraction = (amount / maxAmount).toFloat().coerceIn(0.08f, 1f)
                            val isToday = dayLabel == "Hoy"

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                if (amount > 0) {
                                    Text(
                                        text = "${amount.toInt()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = if (isToday) AppTheme.accentLimeText else AppTheme.brandBlue,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .fillMaxHeight(heightFraction)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            if (isToday) ModernNeonLime
                                            else if (amount > 0) AppTheme.brandBlue
                                            else AppTheme.pillBackground
                                        )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = dayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = if (isToday) AppTheme.textPrimary else AppTheme.textSecondary,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // AMOUNT DISTRIBUTION DONUT CHART
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.cardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DonutLarge,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = AppTheme.brandBlue
                            )
                            Text(
                                text = "DISTRIBUCIÓN POR MONTO",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.textSecondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Text(
                            text = "$displayCount tickets",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppTheme.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    val tiers = listOf(
                        Triple("< S/ 15", tierSmallCount, ModernNeonLime),
                        Triple("S/ 15 - 50", tierMediumCount, AppTheme.brandBlue),
                        Triple("> S/ 50", tierLargeCount, Color(0xFF2563EB))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // Donut Canvas
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(110.dp)
                        ) {
                            Canvas(modifier = Modifier.size(100.dp)) {
                                val strokeWidth = 14.dp.toPx()
                                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                                if (displayCount == 0) {
                                    drawArc(
                                        color = Color.LightGray.copy(alpha = 0.3f),
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                } else {
                                    var currentAngle = -90f
                                    tiers.forEach { (_, count, color) ->
                                        if (count > 0) {
                                            val sweep = (count.toFloat() / displayCount) * 360f
                                            drawArc(
                                                color = color,
                                                startAngle = currentAngle,
                                                sweepAngle = sweep,
                                                useCenter = false,
                                                topLeft = topLeft,
                                                size = arcSize,
                                                style = Stroke(width = strokeWidth)
                                            )
                                            currentAngle += sweep
                                        }
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$displayCount",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.textPrimary
                                )
                                Text(
                                    text = "pagos",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = AppTheme.textSecondary
                                )
                            }
                        }

                        // Legend with counts and percentages
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            tiers.forEach { (label, count, color) ->
                                val pct = if (displayCount > 0) (count * 100) / displayCount else 0
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Column {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AppTheme.textPrimary
                                        )
                                        Text(
                                            text = "$count ops ($pct%)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 11.sp,
                                            color = AppTheme.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // HOURLY BAR CHART CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.cardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Actividad por Hora",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.textPrimary
                            )
                        }

                        Box {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null,
                                tint = AppTheme.textSecondary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { scheduleMenuExpanded = true }
                            )
                            DropdownMenu(
                                expanded = scheduleMenuExpanded,
                                onDismissRequest = { scheduleMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Pico: ${peakHourSlot?.first ?: "N/A"}") },
                                    onClick = { scheduleMenuExpanded = false }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        hourlyCounts.forEach { (hour, count) ->
                            val heightFraction = if (maxHourlyCount > 0 && count > 0) {
                                (count.toFloat() / maxHourlyCount.toFloat()).coerceIn(0.2f, 1.0f)
                            } else 0.12f
                            val barHeight = (heightFraction * 70).dp
                            val isPeak = count == maxHourlyCount && count > 0

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                if (count > 0) {
                                    Text(
                                        text = "$count",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPeak) AppTheme.textPrimary else AppTheme.textSecondary,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(
                                            when {
                                                isPeak -> ModernNeonLime
                                                count > 0 -> AppTheme.brandBlue
                                                else -> if (AppTheme.isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFE6E9EB)
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = hour,
                                    fontSize = 9.sp,
                                    color = if (isPeak) AppTheme.textPrimary else AppTheme.textSecondary,
                                    fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
