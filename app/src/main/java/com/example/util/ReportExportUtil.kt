package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.YapeTransaction
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExportUtil {

    /**
     * Builds a clean text summary ready for WhatsApp, Telegram or Notes.
     */
    fun buildTextReport(
        storeName: String,
        dateString: String,
        transactions: List<YapeTransaction>,
        totalAmount: Double,
        workerName: String? = null,
        branchName: String? = null
    ): String {
        val storeTxs = transactions.filter { it.isStoreTransaction }
        val excludedTxs = transactions.filter { !it.isStoreTransaction }
        val calculatedStoreTotal = if (storeTxs.isNotEmpty()) storeTxs.sumOf { it.amount } else totalAmount
        val excludedTotal = excludedTxs.sumOf { it.amount }

        return buildString {
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("📊 *REPORTE DE VENTAS*\n")
            append("🏪 *Negocio:* ${storeName.ifBlank { "Mi Negocio" }}\n")
            if (!branchName.isNullOrBlank()) {
                append("📍 *Sucursal:* $branchName\n")
            }
            if (!workerName.isNullOrBlank()) {
                append("👤 *Vendedor:* $workerName\n")
            }
            append("📅 *Fecha:* $dateString\n")
            append("💰 *Total Ventas Tienda:* S/ ${String.format(Locale("es", "PE"), "%.2f", calculatedStoreTotal)}\n")
            append("🔢 *Ventas Válidas:* ${storeTxs.size}\n")
            if (storeTxs.isNotEmpty()) {
                val avg = calculatedStoreTotal / storeTxs.size
                append("📈 *Promedio por Venta:* S/ ${String.format(Locale("es", "PE"), "%.2f", avg)}\n")
            }
            append("━━━━━━━━━━━━━━━━━━━━\n\n")

            if (storeTxs.isEmpty()) {
                append("Sin ventas de tienda registradas para este periodo.\n")
            } else {
                append("📝 *DETALLE DE VENTAS DE TIENDA:*\n")
                storeTxs.forEachIndexed { index, tx ->
                    val num = index + 1
                    val secStr = if (!tx.securityCode.isNullOrBlank()) " [Cód: ${tx.securityCode}]" else ""
                    val noteStr = if (tx.note.isNotBlank()) " • 🏷️ ${tx.note}" else ""
                    val workerStr = if (workerName.isNullOrBlank() && tx.claimedByName.isNotBlank()) " • 👤 ${tx.claimedByName}" else ""
                    append("$num. *${tx.formattedTime}* | ${tx.senderName.ifBlank { "Cliente" }}$secStr | *+ S/ ${String.format(Locale("es", "PE"), "%.2f", tx.amount)}*$noteStr$workerStr\n")
                }
            }

            if (excludedTxs.isNotEmpty()) {
                append("\n━━━━━━━━━━━━━━━━━━━━\n")
                append("🚫 *YAPES EXCLUIDOS / DESCONOCIDOS (NO SUMADOS):*\n")
                append("Total Excluido: S/ ${String.format(Locale("es", "PE"), "%.2f", excludedTotal)} (${excludedTxs.size} pagos)\n")
                excludedTxs.forEachIndexed { index, tx ->
                    val reasonTag = if (tx.exclusionReason.isNotBlank()) " [${tx.exclusionReason}]" else ""
                    append("• ${tx.formattedTime} | ${tx.senderName.ifBlank { "Desconocido" }} | + S/ ${String.format(Locale("es", "PE"), "%.2f", tx.amount)}$reasonTag (Excluido)\n")
                }
            }

            val genDateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "PE")).format(Date())
            append("\n_Reporte emitido: $genDateStr • Solo ventas válidas de tienda_")
        }
    }

    /**
     * Copies report to Android system clipboard with instant Toast confirmation.
     */
    fun copyToClipboard(
        context: Context,
        storeName: String,
        dateString: String,
        transactions: List<YapeTransaction>,
        totalAmount: Double,
        workerName: String? = null,
        branchName: String? = null
    ) {
        val report = buildTextReport(storeName, dateString, transactions, totalAmount, workerName, branchName)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Reporte $dateString", report)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(
            context,
            "📋 Reporte copiado al portapapeles (Listo para pegar)",
            Toast.LENGTH_LONG
        ).show()
    }

    /**
     * Shares plain text report via any messaging app.
     */
    fun shareTextReport(
        context: Context,
        storeName: String,
        dateString: String,
        transactions: List<YapeTransaction>,
        totalAmount: Double,
        workerName: String? = null,
        branchName: String? = null
    ) {
        val report = buildTextReport(storeName, dateString, transactions, totalAmount, workerName, branchName)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Reporte de Ventas - $storeName ($dateString)")
            putExtra(Intent.EXTRA_TEXT, report)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir reporte de ventas"))
    }

    /**
     * Generates a CSV file compatible with Microsoft Excel and Google Sheets, and opens Share dialog.
     */
    fun exportToExcelCsv(
        context: Context,
        storeName: String,
        dateString: String,
        transactions: List<YapeTransaction>,
        totalAmount: Double,
        workerName: String? = null,
        branchName: String? = null
    ) {
        try {
            val reportDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val cleanDate = dateString.replace("[^a-zA-Z0-9]".toRegex(), "_")
            val fileName = "Reporte_Ventas_${cleanDate}.csv"
            val file = File(reportDir, fileName)

            val storeTxs = transactions.filter { it.isStoreTransaction }
            val excludedTxs = transactions.filter { !it.isStoreTransaction }
            val calculatedStoreTotal = if (storeTxs.isNotEmpty()) storeTxs.sumOf { it.amount } else totalAmount
            val excludedTotal = excludedTxs.sumOf { it.amount }

            FileOutputStream(file).use { fos ->
                // UTF-8 BOM for Microsoft Excel auto-detect
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                val writer = fos.bufferedWriter(Charsets.UTF_8)

                writer.write("REPORTE DE VENTAS - ${storeName.ifBlank { "MI NEGOCIO" }}\n")
                if (!branchName.isNullOrBlank()) {
                    writer.write("Sucursal;${branchName}\n")
                }
                if (!workerName.isNullOrBlank()) {
                    writer.write("Vendedor;${workerName}\n")
                }
                writer.write("Fecha;${dateString}\n")
                writer.write("Total Ventas Tienda;S/ ${String.format(Locale("es", "PE"), "%.2f", calculatedStoreTotal)}\n")
                writer.write("Cantidad de Ventas Tienda;${storeTxs.size}\n")
                if (excludedTxs.isNotEmpty()) {
                    writer.write("Total Excluidos (Desconocidos/No Tienda);S/ ${String.format(Locale("es", "PE"), "%.2f", excludedTotal)}\n")
                    writer.write("Cantidad Excluidos;${excludedTxs.size}\n")
                }
                writer.write("\n")

                // Table headers with semicolon delimiter (standard for Excel in Spanish locales)
                writer.write("N°;Hora;Cliente / Emisor;Monto (S/);Cód. Seguridad;Detalle Producto;Confirmado por;Sucursal;Categoría;Estado de Caja;Motivo\n")

                transactions.forEachIndexed { index, tx ->
                    val num = index + 1
                    val time = tx.formattedTime.replace(";", ",")
                    val sender = tx.senderName.ifBlank { "Cliente" }.replace(";", ",").replace("\"", "\"\"")
                    val amount = String.format(Locale("es", "PE"), "%.2f", tx.amount)
                    val secCode = tx.securityCode ?: "-"
                    val productNote = tx.note.replace(";", ",").replace("\"", "\"\"")
                    val confirmedBy = tx.claimedByName.replace(";", ",").replace("\"", "\"\"")
                    val branch = tx.branchName.replace(";", ",").replace("\"", "\"\"")
                    val category = if (tx.isStoreTransaction) "Venta Tienda" else "Yape Desconocido / No Tienda"
                    val boxStatus = if (tx.isStoreTransaction) "SUMADO EN CAJA" else "EXCLUIDO DE CAJA"
                    val reason = tx.exclusionReason.replace(";", ",").replace("\"", "\"\"")
                    writer.write("$num;\"$time\";\"$sender\";$amount;\"$secCode\";\"$productNote\";\"$confirmedBy\";\"$branch\";\"$category\";\"$boxStatus\";\"$reason\"\n")
                }

                writer.flush()
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/comma-separated-values"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Reporte Excel CSV - $storeName ($dateString)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Exportar Reporte a Excel / CSV"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error al generar archivo Excel: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Generates a sleek, printable A4 PDF document with business branding, statistics, and transaction list.
     */
    fun exportToPdf(
        context: Context,
        storeName: String,
        dateString: String,
        transactions: List<YapeTransaction>,
        totalAmount: Double,
        workerName: String? = null,
        branchName: String? = null
    ) {
        val document = PdfDocument()
        try {
            val storeTxs = transactions.filter { it.isStoreTransaction }
            val excludedTxs = transactions.filter { !it.isStoreTransaction }
            val calculatedStoreTotal = if (storeTxs.isNotEmpty()) storeTxs.sumOf { it.amount } else totalAmount
            val excludedTotal = excludedTxs.sumOf { it.amount }

            // A4 dimensions at 72 dpi: 595 x 842 points
            val pageWidth = 595
            val pageHeight = 842
            val margin = 36f

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas

            val textPaint = Paint().apply {
                isAntiAlias = true
                color = Color.BLACK
            }

            fun drawHeader(c: Canvas, isFirstPage: Boolean): Float {
                var y = margin

                if (isFirstPage) {
                    // Dark Hero Header Box
                    val headerPaint = Paint().apply {
                        color = Color.parseColor("#121212")
                        isAntiAlias = true
                    }
                    val headerRect = RectF(margin, y, pageWidth - margin, y + 95f)
                    c.drawRoundRect(headerRect, 14f, 14f, headerPaint)

                    // Accent tag
                    val tagPaint = Paint().apply {
                        color = Color.parseColor("#C6F432")
                        isAntiAlias = true
                    }
                    val tagRect = RectF(margin + 16f, y + 16f, margin + 115f, y + 36f)
                    c.drawRoundRect(tagRect, 6f, 6f, tagPaint)

                    textPaint.apply {
                        color = Color.parseColor("#121212")
                        textSize = 9.5f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    c.drawText("REPORTE OFICIAL", margin + 22f, y + 30f, textPaint)

                    // Store Title
                    textPaint.apply {
                        color = Color.WHITE
                        textSize = 18f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    val mainTitle = if (!workerName.isNullOrBlank()) {
                        "${storeName.ifBlank { "MI NEGOCIO" }} • ${workerName}".uppercase()
                    } else {
                        storeName.ifBlank { "MI NEGOCIO" }.uppercase()
                    }
                    val clippedMainTitle = if (mainTitle.length > 28) mainTitle.take(26) + "..." else mainTitle
                    c.drawText(clippedMainTitle, margin + 16f, y + 62f, textPaint)

                    // Subtitle & Date
                    textPaint.apply {
                        color = Color.parseColor("#A1A1AA")
                        textSize = 10f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    }
                    val subTitle = buildString {
                        append("Reporte Oficial de Ventas • $dateString")
                        if (!branchName.isNullOrBlank()) append(" • $branchName")
                        if (!workerName.isNullOrBlank()) append(" • Vendedor: $workerName")
                    }
                    c.drawText(subTitle, margin + 16f, y + 80f, textPaint)

                    // Big Amount on top right of banner
                    val amountStr = String.format(Locale("es", "PE"), "S/ %.2f", calculatedStoreTotal)
                    textPaint.apply {
                        color = Color.parseColor("#C6F432")
                        textSize = 22f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.RIGHT
                    }
                    c.drawText(amountStr, pageWidth - margin - 20f, y + 55f, textPaint)

                    textPaint.apply {
                        color = Color.parseColor("#E4E4E7")
                        textSize = 10f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        textAlign = Paint.Align.RIGHT
                    }
                    c.drawText("${storeTxs.size} ventas de tienda válidas", pageWidth - margin - 20f, y + 74f, textPaint)
                    textPaint.textAlign = Paint.Align.LEFT

                    y += 112f

                    // Summary Stats Row (3 rounded boxes)
                    val cardWidth = (pageWidth - (margin * 2) - 20f) / 3f
                    val cardHeight = 50f
                    val cardBgPaint = Paint().apply {
                        color = Color.parseColor("#F3F4F7")
                        isAntiAlias = true
                    }

                    // Stat 1: Total
                    val card1 = RectF(margin, y, margin + cardWidth, y + cardHeight)
                    c.drawRoundRect(card1, 8f, 8f, cardBgPaint)
                    textPaint.apply {
                        color = Color.parseColor("#71717A")
                        textSize = 9f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    }
                    c.drawText("VENTAS TIENDA", margin + 12f, y + 18f, textPaint)
                    textPaint.apply {
                        color = Color.parseColor("#121212")
                        textSize = 14f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    c.drawText(amountStr, margin + 12f, y + 38f, textPaint)

                    // Stat 2: Pagos
                    val card2 = RectF(margin + cardWidth + 10f, y, margin + (cardWidth * 2) + 10f, y + cardHeight)
                    c.drawRoundRect(card2, 8f, 8f, cardBgPaint)
                    textPaint.apply {
                        color = Color.parseColor("#71717A")
                        textSize = 9f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    }
                    c.drawText("VENTAS VÁLIDAS", margin + cardWidth + 22f, y + 18f, textPaint)
                    textPaint.apply {
                        color = Color.parseColor("#121212")
                        textSize = 14f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    c.drawText("${storeTxs.size} pagos", margin + cardWidth + 22f, y + 38f, textPaint)

                    // Stat 3: Promedio / Excluidos
                    val card3 = RectF(margin + (cardWidth * 2) + 20f, y, pageWidth - margin, y + cardHeight)
                    c.drawRoundRect(card3, 8f, 8f, cardBgPaint)
                    textPaint.apply {
                        color = Color.parseColor("#71717A")
                        textSize = 9f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    }
                    if (excludedTxs.isEmpty()) {
                        val avgStr = if (storeTxs.isNotEmpty()) String.format(Locale("es", "PE"), "S/ %.2f", calculatedStoreTotal / storeTxs.size) else "S/ 0.00"
                        c.drawText("TICKET PROMEDIO", margin + (cardWidth * 2) + 32f, y + 18f, textPaint)
                        textPaint.apply {
                            color = Color.parseColor("#121212")
                            textSize = 14f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        }
                        c.drawText(avgStr, margin + (cardWidth * 2) + 32f, y + 38f, textPaint)
                    } else {
                        c.drawText("EXCLUIDOS / NO TIENDA", margin + (cardWidth * 2) + 32f, y + 18f, textPaint)
                        textPaint.apply {
                            color = Color.parseColor("#D97706")
                            textSize = 12f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        }
                        val exclStr = String.format(Locale("es", "PE"), "S/ %.2f (%d)", excludedTotal, excludedTxs.size)
                        c.drawText(exclStr, margin + (cardWidth * 2) + 32f, y + 38f, textPaint)
                    }

                    y += 65f
                } else {
                    // Small header for subsequent pages
                    textPaint.apply {
                        color = Color.parseColor("#71717A")
                        textSize = 10f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    val pageHeaderTitle = if (!workerName.isNullOrBlank()) {
                        "${storeName.uppercase()} ($workerName) • Reporte Ventas ($dateString) - Pág. $pageNumber"
                    } else {
                        "${storeName.uppercase()} • Reporte Ventas ($dateString) - Pág. $pageNumber"
                    }
                    c.drawText(pageHeaderTitle, margin, y + 12f, textPaint)
                    y += 24f
                }

                // Table Header Row
                val tableHeaderPaint = Paint().apply {
                    color = Color.parseColor("#1E2024")
                    isAntiAlias = true
                }
                val thRect = RectF(margin, y, pageWidth - margin, y + 26f)
                c.drawRoundRect(thRect, 6f, 6f, tableHeaderPaint)

                textPaint.apply {
                    color = Color.WHITE
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                c.drawText("N°", margin + 8f, y + 17f, textPaint)
                c.drawText("HORA", margin + 28f, y + 17f, textPaint)
                c.drawText("CLIENTE", margin + 75f, y + 17f, textPaint)
                c.drawText("CÓD. SEG", margin + 245f, y + 17f, textPaint)
                c.drawText("DETALLE", margin + 315f, y + 17f, textPaint)

                textPaint.textAlign = Paint.Align.RIGHT
                c.drawText("MONTO (S/)", pageWidth - margin - 14f, y + 17f, textPaint)
                textPaint.textAlign = Paint.Align.LEFT

                y += 32f
                return y
            }

            var currentY = drawHeader(canvas, true)
            val rowHeight = 22f

            val rowBgPaint = Paint().apply {
                color = Color.parseColor("#F9FAFB")
                isAntiAlias = true
            }
            val rowExcludedBgPaint = Paint().apply {
                color = Color.parseColor("#FFFBEB")
                isAntiAlias = true
            }
            val linePaint = Paint().apply {
                color = Color.parseColor("#E5E7EB")
                strokeWidth = 0.5f
            }

            if (transactions.isEmpty()) {
                textPaint.apply {
                    color = Color.parseColor("#71717A")
                    textSize = 11f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                }
                canvas.drawText("No se registraron movimientos en este día.", margin + 12f, currentY + 20f, textPaint)
            } else {
                transactions.forEachIndexed { index, tx ->
                    // Check if new page is required
                    if (currentY + rowHeight > pageHeight - margin - 30f) {
                        document.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = document.startPage(pageInfo)
                        canvas = page.canvas
                        currentY = drawHeader(canvas, false)
                    }

                    val isStore = tx.isStoreTransaction

                    // Background color
                    if (!isStore) {
                        val rowRect = RectF(margin, currentY, pageWidth - margin, currentY + rowHeight)
                        canvas.drawRect(rowRect, rowExcludedBgPaint)
                    } else if (index % 2 == 1) {
                        val rowRect = RectF(margin, currentY, pageWidth - margin, currentY + rowHeight)
                        canvas.drawRect(rowRect, rowBgPaint)
                    }

                    // Bottom divider line
                    canvas.drawLine(margin, currentY + rowHeight, pageWidth - margin, currentY + rowHeight, linePaint)

                    val itemY = currentY + 15f

                    // Number
                    textPaint.apply {
                        color = Color.parseColor("#71717A")
                        textSize = 9f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    }
                    canvas.drawText("${index + 1}", margin + 8f, itemY, textPaint)

                    // Time
                    textPaint.color = Color.parseColor("#121212")
                    canvas.drawText(tx.formattedTime, margin + 28f, itemY, textPaint)

                    // Sender
                    textPaint.apply {
                        color = if (isStore) Color.parseColor("#121212") else Color.parseColor("#92400E")
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    val senderPrefix = if (!isStore) "[EXCLUIDO] " else ""
                    val senderRaw = "$senderPrefix${tx.senderName.ifBlank { "Cliente" }}"
                    val clippedSender = if (senderRaw.length > 24) senderRaw.take(22) + "..." else senderRaw
                    canvas.drawText(clippedSender, margin + 75f, itemY, textPaint)

                    // Código de Seguridad
                    val secCode = tx.securityCode ?: "-"
                    textPaint.apply {
                        color = if (secCode != "-") Color.parseColor("#059669") else Color.parseColor("#A1A1AA")
                        typeface = Typeface.create(Typeface.DEFAULT, if (secCode != "-") Typeface.BOLD else Typeface.NORMAL)
                        textSize = 8.5f
                    }
                    canvas.drawText(secCode, margin + 245f, itemY, textPaint)

                    // Detalle de Producto / Motivo
                    textPaint.apply {
                        color = if (tx.note.isNotBlank()) {
                            Color.parseColor("#1E293B")
                        } else if (!isStore) {
                            Color.parseColor("#D97706")
                        } else {
                            Color.parseColor("#94A3B8")
                        }
                        typeface = Typeface.create(Typeface.DEFAULT, if (tx.note.isNotBlank()) Typeface.BOLD else Typeface.NORMAL)
                        textSize = 8.5f
                    }
                    val detailText = when {
                        tx.note.isNotBlank() -> if (tx.note.length > 26) tx.note.take(24) + "..." else tx.note
                        !isStore -> if (tx.exclusionReason.isNotBlank()) tx.exclusionReason.take(22) else "Excluido"
                        else -> "-"
                    }
                    canvas.drawText(detailText, margin + 315f, itemY, textPaint)

                    // Amount
                    textPaint.apply {
                        color = if (isStore) Color.parseColor("#15803D") else Color.parseColor("#B45309")
                        textSize = 9.5f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.RIGHT
                    }
                    val formattedAmount = if (isStore) {
                        String.format(Locale("es", "PE"), "+ S/ %.2f", tx.amount)
                    } else {
                        String.format(Locale("es", "PE"), "+ S/ %.2f (Excl)", tx.amount)
                    }
                    canvas.drawText(formattedAmount, pageWidth - margin - 14f, itemY, textPaint)
                    textPaint.textAlign = Paint.Align.LEFT

                    currentY += rowHeight
                }
            }

            // Footer on last page
            val footerPaint = Paint().apply {
                color = Color.parseColor("#9CA3AF")
                textSize = 8f
                isAntiAlias = true
            }
            val genDateStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("es", "PE")).format(Date())
            canvas.drawText("Reporte Oficial • $genDateStr • Solo ventas válidas de tienda sumadas al balance", margin, pageHeight - margin + 10f, footerPaint)

            document.finishPage(page)

            // Save PDF
            val reportDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val cleanDate = dateString.replace("[^a-zA-Z0-9]".toRegex(), "_")
            val pdfFile = File(reportDir, "Reporte_Ventas_${cleanDate}.pdf")

            FileOutputStream(pdfFile).use { fos ->
                document.writeTo(fos)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Reporte PDF Ventas - $storeName ($dateString)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Exportar / Imprimir Reporte PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error al generar PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        } finally {
            document.close()
        }
    }
}
