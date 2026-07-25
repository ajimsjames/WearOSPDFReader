package com.example.wearpdfreader.pdf

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.regex.Pattern
import java.util.zip.InflaterInputStream

object PdfTextExtractor {

    /**
     * Extracts readable text content from a PDF input stream with FlateDecode (zlib) stream decompression.
     */
    fun extractText(inputStream: InputStream): String {
        return try {
            val bytes = inputStream.readBytes()
            val fullText = StringBuilder()

            // 1. Try extracting from raw uncompressed streams first
            val rawPdfContent = String(bytes, Charsets.ISO_8859_1)
            val uncompressedText = extractTextFromBlocks(rawPdfContent)
            if (uncompressedText.isNotBlank()) {
                fullText.append(uncompressedText).append(" ")
            }

            // 2. Scan and decompress FlateDecode zlib streams
            val decompressedText = extractFlateDecodeStreams(bytes)
            if (decompressedText.isNotBlank()) {
                fullText.append(decompressedText)
            }

            val result = fullText.toString().trim().replace(Regex("\\s+"), " ")
            result
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun extractFlateDecodeStreams(bytes: ByteArray): String {
        val sb = StringBuilder()
        val streamMarker = "stream".toByteArray(Charsets.ISO_8859_1)
        val endStreamMarker = "endstream".toByteArray(Charsets.ISO_8859_1)

        var searchIndex = 0
        while (searchIndex < bytes.size) {
            val streamStart = indexOf(bytes, streamMarker, searchIndex)
            if (streamStart == -1) break

            val streamEnd = indexOf(bytes, endStreamMarker, streamStart + streamMarker.size)
            if (streamEnd == -1) break

            // Skip newline after 'stream' keyword (\r\n or \n)
            var contentStart = streamStart + streamMarker.size
            if (contentStart < bytes.size && bytes[contentStart] == '\r'.code.toByte()) contentStart++
            if (contentStart < bytes.size && bytes[contentStart] == '\n'.code.toByte()) contentStart++

            val compressedLength = streamEnd - contentStart
            if (compressedLength > 0 && contentStart + compressedLength <= bytes.size) {
                val compressedData = bytes.copyOfRange(contentStart, contentStart + compressedLength)
                try {
                    val decompressed = decompressZlib(compressedData)
                    val decompressedStr = String(decompressed, Charsets.ISO_8859_1)
                    val textFromStream = extractTextFromBlocks(decompressedStr)
                    if (textFromStream.isNotBlank()) {
                        sb.append(textFromStream).append(" ")
                    }
                } catch (e: Exception) {
                    // Not a valid zlib stream or different filter, continue
                }
            }

            searchIndex = streamEnd + endStreamMarker.size
        }

        return sb.toString()
    }

    private fun decompressZlib(compressed: ByteArray): ByteArray {
        val inflater = InflaterInputStream(ByteArrayInputStream(compressed))
        val bos = ByteArrayOutputStream()
        val buffer = ByteArray(2048)
        var count: Int
        while (inflater.read(buffer).also { count = it } != -1) {
            bos.write(buffer, 0, count)
        }
        return bos.toByteArray()
    }

    private fun extractTextFromBlocks(content: String): String {
        val sb = StringBuilder()

        // Match BT ... ET blocks
        val btEtPattern = Pattern.compile("BT(.*?)ET", Pattern.DOTALL)
        val matcher = btEtPattern.matcher(content)

        while (matcher.find()) {
            val block = matcher.group(1) ?: continue

            // Match (string) Tj or TJ
            val tjPattern = Pattern.compile("\\((.*?)\\)\\s*(?:Tj|TJ|'|\")", Pattern.DOTALL)
            val tjMatcher = tjPattern.matcher(block)

            while (tjMatcher.find()) {
                val rawText = tjMatcher.group(1) ?: continue
                val clean = cleanPdfString(rawText)
                if (clean.isNotBlank()) {
                    sb.append(clean).append(" ")
                }
            }

            // Match [(str1) (str2)] TJ
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

        return sb.toString().trim()
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

    private fun indexOf(data: ByteArray, target: ByteArray, start: Int): Int {
        if (target.isEmpty()) return 0
        for (i in start..data.size - target.size) {
            var found = true
            for (j in target.indices) {
                if (data[i + j] != target[j]) {
                    found = false
                    break
                }
            }
            if (found) return i
        }
        return -1
    }
}
