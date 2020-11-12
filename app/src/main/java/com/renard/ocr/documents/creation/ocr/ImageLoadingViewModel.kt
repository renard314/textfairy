package com.renard.ocr.documents.creation.ocr

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import androidx.core.graphics.toRectF
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.googlecode.leptonica.android.*
import com.googlecode.leptonica.android.Convert.convertTo8
import com.googlecode.tesseract.android.NativeBinding
import com.googlecode.tesseract.android.OCR
import com.renard.ocr.TextFairyApplication
import com.renard.ocr.cropimage.image_processing.Blur
import com.renard.ocr.cropimage.image_processing.BlurDetectionResult
import com.renard.ocr.documents.creation.PixLoadStatus
import com.renard.ocr.documents.creation.crop.CropImageScaler
import com.renard.ocr.documents.creation.ocr.ImageLoadingViewModel.ImageLoadStatus.PreparedForCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.sqrt


class ImageLoadingViewModel(application: Application) : AndroidViewModel(application) {


    private val application: TextFairyApplication = getApplication()
    private val mNativeBinding: NativeBinding = NativeBinding()

    sealed class ImageLoadStatus {
        object Initial : ImageLoadStatus()
        object Loading : ImageLoadStatus()
        data class Loaded(val pix: Pix, val prepareForCropping: (Int, Int) -> Unit) : ImageLoadStatus()
        data class PreparedForCrop(
                val blurDetectionResult: BlurDetectionResult,
                val bitmap: Bitmap,
                val scaleFactor: Float,
                val pix: Pix,
                val cropAndProject: (FloatArray, Rect, Int) -> Unit
        ) : ImageLoadStatus()

        data class CropSuccess(val pix: Pix) : ImageLoadStatus()
        object CropError : ImageLoadStatus()
        data class Error(val pixLoadStatus: PixLoadStatus) : ImageLoadStatus()
    }

    private val _content = MutableLiveData<ImageLoadStatus>()
    val content: LiveData<ImageLoadStatus>
        get() = _content


    init {
        _content.value = ImageLoadStatus.Initial
    }

    override fun onCleared() {
        mNativeBinding.destroy()
    }

    private fun scaleForCrop(pix: Pix, width: Int, height: Int) {
        application.espressoTestIdlingResource.increment()
        viewModelScope.launch(Dispatchers.Default) {
            val scale = CropImageScaler().scale(pix, width, height)
            val bitmap = WriteFile.writeBitmap(scale.pix)
            if (bitmap == null) {
                _content.postValue(ImageLoadStatus.CropError)
                return@launch
            }
            scale.pix.recycle()

            _content.postValue(PreparedForCrop(
                    Blur.blurDetect(pix),
                    bitmap,
                    scale.scaleFactor,
                    pix
            ) { trapezoid, bounds, rotation -> cropAndProject(pix, scale.scaleFactor, trapezoid, bounds, rotation) })
            application.espressoTestIdlingResource.decrement()
        }
    }

    fun loadContent(contentUri: Uri) {
        application.espressoTestIdlingResource.increment()
        viewModelScope.launch(Dispatchers.IO) {
            _content.postValue(ImageLoadStatus.Loading)
            val type = application.contentResolver.getType(contentUri);
            val result = if (contentUri.path?.endsWith(".pdf") == true || "application/pdf" == type) {
                loadAsPdf(contentUri)
            } else {
                ReadFile.load(application, contentUri)
                        ?.run {
                            val upscaledPix = maybeUpscale(this)
                            ImageLoadStatus.Loaded(upscaledPix) { w, h -> scaleForCrop(upscaledPix, w, h) }
                        }
                        ?: ImageLoadStatus.Error(PixLoadStatus.IMAGE_FORMAT_UNSUPPORTED)
            }
            if (result is ImageLoadStatus.Error) {
                application.crashLogger.setString("content uri", contentUri.toString())
                application.crashLogger.logException(IOException(result.pixLoadStatus.name))
            }
            _content.postValue(result)
            application.espressoTestIdlingResource.decrement()
        }
    }

    private fun loadAsPdf(contentUri: Uri): ImageLoadStatus {
        val page = getPdfDocument(contentUri, application)?.use {
         it.getPage(0)
        }
        return if (page != null) {
            ImageLoadStatus.Loaded(page) { w, h -> scaleForCrop(page, w, h) }
        } else {
            ImageLoadStatus.Error(PixLoadStatus.IMAGE_FORMAT_UNSUPPORTED)
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

    private fun cropAndProject(
            pix: Pix,
            scaleFactor: Float,
            trapezoid: FloatArray,
            perspectiveCorrectedBoundingRect: Rect,
            rotation: Int
    ) {
        application.espressoTestIdlingResource.increment()
        viewModelScope.launch(Dispatchers.IO) {
            val scale = 1f / scaleFactor
            val scaleMatrix = Matrix()
            scaleMatrix.setScale(scale, scale)
            val boundingRect = perspectiveCorrectedBoundingRect.toRectF()
            scaleMatrix.mapRect(boundingRect)
            val bb = Box(boundingRect.left.toInt(), boundingRect.top.toInt(), boundingRect.width().toInt(), boundingRect.height().toInt())
            val pix8 = convertTo8(pix)
            pix.recycle()
            val croppedPix = Clip.clipRectangle2(pix8, bb) ?: throw IllegalStateException()
            pix8.recycle()
            scaleMatrix.postTranslate(-bb.x.toFloat(), -bb.y.toFloat())
            scaleMatrix.mapPoints(trapezoid)
            val dest = floatArrayOf(0f, 0f, bb.width.toFloat(), 0f, bb.width.toFloat(), bb.height.toFloat(), 0f, bb.height.toFloat())
            var bilinear = Projective.projectiveTransform(croppedPix, dest, trapezoid)
            if (bilinear == null) {
                bilinear = croppedPix
            } else {
                croppedPix.recycle()
            }
            if (rotation != 0 && rotation != 4) {
                val rotatedPix = Rotate.rotateOrth(bilinear, rotation)
                bilinear.recycle()
                bilinear = rotatedPix
            }
            checkNotNull(bilinear)
            OCR.savePixToCacheDir(getApplication(), bilinear.copy())
            _content.postValue(ImageLoadStatus.CropSuccess(bilinear))
            application.espressoTestIdlingResource.decrement()
        }

    }

}