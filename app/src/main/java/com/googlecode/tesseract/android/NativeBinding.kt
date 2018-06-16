package com.googlecode.tesseract.android

import android.support.annotation.StringRes
import android.util.Log
import com.googlecode.leptonica.android.Pix
import com.renard.ocr.R

class NativeBinding {

    private var mCallBack: ProgressCallBack? = null

    //Used by the native code
    private val mNativeData: Long = 0


    interface ProgressCallBack {
        fun onProgressImage(nativePix: Long)

        fun onProgressText(@StringRes message: Int)

        fun onLayoutAnalysed(nativePixaText: Long, nativePixaImages: Long)
    }


    fun setProgressCallBack(callBack: ProgressCallBack?) {
        mCallBack = callBack
    }

    init {
        nativeConstruct()
    }

    fun destroy() {
        Log.d(TAG, "destroying native data")
        nativeDestruct()
    }

    fun combinePixa(nativePixaText: Long, nativePixaImages: Long, selectedTexts: IntArray, selectedImages: IntArray): LongArray {
        return combineSelectedPixa(nativePixaText, nativePixaImages, selectedTexts, selectedImages)
    }

    fun analyseLayout(pixs: Pix) {
        nativeAnalyseLayout(pixs.nativePix)
    }

    fun convertBookPage(pixs: Pix): Long {
        return nativeOCRBook(pixs.nativePix)
    }

    /**
     * called from native code
     */
    @Synchronized
    private fun onProgressImage(nativePix: Long) {
        if (mCallBack != null) {
            mCallBack!!.onProgressImage(nativePix)
        }
    }

    /**
     * called from native
     * static const int MESSAGE_IMAGE_DETECTION = 0; static const int
     * MESSAGE_IMAGE_DEWARP = 1; static const int MESSAGE_OCR = 2; static const
     * int MESSAGE_ASSEMBLE_PIX = 3; static const int MESSAGE_ANALYSE_LAYOUT =
     * 4;
     */
    private fun onProgressText(id: Int) {
        var messageId = 0
        when (id) {
            0 -> messageId = R.string.progress_image_detection
            1 -> messageId = R.string.progress_dewarp
            2 -> messageId = R.string.progress_ocr
            3 -> messageId = R.string.progress_assemble_pix
            4 -> messageId = R.string.progress_analyse_layout
        }
        if (messageId != 0 && mCallBack != null) {
            mCallBack!!.onProgressText(messageId)
        }
    }

    /**
     * called from native
     */
    private fun onLayoutElements(nativePixaText: Long, nativePixaImages: Long) {
        if (mCallBack != null) {
            mCallBack!!.onLayoutAnalysed(nativePixaText, nativePixaImages)
        }
    }

    private external fun nativeConstruct()

    private external fun nativeDestruct()

    private external fun nativeOCRBook(nativePix: Long): Long

    private external fun combineSelectedPixa(nativePixaTexts: Long, nativePixaImages: Long, selectedTexts: IntArray, selectedImages: IntArray): LongArray

    private external fun nativeAnalyseLayout(nativePix: Long)

    companion object {

        private val TAG = "NativeBinding"

        init {
            nativeClassInit()
        }


        @JvmStatic
        private external fun nativeClassInit()
    }

}
