package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScanningMode
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LaserCrimson
import com.example.ui.theme.NeonRed
import com.example.ui.theme.RubyRed

@Composable
fun LaserScannerHUD(
    mode: ScanningMode,
    isTorchOn: Boolean,
    isScanning: Boolean,
    onToggleTorch: () -> Unit,
    onSwitchCamera: () -> Unit,
    onPickGallery: () -> Unit,
    onCaptureClick: () -> Unit,
    onModeChange: (ScanningMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_animations")

    // Vertical Laser Sweep Position (0f to 1f and reverse)
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_sweep"
    )

    // Pulsing reticle scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Rotating outer reticle ring
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reticle_rotation"
    )

    // Glowing laser beam alpha
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight

        val boxSize = if (totalWidth < 400.dp) totalWidth * 0.85f else 320.dp
        val boxHeight = if (mode == ScanningMode.BARCODE) boxSize * 0.65f else boxSize

        // 1. Semi-transparent dark vignette mask around the scan window
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x55000000))
        )

        // 2. Center Scanning Area Canvas (Laser, Grid, Reticle, Brackets)
        Box(
            modifier = Modifier
                .size(width = boxSize, height = boxHeight)
                .align(Alignment.Center)
        ) {
            // Background Laser Grid Lines inside window
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 28.dp.toPx()
                val gridColor = Color(0x18FF1744)

                var x = 0f
                while (x <= size.width) {
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f
                    )
                    x += step
                }

                var y = 0f
                while (y <= size.height) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += step
                }

                // Four Glowing Cybernetic Corner Brackets
                val bracketLen = 34.dp.toPx()
                val bracketStroke = 4.dp.toPx()
                val cornerRadius = 14.dp.toPx()
                val cornerColor = NeonRed

                // Top-Left
                val tlPath = Path().apply {
                    moveTo(0f, bracketLen)
                    lineTo(0f, cornerRadius)
                    quadraticTo(0f, 0f, cornerRadius, 0f)
                    lineTo(bracketLen, 0f)
                }
                drawPath(tlPath, cornerColor, style = Stroke(width = bracketStroke, cap = StrokeCap.Round))

                // Top-Right
                val trPath = Path().apply {
                    moveTo(size.width - bracketLen, 0f)
                    lineTo(size.width - cornerRadius, 0f)
                    quadraticTo(size.width, 0f, size.width, cornerRadius)
                    lineTo(size.width, bracketLen)
                }
                drawPath(trPath, cornerColor, style = Stroke(width = bracketStroke, cap = StrokeCap.Round))

                // Bottom-Left
                val blPath = Path().apply {
                    moveTo(0f, size.height - bracketLen)
                    lineTo(0f, size.height - cornerRadius)
                    quadraticTo(0f, size.height, cornerRadius, size.height)
                    lineTo(bracketLen, size.height)
                }
                drawPath(blPath, cornerColor, style = Stroke(width = bracketStroke, cap = StrokeCap.Round))

                // Bottom-Right
                val brPath = Path().apply {
                    moveTo(size.width - bracketLen, size.height)
                    lineTo(size.width - cornerRadius, size.height)
                    quadraticTo(size.width, size.height, size.width, size.height - cornerRadius)
                    lineTo(size.width, size.height - bracketLen)
                }
                drawPath(brPath, cornerColor, style = Stroke(width = bracketStroke, cap = StrokeCap.Round))

                // Rotating Outer Optical Reticle
                val center = Offset(size.width / 2, size.height / 2)
                val reticleRadius = (size.minDimension / 4f) * pulseScale

                rotate(rotationAngle, pivot = center) {
                    drawCircle(
                        color = Color(0x66FF1744),
                        radius = reticleRadius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    // Tick marks
                    val tickLen = 8.dp.toPx()
                    for (i in 0 until 8) {
                        rotate(i * 45f, pivot = center) {
                            drawLine(
                                color = NeonRed,
                                start = Offset(center.x, center.y - reticleRadius),
                                end = Offset(center.x, center.y - reticleRadius - tickLen),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }
                }

                // Inner Crosshairs
                val chLen = 16.dp.toPx()
                drawLine(
                    color = NeonRed,
                    start = Offset(center.x - chLen, center.y),
                    end = Offset(center.x + chLen, center.y),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = NeonRed,
                    start = Offset(center.x, center.y - chLen),
                    end = Offset(center.x, center.y + chLen),
                    strokeWidth = 2.dp.toPx()
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = center
                )

                // Powerful Glowing Horizontal Laser Sweep Beam
                val beamY = size.height * laserProgress
                val beamHeight = 48.dp.toPx()

                // Gradient glow spread behind the laser line
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            NeonRed.copy(alpha = 0.35f * glowAlpha),
                            Color.White.copy(alpha = 0.85f * glowAlpha),
                            NeonRed.copy(alpha = 0.35f * glowAlpha),
                            Color.Transparent
                        ),
                        startY = beamY - beamHeight / 2,
                        endY = beamY + beamHeight / 2
                    ),
                    topLeft = Offset(0f, beamY - beamHeight / 2),
                    size = Size(size.width, beamHeight)
                )

                // Intense center sharp laser line
                drawLine(
                    color = Color.White,
                    start = Offset(0f, beamY),
                    end = Offset(size.width, beamY),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Glowing Outer Blur Halo for the Reticle Frame
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, Color(0x33FF1744), RoundedCornerShape(14.dp))
                    .blur(6.dp)
            )
        }

        // 3. Top Status Telemetry HUD & Mode Selector
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 44.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // High-Tech Cyber Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0xCC120609), RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0x88FF1744), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NeonRed)
                        .shadow(4.dp, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isScanning) "AI SPECTRAL OPTICS: SCANNING..." else "RED LASER MATRIX READY (650nm)",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Selector Pill (AI Product / Barcode)
            Row(
                modifier = Modifier
                    .background(Color(0xCC000000), RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0x44FF1744), RoundedCornerShape(24.dp))
                    .padding(4.dp)
            ) {
                val isAi = mode == ScanningMode.AI_VISION
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isAi) NeonRed else Color.Transparent)
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                        .testTag("mode_ai_product")
                ) {
                    androidx.compose.material3.TextButton(
                        onClick = { onModeChange(ScanningMode.AI_VISION) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "AI Product Scan",
                            color = if (isAi) Color.White else Color(0xFFCCB3B8),
                            fontWeight = if (isAi) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (!isAi) NeonRed else Color.Transparent)
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                        .testTag("mode_barcode")
                ) {
                    androidx.compose.material3.TextButton(
                        onClick = { onModeChange(ScanningMode.BARCODE) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Barcode Scanner",
                            color = if (!isAi) Color.White else Color(0xFFCCB3B8),
                            fontWeight = if (!isAi) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // 4. Instructions helper text below scanning window
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = boxHeight + 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (mode == ScanningMode.AI_VISION)
                    "Center product inside the glowing red matrix and capture"
                else
                    "Point red laser beam directly at any product barcode",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        }

        // 5. Bottom Controls: Flash, Switch, Gallery, and Glowing Trigger
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xDD090406), Color(0xFF090406))
                    )
                )
                .padding(bottom = 36.dp, top = 20.dp, start = 24.dp, end = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Torch Toggle
                IconButton(
                    onClick = onToggleTorch,
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            if (isTorchOn) LaserCrimson else Color(0x33FFFFFF),
                            CircleShape
                        )
                        .border(1.dp, if (isTorchOn) NeonRed else Color.Transparent, CircleShape)
                        .testTag("toggle_torch_button")
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashlightOff,
                        contentDescription = "Toggle Flash",
                        tint = if (isTorchOn) Color.White else Color(0xFFE0E0E0)
                    )
                }

                // Glowing Capture Trigger Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                ) {
                    // Concentric Glowing Pulsing Aura
                    Box(
                        modifier = Modifier
                            .size((78 * pulseScale).dp)
                            .background(Color(0x33FF1744), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .border(3.dp, NeonRed, CircleShape)
                            .padding(6.dp)
                    ) {
                        IconButton(
                            onClick = onCaptureClick,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(NeonRed, RubyRed)
                                    ),
                                    CircleShape
                                )
                                .shadow(12.dp, CircleShape)
                                .testTag("capture_scanner_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Scan Product",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Switch Camera
                IconButton(
                    onClick = onSwitchCamera,
                    modifier = Modifier
                        .size(50.dp)
                        .background(Color(0x33FFFFFF), CircleShape)
                        .testTag("switch_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White
                    )
                }

                // Gallery Upload
                IconButton(
                    onClick = onPickGallery,
                    modifier = Modifier
                        .size(50.dp)
                        .background(Color(0x33FFFFFF), CircleShape)
                        .testTag("pick_gallery_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Select from Gallery",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
