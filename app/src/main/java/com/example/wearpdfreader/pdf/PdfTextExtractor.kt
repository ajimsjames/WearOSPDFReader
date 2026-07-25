package com.example.wearpdfreader.pdf

import java.io.InputStream
import java.util.regex.Pattern

object PdfTextExtractor {

    /**
     * Extracts readable text content from a PDF input stream.
     */
    fun extractText(inputStream: InputStream): String {
        return try {
            val bytes = inputStream.readBytes()
            val pdfContent = String(bytes, Charsets.ISO_8859_1)
            extractTextFromPdfContent(pdfContent)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun extractTextFromPdfContent(content: String): String {
        val sb = StringBuilder()
        
        // Find all Text Blocks between BT (Begin Text) and ET (End Text)
        val btEtPattern = Pattern.compile("BT(.*?)ET", Pattern.DOTALL)
        val matcher = btEtPattern.matcher(content)

        while (matcher.find()) {
            val block = matcher.group(1) ?: continue
            
            // Match strings enclosed in ( ) followed by Tj or TJ operators
            val tjPattern = Pattern.compile("\\((.*?)\\)\\s*(?:Tj|TJ|'|\")", Pattern.DOTALL)
            val tjMatcher = tjPattern.matcher(block)

            while (tjMatcher.find()) {
                val rawText = tjMatcher.group(1) ?: continue
                val clean = cleanPdfString(rawText)
                if (clean.isNotBlank()) {
                    sb.append(clean).append(" ")
                }
            }

            // Match array text strings [(Text1) (Text2)] TJ
            val arrayTjPattern = Pattern.compile("\\[(.*?)\\]\\s*TJ", Pattern.DOTALL)
            val arrayMatcher = arrayTjPattern.matcher(block)

            while (arrayMatcher.find()) {
                val arrayContent = arrayMatcher.group(1) ?: continue
                val strPattern = Pattern.compile("\\((.*?)\\)")
                val strMatcher = strPattern.matcher(arrayContent)
                while (strMatcher.find()) {
                    val rawStr = strMatcher.group(1) ?: continue
                    val clean = cleanPdfString(rawStr)
                    if (clean.isNotBlank()) {
                        sb.append(clean)
                    }
                }
                sb.append(" ")
            }
        }

        val result = sb.toString().trim().replace(Regex("\\s+"), " ")
        return result
    }

    private fun cleanPdfString(raw: String): String {
        var str = raw
        str = str.replace("\\n", "\n")
        str = str.replace("\\r", "\r")
        str = str.replace("\\t", "\t")
        str = str.replace("\\(", "(")
        str = str.replace("\\)", ")")
        str = str.replace("\\\\", "\\")
        return str.filter { it.code in 32..126 || it == '\n' || it == ' ' }
    }
}
