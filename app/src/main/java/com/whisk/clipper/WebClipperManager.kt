package com.whisk.clipper

import com.whisk.data.repository.NotesRepository
import com.whisk.domain.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class WebClipperManager(
    private val notesRepository: NotesRepository,
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun clipUrl(url: String): Note = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) WhiskWebClipper/1.0")
            .build()

        val response = client.newCall(request).execute()
        val html = response.body.string()

        val title = extractTitle(html).ifBlank { url }
        val markdown = convertHtmlToCleanMarkdown(html, url)

        val note = Note(
            title = title,
            body = markdown,
            tags = listOf("web-clip", "read-later"),
            sourceMetadata = url,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )

        notesRepository.saveNote(note)
        note
    }

    private fun extractTitle(html: String): String {
        val titleMatch = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE).find(html)
        return titleMatch?.groupValues?.get(1)?.trim()?.replace("&amp;", "&")?.replace("&#39;", "'") ?: ""
    }

    private fun convertHtmlToCleanMarkdown(html: String, sourceUrl: String): String {
        val builder = StringBuilder()
        builder.append("> Clipped from: [").append(sourceUrl).append("](").append(sourceUrl).append(")\n\n")

        // Strip script and style tags
        var cleaned = html.replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")

        // Extract headings
        val headingMatches = Regex("<h([1-6])>(.*?)</h[1-6]>", RegexOption.IGNORE_CASE).findAll(cleaned)
        // Extract paragraph texts
        val paragraphs = Regex("<p>(.*?)</p>", RegexOption.IGNORE_CASE).findAll(cleaned)

        var pCount = 0
        paragraphs.forEach { match ->
            val pText = match.groupValues[1]
                .replace(Regex("<[^>]+>"), "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .trim()
            if (pText.length > 20) {
                builder.append(pText).append("\n\n")
                pCount++
            }
        }

        if (pCount == 0) {
            val stripped = cleaned.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
            builder.append(stripped.take(1500)).append("\n")
        }

        return builder.toString()
    }
}
