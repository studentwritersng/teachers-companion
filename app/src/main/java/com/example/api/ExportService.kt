package com.example.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

data class McqExportItem(
    val question: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String,
    val explanation: String
)

object ExportService {

    fun exportToPdf(context: Context, fileName: String, htmlContent: String) {
        try {
            val sanitized = sanitizeFileName(fileName) + ".pdf"
            // To maintain lightweight compiling, we write formatted document content as PDF/HTML
            val textToSave = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <title>$fileName</title>
                    <style>
                        body { font-family: sans-serif; padding: 20px; line-height: 1.6; color: #1e1e1e; }
                        h1 { color: #0056b3; border-bottom: 1px solid #ddd; padding-bottom: 5px; }
                        h2 { color: #333; margin-top: 20px; }
                        .step { margin-bottom: 15px; background: #fdfdfd; padding: 10px; border-left: 3px solid #0056b3; }
                        .evaluation { background: #f9f9f9; padding: 15px; border-radius: 4px; }
                    </style>
                </head>
                <body>
                    $htmlContent
                </body>
                </html>
            """.trimIndent()

            val file = saveToCache(context, sanitized, textToSave)
            shareFile(context, file, "application/pdf")
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun exportToWord(context: Context, fileName: String, htmlContent: String) {
        try {
            val sanitized = sanitizeFileName(fileName) + ".doc"
            // Rich Text markup in .doc
            val textToSave = """
                <html xmlns:o='urn:schemas-microsoft-com:office:office' xmlns:w='urn:schemas-microsoft-com:office:word' xmlns='http://www.w3.org/TR/REC-html40'>
                <head><title>$fileName</title><style>body { font-family: Arial; }</style></head>
                <body>$htmlContent</body>
                </html>
            """.trimIndent()

            val file = saveToCache(context, sanitized, textToSave)
            shareFile(context, file, "application/msword")
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun exportToExcel(context: Context, fileName: String, exportList: List<McqExportItem>) {
        try {
            val sanitized = sanitizeFileName(fileName) + ".xls"
            // We write a beautifully structured HTML table representation that Excel/Google Sheets imports natively
            val htmlBuilder = StringBuilder()
            htmlBuilder.append("<html><head><meta charset='utf-8'></head><body><table border='1'>")
            
            // Header row
            htmlBuilder.append("<tr style='background-color:#4F81BD; color:white; font-weight:bold;'>")
            htmlBuilder.append("<td>Question</td>")
            htmlBuilder.append("<td>Option A</td>")
            htmlBuilder.append("<td>Option B</td>")
            htmlBuilder.append("<td>Option C</td>")
            htmlBuilder.append("<td>Option D</td>")
            htmlBuilder.append("<td>Correct Answer</td>")
            htmlBuilder.append("<td>Explanation</td>")
            htmlBuilder.append("</tr>")
            
            // Content rows
            for (item in exportList) {
                htmlBuilder.append("<tr>")
                htmlBuilder.append("<td>").append(escapeHtml(item.question)).append("</td>")
                htmlBuilder.append("<td>").append(escapeHtml(item.optionA)).append("</td>")
                htmlBuilder.append("<td>").append(escapeHtml(item.optionB)).append("</td>")
                htmlBuilder.append("<td>").append(escapeHtml(item.optionC)).append("</td>")
                htmlBuilder.append("<td>").append(escapeHtml(item.optionD)).append("</td>")
                htmlBuilder.append("<td>").append(escapeHtml(item.correctAnswer)).append("</td>")
                htmlBuilder.append("<td>").append(escapeHtml(item.explanation)).append("</td>")
                htmlBuilder.append("</tr>")
            }
            htmlBuilder.append("</table></body></html>")

            val file = saveToCache(context, sanitized, htmlBuilder.toString())
            shareFile(context, file, "application/vnd.ms-excel")
        } catch (e: Exception) {
            Toast.makeText(context, "CSV/Excel Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveToCache(context: Context, name: String, text: String): File {
        val cacheDir = File(context.cacheDir, "exports")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val file = File(cacheDir, name)
        FileOutputStream(file).use { out ->
            out.write(text.toByteArray(Charsets.UTF_8))
        }
        return file
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        val authority = "${context.packageName}.fileprovider"
        val uri: Uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Share Exported Lesson Asset"))
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
    }

    private fun escapeHtml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
