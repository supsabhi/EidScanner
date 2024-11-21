package com.example.eidscanner_mlkit.MainActivity


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var executor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mainactivity) // Ensure the layout name matches your file

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
                    processImage(imageProxy, BitmapFactory.decodeResource(resources, R.drawable.dummyeid2))
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
            val inputImage = InputImage.fromBitmap(image, 0)
               // InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
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
        val recognizedText = text.text
        findViewById<TextView>(R.id.recognizedTextView).text = recognizedText
    }
}