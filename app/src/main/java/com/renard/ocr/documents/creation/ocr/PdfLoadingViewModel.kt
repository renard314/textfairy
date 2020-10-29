package com.renard.ocr.documents.creation.ocr

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.googlecode.leptonica.android.Pix
import com.renard.ocr.documents.creation.PdfDocumentWrapper
import com.shockwave.pdfium.PdfiumCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class PdfLoadingViewModel(application: Application) : AndroidViewModel(application) {
    sealed class Result {
        class Initial(val loadContent: (Uri) -> Unit) : Result()
        data class Success(val loadPage: () -> Unit) : Result()
        data class Page(val index: Int, val pix: Pix, val nextPage: () -> Unit) : Result()
        object Error : Result()
        object NoMorePages : Result()
    }

    private var pdfDocumentWrapper: PdfDocumentWrapper? = null
    private val _content = MutableLiveData<Result>(Result.Initial(::loadContent))
    val content: LiveData<Result>
        get() = _content


    override fun onCleared() {
        super.onCleared()
        pdfDocumentWrapper?.close()
    }

    private fun loadContent(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                val fixedUri = Uri.parse(uri.toString().replace("/file/file", "/file"))
                val fd = getApplication<Application>().contentResolver.openFileDescriptor(fixedUri, "r")
                if (fd == null) {
                    Result.Error
                } else {
                    val pdfiumCore = PdfiumCore(getApplication())
                    val pdfDocument = pdfiumCore.newDocument(fd)
                    val pdfWrapper = PdfDocumentWrapper(pdfiumCore, pdfDocument)
                    fun loadPage(pageNumber: Int) {
                        viewModelScope.launch(Dispatchers.IO) {
                            if (pageNumber <= pdfWrapper.getPageCount()) {
                                val page = pdfWrapper.getPage(pageNumber)
                                _content.postValue(Result.Page(pageNumber, page) { loadPage(pageNumber + 1) })
                            } else {
                                _content.postValue(Result.NoMorePages)
                            }
                        }
                    }
                    pdfDocumentWrapper = pdfWrapper
                    Result.Success { loadPage(0) }
                }
            } catch (ex: IOException) {
                ex.printStackTrace()
                Result.Error
            }

            _content.postValue(result)
        }

    }

}