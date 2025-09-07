package com.elytelabs.labelscan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private lateinit var progressBar: ProgressBar
    private var isScanning = false

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        previewView = findViewById(R.id.previewView)
        progressBar = findViewById(R.id.progressBar)

        cameraExecutor = Executors.newSingleThreadExecutor()

        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) -> {
                Toast.makeText(this, "Camera permission is required to scan barcodes", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
            } catch (e: Exception) {
                Log.e("ScannerActivity", "Camera binding failed", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        if (isScanning) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient()

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        handleBarcode(barcode)
                        break
                    }
                }
                .addOnFailureListener {
                    Log.e("ScannerActivity", "Barcode scan failed", it)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun handleBarcode(barcode: Barcode) {
        val rawValue = barcode.rawValue ?: return
        if (!isScanning) {
            isScanning = true
            progressBar.visibility = View.VISIBLE
            fetchProductDetails(rawValue)
        }
    }

    private fun fetchProductDetails(barcode: String) {
        val apiService = RetrofitInstance.api
        apiService.getProduct(barcode).enqueue(object : Callback<ProductResponse> {
            override fun onResponse(
                call: Call<ProductResponse>,
                response: Response<ProductResponse>
            ) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val product = response.body()?.product
                    if (product != null) {
                        val intent = Intent(this@ScannerActivity, DetailsActivity::class.java).apply {
                            putExtra("name", product.product_name ?: "Unknown")
                            putExtra("image", product.image_url ?: "")
                            putExtra("ingredients", product.ingredients_text ?: "No data available")
                            putExtra("side_effects", detectSideEffects(product.ingredients_text))
                            putExtra("fat_level", product.nutrient_levels?.fat ?: "unknown")
                            putExtra("sugar_level", product.nutrient_levels?.sugars ?: "unknown")
                            putExtra("salt_level", product.nutrient_levels?.salt ?: "unknown")
                            putExtra("sat_fat_level", product.nutrient_levels?.saturatedFat ?: "unknown")
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@ScannerActivity, "No product details found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this@ScannerActivity, "Product not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onFailure(call: Call<ProductResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@ScannerActivity, "Error fetching data", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    private fun detectSideEffects(ingredients: String?): String {
        if (ingredients.isNullOrBlank()) return "No known side effects"

        val lowerIngredients = ingredients.lowercase()
        val warnings = mutableListOf<String>()

        val allergenMap = mapOf(
            "milk" to "May cause issues for lactose-intolerant individuals",
            "lactose" to "Contains lactose",
            "gluten" to "Contains gluten, avoid if celiac",
            "wheat" to "Contains wheat (gluten)",
            "soy" to "Contains soy, possible allergen",
            "egg" to "Contains egg, possible allergen",
            "peanut" to "Contains peanuts, possible allergen",
            "nut" to "Contains nuts, possible allergen",
            "almond" to "Contains almonds (tree nut allergen)",
            "cashew" to "Contains cashews (tree nut allergen)",
            "hazelnut" to "Contains hazelnuts (tree nut allergen)",
            "fish" to "Contains fish, possible allergen",
            "shellfish" to "Contains shellfish, possible allergen",
            "crustacean" to "Contains crustacean shellfish",
            "sesame" to "Contains sesame, possible allergen"
        )

        allergenMap.forEach { (keyword, warning) ->
            if (lowerIngredients.contains(keyword)) {
                warnings.add(warning)
            }
        }

        return if (warnings.isEmpty()) {
            "No major allergens detected"
        } else {
            warnings.joinToString(separator = "\n")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}