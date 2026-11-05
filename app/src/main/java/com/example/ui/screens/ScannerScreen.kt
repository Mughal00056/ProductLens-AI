package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.camera.CameraCaptureController
import com.example.camera.CameraPreview
import com.example.data.model.ScanningMode
import com.example.ui.components.LaserScannerHUD
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.LaserCrimson
import com.example.ui.theme.NeonRed
import com.example.ui.theme.RubyRed
import com.example.ui.viewmodel.ProductLensViewModel
import com.example.ui.viewmodel.ScannerUiState

@Composable
fun ScannerScreen(
    viewModel: ProductLensViewModel,
    initialMode: ScanningMode = ScanningMode.AI_VISION,
    onNavigateBack: () -> Unit,
    onProductIdentified: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Camera permission is needed to scan products", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var isTorchOn by remember { mutableStateOf(false) }
    var isBackCamera by remember { mutableStateOf(true) }
    val mode by viewModel.scanningMode.collectAsState()
    var captureController by remember { mutableStateOf<CameraCaptureController?>(null) }

    LaunchedEffect(initialMode) {
        viewModel.setScanningMode(initialMode)
    }

    val uiState by viewModel.scannerUiState.collectAsState()

    // Pick from Gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        viewModel.scanImage(bitmap)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load selected image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is ScannerUiState.Success) {
            val product = (uiState as ScannerUiState.Success).product
            onProductIdentified(product.id)
            viewModel.resetScannerState()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // Live CameraX Feed
            CameraPreview(
                isTorchOn = isTorchOn,
                isBackCamera = isBackCamera,
                onBarcodeDetected = if (mode == ScanningMode.BARCODE) {
                    { barcode ->
                        viewModel.scanBarcode(barcode)
                    }
                } else null,
                onControllerReady = { ctrl ->
                    captureController = ctrl
                },
                modifier = Modifier.fillMaxSize()
            )

            // Glowing Cyber Red Laser HUD Overlay
            LaserScannerHUD(
                mode = mode,
                isTorchOn = isTorchOn,
                isScanning = uiState is ScannerUiState.Scanning,
                onToggleTorch = { isTorchOn = !isTorchOn },
                onSwitchCamera = { isBackCamera = !isBackCamera },
                onPickGallery = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onCaptureClick = {
                    if (mode == ScanningMode.AI_VISION) {
                        captureController?.capture(
                            onSuccess = { bitmap ->
                                viewModel.scanImage(bitmap)
                            },
                            onError = { err ->
                                Toast.makeText(context, "Capture error: ${err.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        Toast.makeText(context, "Align barcode with the red laser line to auto-detect", Toast.LENGTH_SHORT).show()
                    }
                },
                onModeChange = { newMode ->
                    viewModel.setScanningMode(newMode)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Permission Denied Request View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = NeonRed,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Permission Required",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ProductLens AI needs camera access to identify physical products and scan retail barcodes.",
                    fontSize = 14.sp,
                    color = Color(0xFFCCB3B8),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("grant_camera_permission_button")
                ) {
                    Text("Grant Permission", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Top Back Button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .padding(top = 42.dp, start = 16.dp)
                .size(44.dp)
                .background(Color(0x99120609), CircleShape)
                .border(1.dp, Color(0x66FF1744), CircleShape)
                .testTag("scanner_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        // Scanning / Loading Overlay Dialog
        AnimatedVisibility(
            visible = uiState is ScannerUiState.Scanning,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val statusMsg = (uiState as? ScannerUiState.Scanning)?.statusMessage
                ?: "AI Spectral Analysis in Progress..."

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xBB080204))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF14080C))
                        .border(2.dp, NeonRed, RoundedCornerShape(24.dp))
                        .shadow(16.dp, RoundedCornerShape(24.dp))
                        .padding(28.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            color = NeonRed,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(54.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "IDENTIFYING PRODUCT",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NeonRed,
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = statusMsg,
                            fontSize = 14.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Matching images, barcodes & cross-referencing store catalogs...",
                            fontSize = 11.sp,
                            color = Color(0xFFAA8B92),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Error Dialog Overlay
        AnimatedVisibility(
            visible = uiState is ScannerUiState.Error,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val errMsg = (uiState as? ScannerUiState.Error)?.message ?: "An error occurred."

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xBB000000))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1A0A0E))
                        .border(1.5.dp, LaserCrimson, RoundedCornerShape(24.dp))
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = NeonRed,
                            modifier = Modifier.size(44.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Identification Notice",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = errMsg,
                            fontSize = 13.sp,
                            color = Color(0xFFE0C4C8),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { viewModel.resetScannerState() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("retry_scan_button")
                        ) {
                            Text("Try Again", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
