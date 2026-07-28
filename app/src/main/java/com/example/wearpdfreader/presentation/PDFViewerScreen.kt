package com.example.wearpdfreader.presentation

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.wearpdfreader.pdf.BookmarkManager
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
    val bookmarkManager = remember { BookmarkManager(context) }

    var currentPage by remember { mutableStateOf(pdfManager.currentPageIndex) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var isNightMode by remember { mutableStateOf(bookmarkManager.isNightModeEnabled()) }
    var isCurrentBookmarked by remember { mutableStateOf(bookmarkManager.isBookmarked(pdfManager.pdfKey, currentPage)) }
    var savedBookmarks by remember { mutableStateOf(bookmarkManager.getBookmarks(pdfManager.pdfKey)) }

    var showSettingsModal by remember { mutableStateOf(false) }
    var showPageJumper by remember { mutableStateOf(false) }
    var showBookmarksModal by remember { mutableStateOf(false) }

    fun loadPage(index: Int) {
        if (index !in 0 until pdfManager.pageCount) return
        isLoading = true
        currentPage = index
        isCurrentBookmarked = bookmarkManager.isBookmarked(pdfManager.pdfKey, index)
        coroutineScope.launch {
            currentBitmap = pdfManager.renderPage(index)
            isLoading = false
        }
    }

    LaunchedEffect(pdfManager) {
        loadPage(pdfManager.currentPageIndex)
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

        // =========================================================
        // TOP OVERLAY: PAGE COUNTER + QUICK NIGHT MODE & BOOKMARK TOGGLE
        // =========================================================
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
                .fillMaxWidth(0.92f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 🌙 / ☀️ OLED Night Mode Toggle Button
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isNightMode) Color(0xFFFFB300) else Color(0xAA2C2C2E))
                    .clickable {
                        isNightMode = !isNightMode
                        bookmarkManager.setNightModeEnabled(isNightMode)
                        Toast.makeText(
                            context,
                            if (isNightMode) "🌙 Pitch Black Dark Mode ON" else "☀️ Light Mode ON",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .padding(6.dp)
            ) {
                Text(
                    text = if (isNightMode) "☀️" else "🌙",
                    fontSize = 11.sp
                )
            }

            // Center Page Info Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC1C1C1E))
                    .clickable { showSettingsModal = true }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCurrentBookmarked) {
                        Text("🔖 ", fontSize = 10.sp)
                    }
                    Text(
                        text = "${currentPage + 1} / ${pdfManager.pageCount}",
                        color = if (isCurrentBookmarked) Color(0xFFFFD600) else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 🔖 One-Tap Quick Bookmark Toggle Button
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isCurrentBookmarked) Color(0xFFFFD600) else Color(0xAA2C2C2E))
                    .clickable {
                        val added = bookmarkManager.toggleBookmark(pdfManager.pdfKey, currentPage)
                        isCurrentBookmarked = added
                        savedBookmarks = bookmarkManager.getBookmarks(pdfManager.pdfKey)
                        Toast.makeText(
                            context,
                            if (added) "🔖 Page ${currentPage + 1} Bookmarked!" else "🗑️ Bookmark Removed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .padding(6.dp)
            ) {
                Text(
                    text = "🔖",
                    fontSize = 11.sp
                )
            }
        }

        // =========================================================
        // SETTINGS & PDF CONTROLS MODAL OVERLAY (SCROLLABLE)
        // =========================================================
        if (showSettingsModal) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE000000))
                    .clickable { showSettingsModal = false }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.9f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1E1E24))
                        .padding(10.dp)
                ) {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PDF Controls",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Page ${currentPage + 1} of ${pdfManager.pageCount}",
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )

                        // 1. OLED Pitch Black Dark Mode Toggle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isNightMode) Color(0xFFFFB300) else Color(0xFF2C2C30))
                                .clickable {
                                    isNightMode = !isNightMode
                                    bookmarkManager.setNightModeEnabled(isNightMode)
                                }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isNightMode) "☀️ Invert to Light Mode" else "🌙 Pitch Black OLED Mode",
                                color = if (isNightMode) Color.Black else Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 2. Bookmark Current Page Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCurrentBookmarked) Color(0xFFFFD600) else Color(0xFF2979FF))
                                .clickable {
                                    val added = bookmarkManager.toggleBookmark(pdfManager.pdfKey, currentPage)
                                    isCurrentBookmarked = added
                                    savedBookmarks = bookmarkManager.getBookmarks(pdfManager.pdfKey)
                                }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isCurrentBookmarked) "🔖 Remove Bookmark" else "🔖 Bookmark Page ${currentPage + 1}",
                                color = if (isCurrentBookmarked) Color.Black else Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 3. View Saved Bookmarks Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF7C4DFF))
                                .clickable {
                                    showSettingsModal = false
                                    savedBookmarks = bookmarkManager.getBookmarks(pdfManager.pdfKey)
                                    showBookmarksModal = true
                                }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📋 Saved Bookmarks (${savedBookmarks.size})",
                                color = Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 4. Jump to Page Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF00C853))
                                .clickable {
                                    showSettingsModal = false
                                    showPageJumper = true
                                }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔢 Jump to Page", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }

                        // 5. Close Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFD32F2F))
                                .clickable {
                                    showSettingsModal = false
                                    onBack()
                                }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕ Close Document", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }

        // =========================================================
        // SAVED BOOKMARKS LIST MODAL
        // =========================================================
        if (showBookmarksModal) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFA000000))
                    .clickable { showBookmarksModal = false }
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight(0.88f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1E1E24))
                        .padding(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🔖 Bookmarks Manager",
                            color = Color(0xFFFFD600),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        if (savedBookmarks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No saved bookmarks yet.\nTap 🔖 on any page to save it!",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                items(savedBookmarks) { pageIdx ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.9f)
                                            .padding(vertical = 3.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (pageIdx == currentPage) Color(0xFF7C4DFF) else Color(0xFF2C2C30))
                                            .clickable {
                                                showBookmarksModal = false
                                                loadPage(pageIdx)
                                            }
                                            .padding(vertical = 7.dp, horizontal = 10.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "🔖 Page ${pageIdx + 1}",
                                                color = Color.White,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (pageIdx == currentPage) "Current" else "Jump ➔",
                                                color = Color(0xFF00E5FF),
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { showBookmarksModal = false },
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

        // =========================================================
        // PAGE JUMPER MODAL
        // =========================================================
        if (showPageJumper) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFA000000))
                    .clickable { showPageJumper = false }
                    .padding(14.dp),
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
                            fontSize = 12.sp,
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
                                        .background(if (pageNum == currentPage + 1) Color(0xFF00C853) else Color(0xFF2C2C2E))
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
                                        fontSize = 10.5.sp,
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
