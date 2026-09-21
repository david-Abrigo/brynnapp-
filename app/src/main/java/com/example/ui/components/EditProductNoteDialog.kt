package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.YapeTransaction
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ModernNeonLime
import com.example.ui.theme.ModernMatteBlack
import com.example.ui.theme.SleekPrimaryBlue
import com.example.ui.theme.SleekSuccessGreen

@Composable
fun EditProductNoteDialog(
    transaction: YapeTransaction,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var noteText by remember(transaction.id) { mutableStateOf(transaction.note) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.cardBackground,
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (AppTheme.isDark) ModernNeonLime.copy(alpha = 0.2f) else SleekPrimaryBlue.copy(alpha = 0.12f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = if (AppTheme.isDark) ModernNeonLime else SleekPrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Detalle del Producto",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = AppTheme.textPrimary
                    )
                    Text(
                        text = "${transaction.senderName} • ${transaction.formattedAmount}",
                        fontSize = 12.sp,
                        color = AppTheme.textSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Añade o modifica una etiqueta para identificar el producto o pedido de esta venta:",
                    fontSize = 13.sp,
                    color = AppTheme.textSecondary,
                    lineHeight = 18.sp
                )

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = {
                        Text(
                            text = "Ej. Pollo a la brasa, 2 gaseosas...",
                            fontSize = 13.sp,
                            color = AppTheme.textSecondary.copy(alpha = 0.7f)
                        )
                    },
                    singleLine = true,
                    maxLines = 1,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = AppTheme.textPrimary,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_product_note_input"),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (noteText.isNotBlank()) {
                            IconButton(
                                onClick = { noteText = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpiar texto",
                                    tint = AppTheme.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (AppTheme.isDark) Color(0xFF151922) else Color.White,
                        unfocusedContainerColor = if (AppTheme.isDark) Color(0xFF151922) else Color.White,
                        focusedBorderColor = if (AppTheme.isDark) ModernNeonLime else SleekPrimaryBlue,
                        unfocusedBorderColor = if (AppTheme.isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(noteText) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (AppTheme.isDark) ModernNeonLime else SleekPrimaryBlue,
                    contentColor = if (AppTheme.isDark) ModernMatteBlack else Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_product_note_button")
            ) {
                Text(
                    text = "Guardar",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Cancelar",
                    color = AppTheme.textSecondary,
                    fontSize = 13.sp
                )
            }
        }
    )
}
