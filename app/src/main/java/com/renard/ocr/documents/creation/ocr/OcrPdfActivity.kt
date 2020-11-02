package com.renard.ocr.documents.creation.ocr

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.core.app.NavUtils
import androidx.core.net.toFile
import androidx.core.view.isVisible
import androidx.work.*
import androidx.work.ExistingWorkPolicy.KEEP
import androidx.work.WorkInfo.State.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.renard.ocr.MonitoredActivity
import com.renard.ocr.R
import com.renard.ocr.databinding.ActivityOcrPdfBinding
import com.renard.ocr.documents.creation.DocumentStore
import com.renard.ocr.documents.creation.ocr.OcrWorker.Companion.KEY_INPUT_LANG
import com.renard.ocr.documents.creation.ocr.OcrWorker.Companion.KEY_INPUT_PARENT_ID
import com.renard.ocr.documents.creation.ocr.OcrWorker.Companion.KEY_INPUT_URL
import com.renard.ocr.documents.creation.ocr.OcrWorker.Companion.KEY_OUTPUT_ACCURACY
import com.renard.ocr.documents.creation.ocr.OcrWorker.ProgressData.Companion.fromWorkData
import com.renard.ocr.documents.viewing.single.DocumentActivity
import com.renard.ocr.documents.viewing.single.DocumentActivity.EXTRA_ACCURACY
import com.renard.ocr.documents.viewing.single.DocumentActivity.EXTRA_LANGUAGE
import kotlinx.coroutines.*

class OcrPdfActivity : MonitoredActivity() {

    private lateinit var binding: ActivityOcrPdfBinding
    private var currentPreviewUri: Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOcrPdfBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initToolbar()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val lang = Language.getOcrLanguage(this)!!
        val parentId = intent.getIntExtra(OCRActivity.EXTRA_PARENT_DOCUMENT_ID, -1)
        val data = intent.dataString!!

        val workRequest = OneTimeWorkRequestBuilder<OcrWorker>()
                .setInputData(workDataOf(KEY_INPUT_URL to data, KEY_INPUT_LANG to lang, KEY_INPUT_PARENT_ID to parentId))
                .build()
        val workManager = WorkManager.getInstance(this)

        workManager.enqueueUniqueWork(data, KEEP, workRequest)

        workManager.getWorkInfosForUniqueWorkLiveData(data).observe(this) { workInfos ->
            val workInfo = workInfos.firstOrNull()
            when (workInfo?.state) {
                ENQUEUED -> binding.toolbar.toolbarText.setText(R.string.progress_start)
                RUNNING -> onProgress(workInfo.progress)
                SUCCEEDED -> onSuccess(workInfo, lang)
                FAILED, CANCELLED -> finish()
                BLOCKED -> {
                }
                null -> {
                }
            }
        }
    }

    private fun onSuccess(workInfo: WorkInfo, lang: String) {
        Log.d(LOG_TAG, "SUCCEEDED =$workInfo")
        with(workInfo.outputData) {
            val intent = Intent(applicationContext, DocumentActivity::class.java)
            intent.putExtra(EXTRA_ACCURACY, getInt(KEY_OUTPUT_ACCURACY, 0))
            intent.putExtra(EXTRA_LANGUAGE, lang)
            intent.data = DocumentStore.getDocumentUri(getInt(OcrWorker.KEY_OUTPUT_DOCUMENT_ID, -1))
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }
    }

    private var bitmapJob: Job?=null
    private fun onProgress(progressData: Data) {
        Log.d(LOG_TAG, "RUNNING =$progressData")

        val progress = fromWorkData(progressData) ?: return
        if (progress.previewImage != null && progress.previewImage != currentPreviewUri) {
            //Glide is crashing so decode by hand.
            currentPreviewUri = progress.previewImage
            binding.progressImage.isVisible=false
            bitmapJob?.cancel()
            bitmapJob = CoroutineScope(Dispatchers.IO).launch {
                val bitmap = progress.previewImage?.toFile().inputStream().use {
                    BitmapFactory.decodeStream(it)
                }
                withContext(Dispatchers.Main) {
                    binding.progressImage.isVisible=true
                    binding.progressImage.setImageBitmapResetBase(bitmap, true, 0)
                    binding.progressImage.setProgress(progress.percent, progress.lineBounds, progress.pageBounds, progress.pageBounds.width(), progress.pageBounds.height())
                }
            }
        } else if (currentPreviewUri != null) {
            binding.progressImage.setProgress(progress.percent, progress.lineBounds, progress.pageBounds, progress.pageBounds.width(), progress.pageBounds.height())
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun getScreenName() = "OcrPdfActivity"

    override fun getHintDialogId() = -1

    companion object {
        private val LOG_TAG = OcrPdfActivity::class.java.simpleName
    }

}