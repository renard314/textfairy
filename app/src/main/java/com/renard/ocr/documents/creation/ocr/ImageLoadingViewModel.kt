package com.renard.ocr.documents.creation.ocr

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
import com.renard.ocr.documents.creation.PixLoadStatus
import com.renard.ocr.documents.creation.ocr.ImageLoadingViewModel.Status.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.sqrt

class ImageLoadingViewModel(application: Application) : AndroidViewModel(application) {

    private val application: TextFairyApplication = getApplication()

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
            val result =
                    ReadFile.load(application, contentUri)
                            ?.run { Success(maybeUpscale(this)) }
                            ?: Error(PixLoadStatus.IMAGE_FORMAT_UNSUPPORTED)


            if (result is Error) {
                application.crashLogger.setString("content uri", contentUri.toString())
                application.crashLogger.logException(IOException(result.pixLoadStatus.name))
            }
            _content.postValue(result)
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