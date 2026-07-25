package com.example.wearpdfreader.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfRendererManager(private val context: Context) {
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null

    // Cache high-res rendered pages in memory to eliminate render lag
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 6 // Cache up to ~20MB of high-res bitmaps
    private val bitmapCache = object : LruCache<Int, Bitmap>(cacheSize) {
        override fun sizeOf(key: Int, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    val pageCount: Int
        get() = pdfRenderer?.pageCount ?: 0

    suspend fun openPdf(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            close()
            fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            fileDescriptor?.let { pfd ->
                pdfRenderer = PdfRenderer(pfd)
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }

    suspend fun openSamplePdf(): Boolean = withContext(Dispatchers.IO) {
        try {
            close()
            val sampleFile = File(context.cacheDir, "sample_doc.pdf")
            if (!sampleFile.exists()) {
                val doc = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(1080, 1080, 1).create()
                
                // Page 1
                var page = doc.startPage(pageInfo)
                var canvas = page.canvas
                canvas.drawColor(Color.WHITE)
                var paint = Paint().apply {
                    color = Color.BLACK
                    textSize = 58f
                    isAntiAlias = true
                    isFakeBoldText = true
                    isSubpixelText = true
                }
                canvas.drawText("Galaxy Watch 6 PDF", 90f, 180f, paint)
                paint.textSize = 42f
                paint.color = Color.DKGRAY
                paint.isFakeBoldText = false
                canvas.drawText("Wear OS 4 Ultra Reader", 90f, 280f, paint)
                canvas.drawText("• Super-Sampled High-Res Text", 90f, 400f, paint)
                canvas.drawText("• Crisp Sub-pixel Anti-aliasing", 90f, 500f, paint)
                canvas.drawText("• Hardware Accelerated Zoom", 90f, 600f, paint)
                canvas.drawText("• Built-in File Explorer", 90f, 700f, paint)
                doc.finishPage(page)

                FileOutputStream(sampleFile).use { out ->
                    doc.writeTo(out)
                }
                doc.close()
            }
            fileDescriptor = ParcelFileDescriptor.open(sampleFile, ParcelFileDescriptor.MODE_READ_ONLY)
            fileDescriptor?.let { pfd ->
                pdfRenderer = PdfRenderer(pfd)
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }

    suspend fun renderPage(pageIndex: Int, targetWidth: Int = 1080): Bitmap? = withContext(Dispatchers.IO) {
        // Return cached high-res bitmap instantly if available
        bitmapCache.get(pageIndex)?.let { return@withContext it }

        pdfRenderer?.let { renderer ->
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null
            val page = renderer.openPage(pageIndex)
            
            // Render at high DPI resolution (1080px width) for super-sampled crisp text
            val scaleRatio = targetWidth.toFloat() / page.width.toFloat()
            val targetHeight = (page.height * scaleRatio).toInt()

            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            // Save to memory cache
            bitmapCache.put(pageIndex, bitmap)
            return@withContext bitmap
        }
        return@withContext null
    }

    fun close() {
        bitmapCache.evictAll()
        pdfRenderer?.close()
        fileDescriptor?.close()
        pdfRenderer = null
        fileDescriptor = null
    }
}
