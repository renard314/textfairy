/*
 * Copyright (C) 2012,2013 Renard Wellnitz.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.googlecode.tesseract.android

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.support.annotation.MainThread
import android.support.annotation.StringRes
import android.support.annotation.WorkerThread
import com.googlecode.leptonica.android.Boxa
import com.googlecode.leptonica.android.Pix
import com.googlecode.leptonica.android.Pixa
import com.googlecode.leptonica.android.WriteFile
import com.googlecode.tesseract.android.TessBaseAPI.PageSegMode
import com.renard.ocr.R
import com.renard.ocr.analytics.Analytics
import com.renard.ocr.analytics.CrashLogger
import com.renard.ocr.documents.creation.crop.CropImageScaler
import com.renard.ocr.main_menu.language.OcrLanguageDataStore
import com.renard.ocr.util.MemoryInfo
import com.renard.ocr.util.Util
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

sealed class OcrProgress {
    data class PreviewPicture(val bitmap: Bitmap) : OcrProgress()
    data class Message(@StringRes val message: Int) : OcrProgress()
    data class LayoutElements(val columnBounds: List<RectF>, val imageBounds: List<RectF>, val columns: Pixa, val images: Pixa) : OcrProgress()
    data class Progress(val percent: Int, val wordBounds: RectF, val pageBounds: RectF) : OcrProgress()
    data class Result(val nativePix: Long, val utf8Text: String, val hocrText: String, val accuracy: Int) : OcrProgress()
    data class Error(@StringRes val message: Int) : OcrProgress()
}


class OCR @MainThread
constructor(
        context: Context,
        private val mAnalytics: Analytics,
        private val mCrashLogger: CrashLogger
) {

    private val mApplicationContext: Context = context.applicationContext

    private var mPreviewWith: Int = 0
    private var mPreviewHeight: Int = 0
    private var mOriginalWidth: Int = 0
    private var mOriginalHeight: Int = 0
    private val mNativeBinding: NativeBinding = NativeBinding()
    private val mStopped = AtomicBoolean(false)
    private var mCompleted = AtomicBoolean(false)
    private var mPreviewHeightUnScaled: Int = 0
    private var mPreviewWidthUnScaled: Int = 0
    private val mExecutorService = Executors.newSingleThreadExecutor()
    private val liveData: MutableLiveData<OcrProgress> = MutableLiveData()


    private val mTess: TessBaseAPI = TessBaseAPI(OcrProgressListener { percent, left, right, top, bottom, left2, right2, top2, bottom2 ->

        val availableMegs = MemoryInfo.getFreeMemory(mApplicationContext)
        mCrashLogger.logMessage("available ram = $availableMegs, percent done = $percent")
        mCrashLogger.setLong("ocr progress", percent.toLong())

        val newBottom = bottom2 - top2 - bottom
        val newTop = bottom2 - top2 - top
        // scale the word bounding rectangle to the preview image space
        val xScale = 1.0f * mPreviewWith / mOriginalWidth
        val yScale = 1.0f * mPreviewHeight / mOriginalHeight
        val wordBounds = RectF()
        val ocrBounds = RectF()

        wordBounds.set((left + left2) * xScale, (newTop + top2) * yScale, (right + left2) * xScale, (newBottom + top2) * yScale)
        ocrBounds.set(left2 * xScale, top2 * yScale, right2 * xScale, bottom2 * yScale)

        liveData.postValue(OcrProgress.Progress(percent, wordBounds, ocrBounds))
    })

    init {
        mNativeBinding.setProgressCallBack(object : NativeBinding.ProgressCallBack {
            @WorkerThread
            override fun onProgressImage(nativePix: Long) {
                sendPreview(nativePix)
            }

            @WorkerThread
            override fun onProgressText(@StringRes message: Int) {
                liveData.postValue(OcrProgress.Message(message))
            }

            @WorkerThread
            override fun onLayoutAnalysed(nativePixaText: Long, nativePixaImages: Long) {
                val xScale = 1.0f * mPreviewWith / mOriginalWidth
                val yScale = 1.0f * mPreviewHeight / mOriginalHeight
                val images = Pixa(nativePixaImages, 0, 0)
                val imageBounds = images.boxRects.map {
                    it.scale(xScale, yScale)
                }
                val columns = Pixa(nativePixaText, 0, 0)
                val columnBounds = columns.boxRects.map {
                    it.scale(xScale, yScale)
                }

                liveData.postValue(OcrProgress.LayoutElements(columnBounds, imageBounds, columns, images))
            }
        })
    }

    private fun Rect.scale(xScale: Float, yScale: Float): RectF =
            RectF(
                    left * xScale,
                    top * yScale,
                    right * xScale,
                    bottom * yScale
            )

    private fun sendPreview(nativePix: Long) {
        val preview = Pix(nativePix)
        val scale = CropImageScaler().scale(preview, mPreviewWidthUnScaled, mPreviewHeightUnScaled)
        val previewBitmap = WriteFile.writeBitmap(scale.pix)
        if (previewBitmap != null) {
            scale.pix.recycle()
            mPreviewHeight = previewBitmap.height
            mPreviewWith = previewBitmap.width
            liveData.postValue(OcrProgress.PreviewPicture(previewBitmap))
        }
    }

    fun getOcrProgress(): LiveData<OcrProgress> {
        return liveData
    }

    @MainThread
    fun destroy() {
        mCrashLogger.logMessage("OCR#destroy")
        mStopped.set(true)
        mTess.stop()
        if (!mCompleted.get()) {
            mCrashLogger.logMessage("ocr cancelled")
            mAnalytics.sendOcrCancelled()
        }
        mCrashLogger.logMessage("mNativeBinding.destroy()")
        mNativeBinding.destroy()
    }

    /**
     * native code takes care of the Pix, do not use it after calling this
     * function
     *
     * @param pix source pix on which to do layout analysis
     */
    fun startLayoutAnalysis(pix: Pix, width: Int, height: Int) {
        mPreviewHeightUnScaled = height
        mPreviewWidthUnScaled = width
        mOriginalHeight = pix.height
        mOriginalWidth = pix.width

        mExecutorService.execute {
            mCrashLogger.logMessage("start analyseLayout")
            mNativeBinding.analyseLayout(pix)
            mCrashLogger.logMessage("end analyseLayout")
        }
    }

    /**
     * native code takes care of both Pixa, do not use them after calling this
     * function
     *
     * @param pixaText   must contain the binary text parts
     * @param pixaImages pixaImages must contain the image parts
     */
    fun startOCRForComplexLayout(context: Context, lang: String, pixaText: Pixa?, pixaImages: Pixa, selectedTexts: IntArray, selectedImages: IntArray) {
        if (pixaText == null) {
            throw IllegalArgumentException("text pixa must be non-null")
        }

        mExecutorService.execute(Runnable {
            mCrashLogger.logMessage("startOCRForComplexLayout")
            var pixOcr: Pix? = null
            var boxa: Boxa? = null
            try {
                val tessDir = Util.getTessDir(context)
                val columnData = mNativeBinding.combinePixa(pixaText.nativePixa, pixaImages.nativePixa, selectedTexts, selectedImages)
                pixaText.recycle()
                pixaImages.recycle()
                val pixOrgPointer = columnData[0]
                val pixOcrPointer = columnData[1]
                val boxaColumnsPointer = columnData[2]

                liveData.postValue(OcrProgress.Message(R.string.progress_ocr))

                logMemory(context)
                val ocrLanguages = determineOcrLanguage(lang)

                if (!initTessApi(tessDir, ocrLanguages, TessBaseAPI.OEM_TESSERACT_ONLY)) {
                    return@Runnable
                }
                pixOcr = Pix(pixOcrPointer)
                sendPreview(pixOcrPointer)
                mTess.setPageSegMode(PageSegMode.PSM_SINGLE_BLOCK)
                mTess.setImage(pixOcr)
                boxa = Boxa(boxaColumnsPointer)
                mOriginalHeight = pixOcr.height
                mOriginalWidth = pixOcr.width
                if (mStopped.get()) {
                    return@Runnable
                }
                var xb: Int
                var yb: Int
                var wb: Int
                var hb: Int
                val columnCount = boxa.count
                var accuracy = 0f
                val geometry = IntArray(4)
                val hocrText = StringBuilder()
                val htmlText = StringBuilder()
                for (i in 0 until columnCount) {
                    if (!boxa.getGeometry(i, geometry)) {
                        continue
                    }
                    xb = geometry[0]
                    yb = geometry[1]
                    wb = geometry[2]
                    hb = geometry[3]
                    mTess.setRectangle(xb, yb, wb, hb)
                    if (mStopped.get()) {
                        return@Runnable
                    }
                    hocrText.append(mTess.getHOCRText(0))
                    if (mStopped.get()) {
                        return@Runnable
                    }
                    htmlText.append(mTess.htmlText)
                    accuracy += mTess.meanConfidence().toFloat()
                }
                val totalAccuracy = Math.round(accuracy / columnCount)
                liveData.postValue(OcrProgress.Result(pixOrgPointer, htmlText.toString(), hocrText.toString(), totalAccuracy))
            } finally {
                pixOcr?.recycle()
                boxa?.recycle()
                mTess.end()
                mCompleted.set(true)
                mCrashLogger.logMessage("startOCRForComplexLayout finished")
            }
        })
    }


    /**
     * native code takes care of the Pix, do not use it after calling this
     * function
     *
     * @param context used to access the file system
     * @param pixs    source pix to do ocr on
     */
    fun startOCRForSimpleLayout(context: Context, lang: String, pixs: Pix?, width: Int, height: Int) {
        if (pixs == null) {
            throw IllegalArgumentException("Source pix must be non-null")
        }
        mPreviewHeightUnScaled = height
        mPreviewWidthUnScaled = width
        mOriginalHeight = pixs.height
        mOriginalWidth = pixs.width

        mExecutorService.execute(Runnable {
            mCrashLogger.logMessage("startOCRForSimpleLayout")
            try {
                logMemory(context)
                val tessDir = Util.getTessDir(context)

                sendPreview(pixs.nativePix)

                val nativeTextPix = mNativeBinding.convertBookPage(pixs)

                sendPreview(nativeTextPix)

                val pixText = Pix(nativeTextPix)
                mOriginalHeight = pixText.height
                mOriginalWidth = pixText.width
                liveData.postValue(OcrProgress.Message(R.string.progress_ocr))
                if (mStopped.get()) {
                    return@Runnable
                }
                val ocrLanguages = determineOcrLanguage(lang)
                if (!initTessApi(tessDir, ocrLanguages, TessBaseAPI.OEM_TESSERACT_ONLY)) {
                    return@Runnable
                }

                mTess.setPageSegMode(PageSegMode.PSM_AUTO)
                mTess.setImage(pixText)
                var hocrText = mTess.getHOCRText(0)
                var accuracy = mTess.meanConfidence()
                val utf8Text = mTess.utF8Text

                if (!mStopped.get() && utf8Text.isEmpty()) {
                    mCrashLogger.logMessage("No words found. Looking for sparse text.")
                    mTess.setPageSegMode(PageSegMode.PSM_SPARSE_TEXT)
                    mTess.setImage(pixText)
                    hocrText = mTess.getHOCRText(0)
                    accuracy = mTess.meanConfidence()
                }

                if (mStopped.get()) {
                    return@Runnable
                }
                val htmlText = mTess.htmlText
                if (accuracy == 95) {
                    accuracy = 0
                }
                liveData.postValue(OcrProgress.Result(nativeTextPix, htmlText.toString(), hocrText.toString(), accuracy))
            } finally {

                mTess.end()
                mCompleted.set(true)
                mCrashLogger.logMessage("startOCRForSimpleLayout finished")
            }
        })

    }

    private fun initTessApi(tessDir: String, languages: List<String>, ocrMode: Int): Boolean {
        val languagesString = languages.joinToString("+")
        logTessParams(languagesString, ocrMode)
        val result = mTess.init(tessDir, languagesString, ocrMode)
        if (!result) {
            mCrashLogger.logMessage("init failed. deleting " + languages[0])
            OcrLanguageDataStore.deleteLanguage(languages[0], mApplicationContext)
            liveData.postValue(OcrProgress.Error(R.string.error_tess_init))
            return false
        }
        mCrashLogger.logMessage("init succeeded")
        mTess.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "ﬀﬁﬂﬃﬄﬅﬆ")
        return true
    }

    private fun Int.name(): String =
            when (this) {
                TessBaseAPI.OEM_TESSERACT_ONLY -> "OEM_TESSERACT_ONLY"
                TessBaseAPI.OEM_TESSERACT_CUBE_COMBINED -> "OEM_TESSERACT_CUBE_COMBINED"
                TessBaseAPI.OEM_DEFAULT -> "OEM_DEFAULT"
                else -> "undefined"
            }

    private fun logTessParams(lang: String, ocrMode: Int) {
        with(mCrashLogger) {
            setString(
                    tag = "page seg mode",
                    value = ocrMode.name()
            )
            setString("ocr language", lang)
        }
    }

    private fun logMemory(context: Context) {
        val freeMemory = MemoryInfo.getFreeMemory(context)
        mCrashLogger.setLong("Memory", freeMemory)
    }


    private fun determineOcrLanguage(ocrLanguage: String): List<String> {
        val english = "eng"
        val isEnglishInstalled = OcrLanguageDataStore.isLanguageInstalled(english, mApplicationContext).isInstalled
        return if (ocrLanguage != english && addEnglishData(ocrLanguage) && isEnglishInstalled) {
            listOf(ocrLanguage, english)
        } else {
            listOf(ocrLanguage)
        }
    }

    // when combining languages that have multi byte characters with english
    // training data the ocr text gets corrupted
    // but adding english will improve overall accuracy for the other languages
    private fun addEnglishData(mLanguage: String): Boolean {
        return !(mLanguage.startsWith("chi") || mLanguage.equals("tha", ignoreCase = true)
                || mLanguage.equals("kor", ignoreCase = true)
                || mLanguage.equals("jap", ignoreCase = true)
                || mLanguage.equals("hin", ignoreCase = true)
                || mLanguage.equals("bel", ignoreCase = true)
                || mLanguage.equals("ara", ignoreCase = true)
                || mLanguage.equals("grc", ignoreCase = true)
                || mLanguage.equals("guj", ignoreCase = true)
                || mLanguage.equals("rus", ignoreCase = true)
                || mLanguage.equals("vie", ignoreCase = true))
    }

    companion object {
        const val FILE_NAME = "last_scan"

        fun savePixToCacheDir(context: Context, pix: Pix) {
            val dir = File(context.cacheDir, context.getString(R.string.config_share_file_dir))
            SavePixTask(pix, dir).execute()
        }

        fun getLastOriginalImageFromCache(context: Context): File {
            val dir = File(context.cacheDir, context.getString(R.string.config_share_file_dir))
            return File(dir, "$FILE_NAME.png")

        }
    }


}
