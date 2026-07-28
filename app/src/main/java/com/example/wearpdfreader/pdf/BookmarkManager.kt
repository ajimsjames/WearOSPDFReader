package com.example.wearpdfreader.pdf

import android.content.Context
import android.content.SharedPreferences

class BookmarkManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("wear_pdf_bookmarks", Context.MODE_PRIVATE)

    fun isBookmarked(pdfPath: String, pageIndex: Int): Boolean {
        val set = prefs.getStringSet(pdfPath, emptySet()) ?: emptySet()
        return set.contains(pageIndex.toString())
    }

    fun toggleBookmark(pdfPath: String, pageIndex: Int): Boolean {
        val currentSet = prefs.getStringSet(pdfPath, emptySet())?.toMutableSet() ?: mutableSetOf()
        val pageStr = pageIndex.toString()
        val isAdded: Boolean
        if (currentSet.contains(pageStr)) {
            currentSet.remove(pageStr)
            isAdded = false
        } else {
            currentSet.add(pageStr)
            isAdded = true
        }
        prefs.edit().putStringSet(pdfPath, currentSet).apply()
        return isAdded
    }

    fun getBookmarks(pdfPath: String): List<Int> {
        val set = prefs.getStringSet(pdfPath, emptySet()) ?: emptySet()
        return set.mapNotNull { it.toIntOrNull() }.sorted()
    }

    fun isNightModeEnabled(): Boolean {
        return prefs.getBoolean("night_mode_enabled", false)
    }

    fun setNightModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("night_mode_enabled", enabled).apply()
    }
}
