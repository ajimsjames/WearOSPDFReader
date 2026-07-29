package com.ajimsjames.wearpdfreader

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ajimsjames.wearpdfreader.pdf.PdfRendererManager
import com.ajimsjames.wearpdfreader.presentation.PDFExplorerScreen
import com.ajimsjames.wearpdfreader.presentation.PDFViewerScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var pdfManager: PdfRendererManager
    private val currentScreenState = mutableStateOf<Screen>(Screen.Explorer)
    private val isPermissionGranted = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pdfManager = PdfRendererManager(applicationContext)

        checkStoragePermission()
        handleIntent(intent)

        setContent {
            MaterialTheme {
                val currentScreen by currentScreenState
                val coroutineScope = rememberCoroutineScope()
                val hasPermission by isPermissionGranted

                if (!hasPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "🔒 Storage Permission",
                                color = Color(0xFFFFB300),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                text = "Please open the Companion App on your phone and click 'Grant Files Access' to enable.",
                                color = Color.White,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF2C2C2E))
                                    .clickable {
                                        checkStoragePermission()
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🔄 Check Again",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
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
    }

    override fun onResume() {
        super.onResume()
        checkStoragePermission()
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

    private fun checkStoragePermission() {
        isPermissionGranted.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfManager.close()
    }
}

sealed class Screen {
    object Explorer : Screen()
    object Viewer : Screen()
}
