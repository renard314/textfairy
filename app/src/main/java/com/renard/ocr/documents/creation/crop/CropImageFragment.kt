/*
 * Copyright (C) 2007 The Android Open Source Project
 * Copyright (C) 2012,2013,2014,2015 Renard Wellnitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.renard.ocr.documents.creation.crop

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import com.renard.ocr.MonitoredActivity
import com.renard.ocr.R
import com.renard.ocr.cropimage.image_processing.BlurDetectionResult.Blurriness.STRONG_BLUR
import com.renard.ocr.databinding.ActivityCropimageBinding
import com.renard.ocr.documents.creation.ocr.ImageLoadingViewModel
import com.renard.ocr.documents.creation.ocr.ImageLoadingViewModel.ImageLoadStatus.PreparedForCrop
import com.renard.ocr.util.PreferencesUtils
import com.renard.ocr.util.afterMeasured

/**
 * The activity can crop specific region of interest from an image.
 */
class CropImageFragment : Fragment(R.layout.activity_cropimage) {

    private var mRotation = 0
    private var mCrop: CropHighlightView? = null
    private lateinit var binding: ActivityCropimageBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = ActivityCropimageBinding.bind(view)
        binding.itemRotateLeft.setOnClickListener { onRotateClicked(-1) }
        binding.itemRotateRight.setOnClickListener { onRotateClicked(1) }
        startCropping()
    }


    private fun showCropOnBoarding(blurDetectionResult: PreparedForCrop) {
        PreferencesUtils.setFirstScan(requireContext(), false)
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(R.string.crop_onboarding_title)
        builder.setMessage(R.string.crop_onboarding_message)
        builder.setPositiveButton(R.string.got_it) { dialog, _ -> dialog.dismiss() }
        builder.setOnDismissListener { handleBlurResult(blurDetectionResult) }
        builder.show()
    }


    private fun onRotateClicked(delta: Int) {
        val currentBitmap = binding.cropImageView.mBitmapDisplayed.bitmap ?: return
        mRotation += if (delta < 0) {
            -delta * 3
        } else {
            delta
        }
        mRotation %= 4
        binding.cropImageView.setImageBitmapResetBase(currentBitmap, false, mRotation * 90)
        showDefaultCroppingRectangle(currentBitmap)
    }

    private fun startCropping() {
        (requireActivity() as MonitoredActivity).setToolbarMessage(R.string.crop_title)
        val model by activityViewModels<ImageLoadingViewModel>()
        model.content.observe(viewLifecycleOwner) { status ->
            when (status) {
                is ImageLoadingViewModel.ImageLoadStatus.Loaded -> {
                    binding.root.afterMeasured {
                        val margin = resources.getDimension(R.dimen.crop_margin)
                        val width = (binding.cropLayout.width - 2 * margin).toInt()
                        val height = (binding.cropLayout.height - 2 * margin).toInt()
                        status.prepareForCropping(width, height)
                    }
                }
                is PreparedForCrop -> {
                    analytics().sendBlurResult(status.blurDetectionResult)
                    binding.cropLayout.displayedChild = 1
                    binding.root.afterMeasured {
                        binding.cropImageView.setImageBitmapResetBase(status.bitmap, true, mRotation * 90)
                        if (PreferencesUtils.isFirstScan(requireContext())) {
                            showCropOnBoarding(status)
                        } else {
                            handleBlurResult(status)
                        }

                        binding.itemRotateLeft.isVisible = true
                        binding.itemRotateRight.isVisible = true
                        binding.itemSave.isVisible = true

                        binding.itemSave.setOnClickListener {
                            status.cropAndProject(
                                    mCrop!!.trapezoid,
                                    mCrop!!.perspectiveCorrectedBoundingRect,
                                    mRotation
                            )
                        }
                    }
                }
                else -> { /*ignored*/
                }
            }
        }

    }


    private fun analytics() = (requireActivity() as MonitoredActivity).anaLytics

    private fun handleBlurResult(blurDetectionResult: PreparedForCrop) {
        analytics().sendScreenView(SCREEN_NAME)
        showDefaultCroppingRectangle(blurDetectionResult.bitmap)
        if (blurDetectionResult.blurDetectionResult.blurriness == STRONG_BLUR) {
            binding.cropImageView.zoomTo(1f, 500f);
            (requireActivity() as MonitoredActivity).setToolbarMessage(R.string.image_is_blurred)
            parentFragmentManager.commit(true) {
                add(
                        BlurWarningDialog.newInstance(blurDetectionResult.blurDetectionResult.blurValue.toFloat()),
                        BlurWarningDialog.TAG
                )
            }
        }
    }

    private fun showDefaultCroppingRectangle(bitmap: Bitmap) {
        val width = bitmap.width
        val height = bitmap.height
        val imageRect = Rect(0, 0, width, height)

        // make the default size about 4/5 of the width or height
        val cropWidth = Math.min(width, height) * 4 / 5
        val x = (width - cropWidth) / 2
        val y = (height - cropWidth) / 2
        val cropRect = RectF(x.toFloat(), y.toFloat(), (x + cropWidth).toFloat(), (y + cropWidth).toFloat())
        val hv = CropHighlightView(binding.cropImageView, imageRect, cropRect)
        binding.cropImageView.resetMaxZoom()
        binding.cropImageView.add(hv)
        mCrop = hv
        mCrop!!.setFocus(true)
        binding.cropImageView.invalidate()
    }

    companion object {
        internal const val HINT_DIALOG_ID = 2
        const val SCREEN_NAME = "Crop Image"
    }
}