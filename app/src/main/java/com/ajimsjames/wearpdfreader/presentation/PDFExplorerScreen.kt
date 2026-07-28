package com.ajimsjames.wearpdfreader.presentation

import android.net.Uri
import android.os.Environment
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.ajimsjames.wearpdfreader.pdf.RecentDocsManager
import com.ajimsjames.wearpdfreader.pdf.RecentPdfDoc
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PDFExplorerScreen(
    onSelectSample: () -> Unit,
    onSelectUri: (Uri) -> Unit
) {
    val context = LocalContext.current
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    var activeTab by remember { mutableStateOf("Recent") } // "Recent" or "Downloads"
    var currentDir by remember { mutableStateOf(downloadsDir) }
    var refreshTrigger by remember { mutableStateOf(0) }

    val recentDocs = remember(activeTab, refreshTrigger) {
        if (activeTab == "Recent") RecentDocsManager.getRecentDocs(context) else emptyList()
    }

    val downloadsFiles = remember(currentDir, activeTab, refreshTrigger) {
        if (activeTab == "Downloads") {
            currentDir.listFiles()
                ?.filter { !it.name.startsWith(".") && (it.isDirectory || it.name.endsWith(".pdf", ignoreCase = true)) }
                ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                ?: emptyList()
        } else emptyList()
    }

    fun openDoc(name: String, path: String, uri: Uri) {
        RecentDocsManager.addRecentDoc(context, name, path, uri)
        onSelectUri(uri)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 8.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {
            item {
                Text(
                    text = "Wear PDF Reader",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            item {
                Text(
                    text = if (activeTab == "Recent") "🕒 Recent Opened Docs" else "📁 Downloads Directory",
                    color = Color(0xFF90CAF9),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Storage Source Toggle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 3.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (activeTab == "Recent") Color(0xFF1565C0) else Color(0xFF222222))
                            .clickable { activeTab = "Recent" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Recent Docs", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 3.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (activeTab == "Downloads") Color(0xFF1565C0) else Color(0xFF222222))
                            .clickable { activeTab = "Downloads" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Downloads", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Clear Recent History Button
            if (activeTab == "Recent" && recentDocs.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFD32F2F))
                            .clickable {
                                RecentDocsManager.clearRecentDocs(context)
                                Toast.makeText(context, "Cleared Recent History", Toast.LENGTH_SHORT).show()
                                refreshTrigger++
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🗑️ Clear Recent History", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Sample PDF Button
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF2E7D32))
                        .clickable {
                            val sampleUri = Uri.parse("file:///sample.pdf")
                            RecentDocsManager.addRecentDoc(context, "Sample Doc.pdf", "/sample.pdf", sampleUri)
                            onSelectSample()
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📄 Built-in Sample PDF", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }

            // TAB 1: RECENT DOCS
            if (activeTab == "Recent") {
                if (recentDocs.isEmpty()) {
                    item {
                        Text(
                            text = "No recently opened PDFs",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                } else {
                    items(recentDocs) { doc ->
                        val formattedTime = remember(doc.lastOpenedTimestamp) {
                            try {
                                val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                                sdf.format(Date(doc.lastOpenedTimestamp))
                            } catch (e: Exception) { "" }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (doc.path == "/sample.pdf") {
                                                onSelectSample()
                                            } else {
                                                openDoc(doc.name, doc.path, Uri.parse(doc.uriString))
                                            }
                                        }
                                ) {
                                    Text(
                                        text = "📄 ${doc.name}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Opened $formattedTime",
                                        color = Color(0xFF00E676),
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF331111))
                                        .clickable {
                                            RecentDocsManager.removeRecentDoc(context, doc.path)
                                            refreshTrigger++
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🗑️", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // TAB 2: DOWNLOADS DIRECTORY
            if (activeTab == "Downloads") {
                if (downloadsFiles.isEmpty()) {
                    item {
                        Text(
                            text = "No PDF files in Downloads",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                } else {
                    items(downloadsFiles) { file ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (file.isDirectory) Color(0xFF1E2A38) else Color(0xFF1C1C1E))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (file.isDirectory) {
                                                currentDir = file
                                            } else {
                                                val uri = Uri.fromFile(file)
                                                openDoc(file.name, file.absolutePath, uri)
                                            }
                                        }
                                ) {
                                    Text(
                                        text = if (file.isDirectory) "📁 ${file.name}" else "📄 ${file.name}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!file.isDirectory) {
                                        Text(
                                            text = "${file.length() / 1024} KB",
                                            color = Color.Gray,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }

                                if (!file.isDirectory) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF331111))
                                            .clickable {
                                                val name = file.name
                                                val path = file.absolutePath
                                                if (file.delete()) {
                                                    RecentDocsManager.removeRecentDoc(context, path)
                                                    Toast.makeText(context, "Deleted $name", Toast.LENGTH_SHORT).show()
                                                    refreshTrigger++
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🗑️", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
