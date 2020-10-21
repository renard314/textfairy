package com.renard.ocr.documents.creation

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.googlecode.leptonica.android.Pix
import com.googlecode.leptonica.android.ReadFile
import com.googlecode.leptonica.android.Scale
import com.renard.ocr.TextFairyApplication
import com.renard.ocr.documents.ContentLoading.loadAsPdf
import com.renard.ocr.documents.creation.NewDocumentActivityViewModel.Status.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.sqrt

class NewDocumentActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val application:TextFairyApplication = getApplication()

    sealed class Status {
        object Loading : Status()
        data class Success(val pix: Pix) : Status()
        data class Error(val pixLoadStatus: PixLoadStatus) : Status()
    }

    private val _content = MutableLiveData<Status>()
    val content: LiveData<Status>
        get() = _content


    fun loadContent(contentUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _content.postValue(Loading)
            val pix = loadContentUnscaled(contentUri)
            if (pix != null) {
                _content.postValue(Success(maybeUpscale(pix)))
            } else {
                application.crashLogger.setString("content uri", contentUri.toString())
                application.crashLogger.logException(IOException(PixLoadStatus.IMAGE_FORMAT_UNSUPPORTED.name))
                _content.postValue(Error(PixLoadStatus.IMAGE_FORMAT_UNSUPPORTED))
            }
        }
    }

    private fun loadContentUnscaled(contentUri: Uri): Pix? {
        val type = application.contentResolver.getType(contentUri)
        return if (contentUri.path?.endsWith(".pdf") == true || "application/pdf".equals(type, ignoreCase = true)) {
            loadAsPdf(application, contentUri)
        } else {
            ReadFile.load(application, contentUri)
        }

    }

    private fun maybeUpscale(p: Pix): Pix {
        val pixPixelCount = p.width * p.height.toLong()
        val minPixelCount = 3 * 1024 * 1024
        return if (pixPixelCount < minPixelCount) {
            val scale = sqrt(minPixelCount.toDouble() / pixPixelCount)
            val scaledPix = Scale.scale(p, scale.toFloat())
            if (scaledPix.nativePix == 0L) {
                p
            } else {
                p.recycle()
                scaledPix
            }
        } else {
            p
        }
    }

}