package com.tryingthings.triviatroll

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

// This is not used. Came in from another project.
class ImageAnalyzer(
    private val onResults: (String) -> Unit
): ImageAnalysis.Analyzer {

    private var frameSkipCounter = 0
    private val imageAnalyzer = ImageProcessor()

    override fun analyze(image: ImageProxy) {
        if (frameSkipCounter % 180 == 0) {
//            var rotatedAndContrastAdjustedBitmap = imageAnalyzer.postProcess(image)
//
//            CoroutineScope(Dispatchers.IO).launch {
//                val result = imageAnalyzer.analyzeImageWithOpenAI(rotatedAndContrastAdjustedBitmap)
//                onResults(result)
//            }
        }
        frameSkipCounter++
        image.close()
    }

}