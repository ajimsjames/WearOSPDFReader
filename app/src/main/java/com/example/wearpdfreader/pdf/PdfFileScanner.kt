package com.example.wearpdfreader.pdf

import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File

data class PdfFileInfo(
    val name: String,
    val path: String,
    val size: Long,
    val uri: Uri
)

object PdfFileScanner {

    fun scanPdfFiles(context: Context): List<PdfFileInfo> {
        val pdfList = mutableListOf<PdfFileInfo>()
        
        // Scan standard storage locations on Wear OS
        val dirsToScan = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            Environment.getExternalStorageDirectory(),
            context.getExternalFilesDir(null),
            context.cacheDir
        )

        val searchedPaths = mutableSetOf<String>()

        for (dir in dirsToScan) {
            if (dir != null && dir.exists() && dir.isDirectory) {
                scanDirectory(dir, pdfList, searchedPaths, depth = 0)
            }
        }

        return pdfList.distinctBy { it.path }
    }

    private fun scanDirectory(
        dir: File, 
        result: MutableList<PdfFileInfo>, 
        searchedPaths: MutableSet<String>,
        depth: Int
    ) {
        if (depth > 3 || searchedPaths.contains(dir.absolutePath)) return
        searchedPaths.add(dir.absolutePath)

        try {
            val files = dir.listFiles() ?: return
            for (file in files) {
                if (file.isDirectory && !file.name.startsWith(".")) {
                    scanDirectory(file, result, searchedPaths, depth + 1)
                } else if (file.isFile && file.name.endsWith(".pdf", ignoreCase = true)) {
                    result.add(
                        PdfFileInfo(
                            name = file.name,
                            path = file.absolutePath,
                            size = file.length(),
                            uri = Uri.fromFile(file)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
