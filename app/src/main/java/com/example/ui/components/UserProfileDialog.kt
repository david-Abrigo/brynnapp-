package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AuthProvider
import com.example.ui.theme.SleekBorderLight
import com.example.ui.theme.SleekErrorContainer
import com.example.ui.theme.SleekErrorRed
import com.example.ui.theme.SleekMidnightNavy
import com.example.ui.theme.SleekOnErrorContainer
import com.example.ui.theme.SleekPrimaryBlue
import com.example.ui.theme.SleekPrimaryContainer
import com.example.ui.theme.SleekSubtleTextLight
import com.example.ui.theme.SleekSuccessGreen
import com.example.ui.viewmodel.MainViewModel

@Composable
fun UserProfileDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onOpenAuth: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val storeConfig by viewModel.storeConfig.collectAsStateWithLifecycle()

    var showSignOutConfirm by remember { mutableStateOf(false) }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Cerrar Sesión", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que deseas salir de tu cuenta de Brynn?") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirm = false
                        viewModel.signOut()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekErrorRed)
                ) {
                    Text("Cerrar Sesión", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text("Cancelar", color = SleekSubtleTextLight)
                }
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("user_profile_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mi Cuenta",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Avatar Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            when (currentUser.provider) {
                                AuthProvider.GOOGLE -> Color(0xFFEA4335).copy(alpha = 0.15f)
                                AuthProvider.EMAIL -> SleekPrimaryContainer
                                AuthProvider.GUEST -> Color.Gray.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentUser.initial,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = when (currentUser.provider) {
                            AuthProvider.GOOGLE -> Color(0xFFEA4335)
                            AuthProvider.EMAIL -> SleekMidnightNavy
                            AuthProvider.GUEST -> Color.DarkGray
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentUser.displayName.ifBlank { "Usuario" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = currentUser.email.ifBlank { "Modo sin cuenta" },
                    fontSize = 13.sp,
                    color = SleekSubtleTextLight
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Provider Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (currentUser.provider) {
                        AuthProvider.GOOGLE -> Color(0xFFEA4335).copy(alpha = 0.12f)
                        AuthProvider.EMAIL -> SleekPrimaryContainer
                        AuthProvider.GUEST -> Color.Gray.copy(alpha = 0.12f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (currentUser.provider) {
                                AuthProvider.GOOGLE -> Icons.Default.Security
                                AuthProvider.EMAIL -> Icons.Default.VerifiedUser
                                AuthProvider.GUEST -> Icons.Default.Person
                            },
                            contentDescription = null,
                            tint = when (currentUser.provider) {
                                AuthProvider.GOOGLE -> Color(0xFFEA4335)
                                AuthProvider.EMAIL -> SleekMidnightNavy
                                AuthProvider.GUEST -> Color.DarkGray
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (currentUser.provider) {
                                AuthProvider.GOOGLE -> "Cuenta Google"
                                AuthProvider.EMAIL -> "Correo Verificado"
                                AuthProvider.GUEST -> "Modo Invitado"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (currentUser.provider) {
                                AuthProvider.GOOGLE -> Color(0xFFEA4335)
                                AuthProvider.EMAIL -> SleekMidnightNavy
                                AuthProvider.GUEST -> Color.DarkGray
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Connected Business Details Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderLight)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Business, contentDescription = null, tint = SleekPrimaryBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Negocio Activo:", fontSize = 12.sp, color = SleekSubtleTextLight)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(storeConfig.storeName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = SleekSuccessGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Modo Activo:", fontSize = 12.sp, color = SleekSubtleTextLight)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(storeConfig.deviceRole.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekMidnightNavy)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (currentUser.isLoggedIn) {
                    // Sign out button
                    OutlinedButton(
                        onClick = { showSignOutConfirm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sign_out_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekErrorRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SleekErrorRed.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cerrar Sesión", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            onDismiss()
                            onOpenAuth()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekMidnightNavy)
                    ) {
                        Text("Iniciar Sesión / Registrarse", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
