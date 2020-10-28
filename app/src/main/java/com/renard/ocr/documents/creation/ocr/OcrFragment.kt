package com.renard.ocr.documents.creation.ocr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Transformations
import com.googlecode.leptonica.android.Pix
import com.googlecode.leptonica.android.WriteFile
import com.googlecode.tesseract.android.OCR
import com.googlecode.tesseract.android.OcrProgress.*
import com.renard.ocr.MonitoredActivity
import com.renard.ocr.R
import com.renard.ocr.TextFairyApplication
import com.renard.ocr.databinding.FragmentOcrBinding
import com.renard.ocr.documents.creation.crop.CropImageScaler

/**
 * Allows to select columns for OCR and shows the ocr recognition progress.
 */
class OcrFragment : Fragment() {

    private lateinit var binding: FragmentOcrBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentOcrBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ocr by activityViewModels<OCR>()
        ocr.getOcrProgress().observe(viewLifecycleOwner, {
            when (it) {
                is LayoutElements -> onLayoutElements(it)
                is Progress -> showProgress(it)
            }
        })
        ocr.preview.observe(viewLifecycleOwner){ showPreview(it) }
    }

    private fun showPreview(pix: Pix) {
        val previewBitmap = WriteFile.writeBitmap(pix)
        if (previewBitmap != null) {
            binding.progressImage.setImageBitmapResetBase(previewBitmap, true, 0)
        } else {
            throw IllegalStateException("preview null")
        }
    }

    private fun showProgress(it: Progress) {
        binding.progressImage.setProgress(it.percent, it.wordBounds, it.rectBounds, it.pageWidth, it.pageHeight)
    }

    private fun onLayoutElements(it: LayoutElements) {
        binding.progressImage.setImageRects(it.images.boxRects, it.pageWidth, it.pageHeight)
        binding.progressImage.setTextRects(it.columns.boxRects, it.pageWidth, it.pageHeight)
        binding.columnPickCompleted.visibility = View.VISIBLE
        binding.columnPickCompleted.setOnClickListener { view ->
            val selectedTexts = binding.progressImage.getSelectedTextIndexes()
            val selectedImages = binding.progressImage.getSelectedImageIndexes()
            if (selectedTexts.isNotEmpty() || selectedImages.isNotEmpty()) {
                val ocr by activityViewModels<OCR>()
                (requireActivity() as MonitoredActivity).anaLytics.sendScreenView("Ocr")
                binding.progressImage.clearAllProgressInfo()
                ocr.startOCRForComplexLayout(
                        requireContext(),
                        it.columns,
                        it.images,
                        selectedTexts,
                        selectedImages,
                        it.language
                )
                binding.columnPickCompleted.visibility = View.GONE
            } else {
                Toast.makeText(
                        requireContext(),
                        R.string.please_tap_on_column,
                        Toast.LENGTH_LONG
                ).show()
            }
        }
    }

}

