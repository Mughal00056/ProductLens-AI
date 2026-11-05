package com.example.camera

import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class MLKitBarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)
    private var isBusy = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && !isBusy) {
            isBusy = true
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (!rawValue.isNullOrBlank()) {
                            onBarcodeDetected(rawValue)
                            break
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("BarcodeAnalyzer", "Barcode scanning error", e)
                }
                .addOnCompleteListener {
                    isBusy = false
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    fun analyzeBitmap(bitmap: Bitmap, onResult: (String?) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val code = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                onResult(code)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }
}
