package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

import com.example.ui.theme.AppTheme
import com.example.ui.theme.ModernNeonLime
import com.example.ui.theme.ModernMatteBlack
import com.example.ui.theme.ModernOnNeonLime
import com.example.ui.theme.SleekErrorContainer
import com.example.ui.theme.SleekErrorRed
import com.example.ui.theme.SleekOnErrorContainer
import com.example.ui.theme.SleekSuccessGreen
import com.example.ui.viewmodel.MainViewModel

enum class AuthMode {
    LOGIN,
    REGISTER,
    FORGOT_PASSWORD
}

@Composable
fun AuthDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var mode by remember { mutableStateOf(AuthMode.LOGIN) }
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

    var showGoogleAccountPicker by remember { mutableStateOf(false) }
    var googleCustomEmail by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
                    val gEmail = googleIdTokenCredential.id
                    val gDisplayName = googleIdTokenCredential.displayName ?: gEmail.substringBefore("@")
                    val gPhoto = googleIdTokenCredential.profilePictureUri?.toString() ?: ""
                    val gIdToken = googleIdTokenCredential.idToken

                    viewModel.signInWithGoogle(gEmail, gDisplayName, gPhoto, gIdToken) { success, err ->
                        isLoading = false
                        if (success) {
                            onDismiss()
                        } else {
                            errorMessage = err ?: "Error al autenticar con Google"
                        }
                    }
                } else {
                    isLoading = false
                    errorMessage = "Tipo de credencial no reconocido"
                }
            } catch (e: GetCredentialCancellationException) {
                // Usuario canceló la selección, no mostrar error estridente
                isLoading = false
            } catch (e: GetCredentialException) {
                isLoading = false
                // Si falla en emulador o dispositivo sin Google Play Services, dar alternativa
                errorMessage = "Error de Google Play Services: ${e.message ?: "Cancelado"}"
                showGoogleAccountPicker = true
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.message ?: "No se pudo conectar con Google"
                showGoogleAccountPicker = true
            }
        }
    }


    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("auth_dialog_card"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (mode == AuthMode.FORGOT_PASSWORD) {
                        IconButton(onClick = { mode = AuthMode.LOGIN; errorMessage = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (mode) {
                                AuthMode.LOGIN -> "Iniciar Sesión"
                                AuthMode.REGISTER -> "Crear Cuenta"
                                AuthMode.FORGOT_PASSWORD -> "Recuperar Acceso"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Brynn Cloud",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppTheme.textSecondary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = AppTheme.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Selector (Login / Register)
                if (mode != AuthMode.FORGOT_PASSWORD) {
                    TabRow(
                        selectedTabIndex = if (mode == AuthMode.LOGIN) 0 else 1,
                        containerColor = AppTheme.pillBackground,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp)),
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[if (mode == AuthMode.LOGIN) 0 else 1]),
                                color = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                                height = 3.dp
                            )
                        }
                    ) {
                        Tab(
                            selected = mode == AuthMode.LOGIN,
                            onClick = { mode = AuthMode.LOGIN; errorMessage = null; successMessage = null },
                            text = {
                                Text(
                                    "INGRESAR",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (mode == AuthMode.LOGIN) {
                                        if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack
                                    } else {
                                        AppTheme.textSecondary
                                    }
                                )
                            }
                        )
                        Tab(
                            selected = mode == AuthMode.REGISTER,
                            onClick = { mode = AuthMode.REGISTER; errorMessage = null; successMessage = null },
                            text = {
                                Text(
                                    "REGISTRARSE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (mode == AuthMode.REGISTER) {
                                        if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack
                                    } else {
                                        AppTheme.textSecondary
                                    }
                                )
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // Error / Success Message
                AnimatedVisibility(visible = errorMessage != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = SleekErrorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = SleekOnErrorContainer,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = successMessage != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFDCFCE7)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SleekSuccessGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = successMessage ?: "",
                                color = Color(0xFF166534),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Google Button (Fast Login)
                if (mode != AuthMode.FORGOT_PASSWORD) {
                    OutlinedButton(
                        onClick = { launchGoogleSignIn() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("google_signin_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.cardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Stylized G logo
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEA4335).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("G", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFFEA4335))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Continuar con Google",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = AppTheme.cardBorder)
                        Text(
                            text = "  o con tu correo  ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppTheme.textSecondary
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = AppTheme.cardBorder)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Fields based on Mode
                if (mode == AuthMode.REGISTER) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Nombre o Dueño del Negocio") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = AppTheme.textSecondary) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_name_input"),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = whatsapp,
                        onValueChange = { input -> whatsapp = input.filter { it.isDigit() || it == '+' || it == ' ' } },
                        label = { Text("WhatsApp / Celular (Ej: 987654321)") },
                        placeholder = { Text("Para soporte y notificaciones") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = AppTheme.textSecondary) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_whatsapp_input"),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = { Text("Correo Electrónico") },
                    placeholder = { Text("ejemplo@negocio.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = AppTheme.textSecondary) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_email_input"),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = if (mode == AuthMode.FORGOT_PASSWORD) ImeAction.Done else ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        onDone = { focusManager.clearFocus() }
                    )
                )

                if (mode != AuthMode.FORGOT_PASSWORD) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AppTheme.textSecondary) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Ocultar" else "Mostrar",
                                    tint = AppTheme.textSecondary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input"),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (mode == AuthMode.REGISTER) ImeAction.Next else ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            onDone = { focusManager.clearFocus() }
                        )
                    )
                }

                if (mode == AuthMode.REGISTER) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Confirm Password Field
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMessage = null },
                        label = { Text("Confirmar Contraseña") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AppTheme.textSecondary) },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (confirmPasswordVisible) "Ocultar" else "Mostrar",
                                    tint = AppTheme.textSecondary
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_confirm_password_input"),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                }

                if (mode == AuthMode.LOGIN) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { mode = AuthMode.FORGOT_PASSWORD; errorMessage = null }) {
                            Text(
                                "¿Olvidaste tu contraseña?",
                                fontSize = 12.sp,
                                color = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // Submit Button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        when (mode) {
                            AuthMode.LOGIN -> {
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Por favor ingresa tu correo y contraseña"
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                viewModel.signInWithEmail(email, password) { success, err ->
                                    isLoading = false
                                    if (success) {
                                        onDismiss()
                                    } else {
                                        errorMessage = err ?: "Credenciales incorrectas"
                                    }
                                }
                            }
                            AuthMode.REGISTER -> {
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Por favor completa todos los campos"
                                    return@Button
                                }
                                if (password.length < 6) {
                                    errorMessage = "La contraseña debe tener al menos 6 caracteres"
                                    return@Button
                                }
                                if (password != confirmPassword) {
                                    errorMessage = "Las contraseñas no coinciden"
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                viewModel.signUpWithEmail(email, password, displayName, whatsapp) { success, err ->
                                    isLoading = false
                                    if (success) {
                                        onDismiss()
                                    } else {
                                        errorMessage = err ?: "No se pudo registrar la cuenta"
                                    }
                                }
                            }
                            AuthMode.FORGOT_PASSWORD -> {
                                if (email.isBlank()) {
                                    errorMessage = "Ingresa tu correo para enviarte el enlace"
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                viewModel.sendPasswordReset(email) { success, msg ->
                                    isLoading = false
                                    if (success) {
                                        successMessage = "¡Enviado! Revisa tu bandeja de entrada."
                                    } else {
                                        errorMessage = msg ?: "Error al enviar correo"
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("auth_submit_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                        contentColor = if (AppTheme.isDark) ModernOnNeonLime else Color.White
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = if (AppTheme.isDark) ModernOnNeonLime else Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = when (mode) {
                                AuthMode.LOGIN -> "Iniciar Sesión"
                                AuthMode.REGISTER -> "Crear Cuenta Segura"
                                AuthMode.FORGOT_PASSWORD -> "Enviar Enlace de Recuperación"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }


    // Google Quick Account Select Dialog
    if (showGoogleAccountPicker) {
        Dialog(onDismissRequest = { showGoogleAccountPicker = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Seleccionar cuenta de Google",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Elige la cuenta con la que deseas vincular tu negocio",
                        fontSize = 12.sp,
                        color = AppTheme.textSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Preset accounts or Custom
                    val defaultGoogleAccounts = listOf(
                        Triple("Dueño de Negocio", "dueno.yape@gmail.com", "D"),
                        Triple("Cajero Principal", "caja.notiyape@gmail.com", "C")
                    )

                    defaultGoogleAccounts.forEach { (name, gEmail, letter) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.signInWithGoogle(gEmail, name) { success, _ ->
                                        showGoogleAccountPicker = false
                                        if (success) onDismiss()
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    letter,
                                    fontWeight = FontWeight.Bold,
                                    color = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = gEmail,
                                    fontSize = 12.sp,
                                    color = AppTheme.textSecondary
                                )
                            }
                        }
                        HorizontalDivider(color = AppTheme.cardBorder.copy(alpha = 0.5f))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Custom Google account input
                    OutlinedTextField(
                        value = googleCustomEmail,
                        onValueChange = { googleCustomEmail = it },
                        label = { Text("O escribe tu correo de Google") },
                        placeholder = { Text("tu.correo@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (googleCustomEmail.isNotBlank()) {
                                val name = googleCustomEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                                viewModel.signInWithGoogle(googleCustomEmail, name) { success, _ ->
                                    showGoogleAccountPicker = false
                                    if (success) onDismiss()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                            contentColor = if (AppTheme.isDark) ModernOnNeonLime else Color.White
                        ),
                        enabled = googleCustomEmail.isNotBlank()
                    ) {
                        Text("Acceder con esta cuenta", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
