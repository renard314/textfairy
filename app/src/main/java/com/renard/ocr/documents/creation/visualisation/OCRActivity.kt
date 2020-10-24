/*
 * Copyright (C) Renard Wellnitz.
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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.accessibility.AccessibilityManagerCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.commit
import com.googlecode.leptonica.android.Pix
import com.googlecode.tesseract.android.OCR
import com.googlecode.tesseract.android.OcrProgress.*
import com.googlecode.tesseract.android.OcrProgress.Error
import com.renard.ocr.MonitoredActivity
import com.renard.ocr.R
import com.renard.ocr.TextFairyApplication
import com.renard.ocr.databinding.ActivityOcrBinding
import com.renard.ocr.documents.creation.DocumentStore.saveDocument
import com.renard.ocr.documents.creation.NewDocumentActivity
import com.renard.ocr.documents.creation.NewDocumentActivity.EXTRA_IMAGE_SOURCE
import com.renard.ocr.documents.creation.NewDocumentActivityViewModel
import com.renard.ocr.documents.creation.NewDocumentActivityViewModel.Status.*
import com.renard.ocr.documents.creation.ProgressDialogFragment
import com.renard.ocr.documents.creation.crop.CropImageActivity
import com.renard.ocr.documents.creation.crop.CropImageActivity.RESULT_NEW_IMAGE
import com.renard.ocr.documents.creation.visualisation.LayoutQuestionDialog.LayoutChoseListener
import com.renard.ocr.documents.creation.visualisation.LayoutQuestionDialog.LayoutKind
import com.renard.ocr.documents.creation.visualisation.LayoutQuestionDialog.LayoutKind.COMPLEX
import com.renard.ocr.documents.creation.visualisation.LayoutQuestionDialog.LayoutKind.SIMPLE
import com.renard.ocr.documents.viewing.single.DocumentActivity.EXTRA_ACCURACY
import com.renard.ocr.documents.viewing.single.DocumentActivity.EXTRA_LANGUAGE


/**
 * this activity is shown during the ocr process
 *
 * @author renard
 */
class OCRActivity : MonitoredActivity(), LayoutChoseListener {

    private lateinit var binding: ActivityOcrBinding
    private val ocrModel: OCR by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadImage()
        setupOcr()
        binding = ActivityOcrBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initToolbar()
    }

    private fun setupOcr() {
        ocrModel.getOcrProgress().observe(this, {
            when (it) {
                is Message -> binding.toolbar.toolbarText.setText(it.message)
                is LayoutElements -> {
                    anaLytics.sendScreenView("Pick Columns")
                    binding.toolbar.toolbarText.setText(R.string.progress_choose_columns)
                }
                is Result -> onOcrResult(it)
                is Error -> onOcrError(it)
            }
        })
    }

    private fun onOcrError(it: Error) {
        Toast.makeText(this, getText(it.message), Toast.LENGTH_LONG).show()
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun onOcrResult(it: Result) {
        val parentId = intent.getIntExtra(EXTRA_PARENT_DOCUMENT_ID, -1)
        saveDocument(this,
                it.pix.nativePix,
                it.hocrText,
                it.utf8Text,
                parentId,
                it.language
        ) { uri: Uri? ->
            if (uri != null) {
                val intent = Intent()
                intent.data = uri
                intent.putExtra(EXTRA_ACCURACY, it.accuracy)
                intent.putExtra(EXTRA_LANGUAGE, it.language)
                setResult(RESULT_OK, intent)
                finish()
            } else {
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }

    private fun loadImage() {
        val model by viewModels<NewDocumentActivityViewModel>()
        model.content.observe(this) { status ->
            when (status) {
                is Success -> {
                    dismissLoadingImageProgressDialog()
                    (application as TextFairyApplication).nativePix = status.pix.nativePix
                    val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
                    val isAccessibilityEnabled = am.isEnabled
                    val isExploreByTouchEnabled = AccessibilityManagerCompat.isTouchExplorationEnabled(am)
                    val skipCrop = isExploreByTouchEnabled && isAccessibilityEnabled
                    if (skipCrop) {
                        askForLayout()
                    } else {
                        val actionIntent = Intent(this, CropImageActivity::class.java)
                        startActivityForResult(actionIntent, REQUEST_CODE_CROP_PHOTO)
                    }
                }
                is Loading -> {
                    showLoadingImageProgressDialog()
                }
                is NewDocumentActivityViewModel.Status.Error -> {
                    NewDocumentActivity.showFileError(this, status.pixLoadStatus)
                }
                is SuccessPdf -> {
                    //TODO remove
                }
            }
        }
        model.loadContent(intent.data!!)
    }

    private fun askForLayout() {
        supportFragmentManager.commit {
            add(LayoutQuestionDialog.newInstance(), LayoutQuestionDialog.TAG)
            add(R.id.fragment_container, OcrFragment(), OCR_FRAGMENT_TAG)
        }
    }

    override fun onLayoutChosen(layoutKind: LayoutKind, language: String) {
        binding.toolbar.toolbarText.setText(R.string.progress_start)
        val nativePix = (application as TextFairyApplication).nativePix!!
        when (layoutKind) {
            SIMPLE -> {
                anaLytics.sendScreenView("Ocr")
                ocrModel.startOCRForSimpleLayout(this, Pix(nativePix), language)
            }
            COMPLEX -> {
                ocrModel.startLayoutAnalysis(Pix(nativePix), language)
            }
        }
    }

    override fun onLayoutSelectionCancelled() {
        setResult(RESULT_CANCELED)
        finish()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CODE_CROP_PHOTO) {
            return
        }
        when (resultCode) {
            RESULT_OK -> {
                askForLayout()
            }
            RESULT_NEW_IMAGE -> {
                val intent = Intent()
                intent.putExtra(EXTRA_IMAGE_SOURCE, intent.getStringExtra(EXTRA_IMAGE_SOURCE))
                setResult(resultCode, intent)
                finish()
            }
            RESULT_CANCELED -> {
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }

    override fun finish() {
        super.finish()
        (application as TextFairyApplication).nativePix = null
    }

    override fun getHintDialogId() = -1

    override fun getScreenName() = ""

    private fun dismissLoadingImageProgressDialog() {
        val prev = supportFragmentManager.findFragmentByTag(IMAGE_LOAD_PROGRESS_TAG)
        if (prev != null) {
            Log.i(LOG_TAG, "dismissing dialog")
            val df = prev as DialogFragment
            df.dismissAllowingStateLoss()
        } else {
            Log.i(LOG_TAG, "cannot dismiss dialog. its null! $this")
        }
    }

    private fun showLoadingImageProgressDialog() {
        Log.i(LOG_TAG, "showLoadingImageProgressDialog")
        supportFragmentManager.commit(true){
            add(ProgressDialogFragment.newInstance(R.string.please_wait, R.string.loading_image),IMAGE_LOAD_PROGRESS_TAG)
        }
    }

    companion object {
        private const val IMAGE_LOAD_PROGRESS_TAG = "image_load_progress"
        private val LOG_TAG = OCRActivity::class.java.simpleName
        private const val OCR_FRAGMENT_TAG = "OCR"
        const val REQUEST_CODE_CROP_PHOTO = 2
        const val EXTRA_PARENT_DOCUMENT_ID = "parent_id"
    }

}
