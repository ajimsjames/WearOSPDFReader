package com.example.wearpdfreader.presentation

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
import java.io.File

@Composable
fun PDFExplorerScreen(
    onSelectSample: () -> Unit,
    onSelectUri: (Uri) -> Unit
) {
    val context = LocalContext.current
    val appFilesDir = context.getExternalFilesDir(null) ?: context.filesDir
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    var currentDir by remember {
        mutableStateOf(
            if (appFilesDir.exists() && (appFilesDir.listFiles()?.any { it.name.endsWith(".pdf", ignoreCase = true) } == true)) {
                appFilesDir
            } else if (downloadsDir.exists() && downloadsDir.isDirectory) {
                downloadsDir
            } else {
                Environment.getExternalStorageDirectory()
            }
        )
    }

    var refreshTrigger by remember { mutableStateOf(0) }

    val filesList = remember(currentDir, refreshTrigger) {
        currentDir.listFiles()
            ?.filter { !it.name.startsWith(".") && (it.isDirectory || it.name.endsWith(".pdf", ignoreCase = true)) }
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: emptyList()
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
                    text = if (currentDir.absolutePath.contains("com.example.wearpdfreader")) "📁 App Storage" else "📁 ${currentDir.name}",
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
                            .background(if (currentDir == appFilesDir) Color(0xFF1565C0) else Color(0xFF222222))
                            .clickable { currentDir = appFilesDir }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("App Docs", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 3.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (currentDir == downloadsDir) Color(0xFF1565C0) else Color(0xFF222222))
                            .clickable { currentDir = downloadsDir }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Downloads", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Clear App Docs Button
            if (currentDir == appFilesDir && filesList.any { !it.isDirectory }) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFD32F2F))
                            .clickable {
                                appFilesDir.listFiles()?.forEach { file ->
                                    if (file.name.endsWith(".pdf", ignoreCase = true)) {
                                        file.delete()
                                    }
                                }
                                Toast.makeText(context, "Cleared App Docs", Toast.LENGTH_SHORT).show()
                                refreshTrigger++
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🗑️ Delete All App Docs", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                        .clickable { onSelectSample() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📄 Built-in Sample PDF", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }

            if (filesList.isEmpty()) {
                item {
                    Text(
                        text = "No PDF files found",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            } else {
                items(filesList) { file ->
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
                                            onSelectUri(Uri.fromFile(file))
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
                                            if (file.delete()) {
                                                Toast.makeText(context, "Deleted $name", Toast.LENGTH_SHORT).show()
                                                refreshTrigger++
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🗑️", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
