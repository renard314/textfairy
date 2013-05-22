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

package com.googlecode.tesseract.android;

import java.util.ArrayList;
import java.util.List;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.RectF;
import android.os.Bundle;

import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.Pixa;
import com.renard.ocr.R;

public class OCRIntentService extends IntentService {

	public static final int MESSAGE_PREVIEW_IMAGE = 3;
	public static final int MESSAGE_END = 4;
	public static final int MESSAGE_ERROR = 5;
	public static final int MESSAGE_TESSERACT_PROGRESS = 6;
	public static final int MESSAGE_FINAL_IMAGE = 7;
	public static final int MESSAGE_UTF8_TEXT = 8;
	public static final int MESSAGE_HOCR_TEXT = 9;
	public static final int MESSAGE_LAYOUT_ELEMENTS = 10;
	public static final int MESSAGE_LAYOUT_PIX = 11;
	public static final int MESSAGE_EXPLANATION_TEXT = 12;
	public static final String EXTRA_STATUS = "status";
	public static final String EXTRA_HOCR_TEXT = "hocr_text";
	public static final String EXTRA_UTF8_TEXT = "utf8_text";
	public static final String EXTRA_PROGRESS = "progress";
	public static final String EXTRA_MESSAGE_ID= "message_id";
	public static final String EXTRA_WORD_BOX = "word_box";
	public static final String EXTRA_OCR_BOX = "ocr_box";
	public static final String EXTRA_PIX = "pix";
	public static final String EXTRA_PIXA_TEXT = "pixa_text";
	public static final String EXTRA_PIXA_IMAGE = "pixa_image";
	public static final String EXTRA_SELECTED_IMAGES = "selected_images";
	public static final String EXTRA_SELECTED_TEXTS = "selected_texts";
	public static final String PROGRESS_ACTION = "OCR_PROGRESS";

	private static final String ACTION_PIX_OCR = "pix_ocr";
	private static final String ACTION_PIX_ANALYSE = "pix_analyse_layout";
	private static final String ACTION_PIXA_OCR = "pixa_ocr";
	final private List<Integer> mPixList = new ArrayList<Integer>();
	final private List<Integer> mPixaList = new ArrayList<Integer>();
	final private RectF mWordBoundingBox = new RectF();
	final private RectF mOCRBoundingBox = new RectF();
	private int mPreviewWith;
	private int mPreviewHeight;
	private int mOriginalWidth;
	private int mOriginalHeight;

	static {
		System.loadLibrary("gnustl_shared");		
		System.loadLibrary("lept");
		System.loadLibrary("image_processing");
		System.loadLibrary("tess");
	}

	public OCRIntentService() {
		super(OCRIntentService.class.getName());
	}

	private void destroyAllPix() {
		for (int i : mPixList) {
			new Pix(i).recycle();
		}
		for (int i : mPixaList) {
			new Pixa(i, 0, 0).recycle();
		}
		mPixaList.clear();
		mPixList.clear();

	}

	private void startOCRSimpleLayout(final int nativePix) {
		if (nativePix != 0) {
			final Pix p = new Pix(nativePix);
			mOriginalHeight = p.getHeight();
			mOriginalWidth = p.getWidth();
			nativeOCRBook(nativePix);
		}
	}

	private void startLayoutAnalysis(final int nativePix) {
		if (nativePix != 0) {
			final Pix p = new Pix(nativePix);
			mOriginalHeight = p.getHeight();
			mOriginalWidth = p.getWidth();
			nativeAnalyseLayout(nativePix);
		}
	}

