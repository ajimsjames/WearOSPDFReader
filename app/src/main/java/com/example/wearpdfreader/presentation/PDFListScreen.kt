package com.example.wearpdfreader.presentation

import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Scaffold {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "Wear PDF Reader",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
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
                                "System File Picker not available on Wear OS. Scanning watch storage...",
                                Toast.LENGTH_LONG
                            ).show()
                            pdfFiles = PdfFileScanner.scanPdfFiles(context)
                        }
                    },
                    label = { Text("Open System File Picker") },
                    colors = ChipDefaults.primaryChipColors(backgroundColor = Color(0xFF1565C0)),
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
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
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
                        colors = ChipDefaults.secondaryChipColors(backgroundColor = Color(0xFF333333)),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}
