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

import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import androidx.annotation.StringRes
import androidx.annotation.WorkerThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.map
import androidx.test.espresso.idling.concurrent.IdlingScheduledThreadPoolExecutor
import com.googlecode.leptonica.android.Boxa
import com.googlecode.leptonica.android.Pix
import com.googlecode.leptonica.android.Pixa
import com.googlecode.tesseract.android.OcrProgress.*
import com.googlecode.tesseract.android.TessBaseAPI.OEM_LSTM_ONLY
import com.googlecode.tesseract.android.TessBaseAPI.PageSegMode
import com.renard.ocr.R
import com.renard.ocr.TextFairyApplication
import com.renard.ocr.analytics.CrashLogger
import com.renard.ocr.documents.creation.crop.CropImageScaler
import com.renard.ocr.main_menu.language.OcrLanguage
import com.renard.ocr.main_menu.language.OcrLanguageDataStore.deleteLanguage
import com.renard.ocr.util.AppStorage
import com.renard.ocr.util.MemoryInfo
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

sealed class OcrProgress {
    data class Message(@StringRes val message: Int) : OcrProgress()
    data class LayoutElements(val columns: Pixa, val images: Pixa, val pageWidth: Int, val pageHeight: Int, val language: String) : OcrProgress()
    data class Progress(val percent: Int, val wordBounds: Rect, val rectBounds: Rect, val pageWidth: Int, val pageHeight: Int) : OcrProgress()
    data class Result(val pix: Pix, val utf8Text: String, val hocrText: String, val accuracy: Int, val language: String) : OcrProgress()
    data class Error(@StringRes val message: Int) : OcrProgress()
}

class OCR(application: Application) : AndroidViewModel(application) {

    private val mAnalytics
        get() = getApplication<TextFairyApplication>().analytics
    private val mCrashLogger
        get() = getApplication<TextFairyApplication>().crashLogger
    private val mNativeBinding: NativeBinding = NativeBinding()
    private val mStopped = AtomicBoolean(false)
    private var mCompleted = AtomicBoolean(false)
    private val mExecutorService = IdlingScheduledThreadPoolExecutor("Ocr Thread Pool", 1) { r -> Thread(r) }
    private var lastPreview: Pix? = null

    private val ocrProgress: MutableLiveData<OcrProgress> = MutableLiveData()
    private val _preview = MutableLiveData<Pix>()
    val preview: LiveData<Pix>
        get() = map(_preview) {
            lastPreview?.recycle()
            lastPreview = it
            it
        }


    fun getOcrProgress(): LiveData<OcrProgress> {
        return ocrProgress
    }


    override fun onCleared() {
        super.onCleared()
        lastPreview?.recycle()
        mCrashLogger.logMessage("OCR#onCleared")
        mStopped.set(true)
        mNativeBinding.destroy()
        if (!mCompleted.get()) {
            mCrashLogger.logMessage("ocr cancelled")
            mAnalytics.sendOcrCancelled()
        }
    }

    /**
     * native code takes care of the Pix, do not use it after calling this
     * function
     **/
    fun startLayoutAnalysis(pix: Pix, language: String) {
        setupImageProcessingCallback(pix.width, pix.height, language)
        mExecutorService.execute {
            mNativeBinding.analyseLayout(pix)
        }
    }

    private fun setupImageProcessingCallback(width: Int, height: Int, language: String) {
        mNativeBinding.setProgressCallBack(object : NativeBinding.ProgressCallBack {
            @WorkerThread
            override fun onProgressImage(nativePix: Long) {
                sendPreview(Pix(nativePix))
            }

            @WorkerThread
            override fun onProgressText(@StringRes message: Int) {
                ocrProgress.postValue(Message(message))
            }

            @WorkerThread
            override fun onLayoutAnalysed(nativePixaText: Long, nativePixaImages: Long) {
                ocrProgress.postValue(
                        LayoutElements(
                                Pixa(nativePixaText, 0, 0),
                                Pixa(nativePixaImages, 0, 0),
                                width,
                                height,
                                language
                        )
                )
            }
        })
    }

