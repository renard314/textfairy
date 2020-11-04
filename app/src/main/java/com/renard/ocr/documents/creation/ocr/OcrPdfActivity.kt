package com.renard.ocr.documents.creation.ocr

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.NavUtils
import androidx.core.net.toFile
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.work.*
import androidx.work.WorkInfo.State.*
import com.renard.ocr.MonitoredActivity
import com.renard.ocr.R
import com.renard.ocr.databinding.ActivityOcrPdfBinding
import com.renard.ocr.documents.creation.DocumentStore
import com.renard.ocr.documents.creation.ocr.OcrWorker.Companion.KEY_INPUT_LANG
import com.renard.ocr.documents.creation.ocr.OcrWorker.Companion.KEY_INPUT_NOTIFICATION_ID
import com.renard.ocr.documents.creation.ocr.OcrWorker.Companion.KEY_INPUT_PARENT_ID
import com.renard.ocr.documents.creation.ocr.OcrWorker.Companion.KEY_INPUT_URL
import com.renard.ocr.documents.creation.ocr.OcrWorker.Companion.KEY_OUTPUT_ACCURACY
import com.renard.ocr.documents.creation.ocr.OcrWorker.ProgressData.Companion.fromWorkData
import com.renard.ocr.documents.viewing.single.DocumentActivity
import com.renard.ocr.documents.viewing.single.DocumentActivity.EXTRA_ACCURACY
import com.renard.ocr.documents.viewing.single.DocumentActivity.EXTRA_LANGUAGE
import com.renard.ocr.main_menu.language.OcrLanguage
import com.renard.ocr.main_menu.language.OcrLanguageDataStore.getInstalledOCRLanguages
import com.renard.ocr.util.PreferencesUtils
import kotlinx.coroutines.*
import java.util.*
import kotlin.random.Random

class OcrPdfActivity : MonitoredActivity() {

    private lateinit var binding: ActivityOcrPdfBinding
    private var currentPreviewUri: Uri? = null
    private var workId: UUID? = null
    private var bitmapJob: Job? = null

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (workId != null) {
            outState.putString(KEY_WORK_ID, workId.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOcrPdfBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initToolbar()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val fileName = intent.data?.lastPathSegment?.substringAfterLast("/")
        setToolbarMessage(applicationContext.getString(R.string.scanning_pdf, fileName))

        val lang = Language.getOcrLanguage(this)!!
        val pdfUri = intent.dataString!!
        val workManager = WorkManager.getInstance(this)

        val idString = savedInstanceState?.getString(KEY_WORK_ID)
                ?: intent.getStringExtra(KEY_WORK_ID)
        if (idString != null) {
            binding.group.isVisible = false
            binding.root.doOnLayout {
                observeWorkManager(workManager, pdfUri, lang, UUID.fromString(idString))
            }
        } else {
            val pdf = getPdfDocument(pdfUri, this)
            if (pdf == null) {
                Toast.makeText(this, R.string.could_not_load_pdf, Toast.LENGTH_LONG).show()
                return
            }
            initLanguageSelectionUi(pdf.getPageCount(), lang)
            binding.root.doOnLayout {
                CoroutineScope(Dispatchers.IO).launch {
                    val bitmap = pdf.getPageAsBitmap(0, binding.progressImage.width, binding.progressImage.height)
                    pdf.close()
                    withContext(Dispatchers.Main) {
                        binding.progressImage.isVisible = true
                        binding.progressImage.setImageBitmapResetBase(bitmap, true, 0)
                    }
                }
            }
            binding.itemSave.setOnClickListener {
                binding.group.isVisible = false
                val workRequest = createWorkRequest(pdfUri, lang)
                workManager.enqueue(workRequest)
                workId = workRequest.id
                observeWorkManager(workManager, pdfUri, lang, workRequest.id)
            }
        }
    }

    private fun createWorkRequest(pdfUri: String, lang: String): OneTimeWorkRequest {
        val parentId = intent.getIntExtra(OCRActivity.EXTRA_PARENT_DOCUMENT_ID, -1)
        return OneTimeWorkRequestBuilder<OcrWorker>()
                .setInputData(workDataOf(
                        KEY_INPUT_NOTIFICATION_ID to Random.nextInt(),
                        KEY_INPUT_URL to pdfUri,
                        KEY_INPUT_LANG to lang,
                        KEY_INPUT_PARENT_ID to parentId
                ))
                .build()
    }

    private fun observeWorkManager(workManager: WorkManager, pdfUri: String, lang: String, id: UUID) {
        workManager.getWorkInfoByIdLiveData(id).observe(this) { workInfo ->
            when (workInfo.state) {
                RUNNING -> onProgress(workInfo.progress)
                SUCCEEDED -> onSuccess(workInfo, lang)
                FAILED, CANCELLED -> finish()
                ENQUEUED, BLOCKED, null -> {
                }
            }
        }
    }

    private fun initLanguageSelectionUi(pageCount: Int, lang: String) {
        binding.group.isVisible = true
        binding.textviewOcrPdf.text = getString(R.string.scan_pdf_title, pageCount)
        CoroutineScope(Dispatchers.IO).launch {
            val installedLanguages = getInstalledOCRLanguages(this@OcrPdfActivity)
            withContext(Dispatchers.Main) {
                val adapter: ArrayAdapter<OcrLanguage> = ArrayAdapter<OcrLanguage>(this@OcrPdfActivity, R.layout.item_spinner_language, installedLanguages)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.buttonLanguage.adapter = adapter
                val defaultLanguageIndex = installedLanguages.indexOfFirst { it.value == lang }
                if (defaultLanguageIndex != -1) {
                    binding.buttonLanguage.setSelection(defaultLanguageIndex, false)
                }

            }
        }
        binding.buttonLanguage.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val item = parent.adapter.getItem(position) as OcrLanguage
                PreferencesUtils.saveOCRLanguage(view.context, item)
                anaLytics.sendOcrLanguageChanged(item)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
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

    private fun onProgress(progressData: Data) {
        Log.d(LOG_TAG, "RUNNING =$progressData")
        val progress = fromWorkData(progressData) ?: return
        setToolbarMessage(getString(R.string.scanning_pdf_progress,progress.currentPage, progress.pageCount))
        if (progress.previewImage != null && progress.previewImage != currentPreviewUri) {
            //Glide is crashing so decode by hand.
            currentPreviewUri = progress.previewImage
            bitmapJob?.cancel()
            bitmapJob = CoroutineScope(Dispatchers.IO).launch {
                val bitmap = progress.previewImage.toFile().inputStream().use {
                    BitmapFactory.decodeStream(it)
                }
                withContext(Dispatchers.Main) {
                    binding.progressImage.setImageBitmapResetBase(bitmap, true, 0)
                    binding.progressImage.setProgress(
                            progress.percent,
                            progress.lineBounds,
                            progress.pageBounds,
                            progress.pageBounds.width(),
                            progress.pageBounds.height()
                    )
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
        const val KEY_WORK_ID = "KEY_WORK_ID"
        private val LOG_TAG = OcrPdfActivity::class.java.simpleName
    }

}