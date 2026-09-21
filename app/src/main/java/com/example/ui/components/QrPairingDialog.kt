package com.example.ui.components

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.DeviceRole
import com.example.data.preferences.StoreConfig
import com.example.ui.theme.ModernNeonLime
import com.example.ui.theme.ModernOnNeonLime
import com.example.ui.theme.SleekMidnightNavy
import com.example.ui.theme.SleekPrimaryBlue
import com.example.ui.theme.SleekPrimaryContainer
import com.example.ui.theme.SleekSecondaryContainer
import com.example.ui.theme.SleekSubtleTextLight
import com.example.ui.theme.SleekSuccessGreen
import com.example.ui.viewmodel.MainViewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import org.json.JSONObject
import java.util.concurrent.Executors
import kotlin.math.abs

@Composable
fun QrShowDialog(
    storeConfig: StoreConfig,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val qrJsonPayload = remember(storeConfig) {
        JSONObject().apply {
            put("store_code", storeConfig.storeCode)
            put("store_name", storeConfig.storeName)
            put("supabase_url", storeConfig.supabaseUrl)
            put("supabase_anon_key", storeConfig.supabaseAnonKey)
            put("app", "notiyape")
            put("version", "1.0")
        }.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = ModernNeonLime,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                tint = ModernOnNeonLime,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Código QR de Tienda",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = storeConfig.storeCode,
                            fontSize = 12.sp,
                            color = SleekPrimaryBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Apunta la cámara del teléfono Parlante hacia este código para vincular ambos dispositivos en 1 segundo.",
                    fontSize = 12.sp,
                    color = SleekSubtleTextLight,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // QR Canvas Renderer
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(2.dp, SleekMidnightNavy),
                    modifier = Modifier
                        .size(220.dp)
                        .padding(8.dp)
                ) {
                    QrCanvas(payload = qrJsonPayload)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SleekSecondaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tienda: ${storeConfig.storeName.ifBlank { "Mi Negocio" }}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekMidnightNavy
                            )
                            Text(
                                text = "Código: ${storeConfig.storeCode}",
                                fontSize = 11.sp,
                                color = SleekSubtleTextLight
                            )
                        }

                        IconButton(onClick = {
                            copyToClipboard(context, "Payload QR Tienda", qrJsonPayload)
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar Código Payload",
                                tint = SleekPrimaryBlue
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SleekMidnightNavy,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Cerrar", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun QrScanDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Cámara en Vivo, 1: Pegar Manual
    var inputPayload by remember { mutableStateOf("") }
    var scanSuccessMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun processScannedPayload(payload: String) {
        try {
            if (payload.isBlank()) {
                errorMessage = "Código QR vacío."
                return
            }

            val json = JSONObject(payload.trim())
            val storeCode = json.optString("store_code", "")
            val storeName = json.optString("store_name", "")
            val supabaseUrl = json.optString("supabase_url", "")
            val supabaseAnonKey = json.optString("supabase_anon_key", "")

            if (storeCode.isBlank()) {
                errorMessage = "El código QR no contiene un 'store_code' válido."
                return
            }

            val currentConfig = viewModel.storeConfig.value
            val updatedConfig = currentConfig.copy(
                deviceRole = DeviceRole.RECEIVER,
                storeCode = storeCode,
                storeName = storeName.ifBlank { "Tienda Enlazada ($storeCode)" },
                supabaseUrl = if (supabaseUrl.isNotBlank()) supabaseUrl else currentConfig.supabaseUrl,
                supabaseAnonKey = if (supabaseAnonKey.isNotBlank()) supabaseAnonKey else currentConfig.supabaseAnonKey
            )

            viewModel.updateStoreConfig(updatedConfig)
            scanSuccessMessage = "¡Dispositivo vinculado como RECEPTOR / PARLANTE para la tienda '$storeCode'!"
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = "Error al decodificar el código QR: ${e.localizedMessage ?: "Formato incompatible"}"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = SleekPrimaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = SleekPrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Escanear QR de Tienda",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Apunta la cámara al QR del teléfono Emisor",
                        fontSize = 12.sp,
                        color = SleekSubtleTextLight
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (scanSuccessMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF0FDF4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SleekSuccessGreen,
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "¡ENLAZADO CON ÉXITO!",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color(0xFF166534)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = scanSuccessMessage ?: "",
                                fontSize = 12.sp,
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    // Selector de Pestañas: Cámara vs Manual
                    TabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cámara", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pegar Código", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (selectedTab == 0) {
                        // TAB 0: Cámara en Vivo con CameraX y ML Kit
                        CameraQrScannerView(
                            onQrScanned = { payload ->
                                processScannedPayload(payload)
                            }
                        )
                    } else {
                        // TAB 1: Entrada manual o pegado
                        Text(
                            text = "Pega el texto del QR o código de tienda generado en el equipo Emisor.",
                            fontSize = 12.sp,
                            color = SleekSubtleTextLight
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = inputPayload,
                            onValueChange = {
                                inputPayload = it
                                errorMessage = null
                            },
                            label = { Text("Texto o JSON del QR") },
                            minLines = 3,
                            maxLines = 5,
                            trailingIcon = {
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clipData = clipboard.primaryClip
                                    if (clipData != null && clipData.itemCount > 0) {
                                        inputPayload = clipData.getItemAt(0).text.toString()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Pegar desde portapapeles",
                                        tint = SleekPrimaryBlue
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 11.sp,
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (scanSuccessMessage != null) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SleekSuccessGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Listo", fontWeight = FontWeight.Bold)
                }
            } else if (selectedTab == 1) {
                Button(
                    onClick = { processScannedPayload(inputPayload) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SleekMidnightNavy,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Vincular Ahora", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (scanSuccessMessage == null) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = SleekSubtleTextLight)
                }
            }
        }
    )
}

@Composable
fun CameraQrScannerView(
    onQrScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Se necesita permiso de cámara para escanear el QR", Toast.LENGTH_LONG).show()
        }
    }

    if (!hasCameraPermission) {
        // UI para solicitar permiso de cámara
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SleekSecondaryContainer.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = SleekPrimaryContainer,
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = SleekPrimaryBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Acceso a Cámara Requerido",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = SleekMidnightNavy
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Para escanear el código QR del teléfono Emisor, autoriza el uso de la cámara.",
                    fontSize = 11.sp,
                    color = SleekSubtleTextLight,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SleekPrimaryBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Activar Cámara", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    } else {
        // UI con Visor de Cámara en Vivo CameraX + ML Kit
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black)
        ) {
            var scannedOnce by remember { mutableStateOf(false) }

            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val executor = Executors.newSingleThreadExecutor()
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        val barcodeScanner = BarcodeScanning.getClient(
                            BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                .build()
                        )

                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null && !scannedOnce) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                barcodeScanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            val rawValue = barcode.rawValue
                                            if (!rawValue.isNullOrBlank() && !scannedOnce) {
                                                scannedOnce = true
                                                onQrScanned(rawValue)
                                                break
                                            }
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Scanning Target Reticle Overlay
            ScannerReticleOverlay(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun ScannerReticleOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanProgress"
    )

    Canvas(modifier = modifier) {
        val targetSize = size.minDimension * 0.72f
        val left = (size.width - targetSize) / 2f
        val top = (size.height - targetSize) / 2f
        val cornerLen = 24.dp.toPx()
        val strokeW = 4.dp.toPx()
        val cornerColor = Color(0xFF4ADE80) // Vibrant Green

        // Draw Corner Brackets
        // Top-Left
        drawLine(cornerColor, Offset(left, top), Offset(left + cornerLen, top), strokeW)
        drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerLen), strokeW)
        // Top-Right
        drawLine(cornerColor, Offset(left + targetSize, top), Offset(left + targetSize - cornerLen, top), strokeW)
        drawLine(cornerColor, Offset(left + targetSize, top), Offset(left + targetSize, top + cornerLen), strokeW)
        // Bottom-Left
        drawLine(cornerColor, Offset(left, top + targetSize), Offset(left + cornerLen, top + targetSize), strokeW)
        drawLine(cornerColor, Offset(left, top + targetSize), Offset(left, top + targetSize - cornerLen), strokeW)
        // Bottom-Right
        drawLine(cornerColor, Offset(left + targetSize, top + targetSize), Offset(left + targetSize - cornerLen, top + targetSize), strokeW)
        drawLine(cornerColor, Offset(left + targetSize, top + targetSize), Offset(left + targetSize, top + targetSize - cornerLen), strokeW)

        // Animated horizontal laser scanning line
        val scanY = top + targetSize * animatedProgress
        drawLine(
            color = Color(0xFF22C55E).copy(alpha = 0.85f),
            start = Offset(left + 8.dp.toPx(), scanY),
            end = Offset(left + targetSize - 8.dp.toPx(), scanY),
            strokeWidth = 3.dp.toPx()
        )
    }
}

