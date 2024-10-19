package com.tryingthings.triviatroll

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Base64
import android.util.Log
import androidx.camera.core.ImageProxy
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.ImagePart
import com.aallam.openai.api.chat.TextPart
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import java.io.ByteArrayOutputStream
import kotlin.time.Duration.Companion.seconds

class ImageProcessor() {

    fun postProcess(image: ImageProxy): Bitmap {
        val originalBitmap = image.toBitmap()
        val newWidth = (originalBitmap.width * 1).toInt()
        val newHeight = (originalBitmap.height * 1).toInt()
        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, false)

        val matrix = Matrix().apply {
            // Apply rotation
            postRotate(image.imageInfo.rotationDegrees.toFloat())
        }

        val rotatedAndContrastAdjustedBitmap = Bitmap.createBitmap(
            scaledBitmap,
            0,
            0,
            scaledBitmap.width,
            scaledBitmap.height,
            matrix,
            true
        )

        return rotatedAndContrastAdjustedBitmap
    }

    suspend fun analyzeImageWithOpenAI(bitmap: Bitmap, token: String): String {
        val question = GetTextFromImage(bitmap, token)

        val openai = OpenAI(
            token = token,
            timeout = Timeout(socket = 60.seconds)
        )

        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId("gpt-4o"),
            temperature = 0.0,
            messages = listOf(
                ChatMessage(
                    role = ChatRole.System,
                    content = "You are a helpful Netflix Trivia Quest assistant."
                ),
                ChatMessage(
                    role = ChatRole.User,
                    content = listOf(
                        TextPart("""
                            <Question>
                            {$question}
                            </Question>
                            
                            Respond with the answer(s) in comma delimited string.

                            Examples
                            A,C,D
                            1,3,4
                            """.trimMargin()),
                    )
                )
            )
        )
        val completion: ChatCompletion = openai.chatCompletion(chatCompletionRequest)
        val response = completion.choices.first().message

        return response.content.orEmpty()
    }

    suspend private fun GetTextFromImage(bitmap: Bitmap, token: String): String {
        val base64Image = bitmapToBase64(bitmap)

        val openai = OpenAI(
            token = token,
            timeout = Timeout(socket = 60.seconds)
        )

        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId("gpt-4o"),
            temperature = 0.0,
            messages = listOf(
                ChatMessage(
                    role = ChatRole.System,
                    content = "You are a helpful assistant."
                ),
                ChatMessage(
                    role = ChatRole.User,
                    content = listOf(
                        TextPart("""
                            Extract the question and answer options from the image.
                            
                            Respond in json format
                            {
                                "question": "", // The entire question
                                "answer_options": [] // The answer options
                            }
                           
                            
                            Examples
                            {"question": "what is an APIM?", "answers": ["A: cat", "B: dog", "C: bird", "D: fish"]}
                            """.trimMargin()),
                        ImagePart("data:image/jpeg;base64,{$base64Image}")
                    )
                )
            )
        )
        val completion: ChatCompletion = openai.chatCompletion(chatCompletionRequest)
        val response = completion.choices.first().message

        return response.content.orEmpty()
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()

        //Before sending the image to the API, log its format, size, and any other relevant information to help identify potential issues.
        // Log relevant information
        val imageSizeInBytes = byteArray.size
        val imageFormat = "JPEG" // Since we're using JPEG compression
        Log.d("ImageInfo", "Format: $imageFormat, Size: $imageSizeInBytes bytes")

        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}