package com.example.wearpdfreader.presentation

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.wear.compose.material.*
import com.example.wearpdfreader.pdf.PdfRendererManager
import com.example.wearpdfreader.ui.PdfCanvasView

@Composable
fun PDFViewerScreen(
    pdfManager: PdfRendererManager,
    onBack: () -> Unit
) {
    var currentPage by remember { mutableStateOf(0) }
    var totalPages by remember { mutableStateOf(pdfManager.pageCount) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Render Page
    LaunchedEffect(currentPage) {
        isLoading = true
        totalPages = pdfManager.pageCount
        currentBitmap = pdfManager.renderPage(currentPage, targetWidth = 1080)
        isLoading = false
    }

    Scaffold(
        positionIndicator = {
            if (totalPages > 0) {
                PositionIndicator(
                    value = { currentPage.toFloat() },
                    range = 0f..(totalPages - 1).coerceAtLeast(1).toFloat()
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                // Native Skia Hardware View - Zero Gesture Interfering Swipe
                AndroidView(
                    factory = { context ->
                        PdfCanvasView(context).apply {
                            setPdfBitmap(currentBitmap)
                        }
                    },
                    update = { view ->
                        view.setPdfBitmap(currentBitmap)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Top Exit / Back Button Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                ) {
                    Button(
                        onClick = onBack,
                        modifier = Modifier.size(32.dp),
                        colors = ButtonDefaults.primaryButtonColors(backgroundColor = Color(0xDDCC3333))
                    ) {
                        Text("✕", color = Color.White, fontSize = 14.sp)
                    }
                }

                // Page Navigation Control Overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                        .background(Color(0xDD111111), shape = CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { if (currentPage > 0) currentPage-- },
                        enabled = currentPage > 0,
                        modifier = Modifier.size(28.dp),
                        colors = ButtonDefaults.primaryButtonColors(backgroundColor = Color(0xFF333333))
                    ) {
                        Text("<", color = Color.White, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "${currentPage + 1}/$totalPages",
                        color = Color.White,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = { if (currentPage < totalPages - 1) currentPage++ },
                        enabled = currentPage < totalPages - 1,
                        modifier = Modifier.size(28.dp),
                        colors = ButtonDefaults.primaryButtonColors(backgroundColor = Color(0xFF333333))
                    ) {
                        Text(">", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
