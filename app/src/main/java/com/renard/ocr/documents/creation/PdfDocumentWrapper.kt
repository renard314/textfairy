package com.renard.ocr.documents.creation

import android.graphics.Bitmap
import com.googlecode.leptonica.android.Pix
import com.googlecode.leptonica.android.ReadFile
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import java.io.Closeable

class PdfDocumentWrapper(private val pdfiumCore: PdfiumCore, private val pdfDocument: PdfDocument) : Closeable {

    fun getPage(pageNumber: Int): Pix {
        pdfiumCore.openPage(pdfDocument, pageNumber)
        val width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNumber)
        val height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNumber)
        val widthPixels = (width / (72.0 / 300.0)).toInt()
        val heightPixels = (height / (72.0 / 300.0)).toInt()
        val bitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888)
        pdfiumCore.renderPageBitmap(pdfDocument, bitmap, pageNumber, 0, 0, widthPixels, heightPixels)
        val p = ReadFile.readBitmap(bitmap)
        bitmap.recycle()
        return p
    }

    fun getPageCount() = pdfiumCore.getPageCount(pdfDocument)

    override fun close() {
        pdfiumCore.closeDocument(pdfDocument)
    }
}
