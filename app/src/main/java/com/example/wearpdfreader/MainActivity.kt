package com.example.wearpdfreader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.wear.compose.material.MaterialTheme
import com.example.wearpdfreader.pdf.PdfRendererManager
import com.example.wearpdfreader.presentation.PDFExplorerScreen
import com.example.wearpdfreader.presentation.PDFViewerScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class Screen {
    object Explorer : Screen()
    object Viewer : Screen()
}

class MainActivity : ComponentActivity() {
    private lateinit var pdfManager: PdfRendererManager
    private val currentScreenState = mutableStateOf<Screen>(Screen.Explorer)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pdfManager = PdfRendererManager(applicationContext)

        handleIntent(intent)

        setContent {
            MaterialTheme {
                val currentScreen by currentScreenState
                val coroutineScope = rememberCoroutineScope()

                when (currentScreen) {
                    Screen.Explorer -> {
                        PDFExplorerScreen(
                            onSelectSample = {
                                coroutineScope.launch {
                                    if (pdfManager.openSamplePdf()) {
                                        currentScreenState.value = Screen.Viewer
                                    }
                                }
                            },
                            onSelectUri = { uri: Uri ->
                                coroutineScope.launch {
                                    if (pdfManager.openPdf(uri)) {
                                        currentScreenState.value = Screen.Viewer
                                    }
                                }
                            }
                        )
                    }
                    Screen.Viewer -> {
                        PDFViewerScreen(
                            pdfManager = pdfManager,
                            onBack = {
                                pdfManager.close()
                                currentScreenState.value = Screen.Explorer
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri: Uri? = intent?.data
        if (uri != null) {
            CoroutineScope(Dispatchers.Main).launch {
                if (pdfManager.openPdf(uri)) {
                    currentScreenState.value = Screen.Viewer
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfManager.close()
    }
}
