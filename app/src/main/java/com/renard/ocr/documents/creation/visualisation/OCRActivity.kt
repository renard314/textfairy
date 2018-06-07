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
import android.content.ContentProviderClient
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
import com.renard.ocr.MonitoredActivity
import com.renard.ocr.PermissionGrantedEvent
import com.renard.ocr.R
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

    private var mOriginalHeight = 0
    private var mOriginalWidth = 0
    private var mOcrLanguage: String? = null // is set by dialog in
    private lateinit var mOCR: OCR
    // if >=0 its the id of the parent document to which the current page shall be added
    private var mParentId = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.lockOrientation(this)
        EventBus.getDefault().register(this)
        val nativePix = intent.getLongExtra(DocumentGridActivity.EXTRA_NATIVE_PIX, -1)
        mParentId = intent.getIntExtra(EXTRA_PARENT_DOCUMENT_ID, -1)
        if (nativePix == -1L) {
            mCrashLogger.logException(IllegalStateException("native pix not defined"))
            val intent = Intent(this, DocumentGridActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        mOCR = OCR(this, anaLytics, crashLogger)
        setContentView(R.layout.activity_ocr)
        ButterKnife.bind(this)
        initToolbar()
        ensurePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, R.string.permission_explanation)
        mOCR.getOcrProgress().observe(this, Observer<OcrProgress> {
            when (it) {
                is OcrProgress.PreviewPicture -> previewPicture(it.bitmap)
                is OcrProgress.Message -> setToolbarMessage(it.message)
                is OcrProgress.LayoutElements -> onLayoutElements(it)
                is OcrProgress.Progress -> onProgress(it)
                is OcrProgress.Result -> onRecognisedText(it)
                is OcrProgress.Error -> onError(it)
            }
        })
    }

    private fun onError(it: OcrProgress.Error) {
        Toast.makeText(applicationContext, getText(it.message), Toast.LENGTH_LONG).show()
        Screen.unlockOrientation(this@OCRActivity)
        finish()
    }

    private fun onRecognisedText(it: OcrProgress.Result) {
        saveDocument(Pix(it.nativePix), it.hocrText, it.utf8Text, it.accuracy)
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
        val nativePix = intent.getLongExtra(DocumentGridActivity.EXTRA_NATIVE_PIX, -1)
        if (nativePix != -1L) {
            val pixOrg = Pix(nativePix)
            mOriginalHeight = pixOrg.height
            mOriginalWidth = pixOrg.width
            askUserAboutDocumentLayout()
        }
    }

    override fun onLayoutChosen(layoutKind: LayoutKind, ocrLanguage: String) {
        val nativePix = intent.getLongExtra(DocumentGridActivity.EXTRA_NATIVE_PIX, -1)
        if (nativePix != -1L) {
            val pixOrg = Pix(nativePix)
            if (layoutKind == LayoutKind.DO_NOTHING) {
                saveDocument(pixOrg, null, null, 0)
            } else {
                mOcrLanguage = ocrLanguage

                setToolbarMessage(R.string.progress_start)

                intent.removeExtra(DocumentGridActivity.EXTRA_NATIVE_PIX)
                if (layoutKind == LayoutKind.SIMPLE) {
                    mAnalytics.sendScreenView("Ocr")
                    mOCR.startOCRForSimpleLayout(this@OCRActivity, ocrLanguage, pixOrg, mImageView.width, mImageView.height)
                } else if (layoutKind == LayoutKind.COMPLEX) {
                    mOCR.startLayoutAnalysis(pixOrg, mImageView.width, mImageView.height)
                }
            }
        }

    }

    private var mPreviewWith: Int = 0
    private var mPreviewHeight: Int = 0

    private fun previewPicture(bitmap: Bitmap) {
        mPreviewHeight = bitmap.height
        mPreviewWith = bitmap.width
        mImageView.setImageBitmapResetBase(bitmap, true, 0)
    }

    private fun saveDocument(pix: Pix, hocrString: String?, utf8String: String?, accuracy: Int) {

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
                recycleResultPix(pix)
                if (documentUri != null && !isFinishing) {
                    startActivity(
                            Intent(this@OCRActivity, DocumentActivity::class.java).apply {
                                putExtra(DocumentActivity.EXTRA_ACCURACY, accuracy)
                                putExtra(DocumentActivity.EXTRA_LANGUAGE, mOcrLanguage)
                                data = documentUri
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                    )
                    pix.recycle()
                    finish()
                    Screen.unlockOrientation(this@OCRActivity)
                }
            }
        }, Handler())

    }

    private fun recycleResultPix(pix: Pix?) {
        pix?.recycle()
    }


    @Throws(IOException::class)
    private fun saveImage(p: Pix?): File {
        val id = DateFormat.format("ssmmhhddMMyy", Date(System.currentTimeMillis()))
        return Util.savePixToSD(p, id.toString())
    }

    @Throws(RemoteException::class)
    private fun saveDocumentToDB(imageFile: File?, hocr: String?, plainText: String?): Uri? {
        var client: ContentProviderClient? = null
        try {
            val v = ContentValues()
            if (imageFile != null) {
                v.put(DocumentContentProvider.Columns.PHOTO_PATH,
                        imageFile.path)
            }
            if (hocr != null) {
                v.put(Columns.HOCR_TEXT, hocr)
            }
            if (plainText != null) {
                v.put(Columns.OCR_TEXT, plainText)
            }
            v.put(Columns.OCR_LANG, mOcrLanguage)

            if (mParentId > -1) {
                v.put(Columns.PARENT_ID, mParentId)
            }
            client = contentResolver.acquireContentProviderClient(DocumentContentProvider.CONTENT_URI)
            return client?.insert(DocumentContentProvider.CONTENT_URI, v)
        } finally {
            client?.release()
        }
    }


    override fun getHintDialogId(): Int {
        return -1
    }

    private fun askUserAboutDocumentLayout() {
        val dialog = LayoutQuestionDialog.newInstance()
        dialog.show(supportFragmentManager, LayoutQuestionDialog.TAG)
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mOcrLanguage != null) {
            outState.putString(OCR_LANGUAGE, mOcrLanguage)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (mOcrLanguage == null) {
            mOcrLanguage = savedInstanceState.getString(OCR_LANGUAGE)
        }
    }

    override fun getScreenName(): String {
        return ""
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        mOCR.destroy()
        mImageView.clear()

    }

    companion object {

        const val EXTRA_PARENT_DOCUMENT_ID = "parent_id"
        private const val OCR_LANGUAGE = "ocr_language"
        const val EXTRA_USE_ACCESSIBILITY_MODE = "ACCESSIBILTY_MODE"
    }
}
