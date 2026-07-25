package com.example.wearpdfreader.presentation

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
    var showPageJumper by remember { mutableStateOf(false) }

    // Load page bitmap
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
            Toast.makeText(context, "Resumed from page ${pdfManager.currentPageIndex + 1}", Toast.LENGTH_SHORT).show()
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
                    }
                },
                update = { view ->
                    view.setPageBitmap(currentBitmap)
                    view.setNightMode(isNightMode)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text("Failed to render page", color = Color.Red, fontSize = 12.sp)
        }

        // Top Toolbar (Exit, Night Mode, Page Jumper)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .fillMaxWidth(0.9f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Exit Button
            Button(
                onClick = onBack,
                modifier = Modifier.size(28.dp),
                colors = ButtonDefaults.primaryButtonColors(backgroundColor = Color(0xDDCC3333))
            ) {
                Text("✕", color = Color.White, fontSize = 12.sp)
            }

            // Page Jumper Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xDD1C1C1E))
                    .clickable { showPageJumper = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${currentPage + 1} / ${pdfManager.pageCount}",
                    color = Color(0xFF81D4FA),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Night Mode Toggle Button
            Button(
                onClick = { isNightMode = !isNightMode },
                modifier = Modifier.size(28.dp),
                colors = ButtonDefaults.primaryButtonColors(
                    backgroundColor = if (isNightMode) Color(0xFFFFB300) else Color(0xDD333333)
                )
            ) {
                Text(
                    text = if (isNightMode) "☀️" else "🌙",
                    fontSize = 12.sp
                )
            }
        }

        // Bottom Page Prev / Next Navigation Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .fillMaxWidth(0.85f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Page
            Button(
                onClick = { if (currentPage > 0) loadPage(currentPage - 1) },
                enabled = currentPage > 0,
                modifier = Modifier.size(32.dp),
                colors = ButtonDefaults.primaryButtonColors(backgroundColor = Color(0xDD2C2C2E))
            ) {
                Text("◀", color = Color.White, fontSize = 12.sp)
            }

            // Next Page
            Button(
                onClick = { if (currentPage < pdfManager.pageCount - 1) loadPage(currentPage + 1) },
                enabled = currentPage < pdfManager.pageCount - 1,
                modifier = Modifier.size(32.dp),
                colors = ButtonDefaults.primaryButtonColors(backgroundColor = Color(0xDD2C2C2E))
            ) {
                Text("▶", color = Color.White, fontSize = 12.sp)
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
