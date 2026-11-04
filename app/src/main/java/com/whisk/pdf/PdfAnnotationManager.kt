package com.whisk.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.whisk.data.repository.NotesRepository
import com.whisk.domain.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class PdfAnnotationQuote(
    val pageNumber: Int,
    val quoteText: String,
    val noteSnippet: String,
)

class PdfAnnotationManager(
    private val context: Context,
    private val notesRepository: NotesRepository,
) {

    suspend fun renderPdfPage(pdfUri: Uri, pageIndex: Int): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(pdfUri, "r")
            if (pfd == null) return@withContext null

            val renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            if (pageIndex !in 0 until pageCount) {
                renderer.close()
                pfd.close()
                return@withContext null
            }

            val page = renderer.openPage(pageIndex)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            page.close()
            renderer.close()
            pfd.close()
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    suspend fun createNoteFromPdfAnnotation(
        pdfTitle: String,
        pageNumber: Int,
        selectedQuote: String,
        userComment: String,
    ): Note = withContext(Dispatchers.IO) {
        val title = "PDF Excerpt - $pdfTitle (p. $pageNumber)"
        val body = buildString {
            append("# ").append(title).append("\n\n")
            append("> Source Document: **").append(pdfTitle).append("** (Page ").append(pageNumber).append(")\n\n")
            append("### Highlighted Quote\n\n")
            append("> \"").append(selectedQuote).append("\"\n\n")
            if (userComment.isNotBlank()) {
                append("### Analysis & Notes\n\n")
                append(userComment).append("\n\n")
            }
            append("Tags: #pdf #annotation #reading\n")
        }

        val note = Note(
            title = title,
            body = body,
            tags = listOf("pdf", "annotation", "literature"),
            sourceMetadata = "PDF: $pdfTitle (Page $pageNumber)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )

        notesRepository.saveNote(note)
        note
    }
}
