package dev.arya.adhaarcardscanner.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.animation.TranslateAnimation
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.mlkit.vision.common.InputImage
import dev.arya.adhaarcardscanner.databinding.ActivityMainBinding
import dev.arya.adhaarcardscanner.repository.ScanRepository
import dev.arya.adhaarcardscanner.utils.AadhaarUtils.format
import dev.arya.adhaarcardscanner.viewmodel.ScanViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var viewModel: ScanViewModel

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) startCamera() else showPermissionError()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(ScanViewModel::class.java)
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.btnRetry.setOnClickListener {
            viewModel.startScanning()
            binding.btnRetry.visibility = View.GONE
            binding.btnCopy.visibility = View.GONE
        }

        binding.btnCopy.setOnClickListener {
            val aadhaar = viewModel.aadhaarResult.value
            if (!aadhaar.isNullOrEmpty()) copyToClipboard(aadhaar)
        }

        observeViewModel()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        startScanLineAnimation()

        viewModel.scanning.observe(this) { scanning ->
            if (scanning) {
                binding.tvStatus.text = "Scanning..."
                binding.scanLine.visibility = View.VISIBLE
                setScanColor(ScanStatus.Scanning)
            } else {
                binding.scanLine.visibility = View.GONE
            }
        }

        viewModel.aadhaarResult.observe(this) { aadhaar ->
            if (!aadhaar.isNullOrEmpty()) {
                binding.tvResult.text = "Aadhaar: ${format(aadhaar)}"
                binding.btnCopy.visibility = View.VISIBLE
                binding.btnRetry.visibility = View.VISIBLE
                setScanColor(ScanStatus.Success) // green on success
            } else {
                binding.btnCopy.visibility = View.GONE
            }
        }

        viewModel.errorMsg.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) {
                binding.tvResult.text = msg
                binding.btnRetry.visibility = View.VISIBLE
                setScanColor(ScanStatus.Error) // red on error
            }
        }

    }

    private fun observeViewModel() {
        viewModel.scanning.observe(this) { scanning ->
            binding.tvStatus.text = if (scanning) "Scanning..." else "Paused"
            binding.scanLine.visibility = if (scanning) View.VISIBLE else View.GONE
        }

        viewModel.aadhaarResult.observe(this) { aadhaar ->
            if (!aadhaar.isNullOrEmpty()) {
                binding.tvResult.text = "Aadhaar: ${format(aadhaar)}"
                binding.btnCopy.visibility = View.VISIBLE
                binding.btnRetry.visibility = View.VISIBLE
            } else {
                if (viewModel.errorMsg.value.isNullOrEmpty()) binding.tvResult.text = ""
                binding.btnCopy.visibility = View.GONE
            }
        }

        viewModel.errorMsg.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) {
                binding.tvResult.text = msg
                binding.btnRetry.visibility = View.VISIBLE
            }
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.Companion.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val repository = ScanRepository(this)

            imageAnalysis.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy ->
                if (viewModel.aadhaarResult.value != null) {
                    imageProxy.close()
                    return@Analyzer
                }

                if (viewModel.isProcessing) {
                    imageProxy.close()
                    return@Analyzer
                }

                viewModel.setProcessing(true)

                val mediaImage = imageProxy.image
                if (mediaImage == null) {
                    viewModel.setProcessing(false)
                    imageProxy.close()
                    return@Analyzer
                }

                val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                repository.scanBarcode(inputImage, { result ->
                    if (!result.aadhaar.isNullOrEmpty()) {
                        viewModel.setAadhaar(result.aadhaar)
                        viewModel.setProcessing(false)
                        imageProxy.close()
                    } else if (result.hasBarcode) {
                        viewModel.setError("Aadhaar number (12 digits) not found in QR. The QR may be secure/encrypted UIDAI QR.")
                        viewModel.setProcessing(false)
                        imageProxy.close()
                    } else {
                        repository.scanText(inputImage, { textAadhaar ->
                            if (!textAadhaar.isNullOrEmpty()) {
                                viewModel.setAadhaar(textAadhaar)
                            } else {
                                viewModel.setError("No Aadhaar detected. Move camera closer, increase lighting, or try QR side of the card.")
                            }
                            viewModel.setProcessing(false)
                            imageProxy.close()
                        }, { ex ->
                            viewModel.setError("Text recognition failed: ${ex.localizedMessage}")
                            viewModel.setProcessing(false)
                            imageProxy.close()
                        })
                    }
                }, { ex ->
                    viewModel.setError("Barcode scan failed: ${ex.localizedMessage}")
                    viewModel.setProcessing(false)
                    imageProxy.close()
                })
            })

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                viewModel.startScanning()
            } catch (e: Exception) {
                Toast.makeText(this, "Camera start failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("SetTextI18n")
    private fun showPermissionError() {
        Toast.makeText(this, "Camera permission is required to scan Aadhaar.", Toast.LENGTH_LONG).show()
        binding.tvResult.text = "Camera permission denied. Please allow camera."
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Aadhaar", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun startScanLineAnimation() {
        val anim = TranslateAnimation(0f, 0f, -80f, 80f)
        anim.duration = 900
        anim.repeatMode = TranslateAnimation.REVERSE
        anim.repeatCount = TranslateAnimation.INFINITE
        binding.scanLine.startAnimation(anim)
    }
    private fun setScanColor(status: ScanStatus) {
        val color = when (status) {
            ScanStatus.Scanning -> 0xFFFFFF00.toInt()
            ScanStatus.Success -> 0xFF00FF00.toInt()
            ScanStatus.Error -> 0xFFFF0000.toInt()
        }
        binding.scanLine.setBackgroundColor(color)

        val drawable = binding.scanBox.background.mutate()
        drawable.setTint(color)
        binding.scanBox.background = drawable
    }



    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}