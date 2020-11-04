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

    fun getPageAsBitmap(pageNumber: Int, targetWidth: Int = -1, targetHeight: Int = -1): Bitmap {
        pdfiumCore.openPage(pdfDocument, pageNumber)
        val height = if(targetHeight==-1){
            pdfiumCore.getPageHeightPoint(pdfDocument, pageNumber) * (200 / 72)
        } else {
            targetHeight
        }
        val width = if(targetWidth==-1){
            pdfiumCore.getPageWidthPoint(pdfDocument, pageNumber) *(200 / 72)
        } else {
            targetWidth
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Log.d(OcrPdfActivity::class.java.simpleName, "renderPageBitmap($pageNumber)")
        pdfiumCore.renderPageBitmap(pdfDocument, bitmap, pageNumber, 0, 0, width, height)
        return bitmap
    }

    fun getPage(pageNumber: Int): Pix {
        val bitmap = getPageAsBitmap(pageNumber)
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