    /**
     * native code takes care of both Pixa, do not use them after calling this
     * function
     *
     * @param pixaText   must contain the binary text parts
     * @param pixaImages pixaImages must contain the image parts
     */
    fun startOCRForComplexLayout(context: Context, pixaText: Pixa, pixaImages: Pixa, selectedTexts: IntArray, selectedImages: IntArray, lang: String) {
        mExecutorService.execute {
            mCrashLogger.logMessage("startOCRForComplexLayout")
            var pixOcr: Pix? = null
            var boxa: Boxa? = null
            try {
                logMemory(context)

                val columnData = mNativeBinding.combinePixa(pixaText.nativePixa, pixaImages.nativePixa, selectedTexts, selectedImages)
                pixaText.recycle()
                pixaImages.recycle()
                val pixOrgPointer = columnData[0]
                pixOcr = Pix(columnData[1])
                boxa = Boxa(columnData[2])

                sendPreview(pixOcr)
                ocrProgress.postValue(Message(R.string.progress_ocr))

                val tessApi = initTessApi(getApplication(), lang, mCrashLogger) { progressValues ->
                    ocrProgress.postValue(
                            Progress(
                                    percent = progressValues.percent,
                                    wordBounds = progressValues.currentWordRect,
                                    rectBounds = progressValues.currentRect,
                                    pageWidth = pixOcr.width,
                                    pageHeight = pixOcr.height
                            )
                    )
                }
                if (tessApi == null) {
                    ocrProgress.postValue(Error(R.string.error_tess_init))
                }
                tessApi?.use { tess ->
                    tess.pageSegMode = PageSegMode.PSM_SINGLE_BLOCK
                    tess.setImage(pixOcr)

                    if (mStopped.get()) {
                        return@use
                    }
                    var accuracy = 0f
                    val geometry = IntArray(4)
                    val hocrText = StringBuilder()
                    val htmlText = StringBuilder()
                    for (i in 0 until boxa.count) {
                        if (!boxa.getGeometry(i, geometry)) {
                            continue
                        }
                        tess.setRectangle(geometry[0], geometry[1], geometry[2], geometry[3])
                        hocrText.append(tess.getHOCRText(0))
                        htmlText.append(tess.htmlText)
                        accuracy += tess.meanConfidence().toFloat()
                        if (mStopped.get()) {
                            return@use
                        }
                    }
                    val totalAccuracy = (accuracy / boxa.count).roundToInt()
                    ocrProgress.postValue(Result(
                            Pix(pixOrgPointer),
                            htmlText.toString(),
                            hocrText.toString(),
                            totalAccuracy,
                            lang
                    ))
                }

            } finally {
                pixOcr?.recycle()
                boxa?.recycle()
                mCompleted.set(true)
                mCrashLogger.logMessage("startOCRForComplexLayout finished")
            }
        }
    }

    /**
     * native code takes care of the Pix, do not use it after calling this
     * function
     *
     * @param context used to access the file system
     * @param pix    source pix to do ocr on
     */
    fun startOCRForSimpleLayout(context: Context, pix: Pix, lang: String) {
        setupImageProcessingCallback(pix.width, pix.height, lang)
        mExecutorService.execute {
            mCrashLogger.logMessage("startOCRForSimpleLayout")
            try {
                logMemory(context)
                sendPreview(pix)
                val pixText = Pix(mNativeBinding.convertBookPage(pix))
                sendPreview(pixText)
                pix.recycle()
                if (mStopped.get()) {
                    pixText.recycle()
                    return@execute
                }
                ocrProgress.postValue(Message(R.string.progress_ocr))
                val tessApi = initTessApi(getApplication(), lang, mCrashLogger) { progressValues ->
                    ocrProgress.postValue(
                            Progress(
                                    percent = progressValues.percent,
                                    wordBounds = progressValues.currentWordRect,
                                    rectBounds = progressValues.currentRect,
                                    pageWidth = pixText.width,
                                    pageHeight = pixText.height
                            )
                    )
                }
                if(tessApi==null){
                    ocrProgress.postValue(Error(R.string.error_tess_init))
                }
                tessApi?.use scan@{ tess ->
                    tess.pageSegMode = PageSegMode.PSM_AUTO
                    tess.setImage(pixText)
                    var hocrText = tess.getHOCRText(0)
                    var accuracy = tess.meanConfidence()
                    val utf8Text = tess.utF8Text

                    if (!mStopped.get() && utf8Text.isEmpty()) {
                        mCrashLogger.logMessage("No words found. Looking for sparse text.")
                        tess.pageSegMode = PageSegMode.PSM_SPARSE_TEXT
                        tess.setImage(pixText)
                        hocrText = tess.getHOCRText(0)
                        accuracy = tess.meanConfidence()
                    }

                    if (mStopped.get()) {
                        return@scan
                    }
                    val htmlText = tess.htmlText
                    if (accuracy == 95) {
                        accuracy = 0
                    }
                    ocrProgress.postValue(Result(pixText, htmlText.toString(), hocrText.toString(), accuracy, lang))
                }

            } finally {
                mCompleted.set(true)
                mCrashLogger.logMessage("startOCRForSimpleLayout finished")
            }
        }
    }

    private fun sendPreview(it: Pix) {
        val widthPixels = Resources.getSystem().displayMetrics.widthPixels
        val heightPixels = Resources.getSystem().displayMetrics.heightPixels
        val scale = CropImageScaler().scale(it, widthPixels, heightPixels)
        _preview.postValue(scale.pix)
    }


    private fun logMemory(context: Context) {
        val freeMemory = MemoryInfo.getFreeMemory(context)
        mCrashLogger.setLong("Memory", freeMemory)
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

    private fun <T> Pix.use(block: (Pix) -> T) = block(this).also { recycle() }

}

fun initTessApi(context: Context, lang: String, crashLogger: CrashLogger, onProgress: (TessBaseAPI.ProgressValues) -> Unit): TessBaseAPI? {
    val mTess = TessBaseAPI(onProgress)
    val tessDir = AppStorage.getTrainingDataDir(context)?.path ?: return null
    with(crashLogger) {
        setString(
                tag = "page seg mode",
                value = "OEM_LSTM_ONLY"
        )
        setString("ocr language", lang)
    }
    val result = mTess.init(tessDir, lang, OEM_LSTM_ONLY)
    if (!result) {
        crashLogger.logMessage("init failed. deleting $lang")
        deleteLanguage(lang, context)
        OcrLanguage(lang).installLanguage(context)
        return null
    }
    crashLogger.logMessage("init succeeded")
    mTess.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "ﬀﬁﬂﬃﬄﬅﬆ")
    return mTess
}

public fun TessBaseAPI.use(block: (TessBaseAPI) -> Unit) {
    block(this)
    this.end()
}
