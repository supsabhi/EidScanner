package com.example.eidscanner_mlkit.MainActivity


import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.text.SimpleDateFormat
import java.util.*

data class EmirateIdModel(
    val name: String?,
    val number: String?,
    val nationality: String?,
    val sex: String?,
    val dateOfBirth: String?,
    val issueDate: String?,
    val expiryDate: String?
)
class SecondMainActivity :AppCompatActivity() {

/*
    suspend fun scanEmirateId(image: Bitmap): EmirateIdModel? {
        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val inputImage = InputImage.fromBitmap(image, 0)

        // Process the image to extract recognized text
        val recognizedText = textRecognizer.process(inputImage).await()

        // Check if it's an Emirates ID card
        val fullText = recognizedText.text.lowercase(Locale.getDefault())
        if (!fullText.contains("resident identity card".lowercase(Locale.getDefault())) &&
            !fullText.contains("united arab emirates".lowercase(Locale.getDefault()))
        ) {
            return null // Not an Emirates ID
        }

        // Split text into lines
        val lines = recognizedText.text.split("\n")

        // Attributes
        var name: String? = null
        var number: String? = null
        var nationality: String? = null
        var sex: String? = null
        val eIdDates = mutableListOf<String>()

        for (line in lines) {
            val trimmedLine = line.trim()
            when {
                isDate(trimmedLine) -> eIdDates.add(trimmedLine)
                isName(trimmedLine) != null -> name = isName(trimmedLine)
                isNationality(trimmedLine) != null -> nationality = isNationality(trimmedLine)
                isSex(trimmedLine) != null -> sex = isSex(trimmedLine)
                isNumberID(trimmedLine) -> number = trimmedLine
            }
        }

        // Sort dates and extract DOB, issue, and expiry dates
        val sortedDates = sortDateList(eIdDates)

        textRecognizer.close()

        return EmirateIdModel(
            name = name,
            number = number,
            nationality = nationality,
            sex = sex,
            dateOfBirth = if (sortedDates.size == 3) sortedDates[0] else null,
            issueDate = if (sortedDates.size == 3) sortedDates[1] else null,
            expiryDate = if (sortedDates.size == 3) sortedDates[2] else null
        )
    }
*/

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
        return if (text.matches(Regex("[A-Z ]+")) && text.length > 3) text else null
    }

    private fun isNationality(text: String): String? {
        // Check common nationalities
        val nationalities = listOf("UAE", "INDIA", "PAKISTAN", "USA", "CANADA")
        return nationalities.find { text.contains(it, ignoreCase = true) }
    }

    private fun isSex(text: String): String? {
        return when {
            text.equals("male", ignoreCase = true) -> "Male"
            text.equals("female", ignoreCase = true) -> "Female"
            else -> null
        }
    }

    private fun isNumberID(text: String): Boolean {
        // Check if it's a valid ID number (e.g., numeric and length constraints)
        return text.matches(Regex("\\d{15}"))
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