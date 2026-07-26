package com.example.wearpdfreader.presentation

import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.*
import com.example.wearpdfreader.pdf.PdfFileInfo
import com.example.wearpdfreader.pdf.PdfFileScanner

@Composable
fun PDFListScreen(
    onSelectSample: () -> Unit,
    onSelectUri: (Uri) -> Unit
) {
    val context = LocalContext.current
    var pdfFiles by remember { mutableStateOf<List<PdfFileInfo>>(emptyList()) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Scan storage on screen load
    LaunchedEffect(Unit) {
        pdfFiles = PdfFileScanner.scanPdfFiles(context)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let { onSelectUri(it) }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 38.dp, bottom = 24.dp)
        ) {
            item {
                Text(
                    text = "📄 Wear PDF Reader",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            item {
                Chip(
                    onClick = {
                        try {
                            launcher.launch(arrayOf("application/pdf"))
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(
                                context,
                                "Scanning watch storage...",
                                Toast.LENGTH_SHORT
                            ).show()
                            pdfFiles = PdfFileScanner.scanPdfFiles(context)
                        }
                    },
                    label = { Text("Open System File Picker") },
                    colors = ChipDefaults.primaryChipColors(backgroundColor = Color(0xFFD50000)),
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                Chip(
                    onClick = onSelectSample,
                    label = { Text("Open Sample Doc") },
                    colors = ChipDefaults.secondaryChipColors(backgroundColor = Color(0xFF2E7D32)),
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
            }

            item {
                Text(
                    text = if (pdfFiles.isEmpty()) "No local PDFs found" else "PDFs on Watch (${pdfFiles.size})",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
            }

            if (pdfFiles.isNotEmpty()) {
                items(pdfFiles) { file ->
                    Chip(
                        onClick = { onSelectUri(file.uri) },
                        label = {
                            Text(
                                text = file.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        secondaryLabel = {
                            Text(
                                text = "${file.size / 1024} KB",
                                fontSize = 10.sp,
                                color = Color.LightGray
                            )
                        },
                        colors = ChipDefaults.secondaryChipColors(backgroundColor = Color(0xFF2C2C2E)),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(vertical = 2.dp)
                    )
                }
            }
        }

        // Curved Bezel Top Navigation Bar
        CurvedLayout(
            anchor = 270f,
            modifier = Modifier.fillMaxSize()
        ) {
            curvedComposable {
                BezelPill("📄 Docs", selected = true) { }
            }
            curvedComposable {
                Spacer(modifier = Modifier.width(4.dp))
            }
            curvedComposable {
                BezelPill("⚙️ About", selected = false) { showAboutDialog = true }
            }
        }

        // About App Dialog Modal
        if (showAboutDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF0000000))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1C1C1E))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚙️ About App", color = Color(0xFFFF3D00), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF333336))
                                .clickable { showAboutDialog = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            Text("📄 Wear PDF Reader v1.4.0", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("By Aju George", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(bottom = 6.dp))
                        }
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF2C2C2E))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("• Dark Mode PDF Inversion", color = Color.LightGray, fontSize = 9.sp)
                                Text("• Rotary Bezel Physical Scrolling", color = Color.LightGray, fontSize = 9.sp)
                                Text("• Page Pinch Zoom & Quick Jump", color = Color.LightGray, fontSize = 9.sp)
                                Text("• Target: Samsung Galaxy Watch 6", color = Color(0xFFFF3D00), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BezelPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFFFF3D00) else Color(0xFF2C2C2E))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.Gray,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
