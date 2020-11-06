package com.renard.ocr.documents.creation.ocr

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.RemoteException
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.work.*
import com.googlecode.leptonica.android.Pix
import com.googlecode.leptonica.android.ReadFile
import com.googlecode.leptonica.android.WriteFile
import com.googlecode.tesseract.android.NativeBinding
import com.googlecode.tesseract.android.TessBaseAPI
import com.googlecode.tesseract.android.initTessApi
import com.renard.ocr.R
import com.renard.ocr.applicationInstance
import com.renard.ocr.documents.creation.DocumentStore
import com.renard.ocr.documents.creation.PdfDocumentWrapper
import com.renard.ocr.documents.creation.crop.CropImageScaler
import com.renard.ocr.util.Util
import kotlinx.coroutines.coroutineScope
import java.io.Closeable
import java.io.File
import java.io.IOException


class OcrWorker(context: Context, parameters: WorkerParameters) :
        CoroutineWorker(context, parameters) {


    override suspend fun doWork(): Result {
        val inputUrls = inputData.getStringArray(KEY_INPUT_URLS)?.map(Uri::parse)
                ?: return Result.failure()
        val inputLang = inputData.getString(KEY_INPUT_LANG) ?: return Result.failure()
        val parentId = inputData.getInt(KEY_INPUT_PARENT_ID, -1)
        setForeground(createForegroundInfo(inputUrls.first(), parentId))
        takeUriPermission(inputUrls)

        return coroutineScope {
            when (val result = scanUris(inputUrls, inputLang, parentId)) {
                is ScanPdfResult.Success -> Result.success(workDataOf(
                        KEY_OUTPUT_ACCURACY to result.accuracy,
                        KEY_OUTPUT_DOCUMENT_ID to result.documentId
                ))
                is ScanPdfResult.Failure -> Result.failure()
            }
        }.also {
            releaseUriPermission(inputUrls)
        }
    }

    private fun releaseUriPermission(inputUrls: List<Uri>) {
        if (Build.VERSION.SDK_INT >= 19) {
            inputUrls.forEach {
                applicationContext.contentResolver.releasePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

    private fun takeUriPermission(inputUrls: List<Uri>) {
        if (Build.VERSION.SDK_INT >= 19) {
            inputUrls.forEach {
                applicationContext.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

    private fun sendProgressImage(nativePix: Long, progress: ProgressData): ProgressData {
        val pix = Pix(nativePix)
        val file = File(applicationContext.cacheDir, "progress/")
        val widthPixels = Resources.getSystem().displayMetrics.widthPixels
        val heightPixels = Resources.getSystem().displayMetrics.heightPixels
        val uri = CropImageScaler().scale(pix, widthPixels, heightPixels).pix.use {
            try {
                val image = File(file, "progress_${id}_${progress.progressCount}.png")
                if (WriteFile.writeImpliedFormat(pix, image)) {
                    image.toUri()
                } else {
                    null
                }
            } catch (e: java.lang.Exception) {
                null
            }
        }

        progress.previewImage?.toFile()?.delete()

        return progress.copy(
                previewImage = uri,
                progressCount = progress.progressCount + 1
        )
    }

    private suspend fun scanUris(uris: List<Uri>, inputLang: String, parentIdParam: Int): ScanPdfResult {
        var parentId = parentIdParam
        var progress = ProgressData(pageCount = getPageCount(applicationContext, uris))
        val accuracy = mutableListOf<Int>()

        NativeBinding().use { binding ->
            binding.setProgressCallBack(object : NativeBinding.ProgressCallBack {

                override fun onProgressImage(nativePix: Long) {
                    progress = sendProgressImage(nativePix, progress)
                    setProgressAsync(progress.asWorkData())
                }

                override fun onProgressText(message: Int) {}

                override fun onLayoutAnalysed(nativePixaText: Long, nativePixaImages: Long) {}

            })

            initTessApi(applicationContext, inputLang, applicationInstance.crashLogger) {
                progress = progress.copy(percent = it.percent, pageBounds = it.currentRect, lineBounds = it.currentWordRect)
                setProgressAsync(progress.asWorkData())
            }?.use { tess ->
                for (uri in uris) {
                    pages(uri).forEach {
                        progress = ProgressData(currentPage = progress.currentPage + 1, pageCount = progress.pageCount)
                        setForeground(createForegroundInfo(uri, parentId, progress.currentPage, progress.pageCount))
                        Pix(binding.convertBookPage(it))
                                .use { pixText ->
                                    it.recycle()
                                    progress = sendProgressImage(pixText.nativePix, progress)
                                    setProgress(progress.asWorkData())
                                    val scan = ocr(tess, pixText)
                                    accuracy.add(scan.accuracy)
                                    val documentUri = saveDocument(scan, inputLang, pixText, parentId)
                                    if (parentId == -1 && documentUri != null) {
                                        parentId = DocumentStore.getDocumentId(documentUri)
                                    }
                                }
                    }
                }
                return if (accuracy.isEmpty()) {
                    ScanPdfResult.Failure
                } else {
                    ScanPdfResult.Success(accuracy.average().toInt(), parentId)
                }
            }
        }
        return ScanPdfResult.Failure
    }

    private fun pages(uri: Uri): Sequence<Pix> = sequence {
        if (uri.isPdf(applicationContext.contentResolver)) {
            getPdfDocument(uri, applicationContext)?.use {
                for(i in 0 until it.getPageCount()){
                    yield(it.getPage(i))
                }
            }
        } else {
            ReadFile.load(applicationContext, uri)?.let { yield(it) }
        }
    }


    private fun saveDocument(scan: ScanPageResult, lang: String, pixText: Pix, parentId: Int): Uri? {
        val imageFile = try {
            DocumentStore.saveImage(applicationContext, pixText)
        } catch (ignored: IOException) {
            null
        }
        val documentUri = try {
            DocumentStore.saveDocumentToDB(
                    parentId,
                    lang,
                    applicationContext,
                    imageFile,
                    scan.hocrText,
                    scan.htmlText
            )

        } catch (ignored: RemoteException) {
            null
        }
        if (documentUri != null && imageFile != null) {
            val documentId = DocumentStore.getDocumentId(documentUri)
            Util.createThumbnail(applicationContext, imageFile, documentId)
        }
        return documentUri
    }


    private fun ocr(tess: TessBaseAPI, pixText: Pix): ScanPageResult {
        tess.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
        tess.setImage(pixText)
        var hocrText = tess.getHOCRText(0)
        var accuracy = tess.meanConfidence()
        val utf8Text = tess.utF8Text
        if (utf8Text.isEmpty()) {
            applicationInstance.crashLogger.logMessage("No words found. Looking for sparse text.")
            tess.pageSegMode = TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT
            tess.setImage(pixText)
            hocrText = tess.getHOCRText(0)
            accuracy = tess.meanConfidence()
        }

        val htmlText = tess.htmlText
        if (accuracy == 95) {
            accuracy = 0
        }
        return ScanPageResult(htmlText, hocrText, accuracy)
    }

    private fun createForegroundInfo(pdfFileUri: Uri, parentId: Int, currentPage: Int, pageCount: Int): ForegroundInfo {
        return createForegroundInfo(pdfFileUri, parentId) {
            it.setProgress(pageCount, currentPage, false)
            it.setContentText(applicationContext.getString(R.string.scanning_pdf_progress, currentPage, pageCount))
        }
    }

    private fun createForegroundInfo(fileUri: Uri, parentId: Int, applyToNotif: (NotificationCompat.Builder) -> Unit = {}): ForegroundInfo {
        val id = applicationContext.packageName
        val title = applicationContext.getString(R.string.notification_scanning_title, fileUri.lastPathSegment?.substringAfterLast("/"))
        val cancel = applicationContext.getString(R.string.cancel)
        // This PendingIntent can be used to cancel the worker
        val intent = WorkManager.getInstance(applicationContext)
                .createCancelPendingIntent(getId())

        // Create a Notification channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel(id)
        }

        val ocrPdfActivityIntent = Intent(applicationContext, OcrPdfActivity::class.java)
        ocrPdfActivityIntent.data = fileUri
        ocrPdfActivityIntent.putExtra(OCRActivity.EXTRA_PARENT_DOCUMENT_ID, parentId)
        ocrPdfActivityIntent.putExtra(OcrPdfActivity.KEY_WORK_ID, getId().toString())

        val contentIntent = TaskStackBuilder.create(applicationContext)
                .addNextIntentWithParentStack(ocrPdfActivityIntent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(applicationContext, id)
                .setContentTitle(title)
                .setTicker(title)
                .setSmallIcon(R.drawable.ic_fairy_happy)
                .setOngoing(true)
                .setContentIntent(contentIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, cancel, intent)

        applyToNotif(notification)
        return ForegroundInfo(inputData.getInt(KEY_INPUT_NOTIFICATION_ID, 314), notification.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel(id: String) {
        val name: CharSequence = applicationContext.getString(R.string.notification_channel_title)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(id, name, importance)
        channel.setSound(null, null);
        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private sealed class ScanPdfResult {
        data class Success(val accuracy: Int, val documentId: Int) : ScanPdfResult()
        object Failure : ScanPdfResult()
    }

    private data class ScanPageResult(val htmlText: String, val hocrText: String, val accuracy: Int)

    data class ProgressData(
            val currentPage: Int = 0,
            val pageCount: Int = 0,
            val percent: Int = 0,
            val previewImage: Uri? = null,
            val pageBounds: Rect = Rect(),
            val lineBounds: Rect = Rect(),
            val progressCount: Int = 0
    ) {
        fun asWorkData() = workDataOf(
                CurrentPage to currentPage,
                PageCount to pageCount,
                Progress to percent,
                PreviewImage to previewImage?.toString(),
                PageBounds to pageBounds.flattenToString(),
                LineBounds to lineBounds.flattenToString(),
                "count" to progressCount
        )

        companion object {
            fun fromWorkData(data: Data) = if (data.keyValueMap.isEmpty()) {
                null
            } else
                ProgressData(
                        currentPage = data.getInt(CurrentPage, 0),
                        pageCount = data.getInt(PageCount, 0),
                        percent = data.getInt(Progress, 0),
                        previewImage = getUri(data),
                        pageBounds = Rect.unflattenFromString(data.getString(PageBounds))!!,
                        lineBounds = Rect.unflattenFromString(data.getString(LineBounds))!!,
                        progressCount = data.getInt("count", 0)

                )

            private fun getUri(data: Data): Uri? {
                val uriString = data.getString(PreviewImage)
                return if (uriString != null) {
                    Uri.parse(uriString)
                } else {
                    null
                }
            }
        }
    }

    companion object {
        const val CurrentPage = "CurrentPage"
        const val PageCount = "PageCount"
        const val Progress = "Progress"
        const val PreviewImage = "Preview"
        const val PageBounds = "PageBounds"
        const val LineBounds = "LineBounds"
        const val KEY_INPUT_NOTIFICATION_ID = "KEY_INPUT_NOTIFICATION_ID"
        const val KEY_INPUT_URLS = "KEY_INPUT_URLS"
        const val KEY_INPUT_LANG = "KEY_INPUT_LANG"
        const val KEY_INPUT_PARENT_ID = "KEY_INPUT_PARENT_ID"
        const val KEY_OUTPUT_ACCURACY = "KEY_OUTPUT_ACCURACY"
        const val KEY_OUTPUT_DOCUMENT_ID = "KEY_OUTPUT_DOCUMENT_ID"
    }
}

internal fun getPdfDocument(inputUrl: Uri, context: Context): PdfDocumentWrapper? {
    val fixedUri = Uri.parse(inputUrl.toString().replace("/file/file", "/file"))
    val fd = try {
        context.contentResolver.openFileDescriptor(fixedUri, "r") ?: return null
    } catch (e: Exception) {
        return null
    }
    return PdfDocumentWrapper(context, fd)
}