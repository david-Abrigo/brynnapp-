package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.YapeTransaction
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ModernNeonLime
import com.example.ui.theme.ModernMatteBlack
import com.example.ui.theme.ModernBorderLight
import com.example.ui.theme.SleekBorderLight
import com.example.ui.theme.SleekErrorRed
import com.example.ui.theme.SleekMidnightNavy
import com.example.ui.theme.SleekPrimaryBlue
import com.example.ui.theme.SleekPrimaryContainer
import com.example.ui.theme.SleekSecondaryContainer
import com.example.ui.theme.SleekSubtleTextLight
import com.example.ui.theme.SleekSuccessGreen
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionItemCard(
    transaction: YapeTransaction,
    onDelete: (() -> Unit)? = null,
    onSpeakAgain: (() -> Unit)? = null,
    onEditNote: (() -> Unit)? = null,
    canEditNote: Boolean = true,
    onToggleStoreStatus: ((isStore: Boolean, reason: String) -> Unit)? = null,
    onConfirmPayment: (() -> Unit)? = null,
    onUnconfirmPayment: (() -> Unit)? = null,
    canUnconfirm: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }

    val isStore = transaction.isStoreTransaction
    val isConfirmed = transaction.isConfirmed

    val initials = remember(transaction.senderName) {
        val parts = transaction.senderName.trim().split(" ")
        if (parts.size >= 2) {
            "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
        } else {
            transaction.senderName.take(2).uppercase()
        }
    }

    // Dynamic avatar color based on name hash for visual rhythm
    val (avatarBg, avatarText) = remember(transaction.senderName, isStore) {
        if (!isStore) {
            Pair(Color(0xFFF59E0B), Color.White) // Amber for non-store / unknown
        } else {
            val hash = abs(transaction.senderName.hashCode()) % 4
            when (hash) {
                0 -> Pair(Color(0xFFBA1A1A), Color.White)
                1 -> Pair(Color(0xFF0061A4), Color.White)
                2 -> Pair(Color(0xFF0284C7), Color.White)
                else -> Pair(Color(0xFF16A34A), Color.White)
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transaction_card_${transaction.id}")
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isStore) {
                AppTheme.cardBackground
            } else {
                if (AppTheme.isDark) Color(0xFF261D11) else Color(0xFFFFFBEB)
            }
        ),
        border = if (!isStore) {
            androidx.compose.foundation.BorderStroke(1.dp, if (AppTheme.isDark) Color(0xFF78350F) else Color(0xFFFDE68A))
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder)
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Avatar with Initials or Warning Icon
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isStore) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Yape Desconocido",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Text(
                            text = initials,
                            color = avatarText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Sender Name and Time / Tag
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isPlin = remember(transaction.rawNotification) {
                            transaction.rawNotification.contains("plin", ignoreCase = true)
                        }
                        // Quitar la etiqueta de YAPE si los nombres no llevan ningún asterisco
                        val shouldShowTag = isPlin || transaction.hasAsterisk

                        if (shouldShowTag) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isPlin) Color(0xFF00C2C7) else Color(0xFF742284)
                            ) {
                                Text(
                                    text = if (isPlin) "PLIN" else "YAPE",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Text(
                            text = transaction.senderName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isStore) AppTheme.textPrimary else (if (AppTheme.isDark) Color(0xFFFBBF24) else Color(0xFF92400E)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))

                    if (!isStore) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (AppTheme.isDark) Color(0xFF451A03) else Color(0xFFFEF3C7),
                            modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                        ) {
                            Text(
                                text = "⚠️ YAPE DESCONOCIDO • NO SUMA EN CAJA",
                                color = if (AppTheme.isDark) Color(0xFFFCD34D) else Color(0xFFB45309),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isConfirmed) {
                                if (AppTheme.isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)
                            } else {
                                if (AppTheme.isDark) Color(0xFF451A03).copy(alpha = 0.5f) else Color(0xFFFEF3C7).copy(alpha = 0.7f)
                            },
                            modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = if (isConfirmed) Icons.Default.CheckCircle else Icons.Default.PendingActions,
                                    contentDescription = null,
                                    tint = if (isConfirmed) SleekSuccessGreen else Color(0xFFF59E0B),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                val whoBadge = transaction.claimedByName.ifBlank { transaction.claimedBy }
                                Text(
                                    text = if (isConfirmed) {
                                        if (whoBadge.isNotBlank()) "CONFIRMADO • ${whoBadge.uppercase()}" else "CONFIRMADO"
                                    } else "POR CONFIRMAR",
                                    color = if (isConfirmed) {
                                        if (AppTheme.isDark) Color(0xFF34D399) else Color(0xFF15803D)
                                    } else {
                                        if (AppTheme.isDark) Color(0xFFFCD34D) else Color(0xFFB45309)
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    val confirmedSubtitle = buildString {
                        append(transaction.formattedTime)
                        if (isStore) {
                            if (isConfirmed) {
                                append(" • Confirmado")
                                val who = transaction.claimedByName.ifBlank { transaction.claimedBy }
                                if (who.isNotBlank()) {
                                    append(" por $who")
                                }
                                if (transaction.branchName.isNotBlank()) {
                                    append(" (${transaction.branchName})")
                                }
                            } else {
                                append(" • Venta de tienda")
                            }
                        } else {
                            append(" • ${transaction.exclusionReason.ifBlank { "Desconocido" }}")
                        }
                    }
                    Text(
                        text = confirmedSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isStore) AppTheme.textSecondary else (if (AppTheme.isDark) Color(0xFFF59E0B) else Color(0xFFB45309))
                    )
                    if (transaction.note.isNotBlank()) {
                        Text(
                            text = "📝 ${transaction.note}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (AppTheme.isDark) ModernNeonLime else Color(0xFF047857)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Amount Badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "+ ${transaction.formattedAmount}",
                        color = if (isStore) AppTheme.textPrimary else (if (AppTheme.isDark) Color(0xFFFBBF24) else Color(0xFFB45309)),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (!isStore) {
                        Text(
                            text = "(Excluido de caja)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (AppTheme.isDark) Color(0xFFF59E0B) else Color(0xFFD97706)
                        )
                    }
                }
            }

            // Expanded details
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    if (transaction.rawNotification.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AppTheme.pillBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.pillBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Texto capturado",
                                    tint = AppTheme.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Notificación: \"${transaction.rawNotification}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppTheme.textSecondary
                                    )
                                    if (transaction.exclusionReason.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Motivo de exclusión: ${transaction.exclusionReason}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (AppTheme.isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
                                        )
                                    }
                                    if (transaction.note.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Producto / Detalle: ${transaction.note}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (AppTheme.isDark) ModernNeonLime else Color(0xFF047857)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Action buttons (Confirmar Pago + Marcar como Desconocido)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            // Botón Confirmar Pago / Desmarcar Pago (Trabajadores) o Estado Informativo (Dueño / Emisor)
                            if (isStore) {
                                if (onConfirmPayment != null || onUnconfirmPayment != null) {
                                    if (!isConfirmed) {
                                        val confirmBg = if (AppTheme.isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)
                                        val confirmBorder = if (AppTheme.isDark) Color(0xFF059669) else Color(0xFF86EFAC)
                                        val confirmContentColor = if (AppTheme.isDark) Color(0xFF34D399) else SleekSuccessGreen

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = confirmBg,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, confirmBorder),
                                            modifier = Modifier.clickable {
                                                onConfirmPayment?.invoke()
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = confirmContentColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Confirmar Pago",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = confirmContentColor
                                                )
                                            }
                                        }
                                    } else if (canUnconfirm) {
                                        val confirmBg = if (AppTheme.isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                                        val confirmBorder = if (AppTheme.isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                                        val confirmContentColor = if (AppTheme.isDark) Color(0xFF94A3B8) else Color(0xFF475569)

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = confirmBg,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, confirmBorder),
                                            modifier = Modifier.clickable {
                                                onUnconfirmPayment?.invoke()
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = confirmContentColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Desmarcar Pago",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = confirmContentColor
                                                )
                                            }
                                        }
                                    } else {
                                        // Confirmado por otro trabajador: sólo informativo / protegido contra cambios no autorizados
                                        val whoClaimed = transaction.claimedByName.ifBlank { transaction.claimedBy.ifBlank { "otro trabajador" } }
                                        val branchInfo = if (transaction.branchName.isNotBlank()) " (${transaction.branchName})" else ""
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (AppTheme.isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFF8FAFC),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (AppTheme.isDark) Color(0xFF334155).copy(alpha = 0.6f) else Color(0xFFE2E8F0)
                                            ),
                                            modifier = Modifier.clickable {
                                                Toast.makeText(
                                                    context,
                                                    "Solo el trabajador que confirmó este cobro ($whoClaimed) puede desmarcarlo",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = SleekSuccessGreen,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Confirmado por $whoClaimed$branchInfo",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = AppTheme.textSecondary
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Modo Dueño / Solo lectura: Mostrar quién confirmó la compra sin botones interactivos
                                    if (isConfirmed) {
                                        val whoClaimed = transaction.claimedByName.ifBlank { transaction.claimedBy.ifBlank { "un trabajador" } }
                                        val branchInfo = if (transaction.branchName.isNotBlank()) " (${transaction.branchName})" else ""
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (AppTheme.isDark) Color(0xFF064E3B).copy(alpha = 0.4f) else Color(0xFFDCFCE7),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (AppTheme.isDark) Color(0xFF059669).copy(alpha = 0.6f) else Color(0xFF86EFAC)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = SleekSuccessGreen,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Confirmado por $whoClaimed$branchInfo",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (AppTheme.isDark) Color(0xFF34D399) else Color(0xFF15803D)
                                                )
                                            }
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (AppTheme.isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFF1F5F9),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (AppTheme.isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFCBD5E1)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PendingActions,
                                                    contentDescription = null,
                                                    tint = AppTheme.textSecondary,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Sin confirmar por trabajador",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = AppTheme.textSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Toggle Store / Non-Store Action Button (solo si no es nulo)
                            if (onToggleStoreStatus != null) {
                                val btnBg = if (isStore) {
                                    if (AppTheme.isDark) Color(0xFF2E1A1A) else Color(0xFFFFFBEB)
                                } else {
                                    if (AppTheme.isDark) Color(0xFF142B1F) else Color(0xFFDCFCE7)
                                }
                                val btnBorder = if (isStore) {
                                    if (AppTheme.isDark) Color(0xFF7F1D1D) else Color(0xFFFDE68A)
                                } else {
                                    if (AppTheme.isDark) Color(0xFF166534) else Color(0xFF86EFAC)
                                }
                                val btnContentColor = if (isStore) {
                                    if (AppTheme.isDark) Color(0xFFF87171) else Color(0xFFB45309)
                                } else {
                                    if (AppTheme.isDark) Color(0xFF4ADE80) else SleekSuccessGreen
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = btnBg,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, btnBorder),
                                    modifier = Modifier
                                        .clickable {
                                            if (isStore) {
                                                if (isConfirmed && !canUnconfirm) {
                                                    val whoClaimed = transaction.claimedByName.ifBlank { "el trabajador que lo confirmó" }
                                                    Toast.makeText(
                                                        context,
                                                        "Solo $whoClaimed puede modificar o excluir este cobro",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    showTagDialog = true
                                                }
                                            } else {
                                                onToggleStoreStatus(true, "")
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isStore) Icons.Default.Block else Icons.Default.Store,
                                            contentDescription = null,
                                            tint = btnContentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isStore) "Marcar como Desconocido" else "Válido para Tienda",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = btnContentColor
                                        )
                                    }
                                }
                            }
                        }

                        // Right actions (Editar detalle de producto con lápiz, solo visible si onEditNote != null)
                        if (onEditNote != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (canEditNote) {
                                    if (AppTheme.isDark) ModernNeonLime else Color(0xFFEFF6FF)
                                } else {
                                    if (AppTheme.isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                IconButton(
                                    onClick = { onEditNote.invoke() },
                                    modifier = Modifier.testTag("edit_note_button_${transaction.id}")
                                ) {
                                    Icon(
                                        imageVector = if (canEditNote) Icons.Default.Edit else Icons.Default.Lock,
                                        contentDescription = if (canEditNote) "Editar detalle de producto" else "Detalle protegido por autor",
                                        tint = if (canEditNote) {
                                            if (AppTheme.isDark) ModernMatteBlack else SleekPrimaryBlue
                                        } else {
                                            Color(0xFF94A3B8)
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal to choose exclusion reason when marking as unknown/non-store
    if (showTagDialog && onToggleStoreStatus != null) {
        val reasons = listOf(
            "Yape Personal (No es del negocio)",
            "Yape Desconocido / No identificado",
            "Pago duplicado o erróneo",
            "Propina / Donación particular",
            "Otro motivo no comercial"
        )
        var selectedReason by remember { mutableStateOf(reasons[0]) }

        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = {
                Text(
                    text = "Marcar como Desconocido / Excluir",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = AppTheme.textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Este Yape de ${transaction.formattedAmount} de ${transaction.senderName} quedará registrado para auditoría pero será etiquetado como 'Desconocido' y EXCLUIDO de los totales de caja.",
                        fontSize = 13.sp,
                        color = AppTheme.textSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Selecciona el motivo:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (AppTheme.isDark) ModernNeonLime else SleekMidnightNavy
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    reasons.forEach { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = reason,
                                fontSize = 12.sp,
                                color = AppTheme.textPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTagDialog = false
                        onToggleStoreStatus(false, selectedReason)
                    }
                ) {
                    Text(
                        text = "Marcar Desconocido",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showTagDialog = false }) {
                    Text("Cancelar", color = AppTheme.textSecondary)
                }
            }
        )
    }
}


