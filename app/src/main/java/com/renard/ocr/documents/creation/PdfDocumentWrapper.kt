package com.renard.ocr.documents.creation

import android.content.Context
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import android.util.Log
import com.googlecode.leptonica.android.Pix
import com.googlecode.leptonica.android.ReadFile
import com.renard.ocr.documents.creation.ocr.OcrPdfActivity
import com.shockwave.pdfium.PdfiumCore
import java.io.Closeable

class PdfDocumentWrapper(context: Context, fd: ParcelFileDescriptor) : Closeable, Iterable<Pix> {

    private val pdfiumCore = PdfiumCore(context)
    private val pdfDocument = pdfiumCore.newDocument(fd)


    fun getPage(pageNumber: Int): Pix {
        pdfiumCore.openPage(pdfDocument, pageNumber)
        val width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNumber)
        val height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNumber)
        val widthPixels = (width / (72.0 / 300.0)).toInt()
        val heightPixels = (height / (72.0 / 300.0)).toInt()
        val bitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888)
        Log.d(OcrPdfActivity::class.java.simpleName,"renderPageBitmap($pageNumber)")
        pdfiumCore.renderPageBitmap(pdfDocument, bitmap, pageNumber, 0, 0, widthPixels, heightPixels)
        val p = ReadFile.readBitmap(bitmap)
        bitmap.recycle()
        return p
    }

    fun getPageCount() = pdfiumCore.getPageCount(pdfDocument)

    override fun close() {
        pdfiumCore.closeDocument(pdfDocument)
    }

    override fun iterator(): Iterator<Pix> {
        return object : Iterator<Pix> {
            private var i = 0
            override fun hasNext() = i < getPageCount() - 1

            override fun next() = getPage(i).also { i += 1 }

        }
    }
}
