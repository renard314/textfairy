package com.renard.ocr.documents

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.googlecode.leptonica.android.Pix
import com.googlecode.leptonica.android.ReadFile
import com.shockwave.pdfium.PdfiumCore
import java.io.IOException

object ContentLoading {
    fun loadAsPdf(context: Context, cameraPicUri: Uri): Pix? {
        var p: Pix? = null
        var bitmap: Bitmap? = null
        val pageNum = 0
        val pdfiumCore = PdfiumCore(context)
        try {
            val replace = cameraPicUri.toString().replace("/file/file", "/file")
            val fixedUri = Uri.parse(replace)
            val fd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(fixedUri, "r")
            val pdfDocument = pdfiumCore.newDocument(fd)
            pdfiumCore.openPage(pdfDocument, pageNum)
            val width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNum)
            val height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNum)
            val widthPixels = (width / (72.0 / 300.0)).toInt()
            val heightPixels = (height / (72.0 / 300.0)).toInt()
            bitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888)
            pdfiumCore.renderPageBitmap(pdfDocument, bitmap, pageNum, 0, 0, widthPixels, heightPixels)
            p = ReadFile.readBitmap(bitmap)
            pdfiumCore.closeDocument(pdfDocument)
        } catch (ex: IOException) {
            ex.printStackTrace()
        } finally {
            bitmap?.recycle()
        }
        return p
    }
}