package dev.arya.adhaarcardscanner.repository

import android.content.Context
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dev.arya.adhaarcardscanner.utils.AadhaarUtils.findAadhaar
import java.lang.Exception

class ScanRepository(private val context: Context) {

    data class BarcodeResult(val aadhaar: String?, val hasBarcode: Boolean)

    private val barcodeScanner by lazy {
        BarcodeScanning.getClient()
    }

    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    fun scanBarcode(image: InputImage, onSuccess: (BarcodeResult) -> Unit, onFailure: (Exception) -> Unit) {
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                var foundAadhaar: String? = null
                var anyBarcode = false
                for (barcode in barcodes) {
                    anyBarcode = true
                    val raw = barcode.rawValue ?: ""
                    val aad = findAadhaar(raw)
                    if (!aad.isNullOrEmpty()) {
                        foundAadhaar = aad
                        break
                    }
                }
                onSuccess(BarcodeResult(foundAadhaar, anyBarcode))
            }
            .addOnFailureListener { ex ->
                onFailure(Exception(ex))
            }
    }

    fun scanText(image: InputImage, onSuccess: (String?) -> Unit, onFailure: (Exception) -> Unit) {
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val text = visionText.text ?: ""
                val aad = findAadhaar(text)
                onSuccess(aad)
            }
            .addOnFailureListener { ex ->
                onFailure(Exception(ex))
            }
    }
}