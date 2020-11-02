package com.renard.ocr.documents.creation

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.RemoteException
import android.text.format.DateFormat
import android.widget.Toast
import com.googlecode.leptonica.android.Pix
import com.renard.ocr.MonitoredActivity
import com.renard.ocr.R
import com.renard.ocr.documents.viewing.DocumentContentProvider
import com.renard.ocr.util.Util
import java.io.File
import java.io.IOException
import java.util.*

object DocumentStore {

    internal fun getDocumentUri(id: Int): Uri =
            ContentUris.withAppendedId(DocumentContentProvider.CONTENT_URI, id.toLong())

    internal fun getDocumentId(uri: Uri) =
            Integer.parseInt(uri.lastPathSegment!!)

    @JvmStatic
    @JvmName("saveDocument")
    internal fun saveDocument(
            monitoredActivity: MonitoredActivity,
            pix: Pix,
            hocrString: String?,
            utf8String: String?,
            parentId: Int,
            lang: String,
            onCompleted: (Uri?) -> Unit
    ) {

        Util.startBackgroundJob(monitoredActivity, "",
                monitoredActivity.getText(R.string.saving_document).toString(), {
            var imageFile: File? = null
            var documentUri: Uri? = null

            try {
                imageFile = saveImage(monitoredActivity, pix)
            } catch (e: IOException) {
                e.printStackTrace()
                monitoredActivity.runOnUiThread {
                    Toast.makeText(
                            monitoredActivity,
                            monitoredActivity.getText(R.string.error_create_file),
                            Toast.LENGTH_LONG).show()
                }
            }

            try {

                documentUri = saveDocumentToDB(parentId, lang, monitoredActivity, imageFile, hocrString, utf8String)
                if (imageFile != null) {
                    Util.createThumbnail(monitoredActivity, imageFile, Integer.valueOf(documentUri!!.lastPathSegment!!))
                }
            } catch (e: RemoteException) {
                e.printStackTrace()

                monitoredActivity.runOnUiThread {
                    Toast.makeText(
                            monitoredActivity,
                            monitoredActivity.getText(R.string.error_create_file),
                            Toast.LENGTH_LONG).show()
                }
            } finally {
                onCompleted(documentUri)
            }
        }, Handler())

    }

    fun saveImage(context: Context, p: Pix): File {
        val id = DateFormat.format("ssmmhhddMMyy", Date(System.currentTimeMillis()))
        return Util.savePixToSD(context, p, id.toString())
    }

    fun saveDocumentToDB(parentId: Int, lang: String, context: Context, imageFile: File?, hocr: String?, plainText: String?): Uri? {
        val client = context.contentResolver.acquireContentProviderClient(DocumentContentProvider.CONTENT_URI)
        try {
            return client?.insert(DocumentContentProvider.CONTENT_URI, ContentValues().apply {
                if (imageFile != null) {
                    put(DocumentContentProvider.Columns.PHOTO_PATH, imageFile.path)
                }
                if (hocr != null) {
                    put(DocumentContentProvider.Columns.HOCR_TEXT, hocr)
                }
                if (plainText != null) {
                    put(DocumentContentProvider.Columns.OCR_TEXT, plainText)
                }
                if (parentId > -1) {
                    put(DocumentContentProvider.Columns.PARENT_ID, parentId)
                }
                put(DocumentContentProvider.Columns.OCR_LANG, lang)
            })
        } finally {
            client?.release()
        }
    }
}