	private void startOCRComplexLayout(final int nativePixaImages, final int nativePixaTexts, final int[] selectedTexts, final int[] selectedImages) {
		if (nativePixaImages != 0 && nativePixaTexts != 0 && selectedImages != null && selectedTexts != null) {
			nativeOCR(nativePixaTexts, nativePixaImages, selectedTexts, selectedImages);
		}
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			destroyAllPix();
			final String action = intent.getAction();
			if (action == ACTION_PIX_OCR) {
				final int nativePix = intent.getIntExtra(EXTRA_PIX, 0);
				startOCRSimpleLayout(nativePix);
			} else if (action == ACTION_PIXA_OCR) {
				final int nativePixaImages = intent.getIntExtra(EXTRA_PIXA_IMAGE, 0);
				final int nativePixaTexts = intent.getIntExtra(EXTRA_PIXA_TEXT, 0);
				final int[] selectedTexts = intent.getIntArrayExtra(EXTRA_SELECTED_TEXTS);
				final int[] selectedImages = intent.getIntArrayExtra(EXTRA_SELECTED_IMAGES);
				startOCRComplexLayout(nativePixaImages, nativePixaTexts, selectedTexts, selectedImages);

			} else if (action == ACTION_PIX_ANALYSE) {
				final int nativePix = intent.getIntExtra(EXTRA_PIX, 0);
				startLayoutAnalysis(nativePix);

			}
		} finally {
			Bundle extras = new Bundle(1);
			extras.putInt(EXTRA_STATUS, MESSAGE_END);
			sendMessage(extras);
		}
	}

	private void sendMessage(Bundle extras) {
		Intent intent = new Intent(PROGRESS_ACTION);
		intent.putExtras(extras);
		sendBroadcast(intent);
	}

	/**
	 * 
	 * @param nativePix
	 */
	private void onProgressImage(final int nativePix) {
		Pix preview = new Pix(nativePix);
		mPreviewHeight = preview.getHeight();
		mPreviewWith = preview.getWidth();
		Bundle extras = new Bundle(1);
		extras.putInt(EXTRA_STATUS,MESSAGE_PREVIEW_IMAGE);
		extras.putInt(EXTRA_PIX, nativePix);
		sendMessage(extras);
	}

	/**
	 * called from native code
	 * 
	 * @param percent
	 * @param left
	 * @param right
	 * @param top
	 * @param bottom
	 */
	private void onProgressValues(final int percent, final int left, final int right, final int top, final int bottom, final int left2, final int right2, final int top2, final int bottom2) {

		int newBottom = (bottom2 - top2) - bottom;
		int newTop = (bottom2 - top2) - top;
		// scale the word bounding rectangle to the preview image space
		float xScale = (1.0f * mPreviewWith) / mOriginalWidth;
		float yScale = (1.0f * mPreviewHeight) / mOriginalHeight;
		mWordBoundingBox.set((left + left2) * xScale, (newTop + top2) * yScale, (right + left2) * xScale, (newBottom + top2) * yScale);
		mOCRBoundingBox.set(left2 * xScale, top2 * yScale, right2 * xScale, bottom2 * yScale);
		Bundle b = new Bundle(3);
		b.putParcelable(EXTRA_OCR_BOX, mOCRBoundingBox);
		b.putParcelable(EXTRA_WORD_BOX, mWordBoundingBox);
		b.putInt(EXTRA_PROGRESS, percent);
		sendMessage(b);
	}

	private void onProgressText(int id) {
		int messageId = 0;
		switch (id) {
		case 0:
			messageId = R.string.progress_image_detection;
			break;
		case 1:
			messageId = R.string.progress_dewarp;
			break;
		case 2:
			messageId = R.string.progress_ocr;
			break;
		case 3:
			messageId = R.string.progress_assemble_pix;
			break;
		case 4:
			messageId = R.string.progress_analyse_layout;
			break;

		}
		if (messageId != 0) {
			Bundle extras = new Bundle(2);
			extras.putInt(EXTRA_STATUS,MESSAGE_EXPLANATION_TEXT);
			extras.putInt(EXTRA_MESSAGE_ID, messageId);
			sendMessage(extras);
		}
	}

	/**
	 * called from native
	 * 
	 * @param native pix pointer
	 */
	private void onLayoutPix(int nativePix) {
		Bundle extras = new Bundle(2);
		extras.putInt(EXTRA_STATUS,MESSAGE_LAYOUT_PIX);
		extras.putInt(EXTRA_PIX, nativePix);
		sendMessage(extras);
	}

	/**
	 * called from native
	 * 
	 * @param native pix pointer
	 */
	private void onFinalPix(int nativePix) {
		Pix pix = new Pix(nativePix);
		mOriginalHeight = pix.getHeight();
		mOriginalWidth = pix.getWidth();
		Bundle extras = new Bundle(2);
		extras.putInt(EXTRA_STATUS,MESSAGE_FINAL_IMAGE);
		extras.putInt(EXTRA_PIX, nativePix);
		sendMessage(extras);
	}

	/**
	 * called from native
	 * 
	 * @param hocr
	 *            string
	 */
	private void onHOCRResult(String hocr) {
		Bundle extras = new Bundle(2);
		extras.putInt(EXTRA_STATUS,MESSAGE_HOCR_TEXT);
		extras.putString(EXTRA_HOCR_TEXT, hocr);
		sendMessage(extras);
	}

	/**
	 * called from native
	 * 
	 * @param utf8
	 *            string
	 */
	private void onUTF8Result(String utf8Text) {
		Bundle extras = new Bundle(2);
		extras.putInt(EXTRA_STATUS,MESSAGE_UTF8_TEXT);
		extras.putString(EXTRA_UTF8_TEXT, utf8Text);
		sendMessage(extras);
	}

	private void onLayoutElements(int nativePixaText, int nativePixaImages) {
		Bundle extras = new Bundle(3);
		extras.putInt(EXTRA_STATUS,MESSAGE_LAYOUT_ELEMENTS);
		extras.putInt(EXTRA_PIXA_IMAGE, nativePixaImages);
		extras.putInt(EXTRA_PIXA_TEXT, nativePixaText);
		sendMessage(extras);
	}

	// ***************
	// * NATIVE CODE *
	// ***************

	private native int nativeOCRBook(int nativePix);

	private native int nativeOCR(int nativePixaTexts, int nativePixaImages, int[] selectedTexts, int[] selectedImages);

	private native int nativeAnalyseLayout(int nativePix);

	private native int nativeCancelOCR();

}
