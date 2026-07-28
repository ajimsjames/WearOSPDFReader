package com.ajimsjames.wearpdfreader.pdf

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class PdfRendererManager(private val context: Context) {

    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    
    var pageCount: Int = 0
        private set

    var currentPageIndex: Int = 0
        private set

    var currentFileKey: String = ""
    val pdfKey: String get() = currentFileKey
    private val prefs: SharedPreferences = context.getSharedPreferences("pdf_bookmarks", Context.MODE_PRIVATE)

    // Memory cache for 1080px rendered bitmaps
    private val memoryCache: LruCache<Int, Bitmap> = object : LruCache<Int, Bitmap>(16) {
        override fun entryRemoved(evicted: Boolean, key: Int?, oldValue: Bitmap?, newValue: Bitmap?) {
            if (evicted && oldValue != null && !oldValue.isRecycled) {
                oldValue.recycle()
            }
        }
    }

    suspend fun openSamplePdf(): Boolean = withContext(Dispatchers.IO) {
        try {
            close()
            val sampleFile = File(context.cacheDir, "sample.pdf")
            if (!sampleFile.exists()) {
                context.assets.open("sample.pdf").use { input ->
                    FileOutputStream(sampleFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            openFileDescriptor(ParcelFileDescriptor.open(sampleFile, ParcelFileDescriptor.MODE_READ_ONLY), "sample.pdf")
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun openPdf(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            close()
            val fileKey = uri.toString()
            if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                if (file.exists()) {
                    val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    return@withContext openFileDescriptor(pfd, file.name)
                }
            }
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                openFileDescriptor(pfd, fileKey)
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun openFileDescriptor(pfd: ParcelFileDescriptor, key: String): Boolean {
        parcelFileDescriptor = pfd
        pdfRenderer = PdfRenderer(pfd)
        pageCount = pdfRenderer?.pageCount ?: 0
        currentFileKey = hashKey(key)

        val savedPage = prefs.getInt("page_$currentFileKey", 0)
        currentPageIndex = if (savedPage in 0 until pageCount) savedPage else 0
        return pageCount > 0
    }

    fun saveCurrentPageBookmark() {
        if (currentFileKey.isNotEmpty() && pageCount > 0) {
            prefs.edit().putInt("page_$currentFileKey", currentPageIndex).apply()
        }
    }

    suspend fun renderPage(pageIndex: Int): Bitmap? = withContext(Dispatchers.IO) {
        val renderer = pdfRenderer ?: return@withContext null
        if (pageIndex !in 0 until pageCount) return@withContext null

        currentPageIndex = pageIndex
        saveCurrentPageBookmark()

        val cached = memoryCache.get(pageIndex)
        if (cached != null && !cached.isRecycled) {
            return@withContext cached
        }

        try {
            renderer.openPage(pageIndex).use { page ->
                val renderWidth = 1080
                val renderHeight = (renderWidth * (page.height.toFloat() / page.width.toFloat())).toInt()
                
                val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                memoryCache.put(pageIndex, bitmap)
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun close() {
        saveCurrentPageBookmark()
        memoryCache.evictAll()
        try {
            pdfRenderer?.close()
            parcelFileDescriptor?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        pdfRenderer = null
        parcelFileDescriptor = null
        pageCount = 0
        currentPageIndex = 0
    }

    private fun hashKey(key: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(key.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            key.hashCode().toString()
        }
    }
}
