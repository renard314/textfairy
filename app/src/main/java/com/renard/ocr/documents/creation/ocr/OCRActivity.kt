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
package com.renard.ocr.documents.creation.ocr

import android.app.Dialog
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
import com.googlecode.tesseract.android.OCR
import com.googlecode.tesseract.android.OcrProgress.*
import com.googlecode.tesseract.android.OcrProgress.Error
import com.renard.ocr.HintDialog
import com.renard.ocr.MonitoredActivity
import com.renard.ocr.R
import com.renard.ocr.databinding.ActivityOcrBinding
import com.renard.ocr.documents.creation.DocumentStore.saveDocument
import com.renard.ocr.documents.creation.NewDocumentActivity
import com.renard.ocr.documents.creation.NewDocumentActivity.EXTRA_IMAGE_SOURCE
import com.renard.ocr.documents.creation.ProgressDialogFragment
import com.renard.ocr.documents.creation.crop.BlurWarningDialog
import com.renard.ocr.documents.creation.crop.CropImageFragment
import com.renard.ocr.documents.creation.crop.CropImageFragment.Companion.HINT_DIALOG_ID
import com.renard.ocr.documents.creation.ocr.ImageLoadingViewModel.ImageLoadStatus
import com.renard.ocr.documents.creation.ocr.ImageLoadingViewModel.ImageLoadStatus.*
import com.renard.ocr.documents.creation.ocr.LayoutQuestionDialog.LayoutChoseListener
import com.renard.ocr.documents.creation.ocr.LayoutQuestionDialog.LayoutKind
import com.renard.ocr.documents.creation.ocr.LayoutQuestionDialog.LayoutKind.COMPLEX
import com.renard.ocr.documents.creation.ocr.LayoutQuestionDialog.LayoutKind.SIMPLE
import com.renard.ocr.documents.viewing.single.DocumentActivity.EXTRA_ACCURACY
import com.renard.ocr.documents.viewing.single.DocumentActivity.EXTRA_LANGUAGE


/**
 * this activity is shown during the ocr process
 *
 * @author renard
 */
class OCRActivity : MonitoredActivity(), LayoutChoseListener, BlurWarningDialog.BlurDialogClickListener {

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
                it.pix,
                it.hocrText,
                it.utf8Text,
                parentId,
                it.language
        ) { uri: Uri? ->
            it.pix.recycle()
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
        val model by viewModels<ImageLoadingViewModel>()
        model.content.observe(this) { status ->
            when (status) {
                is Success -> {
                    dismissLoadingImageProgressDialog()
                    val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
                    val isAccessibilityEnabled = am.isEnabled
                    val isExploreByTouchEnabled = AccessibilityManagerCompat.isTouchExplorationEnabled(am)
                    val skipCrop = isExploreByTouchEnabled && isAccessibilityEnabled
                    if (skipCrop) {
                        askForLayout()
                    } else {
                        supportFragmentManager.commit {
                            replace(R.id.fragment_container, CropImageFragment(), CROP_FRAGMENT_TAG)
                        }
                    }
                }
                Loading -> {
                    showLoadingImageProgressDialog()
                }
                is ImageLoadStatus.Error -> {
                    NewDocumentActivity.showFileError(this, status.pixLoadStatus)
                    finish()
                }
                Initial -> model.loadContent(intent.data!!)
                is CropSuccess -> askForLayout()
                CropError -> {
                    //should not happen. Scaling of the original document failed some how. Maybe out of memory?
                    anaLytics.sendCropError()
                    Toast.makeText(this, R.string.could_not_load_image, Toast.LENGTH_LONG).show()
                    onNewImageClicked()
                }
            }
        }

    }

    private fun askForLayout() {
        supportFragmentManager.commit {
            replace(R.id.fragment_container, OcrFragment(), OCR_FRAGMENT_TAG)
            add(LayoutQuestionDialog.newInstance(), LayoutQuestionDialog.TAG)
        }
    }

    override fun onLayoutChosen(layoutKind: LayoutKind, language: String) {
        binding.toolbar.toolbarText.setText(R.string.progress_start)

        val model by viewModels<ImageLoadingViewModel>()
        model.content.observe(this) { status ->
            val pix = when (status) {
                is Success -> status.pix
                is CropSuccess -> status.pix
                else -> throw IllegalStateException()
            }
            when (layoutKind) {
                SIMPLE -> {
                    anaLytics.sendScreenView("Ocr")
                    ocrModel.startOCRForSimpleLayout(this, pix, language)
                }
                COMPLEX -> {
                    ocrModel.startLayoutAnalysis(pix, language)
                }
            }
        }
    }

    override fun onLayoutSelectionCancelled() {
        setResult(RESULT_CANCELED)
        finish()
    }

    override fun onContinueClicked() {
        setToolbarMessage(R.string.crop_title)
    }

    override fun onNewImageClicked() {
        val intent = Intent()
        intent.putExtra(EXTRA_IMAGE_SOURCE, getIntent().getStringExtra(EXTRA_IMAGE_SOURCE))
        setResult(RESULT_NEW_IMAGE, intent)
        finish()
    }

    override fun getHintDialogId() =
            if (supportFragmentManager.findFragmentByTag(CROP_FRAGMENT_TAG) != null) {
                HINT_DIALOG_ID
            } else {
                -1
            }

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
        supportFragmentManager.commit(true) {
            add(ProgressDialogFragment.newInstance(R.string.please_wait, R.string.loading_image), IMAGE_LOAD_PROGRESS_TAG)
        }
    }


    override fun onCreateDialog(id: Int, args: Bundle): Dialog? {
        when (id) {
            HINT_DIALOG_ID -> return HintDialog.createDialog(this, R.string.crop_help_title, R.raw.crop_help)
        }
        return super.onCreateDialog(id, args)
    }


    companion object {
        const val RESULT_NEW_IMAGE = RESULT_CANCELED + 1
        private const val IMAGE_LOAD_PROGRESS_TAG = "image_load_progress"
        private val LOG_TAG = OCRActivity::class.java.simpleName
        private const val OCR_FRAGMENT_TAG = "OCR"
        private const val CROP_FRAGMENT_TAG = "CROP"
        const val EXTRA_PARENT_DOCUMENT_ID = "parent_id"
    }

}
