package com.example.wearpdfreader.presentation

import android.graphics.Bitmap
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Text
import com.example.wearpdfreader.pdf.PdfRendererManager
import com.example.wearpdfreader.ui.PdfCanvasView
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun PDFViewerScreen(
    pdfManager: PdfRendererManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentPage by remember { mutableStateOf(pdfManager.currentPageIndex) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isNightMode by remember { mutableStateOf(false) }
    var showSettingsModal by remember { mutableStateOf(false) }
    var showPageJumper by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }

    // TextToSpeech Engine initialization
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                ttsEngine = tts
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    fun loadPage(index: Int) {
        if (index !in 0 until pdfManager.pageCount) return
        isLoading = true
        currentPage = index
        coroutineScope.launch {
            currentBitmap = pdfManager.renderPage(index)
            isLoading = false
        }
    }

    LaunchedEffect(pdfManager) {
        loadPage(pdfManager.currentPageIndex)
        if (pdfManager.currentPageIndex > 0) {
            Toast.makeText(context, "Resumed at page ${pdfManager.currentPageIndex + 1}", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isNightMode) Color.Black else Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (currentBitmap != null) {
            AndroidView(
                factory = { ctx ->
                    PdfCanvasView(ctx).apply {
                        setPageBitmap(currentBitmap)
                        setNightMode(isNightMode)
                        onSingleTapListener = {
                            showSettingsModal = true
                        }
                        onSwipeNextPageListener = {
                            if (currentPage < pdfManager.pageCount - 1) {
                                loadPage(currentPage + 1)
                            }
                        }
                        onSwipePrevPageListener = {
                            if (currentPage > 0) {
                                loadPage(currentPage - 1)
                            }
                        }
                    }
                },
                update = { view ->
                    view.setPageBitmap(currentBitmap)
                    view.setNightMode(isNightMode)
                    view.onSingleTapListener = {
                        showSettingsModal = true
                    }
                    view.onSwipeNextPageListener = {
                        if (currentPage < pdfManager.pageCount - 1) {
                            loadPage(currentPage + 1)
                        }
                    }
                    view.onSwipePrevPageListener = {
                        if (currentPage > 0) {
                            loadPage(currentPage - 1)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text("Failed to render page", color = Color.Red, fontSize = 12.sp)
        }

        // Tap Hint Overlay at top
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x99000000))
                .clickable { showSettingsModal = true }
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(
                text = "${currentPage + 1} / ${pdfManager.pageCount} (Tap for Settings)",
                color = Color.LightGray,
                fontSize = 10.sp
            )
        }

        // Settings & Controls Modal Overlay
        if (showSettingsModal) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFEE000000))
                    .clickable { showSettingsModal = false }
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF222224))
                        .padding(14.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PDF Controls",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Page ${currentPage + 1} of ${pdfManager.pageCount}",
                            color = Color(0xFF81D4FA),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )

                        // 1. TTS Read Aloud Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSpeaking) Color(0xFFD32F2F) else Color(0xFF00E676))
                                .clickable {
                                    if (isSpeaking) {
                                        ttsEngine?.stop()
                                        isSpeaking = false
                                    } else {
                                        val speechText = "Page ${currentPage + 1} of ${pdfManager.pageCount}"
                                        ttsEngine?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "pdf_tts")
                                        isSpeaking = true
                                        Toast.makeText(context, "Reading page...", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isSpeaking) "🛑 Stop Voice Reader" else "🔊 Read Aloud TTS",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 2. Night Mode Toggle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isNightMode) Color(0xFFFFB300) else Color(0xFF333336))
                                .clickable {
                                    isNightMode = !isNightMode
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isNightMode) "☀️ Switch to Light Mode" else "🌙 Switch to Night Mode",
                                color = if (isNightMode) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 3. Jump to Page Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1565C0))
                                .clickable {
                                    showSettingsModal = false
                                    showPageJumper = true
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔢 Jump to Page", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // 4. Close PDF Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFD32F2F))
                                    .clickable {
                                        ttsEngine?.stop()
                                        showSettingsModal = false
                                        onBack()
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✕ Close PDF", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF444446))
                                    .clickable { showSettingsModal = false }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Resume", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Circular Page Jumper Picker Modal
        if (showPageJumper) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFA000000))
                    .clickable { showPageJumper = false }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.85f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1C1C1E))
                        .padding(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Jump to Page",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        val pageNumbers = (1..pdfManager.pageCount).toList()
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            items(pageNumbers) { pageNum ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .padding(vertical = 2.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (pageNum == currentPage + 1) Color(0xFF1565C0) else Color(0xFF2C2C2E))
                                        .clickable {
                                            showPageJumper = false
                                            loadPage(pageNum - 1)
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Page $pageNum",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { showPageJumper = false },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(28.dp)
                                .padding(top = 4.dp),
                            colors = ButtonDefaults.primaryButtonColors(backgroundColor = Color(0xFF444446))
                        ) {
                            Text("Close", color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
