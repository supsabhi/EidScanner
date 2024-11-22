package com.example.eidscanner_mlkit.MainActivity


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.eidscanner_mlkit.R
import com.google.mlkit.nl.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition

import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var executor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mainactivity)

        previewView = findViewById(R.id.previewView)
        executor = Executors.newSingleThreadExecutor()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Set up preview use case
            val preview = androidx.camera.core.Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

            // Set up analysis use case
            val analysisUseCase = ImageAnalysis.Builder().build().apply {
                setAnalyzer(executor) { imageProxy ->
                    processImage(imageProxy, BitmapFactory.decodeResource(resources, R.drawable.dummyeid3))
                }
            }

            // Bind use cases to lifecycle
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                analysisUseCase
            )
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy,image:Bitmap) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = //InputImage.fromBitmap(image, 0)
                InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(inputImage)
                .addOnSuccessListener { text ->
                    handleDetectedText(text)
                    imageProxy.close()
                }
                .addOnFailureListener { exception ->
                    exception.printStackTrace()
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
        }
    private fun handleDetectedText(text: com.google.mlkit.vision.text.Text) {
        val lines = text.text.split("\n")

        // Attributes
        var name: String? = null
        var number: String? = null
        var nationality: String? = null
        var sex: String? = null
        val eIdDates = mutableListOf<String>()

        for (line in lines) {
            val trimmedLine = line.trim()
            when {
                line.matches(Regex("^[A-Z ]+$"))->{}
                isDate(trimmedLine) -> eIdDates.add(trimmedLine)
                isName(trimmedLine) != null -> name = isName(trimmedLine)
                isNationality(trimmedLine) != null -> nationality = isNationality(trimmedLine)
                isSex(trimmedLine) != null -> sex = isSex(trimmedLine)
                isNumberID(trimmedLine) -> number =(trimmedLine)
            }
        }

        // Sort dates and extract DOB, issue, and expiry dates
        val sortedDates = sortDateList(eIdDates)
        val eid= EmirateIdModel(
            name = name,
            number = number,
            nationality = nationality,
            sex = sex,
            dateOfBirth = if (sortedDates.size == 3) sortedDates[0] else null,
            issueDate = if (sortedDates.size == 3) sortedDates[1] else null,
            expiryDate = if (sortedDates.size == 3) sortedDates[2] else null
        )
        val recognizedText = text.text
        findViewById<TextView>(R.id.recognizedTextView).text = eid.toString()
    }

    private fun isDate(text: String): Boolean {
        val datePatterns = listOf("dd/MM/yyyy", "yyyy-MM-dd")
        return datePatterns.any { pattern ->
            try {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                sdf.isLenient = false
                sdf.parse(text)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun isName(text: String): String? {
        // Add logic for identifying names (e.g., uppercase, specific patterns)
        if (text.contains("Name",true)) {
            val newText= text.replace("Name:", "")
           return  newText
        }
        else
            return null
    }

    private fun isNationality(text: String): String? {
        // Check common nationalities
        val nationalities = listOf("UAE", "INDIA", "PAKISTAN", "USA", "CANADA","UNITED ARAB EMIRATES","PAKISTAN","IRAN","SYRIA","JORDAN",
            "PHILIPPINES","IRAQ","NEPAL","SAUDI ARABIA","SRI LANKA","QATAR","KUWAIT","MALAYSIA","BAHRAIN","UNITED KINGDOM")
        return nationalities.find { text.contains(it, ignoreCase = true) }
    }

    private fun isSex(text: String): String? {
        if(text.contains("sex",ignoreCase = true)) {
            val newText= text.replace("Sex:", "")
            return when {
                newText.contains("m", ignoreCase = true) -> "Male"
                newText.contains("f", ignoreCase = true) -> "Female"
                else -> null

            }
        }
        else
            return null
    }

    private fun isNumberID(text: String): Boolean {
        // Check if it's a valid ID number (e.g., numeric and length constraints)
        return text.matches(Regex("^\\d{3}-\\d{4}-\\d{7}-\\d{1}$"))
    }

    private fun sortDateList(dates: List<String>): List<String> {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dates.sortedBy { date ->
            try {
                sdf.parse(date)
            } catch (e: Exception) {
                null
            }
        }
    }
}
data class EmirateIdModel(
    val name: String?,
    val number: String?,
    val nationality: String?,
    val sex: String?,
    val dateOfBirth: String?,
    val issueDate: String?,
    val expiryDate: String?
)