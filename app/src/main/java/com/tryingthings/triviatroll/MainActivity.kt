package com.tryingthings.triviatroll

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.tryingthings.triviatroll.ui.theme.TriviatrollTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    private lateinit var cameraController: LifecycleCameraController
    private lateinit var analyzer: ImageAnalyzer

    private val viewModel by viewModels<MainViewModel>()
    private val zoomLevels = listOf(0.2f, 0.3f, 0.4f)
    private var currentZoomIndex = 0

    private val _triggerFlash = MutableStateFlow(false)
    private val triggerFlash: StateFlow<Boolean> = _triggerFlash.asStateFlow()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(!hasCameraPermission()) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), 0
            )
        }
        if(!hasInternetPermission()) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.INTERNET), 0
            )
        }
        setContent {
            TriviatrollTheme {
//                var theAnswer by remember {
//                    mutableStateOf(String())
//                }

                val viewModel = viewModel<MainViewModel>()
                val theAnswer by viewModel.theAnswer

//                analyzer = remember {
//                    LandmarkImageAnalyzer(
//                        onResults = {
//                            theAnswer = it
//                        }
//                    )
//                }
                cameraController = remember {
                    LifecycleCameraController(applicationContext).apply {
                        setEnabledUseCases(
                            CameraController.IMAGE_CAPTURE or
                                    CameraController.VIDEO_CAPTURE)
//                              CameraController.IMAGE_ANALYSIS
//                        setImageAnalysisAnalyzer(
//                            ContextCompat.getMainExecutor(applicationContext),
//                            analyzer
//                        )
                        setLinearZoom(zoomLevels[currentZoomIndex])
                    }
                }
                AnswerLayout(theAnswer, Modifier.fillMaxSize())
            }
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun AnswerLayout(
        answer: String,
        modifier: Modifier = Modifier
    ) {
        val configuration = LocalConfiguration.current
        val colorScheme = MaterialTheme.colorScheme

        // Define color map using Material Design colors
        val answerColorMap = mapOf(
            "A" to Color(0xFF2196F3), // Blue 500
            "B" to Color(0xFFF44336), // Red 500
            "C" to Color(0xFF4CAF50), // Green 500
            "D" to Color(0xFFFFEB3B), // Yellow 500
            "E" to Color(0xFF9C27B0), // Purple 500
            "F" to Color(0xFFFF9800), // Orange 500
            "1" to Color(0xFF00BCD4), // Cyan 500
            "2" to Color(0xFF673AB7), // Deep Purple 500
            "3" to Color(0xFF009688), // Teal 500
            "4" to Color(0xFFE91E63), // Pink 500
            "5" to Color(0xFF3F51B5), // Indigo 500
            "6" to Color(0xFF8BC34A)  // Light Green 500
        )

        val answers = answer.split(",").map { it.trim() }

        // Collect the triggerFlash state
        val shouldFlash by triggerFlash.collectAsState()

        // Add this block to create the flash animation
        var animationKey by remember { mutableStateOf(0) }
        val flashAlpha by animateFloatAsState(
            targetValue = if (animationKey % 2 == 0) 0f else 0.3f,
            animationSpec = tween(durationMillis = 200),
            label = "flashAlpha"
        )

        LaunchedEffect(key1 = shouldFlash) {
            if (shouldFlash) {
                animationKey++
                delay(200)
                animationKey++
                _triggerFlash.value = false // Reset the trigger
            }
        }

        val content: @Composable (Modifier) -> Unit = { boxModifier ->
            Surface(
                modifier = boxModifier.fillMaxSize(),
                color = colorScheme.background,
                tonalElevation = 2.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        answers.forEach { currentAnswer ->
                            val backgroundColor = if (currentAnswer.length == 1) {
                                answerColorMap[currentAnswer.uppercase()] ?: colorScheme.background
                            } else {
                                colorScheme.background
                            }

                            Surface(
                                color = backgroundColor,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AdaptiveText(
                                        text = currentAnswer,
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.onBackground,
                                            textAlign = TextAlign.Center
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                    // Add this to create the flash effect
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = flashAlpha))
                    )
                }
            }
        }

        when (configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                Row(modifier = modifier.fillMaxSize()) {
                    Box(modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()) {
                        CameraPreview(cameraController, Modifier.fillMaxSize())
                    }
                    Box(modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()) {
                        content(Modifier.fillMaxSize())
                    }
                }
            }
            else -> {
                Column(modifier = modifier.fillMaxSize()) {
                    Box(modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()) {
                        CameraPreview(cameraController, Modifier.fillMaxSize())
                    }
                    Box(modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()) {
                        content(Modifier.fillMaxSize())
                    }
                }
            }
        }
    }

    @Composable
    fun AdaptiveText(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = TextStyle(),
        maxLines: Int = Int.MAX_VALUE
    ) {
        val fontSize = when (text.length) {
            1 -> MaterialTheme.typography.displayLarge.fontSize
            in 2..3 -> MaterialTheme.typography.displayMedium.fontSize
            in 4..5 -> MaterialTheme.typography.headlineLarge.fontSize
            in 6..10 -> MaterialTheme.typography.headlineMedium.fontSize
            else -> MaterialTheme.typography.bodyLarge.fontSize
        }

        Text(
            text = text,
            modifier = modifier,
            style = style.copy(
                fontSize = fontSize,
                shadow = Shadow(Color.Black, Offset(5f, 5f), 2f),
            ),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }

    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun hasInternetPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.INTERNET
    ) == PackageManager.PERMISSION_GRANTED

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                captureImage()
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                cycleZoom()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun cycleZoom() {
        currentZoomIndex = (currentZoomIndex + 1) % zoomLevels.size
        val newZoom = zoomLevels[currentZoomIndex]
        cameraController.setLinearZoom(newZoom)
    }

    private fun captureImage(
    ) {
        cameraController.takePicture(
            ContextCompat.getMainExecutor(applicationContext),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    var imageAnalyzer = ImageProcessor()
                    var rotatedAndContrastAdjustedBitmap = ImageProcessor().postProcess(image)
                    CoroutineScope(Dispatchers.IO).launch {
                        val OPENAI_API_KEY = BuildConfig.OPENAI_API_KEY
                        val result = imageAnalyzer.analyzeImageWithOpenAI(rotatedAndContrastAdjustedBitmap, OPENAI_API_KEY)
                        viewModel.updateAnswer(result)
                        _triggerFlash.value = true
                    }

                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Log.e("Camera", "Couldn't take photo: ", exception)
                }
            }
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TriviatrollTheme {
        Greeting("Android")
    }
}