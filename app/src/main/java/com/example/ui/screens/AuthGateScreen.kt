package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ModernMatteBlack
import com.example.ui.theme.ModernNeonLime
import com.example.ui.theme.ModernOnNeonLime
import com.example.ui.theme.SleekErrorRed
import com.example.ui.viewmodel.MainViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

private enum class GateMode {
    LOGIN,
    REGISTER,
    FORGOT_PASSWORD
}

/**
 * Pantalla completa de acceso obligatorio.
 * La app no permite entrar ni interactuar a menos que el usuario inicie sesión formalmente.
 */
@Composable
fun AuthGateScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var mode by remember { mutableStateOf(GateMode.LOGIN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val googleClientId = "687610613063-i3u2768obf66tbscd9en6n16n86tf136.apps.googleusercontent.com"

    fun launchGoogleSignIn() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(googleClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                val credential = result.credential
                if (credential is androidx.credentials.CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    viewModel.signInWithGoogle(
                        email = googleIdTokenCredential.id,
                        name = googleIdTokenCredential.displayName ?: "",
                        photo = googleIdTokenCredential.profilePictureUri?.toString() ?: "",
                        idToken = googleIdTokenCredential.idToken
                    ) { success, msg ->
                        isLoading = false
                        if (success) {
                            Toast.makeText(context, "¡Bienvenido, ${googleIdTokenCredential.displayName ?: ""}!", Toast.LENGTH_SHORT).show()
                        } else {
                            errorMessage = msg ?: "Error al autenticar con Google"
                        }
                    }
                } else {
                    isLoading = false
                    errorMessage = "Credencial de Google no reconocida"
                }
            } catch (e: GetCredentialCancellationException) {
                isLoading = false
            } catch (e: GetCredentialException) {
                isLoading = false
                errorMessage = "Error de Google Play Services: ${e.message}"
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "Error al iniciar sesión con Google: ${e.localizedMessage}"
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Brynn • NotiYape",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Inicia sesión para comenzar a cantar cobros y vincular dispositivos",
                fontSize = 13.sp,
                color = AppTheme.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header de Modo o Volver
                    if (mode == GateMode.FORGOT_PASSWORD) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { mode = GateMode.LOGIN; errorMessage = null }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Recuperar Contraseña",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = AppTheme.textPrimary
                            )
                        }
                    } else {
                        // Selector de Tabs: Iniciar Sesión vs Crear Cuenta
                        TabRow(
                            selectedTabIndex = if (mode == GateMode.LOGIN) 0 else 1,
                            containerColor = AppTheme.pillBackground,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[if (mode == GateMode.LOGIN) 0 else 1]),
                                    color = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack
                                )
                            }
                        ) {
                            Tab(
                                selected = mode == GateMode.LOGIN,
                                onClick = { mode = GateMode.LOGIN; errorMessage = null },
                                text = {
                                    Text(
                                        "Iniciar Sesión",
                                        fontWeight = if (mode == GateMode.LOGIN) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            )
                            Tab(
                                selected = mode == GateMode.REGISTER,
                                onClick = { mode = GateMode.REGISTER; errorMessage = null },
                                text = {
                                    Text(
                                        "Crear Cuenta",
                                        fontWeight = if (mode == GateMode.REGISTER) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Botón Google Sign-In (si no es forgot password)
                    if (mode != GateMode.FORGOT_PASSWORD) {
                        OutlinedButton(
                            onClick = { launchGoogleSignIn() },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "G",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = Color(0xFF4285F4)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (mode == GateMode.LOGIN) "Continuar con Google" else "Registrarse con Google",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = AppTheme.textPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = AppTheme.cardBorder)
                            Text(
                                text = "  o con correo  ",
                                fontSize = 11.sp,
                                color = AppTheme.textSecondary
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = AppTheme.cardBorder)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Campo Nombre (solo en registro)
                    if (mode == GateMode.REGISTER) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Tu Nombre Completo") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = AppTheme.textSecondary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = whatsapp,
                            onValueChange = { whatsapp = it },
                            label = { Text("WhatsApp (Opcional)") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = AppTheme.textSecondary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Campo Correo Electrónico
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        label = { Text("Correo Electrónico") },
                        placeholder = { Text("ejemplo@correo.com") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = AppTheme.textSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = if (mode == GateMode.FORGOT_PASSWORD) ImeAction.Done else ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    // Campo Contraseña
                    if (mode != GateMode.FORGOT_PASSWORD) {
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; errorMessage = null },
                            label = { Text("Contraseña") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AppTheme.textSecondary) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = AppTheme.textSecondary
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = if (mode == GateMode.LOGIN) ImeAction.Done else ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            )
                        )
                    }

                    // Confirmar Contraseña (solo en registro)
                    if (mode == GateMode.REGISTER) {
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; errorMessage = null },
                            label = { Text("Confirmar Contraseña") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AppTheme.textSecondary) },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = AppTheme.textSecondary
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                        )
                    }

                    // Olvidé mi contraseña
                    if (mode == GateMode.LOGIN) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { mode = GateMode.FORGOT_PASSWORD; errorMessage = null }
                            ) {
                                Text(
                                    text = "¿Olvidaste tu contraseña?",
                                    fontSize = 11.sp,
                                    color = AppTheme.textSecondary
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Feedback de Error
                    AnimatedVisibility(visible = errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SleekErrorRed.copy(alpha = 0.12f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = SleekErrorRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Feedback de Éxito
                    AnimatedVisibility(visible = successMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF16A34A).copy(alpha = 0.12f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = successMessage ?: "",
                                color = Color(0xFF16A34A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Botón Principal
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            errorMessage = null
                            successMessage = null

                            val cleanEmail = email.trim()
                            if (cleanEmail.isBlank()) {
                                errorMessage = "Ingresa tu correo electrónico"
                                return@Button
                            }

                            when (mode) {
                                GateMode.LOGIN -> {
                                    if (password.isBlank()) {
                                        errorMessage = "Ingresa tu contraseña"
                                        return@Button
                                    }
                                    isLoading = true
                                    viewModel.signInWithEmail(cleanEmail, password) { success, msg ->
                                        isLoading = false
                                        if (success) {
                                            Toast.makeText(context, "¡Sesión iniciada!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            errorMessage = msg ?: "Error al iniciar sesión"
                                        }
                                    }
                                }
                                GateMode.REGISTER -> {
                                    if (password.length < 6) {
                                        errorMessage = "La contraseña debe tener al menos 6 caracteres"
                                        return@Button
                                    }
                                    if (password != confirmPassword) {
                                        errorMessage = "Las contraseñas no coinciden"
                                        return@Button
                                    }
                                    isLoading = true
                                    viewModel.signUpWithEmail(cleanEmail, password, displayName, whatsapp) { success, msg ->
                                        isLoading = false
                                        if (success) {
                                            Toast.makeText(context, "¡Cuenta creada exitosamente!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            errorMessage = msg ?: "Error al registrar cuenta"
                                        }
                                    }
                                }
                                GateMode.FORGOT_PASSWORD -> {
                                    isLoading = true
                                    viewModel.sendPasswordReset(cleanEmail) { success, msg ->
                                        isLoading = false
                                        if (success) {
                                            successMessage = "Revisa tu correo para restablecer la contraseña"
                                        } else {
                                            errorMessage = msg ?: "No se pudo enviar el correo de recuperación"
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                            contentColor = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = when (mode) {
                                    GateMode.LOGIN -> "Entrar a la Aplicación"
                                    GateMode.REGISTER -> "Crear mi Cuenta"
                                    GateMode.FORGOT_PASSWORD -> "Enviar Enlace de Recuperación"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
