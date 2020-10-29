package com.renard.ocr.documents.creation.ocr

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.fragment.app.commit
import com.googlecode.tesseract.android.OCR
import com.googlecode.tesseract.android.OcrProgress
import com.renard.ocr.MonitoredActivity
import com.renard.ocr.R
import com.renard.ocr.databinding.ActivityOcrPdfBinding
import com.renard.ocr.documents.creation.DocumentStore
import com.renard.ocr.documents.creation.DocumentStore.getDocumentUri
import com.renard.ocr.documents.creation.ocr.PdfLoadingViewModel.Result.*
import com.renard.ocr.documents.viewing.single.DocumentActivity

class OcrPdfActivity : MonitoredActivity() {

    private val ocrModel: OCR by viewModels()
    private lateinit var binding: ActivityOcrPdfBinding
    private var parentId: Int = -1
    private var nextPage = {}
    private val accuracies = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOcrPdfBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initToolbar()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        parentId = intent.getIntExtra(OCRActivity.EXTRA_PARENT_DOCUMENT_ID, -1)

        observePdf()
        observeOcrProgress()
    }

    private fun observePdf() {
        val model by viewModels<PdfLoadingViewModel>()
        model.content.observe(this, {
            when (it) {
                is Initial -> it.loadContent(intent.data!!)
                is Success -> {
                    it.loadPage()
                    supportFragmentManager.commit {
                        replace(R.id.fragment_container, OcrFragment(), OCR_FRAGMENT_TAG)
                    }
                }
                is Page -> {
                    nextPage = it.nextPage
                    ocrModel.startOCRForSimpleLayout(this, it.pix, Language.getOcrLanguage(this)
                            ?: throw IllegalStateException("No Language Available"))
                }
                NoMorePages -> {
                    val intent = Intent()
                    intent.data = getDocumentUri(parentId)
                    intent.putExtra(DocumentActivity.EXTRA_ACCURACY, accuracies.average())
                    intent.putExtra(DocumentActivity.EXTRA_LANGUAGE, Language.getOcrLanguage(this))
                    setResult(RESULT_OK)
                    finish()
                }
                Error -> {
                    Toast.makeText(this, R.string.could_not_load_pdf, Toast.LENGTH_LONG).show()
                    setResult(RESULT_CANCELED)
                    finish()
                }

            }

        })
    }

    private fun observeOcrProgress() {
        ocrModel.getOcrProgress().observe(this, {
            when (it) {
                is OcrProgress.Message -> binding.toolbar.toolbarText.setText(it.message)
                is OcrProgress.Result -> {
                    onOcrResult(this, it, parentId) { uri ->
                        if (uri != null) {
                            if (parentId == -1) {
                                parentId = DocumentStore.getDocumentId(uri)
                            }
                        }
                        accuracies.add(it.accuracy)
                        nextPage()
                    }
                }
                is OcrProgress.Error -> onOcrError(it)
                else -> {
                }//ignore
            }
        })
    }


    private fun onOcrError(it: OcrProgress.Error) {
        Toast.makeText(this, getText(it.message), Toast.LENGTH_LONG).show()
        setResult(RESULT_CANCELED)
        finish()
    }


    override fun getScreenName() = ""

    override fun getHintDialogId() = -1

    companion object {
        private const val OCR_FRAGMENT_TAG = "OCR"
    }

}

private fun onOcrResult(activity: MonitoredActivity, it: OcrProgress.Result, parentId: Int, onDocumentSaved: (Uri?) -> Unit) {
    DocumentStore.saveDocument(activity,
            it.pix,
            it.hocrText,
            it.utf8Text,
            parentId,
            it.language
    ) { uri: Uri? ->
        it.pix.recycle()
        onDocumentSaved(uri)
    }
}
