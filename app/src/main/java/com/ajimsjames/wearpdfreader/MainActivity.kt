package com.ajimsjames.wearpdfreader

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.wear.compose.material.MaterialTheme
import com.ajimsjames.wearpdfreader.pdf.PdfRendererManager
import com.ajimsjames.wearpdfreader.presentation.PDFExplorerScreen
import com.ajimsjames.wearpdfreader.presentation.PDFViewerScreen
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

        // Request standard storage permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                101
            )
        }

        // Safe Request Manage External Storage permission on Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                if (!Environment.isExternalStorageManager()) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivity(intent)
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to open storage settings activity", e)
            }
        }

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
