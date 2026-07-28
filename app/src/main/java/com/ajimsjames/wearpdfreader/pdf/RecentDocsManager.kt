package com.ajimsjames.wearpdfreader.pdf

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class RecentPdfDoc(
    val name: String,
    val path: String,
    val uriString: String,
    val lastOpenedTimestamp: Long
)

object RecentDocsManager {

    private const val PREF_NAME = "recent_pdf_docs_pref"
    private const val KEY_RECENT_LIST = "recent_pdf_list_json"
    private const val MAX_RECENT = 30

    fun addRecentDoc(context: Context, name: String, path: String, uri: Uri) {
        try {
            val list = getRecentDocs(context).toMutableList()
            // Remove existing entry with same path or URI if present
            list.removeAll { it.path == path || it.uriString == uri.toString() }

            // Add new entry at top
            list.add(0, RecentPdfDoc(
                name = name,
                path = path,
                uriString = uri.toString(),
                lastOpenedTimestamp = System.currentTimeMillis()
            ))

            // Limit to MAX_RECENT
            val trimmedList = list.take(MAX_RECENT)
            saveRecentDocs(context, trimmedList)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getRecentDocs(context: Context): List<RecentPdfDoc> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_RECENT_LIST, null) ?: return emptyList()
        val result = mutableListOf<RecentPdfDoc>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val path = obj.optString("path", "")
                val uriStr = obj.optString("uriString", "")
                val file = File(path)

                // Verify file still exists if it's a file path
                if (path.isEmpty() || (path.startsWith("/") && !file.exists())) {
                    continue
                }

                result.add(
                    RecentPdfDoc(
                        name = obj.optString("name", "Document.pdf"),
                        path = path,
                        uriString = uriStr,
                        lastOpenedTimestamp = obj.optLong("lastOpenedTimestamp", 0L)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun removeRecentDoc(context: Context, path: String) {
        val list = getRecentDocs(context).filter { it.path != path }
        saveRecentDocs(context, list)
    }

    fun clearRecentDocs(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_RECENT_LIST).apply()
    }

    private fun saveRecentDocs(context: Context, list: List<RecentPdfDoc>) {
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("name", item.name)
                put("path", item.path)
                put("uriString", item.uriString)
                put("lastOpenedTimestamp", item.lastOpenedTimestamp)
            }
            jsonArray.put(obj)
        }
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_RECENT_LIST, jsonArray.toString()).apply()
    }
}