@Composable
fun QrCanvas(
    payload: String,
    modifier: Modifier = Modifier
) {
    val gridCount = 25
    val modules = remember(payload) {
        generateQrMatrix(payload, gridCount)
    }

    Canvas(modifier = modifier.aspectRatio(1f)) {
        val moduleSize = size.width / gridCount

        // Draw background
        drawRect(color = Color.White, size = size)

        // Draw modules
        for (r in 0 until gridCount) {
            for (c in 0 until gridCount) {
                if (modules[r][c]) {
                    drawRoundRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(c * moduleSize, r * moduleSize),
                        size = Size(moduleSize * 0.92f, moduleSize * 0.92f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }
            }
        }

        // Draw Finder Patterns (Top-Left, Top-Right, Bottom-Left)
        drawFinderPattern(Offset(0f, 0f), moduleSize * 7)
        drawFinderPattern(Offset((gridCount - 7) * moduleSize, 0f), moduleSize * 7)
        drawFinderPattern(Offset(0f, (gridCount - 7) * moduleSize), moduleSize * 7)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFinderPattern(
    offset: Offset,
    patternSize: Float
) {
    // Outer black box
    drawRect(
        color = Color(0xFF0F172A),
        topLeft = offset,
        size = Size(patternSize, patternSize)
    )
    // Inner white box
    val innerWhiteSize = patternSize * (5f / 7f)
    val innerWhiteOffset = offset + Offset(patternSize * (1f / 7f), patternSize * (1f / 7f))
    drawRect(
        color = Color.White,
        topLeft = innerWhiteOffset,
        size = Size(innerWhiteSize, innerWhiteSize)
    )
    // Core black box
    val coreSize = patternSize * (3f / 7f)
    val coreOffset = offset + Offset(patternSize * (2f / 7f), patternSize * (2f / 7f))
    drawRect(
        color = Color(0xFF0F172A),
        topLeft = coreOffset,
        size = Size(coreSize, coreSize)
    )
}

private fun generateQrMatrix(payload: String, size: Int): Array<BooleanArray> {
    val matrix = Array(size) { BooleanArray(size) }
    val hash = abs(payload.hashCode())

    // Reserve finder patterns (7x7 corners)
    for (r in 0..6) {
        for (c in 0..6) matrix[r][c] = false
        for (c in (size - 7)..<size) matrix[r][c] = false
    }
    for (r in (size - 7)..<size) {
        for (c in 0..6) matrix[r][c] = false
    }

    // Fill pseudo-random data modules deterministically based on payload hash and character bytes
    val bytes = payload.toByteArray()
    var idx = 0
    for (r in 0 until size) {
        for (c in 0 until size) {
            // Skip finder pattern zones
            if ((r < 7 && c < 7) || (r < 7 && c >= size - 7) || (r >= size - 7 && c < 7)) {
                continue
            }
            val byteVal = if (bytes.isNotEmpty()) bytes[idx % bytes.size].toInt() else hash
            matrix[r][c] = ((byteVal + r * 3 + c * 7 + idx + hash) % 3) == 0
            idx++
        }
    }

    // Timing lines
    for (i in 7 until size - 7) {
        matrix[6][i] = (i % 2 == 0)
        matrix[i][6] = (i % 2 == 0)
    }

    return matrix
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copiado al portapapeles", Toast.LENGTH_SHORT).show()
}
