/*
 * Copyright (C) 2012, 2013, 2014, 2015 Renard Wellnitz.
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
package com.renard.ocr.documents.creation.visualisation

import android.Manifest
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.RemoteException
import android.text.format.DateFormat
import android.view.View
import android.widget.Button
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import com.googlecode.leptonica.android.Pix
import com.googlecode.tesseract.android.OCR
import com.googlecode.tesseract.android.OcrProgress
import com.googlecode.tesseract.android.PreviewPicture
import com.renard.ocr.MonitoredActivity
import com.renard.ocr.PermissionGrantedEvent
import com.renard.ocr.R
import com.renard.ocr.TextFairyApplication
import com.renard.ocr.documents.creation.visualisation.LayoutQuestionDialog.LayoutChoseListener
import com.renard.ocr.documents.creation.visualisation.LayoutQuestionDialog.LayoutKind
import com.renard.ocr.documents.viewing.DocumentContentProvider
import com.renard.ocr.documents.viewing.DocumentContentProvider.Columns
import com.renard.ocr.documents.viewing.grid.DocumentGridActivity
import com.renard.ocr.documents.viewing.single.DocumentActivity
import com.renard.ocr.util.Screen
import com.renard.ocr.util.Util
import de.greenrobot.event.EventBus
import java.io.File
import java.io.IOException
import java.util.*

private const val LOG_TAG = "OcrActivity"


/**
 * this activity is shown during the ocr process
 *
 * @author renard
 */
class OCRActivity : MonitoredActivity(), LayoutChoseListener {

    @BindView(R.id.column_pick_completed)
    lateinit var mButtonStartOCR: Button
    @BindView(R.id.progress_image)
    lateinit var mImageView: OCRImageView

    private var mOcrLanguage: String? = null
    private lateinit var mOCR: OCR
    private var mParentId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.lockOrientation(this)
        EventBus.getDefault().register(this)
        val nativePix = intent.getLongExtra(DocumentGridActivity.EXTRA_NATIVE_PIX, -1)
        mParentId = intent.getIntExtra(EXTRA_PARENT_DOCUMENT_ID, -1)

