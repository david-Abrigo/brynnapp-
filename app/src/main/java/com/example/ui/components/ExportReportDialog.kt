package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.YapeTransaction
import com.example.ui.theme.ModernBorderLight
import com.example.ui.theme.ModernLimeVolt
import com.example.ui.theme.SleekBorderLight
import com.example.ui.theme.SleekMidnightNavy
import com.example.ui.theme.SleekPrimaryBlue
import com.example.ui.theme.SleekPrimaryContainer
import com.example.ui.theme.SleekSecondaryContainer
import com.example.ui.theme.SleekSubtleTextLight
import com.example.ui.theme.SleekSuccessGreen
import com.example.util.ReportExportUtil
import java.util.Locale

@Composable
fun ExportReportDialog(
    storeName: String,
    dateString: String,
    transactions: List<YapeTransaction>,
    totalAmount: Double,
    isSeller: Boolean = false,
    currentSellerName: String = "",
    currentSellerId: String = "",
    currentSellerEmail: String = "",
    branchName: String = "",
    initialSellerScope: String = "MINE",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Seller scope: "MINE" or "ALL"
    var selectedScope by remember { mutableStateOf(if (initialSellerScope == "ALL") "ALL" else "MINE") }

    val myConfirmedTxs = remember(transactions, currentSellerId, currentSellerName, currentSellerEmail) {
        transactions.filter { it.isConfirmedBy(currentSellerId, currentSellerName, currentSellerEmail) }
    }
    val myStoreTxs = remember(myConfirmedTxs) {
        myConfirmedTxs.filter { it.isStoreTransaction }
    }
    val myConfirmedTotal = remember(myStoreTxs) {
        myStoreTxs.sumOf { it.amount }
    }

    val allStoreTxs = remember(transactions) {
        transactions.filter { it.isStoreTransaction }
    }
    val allStoreTotal = remember(allStoreTxs, totalAmount) {
        if (allStoreTxs.isNotEmpty()) allStoreTxs.sumOf { it.amount } else totalAmount
    }

    // Determine effective transactions and values according to active scope
    val effectiveTransactions = if (isSeller && selectedScope == "MINE") myConfirmedTxs else transactions
    val effectiveStoreTxs = if (isSeller && selectedScope == "MINE") myStoreTxs else allStoreTxs
    val effectiveTotal = if (isSeller && selectedScope == "MINE") myConfirmedTotal else allStoreTotal
    val effectiveWorkerName = if (isSeller && selectedScope == "MINE") currentSellerName.ifBlank { "Vendedor" } else null
    val effectiveBranchName = branchName.ifBlank { null }

    val excludedTxs = effectiveTransactions.filter { !it.isStoreTransaction }
    val excludedTotal = excludedTxs.sumOf { it.amount }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("export_report_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "EXPORTAR REPORTE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = SleekSubtleTextLight
                        )
                        Text(
                            text = "Ventas de $dateString",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = SleekSubtleTextLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Selector de Alcance para Modo Vendedor (Mis cobros vs Toda la sucursal)
                if (isSeller) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, ModernBorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "ALCANCE DE LA EXPORTACIÓN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = SleekSubtleTextLight,
                                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Opción 1: Mis cobros
                                val isMine = selectedScope == "MINE"
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isMine) Color(0xFF131412) else Color.Transparent,
                                    border = BorderStroke(
                                        width = if (isMine) 1.5.dp else 1.dp,
                                        color = if (isMine) ModernLimeVolt else ModernBorderLight
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedScope = "MINE" }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "👤 Mis cobros",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMine) ModernLimeVolt else MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = String.format(Locale("es", "PE"), "S/ %.2f", myConfirmedTotal),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${myStoreTxs.size} ventas",
                                            fontSize = 10.sp,
                                            color = if (isMine) Color(0xFFA1A1AA) else SleekSubtleTextLight
                                        )
                                    }
                                }

                                // Opción 2: Toda la sucursal
                                val isAll = selectedScope == "ALL"
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isAll) Color(0xFF131412) else Color.Transparent,
                                    border = BorderStroke(
                                        width = if (isAll) 1.5.dp else 1.dp,
                                        color = if (isAll) ModernLimeVolt else ModernBorderLight
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedScope = "ALL" }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "🏪 Toda la sucursal",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAll) ModernLimeVolt else MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = String.format(Locale("es", "PE"), "S/ %.2f", allStoreTotal),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isAll) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${allStoreTxs.size} ventas",
                                            fontSize = 10.sp,
                                            color = if (isAll) Color(0xFFA1A1AA) else SleekSubtleTextLight
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (selectedScope == "MINE") {
                                    "💡 Recomendado para tu cierre de caja personal y rendición de cuentas."
                                } else {
                                    "🏪 Ventas totales registradas por toda la sucursal."
                                },
                                fontSize = 10.sp,
                                color = if (selectedScope == "MINE") SleekSuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Summary Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF131412),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val titleLabel = if (isSeller && selectedScope == "MINE") {
                                "${storeName.ifBlank { "MI NEGOCIO" }} • ${currentSellerName.ifBlank { "VENDEDOR" }}".uppercase()
                            } else if (branchName.isNotBlank()) {
                                "${storeName.ifBlank { "MI NEGOCIO" }} • $branchName".uppercase()
                            } else {
                                storeName.ifBlank { "MI NEGOCIO" }.uppercase()
                            }
                            Text(
                                text = titleLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA1A1AA),
                                maxLines = 1
                            )
                            val subCountLabel = if (isSeller && selectedScope == "MINE") {
                                "${effectiveStoreTxs.size} ventas confirmadas por ti"
                            } else {
                                "${effectiveStoreTxs.size} ventas válidas de tienda"
                            }
                            Text(
                                text = subCountLabel,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = String.format(Locale("es", "PE"), "S/ %.2f", effectiveTotal),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ModernLimeVolt
                        )
                    }
                }

                if (excludedTxs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFFBEB),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️ ${excludedTxs.size} pagos desconocidos (S/ ${String.format(Locale("es", "PE"), "%.2f", excludedTotal)}) han sido excluidos del total de ventas.",
                                fontSize = 11.sp,
                                color = Color(0xFFB45309),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Selecciona el formato deseado:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 1. Export PDF
                ExportOptionItem(
                    title = "Documento PDF Oficial",
                    subtitle = "Diseño A4 con membrete, tabla y métricas",
                    icon = Icons.Default.PictureAsPdf,
                    iconTint = Color(0xFFDC2626),
                    iconBg = Color(0xFFFEE2E2),
                    badge = "Recomendado",
                    onClick = {
                        ReportExportUtil.exportToPdf(
                            context = context,
                            storeName = storeName,
                            dateString = dateString,
                            transactions = effectiveTransactions,
                            totalAmount = effectiveTotal,
                            workerName = effectiveWorkerName,
                            branchName = effectiveBranchName
                        )
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Export Excel / CSV
                ExportOptionItem(
                    title = "Hoja de Cálculo Excel / CSV",
                    subtitle = "Archivo estructurado para Excel y Google Sheets",
                    icon = Icons.Default.TableChart,
                    iconTint = Color(0xFF16A34A),
                    iconBg = Color(0xFFDCFCE7),
                    badge = "Excel / Sheets",
                    onClick = {
                        ReportExportUtil.exportToExcelCsv(
                            context = context,
                            storeName = storeName,
                            dateString = dateString,
                            transactions = effectiveTransactions,
                            totalAmount = effectiveTotal,
                            workerName = effectiveWorkerName,
                            branchName = effectiveBranchName
                        )
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Copy to Clipboard
                ExportOptionItem(
                    title = "Copiar al Portapapeles",
                    subtitle = "Listo para pegar en WhatsApp, Telegram o Notas",
                    icon = Icons.Default.ContentCopy,
                    iconTint = SleekPrimaryBlue,
                    iconBg = SleekPrimaryContainer,
                    badge = "WhatsApp",
                    onClick = {
                        ReportExportUtil.copyToClipboard(
                            context = context,
                            storeName = storeName,
                            dateString = dateString,
                            transactions = effectiveTransactions,
                            totalAmount = effectiveTotal,
                            workerName = effectiveWorkerName,
                            branchName = effectiveBranchName
                        )
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 4. Share Text via Intent
                ExportOptionItem(
                    title = "Compartir como Mensaje de Texto",
                    subtitle = "Enviar por apps de mensajería instaladas",
                    icon = Icons.Default.IosShare,
                    iconTint = SleekMidnightNavy,
                    iconBg = SleekSecondaryContainer,
                    onClick = {
                        ReportExportUtil.shareTextReport(
                            context = context,
                            storeName = storeName,
                            dateString = dateString,
                            transactions = effectiveTransactions,
                            totalAmount = effectiveTotal,
                            workerName = effectiveWorkerName,
                            branchName = effectiveBranchName
                        )
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportOptionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    badge: String? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, ModernBorderLight),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ModernLimeVolt
                        ) {
                            Text(
                                text = badge,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF131412),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
