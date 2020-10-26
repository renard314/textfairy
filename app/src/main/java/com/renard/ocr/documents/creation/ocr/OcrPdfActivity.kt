package com.renard.ocr.documents.creation.ocr

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.renard.ocr.MonitoredActivity
import com.renard.ocr.R
import com.renard.ocr.documents.creation.PdfDocumentWrapper
import com.renard.ocr.documents.creation.ocr.PdfLoadingViewModel.Result.Error
import com.renard.ocr.documents.creation.ocr.PdfLoadingViewModel.Result.Success
import com.shockwave.pdfium.PdfiumCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class PdfLoadingViewModel(application: Application) : AndroidViewModel(application) {
    sealed class Result {
        data class Success(val pdfDocument: PdfDocumentWrapper) : Result()
        object Error : Result()

    }

    private val _content = MutableLiveData<Result>()
    val content: LiveData<Result>
        get() = _content

    fun loadContent(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                val fixedUri = Uri.parse(uri.toString().replace("/file/file", "/file"))
                val fd = getApplication<Application>().contentResolver.openFileDescriptor(fixedUri, "r")
                if (fd == null) {
                    Error
                } else {
                    val pdfiumCore = PdfiumCore(getApplication())
                    val pdfDocument = pdfiumCore.newDocument(fd)
                    Success(PdfDocumentWrapper(pdfiumCore, pdfDocument))
                }
            } catch (ex: IOException) {
                ex.printStackTrace()
                Error
            }

            _content.postValue(result)
        }

    }

}

class OcrPdfActivity : MonitoredActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val model by viewModels<PdfLoadingViewModel>()
        model.content.observe(this, {
            when (it) {
                is Success -> showAllFiles(it)
                Error -> {
                    Toast.makeText(this, R.string.could_not_load_pdf, Toast.LENGTH_LONG).show()
                    setResult(RESULT_CANCELED)
                    finish()
                }
            }
        })
        model.loadContent(intent.data!!)

    }

    private fun showAllFiles(it: Success) {
        //it.pdfDocument.
        //TODO("Not yet implemented")
    }

    override fun getScreenName() = ""

    override fun getHintDialogId() = -1

}