        mOCR = ViewModelProviders.of(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return OCR(Pix(nativePix), application as TextFairyApplication) as T
            }

        }).get(OCR::class.java)

        setContentView(R.layout.activity_ocr)
        ButterKnife.bind(this)
        initToolbar()
        ensurePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, R.string.permission_explanation)
        mOCR.getOcrProgress().observe(this, Observer<OcrProgress> {
            when (it) {
                is OcrProgress.Message -> setToolbarMessage(it.message)
                is OcrProgress.LayoutElements -> onLayoutElements(it)
                is OcrProgress.Progress -> onProgress(it)
                is OcrProgress.Result -> onRecognisedText(it)
                is OcrProgress.Error -> onError(it)
            }
        })
        mOCR.getPreviewPictures().observe(this, Observer<PreviewPicture> {
            previewPicture(it?.bitmap)
        })
    }

    private fun onError(it: OcrProgress.Error) {
        Toast.makeText(applicationContext, getText(it.message), Toast.LENGTH_LONG).show()
        Screen.unlockOrientation(this@OCRActivity)
        finish()
    }

    private fun onRecognisedText(it: OcrProgress.Result) {
        saveDocument(it.nativePix, it.hocrText, it.utf8Text, it.accuracy)
    }

    private fun onLayoutElements(it: OcrProgress.LayoutElements) {

        mImageView.setImageRects(it.imageBounds)
        mImageView.setTextRects(it.columnBounds)

        mButtonStartOCR.visibility = View.VISIBLE
        mButtonStartOCR.setOnClickListener { view ->
            val selectedTexts = mImageView.selectedTextIndexes
            val selectedImages = mImageView.selectedImageIndexes
            if (selectedTexts.isNotEmpty() || selectedImages.isNotEmpty()) {
                mAnalytics.sendScreenView("Ocr")
                mImageView.clearAllProgressInfo()
                mOCR.startOCRForComplexLayout(
                        this@OCRActivity,
                        mOcrLanguage!!,
                        it.columns,
                        it.images,
                        selectedTexts,
                        selectedImages
                )
                mButtonStartOCR.visibility = View.GONE
            } else {
                Toast.makeText(
                        applicationContext,
                        R.string.please_tap_on_column,
                        Toast.LENGTH_LONG
                ).show()
            }
        }
        mAnalytics.sendScreenView("Pick Columns")

        setToolbarMessage(R.string.progress_choose_columns)

    }

    private fun onProgress(it: OcrProgress.Progress) {
        mImageView.setProgress(it.percent, it.wordBounds, it.pageBounds)
    }


    @Suppress("unused")
    fun onEventMainThread(event: PermissionGrantedEvent) {
        askUserAboutDocumentLayout()
    }

    override fun onLayoutChosen(layoutKind: LayoutKind, ocrLanguage: String) {
        if (layoutKind == LayoutKind.DO_NOTHING) {
            //saveDocument(mNativePix, null, null, 0)
            //TODO
        } else {
            mOcrLanguage = ocrLanguage

            setToolbarMessage(R.string.progress_start)

            if (layoutKind == LayoutKind.SIMPLE) {
                mAnalytics.sendScreenView("Ocr")
                mOCR.startOCRForSimpleLayout(this@OCRActivity, ocrLanguage, mImageView.width, mImageView.height)
            } else if (layoutKind == LayoutKind.COMPLEX) {
                mOCR.startLayoutAnalysis(mImageView.width, mImageView.height)
            }
        }
    }

    private fun previewPicture(bitmap: Bitmap?) {
        if (bitmap == null) {
            return
        }
        mImageView.setImageBitmapResetBase(bitmap, true, 0)
    }

    private fun saveDocument(pix: Long, hocrString: String?, utf8String: String?, accuracy: Int) {

        Util.startBackgroundJob(this@OCRActivity, "",
                getText(R.string.saving_document).toString(), {
            var imageFile: File? = null
            var documentUri: Uri? = null

            try {
                imageFile = saveImage(pix)
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(
                            applicationContext,
                            getText(R.string.error_create_file),
                            Toast.LENGTH_LONG).show()
                }
            }

            try {

                documentUri = saveDocumentToDB(imageFile, hocrString, utf8String)
                if (imageFile != null) {
                    Util.createThumbnail(this@OCRActivity, imageFile, Integer.valueOf(documentUri!!.lastPathSegment))
                }
            } catch (e: RemoteException) {
                e.printStackTrace()

                runOnUiThread {
                    Toast.makeText(
                            applicationContext,
                            getText(R.string.error_create_file),
                            Toast.LENGTH_LONG).show()
                }
            } finally {
                if (documentUri != null && !isFinishing) {
                    startActivity(
                            Intent(this@OCRActivity, DocumentActivity::class.java).apply {
                                putExtra(DocumentActivity.EXTRA_ACCURACY, accuracy)
                                putExtra(DocumentActivity.EXTRA_LANGUAGE, mOcrLanguage)
                                data = documentUri
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                    )
                    finish()
                    Screen.unlockOrientation(this@OCRActivity)
                }
            }
        }, Handler())

    }

    @Throws(IOException::class)
    private fun saveImage(p: Long): File {
        val id = DateFormat.format("ssmmhhddMMyy", Date(System.currentTimeMillis()))
        return Util.savePixToSD(Pix(p), id.toString())
    }

    @Throws(RemoteException::class)
    private fun saveDocumentToDB(imageFile: File?, hocr: String?, plainText: String?): Uri? {
        val client = contentResolver.acquireContentProviderClient(DocumentContentProvider.CONTENT_URI)
        try {
            return client.insert(DocumentContentProvider.CONTENT_URI, ContentValues().apply {
                if (imageFile != null) {
                    put(DocumentContentProvider.Columns.PHOTO_PATH, imageFile.path)
                }
                if (hocr != null) {
                    put(Columns.HOCR_TEXT, hocr)
                }
                if (plainText != null) {
                    put(Columns.OCR_TEXT, plainText)
                }
                if (mParentId > -1) {
                    put(Columns.PARENT_ID, mParentId)
                }
                put(Columns.OCR_LANG, mOcrLanguage)
            })
        } finally {
            client.release()
        }
    }


    override fun getHintDialogId() = -1

    private fun askUserAboutDocumentLayout() {
        LayoutQuestionDialog.newInstance().show(supportFragmentManager, LayoutQuestionDialog.TAG)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(OCR_LANGUAGE, mOcrLanguage)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (mOcrLanguage == null) {
            mOcrLanguage = savedInstanceState.getString(OCR_LANGUAGE)
        }
    }

    override fun getScreenName() = ""

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        mImageView.clear()

    }

    companion object {
        const val EXTRA_PARENT_DOCUMENT_ID = "parent_id"
        private const val OCR_LANGUAGE = "ocr_language"
        const val EXTRA_USE_ACCESSIBILITY_MODE = "ACCESSIBILTY_MODE"
    }
}
