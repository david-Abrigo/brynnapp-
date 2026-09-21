package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ModernMatteBlack
import com.example.ui.theme.ModernNeonLime
import com.example.ui.theme.ModernOnNeonLime
import com.example.ui.theme.SleekErrorRed

@Composable
fun WorkerRevokedDialog(
    storeName: String,
    onExitToModeSelection: () -> Unit
) {
    val displayStore = if (storeName.isNotBlank() && storeName != "Sin tienda vinculada") storeName else "el negocio"

    AlertDialog(
        onDismissRequest = { /* Bloqueado: Debe presionar el botón de salida obligatoriamente */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        containerColor = AppTheme.cardBackground,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Surface(
                shape = CircleShape,
                color = SleekErrorRed.copy(alpha = 0.15f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = SleekErrorRed,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = "Acceso al Negocio Finalizado",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = AppTheme.textPrimary,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "El dueño de '$displayStore' ha finalizado la vinculación de este dispositivo como receptor.",
                    fontSize = 13.sp,
                    color = AppTheme.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SleekErrorRed.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, SleekErrorRed.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SleekErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Este teléfono ya no recibirá notificaciones ni alertas de cobro de esta tienda.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SleekErrorRed,
                            lineHeight = 15.sp
                        )
                    }
                }

                Text(
                    text = "Presiona el botón a continuación para salir y elegir tu nuevo modo de uso (vincularte a otra tienda o usar la app por tu cuenta).",
                    fontSize = 12.sp,
                    color = AppTheme.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onExitToModeSelection,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                    contentColor = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Salir y Elegir Modo de Uso",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }
        }
    )
}
