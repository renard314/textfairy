package com.renard.ocr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import android.app.AlertDialog;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.FragmentManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.Pixa;
import com.googlecode.leptonica.android.WriteFile;
import com.googlecode.tesseract.android.OCR;
import com.renard.documentview.DocumentActivity;
import com.renard.ocr.DocumentContentProvider.Columns;
import com.renard.ocr.LayoutQuestionDialog.LayoutChoseListener;
import com.renard.ocr.LayoutQuestionDialog.LayoutKind;
import com.renard.ocr.SdWarningDialogFragment.SdReadyListener;
import com.renard.ocr.cropimage.MonitoredActivity;
import com.renard.util.Util;

/**
 * this activity is shown during the ocr process
 * 
 * @author renard
 * 
 */
public class OCRActivity extends MonitoredActivity implements SdReadyListener {

	@SuppressWarnings("unused")
	private static final String TAG = OCRActivity.class.getSimpleName();

	public static final String EXTRA_PARENT_DOCUMENT_ID = "parent_id";
	private static final String OCR_LANGUAGE = "ocr_language";

	private Button mButtonStartOCR;
	private OCRImageView mImageView;
	private int mOriginalHeight = 0;
	private int mOriginalWidth = 0;
	private Pix mFinalPix;
	private String mOcrLanguage; // is set by dialog in
									// askUserAboutDocumentLayout
	private OCR mOCR;
	private Messenger mMessageReceiver = new Messenger(new ProgressActivityHandler()); // receives
																						// messages
																						// from
																						// background
																						// task
	private int mParentId = -1; // if >=0 its the id of the parentdocument to
								// which the current page shall be added

	/**
	 * receives progress status messages from the background ocr task and
	 * displays them in the current activity
	 * 
	 * @author renard
	 * 
	 */
	private class ProgressActivityHandler extends Handler {

		private String hocrString;
		private String utf8String;
		private int layoutPix;
		private int mPreviewWith;
		private int mPreviewHeight;

		public void handleMessage(Message msg) {
			switch (msg.what) {

			case OCR.MESSAGE_EXPLANATION_TEXT: {
				// CharSequence text = getText(msg.arg1);
				getSupportActionBar().setTitle(msg.arg1);
				// mFairyText.setText(text);
				break;
			}
			case OCR.MESSAGE_TESSERACT_PROGRESS: {
				int percent = msg.arg1;
				Bundle data = msg.getData();
				mImageView.setProgress(percent, (RectF) data.getParcelable(OCR.EXTRA_WORD_BOX), (RectF) data.getParcelable(OCR.EXTRA_OCR_BOX));
				break;
			}
			case OCR.MESSAGE_PREVIEW_IMAGE: {
				int nativePix = msg.arg1;

				if (nativePix > 0) {
					Pix pix = new Pix(nativePix);
					final Bitmap preview = WriteFile.writeBitmap(pix);
					pix.recycle();

					mImageView.setImageBitmapResetBase(preview, true, 0);
				}
				// mTextView.setText(msg.arg2);
				break;
			}
			case OCR.MESSAGE_FINAL_IMAGE: {
				int nativePix = msg.arg1;

				if (nativePix > 0) {
					mFinalPix = new Pix(nativePix);
				}
				break;
			}
			case OCR.MESSAGE_LAYOUT_PIX: {
				layoutPix = msg.arg1;
				if (layoutPix > 0) {
					Pix pix = new Pix(layoutPix);
					final Bitmap preview = WriteFile.writeBitmap(pix);
					mPreviewHeight = pix.getHeight();
					mPreviewWith = pix.getWidth();
					mImageView.setImageBitmapResetBase(preview, true, 0);
					pix.recycle();
				}
				break;
			}

			case OCR.MESSAGE_LAYOUT_ELEMENTS: {
				int nativePixaText = msg.arg1;
				int nativePixaImages = msg.arg2;
				final Pixa texts = new Pixa(nativePixaText, 0, 0);
				final Pixa images = new Pixa(nativePixaImages, 0, 0);
				ArrayList<Rect> boxes = images.getBoxRects();
				ArrayList<RectF> scaledBoxes = new ArrayList<RectF>(boxes.size());
				float xScale = (1.0f * mPreviewWith) / mOriginalWidth;
				float yScale = (1.0f * mPreviewHeight) / mOriginalHeight;
				// scale the to the preview image space
				for (Rect r : boxes) {
					scaledBoxes.add(new RectF(r.left * xScale, r.top * yScale, r.right * xScale, r.bottom * yScale));
				}
				mImageView.setImageRects(scaledBoxes);
				boxes = texts.getBoxRects();
				scaledBoxes = new ArrayList<RectF>(boxes.size());
				for (Rect r : boxes) {
					scaledBoxes.add(new RectF(r.left * xScale, r.top * yScale, r.right * xScale, r.bottom * yScale));
				}
				mImageView.setTextRects(scaledBoxes);

				mButtonStartOCR.setVisibility(View.VISIBLE);
				mButtonStartOCR.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {

						int[] selectedTexts = mImageView.getSelectedTextIndexes();
						int[] selectedImages = mImageView.getSelectedImageIndexes();
						mImageView.clearAllProgressInfo();

						mOCR.startOCRForComplexLayout(OCRActivity.this, mOcrLanguage, texts, images, selectedTexts, selectedImages);
						mButtonStartOCR.setVisibility(View.GONE);

					}
				});

				// CharSequence text =
				// getText(R.string.progress_choose_columns);
				// mFairyText.setText(text);
				getSupportActionBar().setTitle(R.string.progress_choose_columns);

				break;
			}
			case OCR.MESSAGE_HOCR_TEXT: {
				this.hocrString = (String) msg.obj;
				break;
			}
			case OCR.MESSAGE_UTF8_TEXT: {
				this.utf8String = (String) msg.obj;
				break;
			}
			case OCR.MESSAGE_END: {
				saveDocument(mFinalPix, hocrString, utf8String, true);
				break;
			}
			case OCR.MESSAGE_ERROR: {
				Toast.makeText(getApplicationContext(), getText(msg.arg1), Toast.LENGTH_LONG).show();
				break;
			}
			}
		}
	}

	private void saveDocument(final Pix pix, final String hocrString, final String utf8String, final boolean checkSd) {
//		if (checkSd && !Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//			waitForSdCard(pix, hocrString, utf8String);
//		} else {

			Util.startBackgroundJob(OCRActivity.this, "", getText(R.string.saving_document).toString(), new Runnable() {

				@Override
				public void run() {
					File imageFile = null;
					Uri documentUri = null;
					try {
						if (checkSd) {
							imageFile = saveImage(pix);
						}
						documentUri = saveDocumentToDB(imageFile, hocrString, utf8String);
						Util.createThumbnail(OCRActivity.this, imageFile, Integer.valueOf(documentUri.getLastPathSegment()));
					} catch (RemoteException e) {
						e.printStackTrace();
						Log.e(TAG,e.getMessage());
						e.printStackTrace();
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								Toast.makeText(getApplicationContext(), getText(R.string.error_create_file), Toast.LENGTH_LONG).show();
							}
						});
					} catch (IOException e) {
						e.printStackTrace();
						Log.e(TAG,e.getMessage());
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								Toast.makeText(getApplicationContext(), getText(R.string.error_create_file), Toast.LENGTH_LONG).show();
							}
						});
					} finally {
						if (pix != null) {
							pix.recycle();
						}
						if (documentUri != null) {
							Intent i = new Intent(OCRActivity.this, DocumentActivity.class);
							i.putExtra(DocumentActivity.EXTRA_ASK_FOR_TITLE, true);
							i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							i.setData(documentUri);
							startActivity(i);
							finish();
						}
					}
				}
			}, new Handler());
	//	}

	}
	
	@Override
	public void onSdReady() {
		//saveDocument(pix, hocrString, utf8String, true);
	}

	@Override
	public void onSdNotReady() {
		//saveDocument(pix, hocrString, utf8String, false);
	}


	private void waitForSdCard(final Pix pix, final String hocrString, final String utf8String) {

		SdWarningDialogFragment warningDialog = new SdWarningDialogFragment();
		FragmentManager fm = getSupportFragmentManager();
		warningDialog.show(fm, OCRActivity.class.getSimpleName());
	}

	private File saveImage(Pix p) throws IOException {
		CharSequence id = DateFormat.format("ssmmhhddMMyy", new Date(System.currentTimeMillis()));
		return Util.savePixToSD(p, id.toString());
	}

	private Uri saveDocumentToDB(File imageFile, String hocr, String plainText) throws RemoteException {
		ContentProviderClient client = null;
		try {
			ContentValues v = null;
			if (imageFile != null) {
				v = new ContentValues();
				v.put(com.renard.ocr.DocumentContentProvider.Columns.PHOTO_PATH, imageFile.getPath());
			}
			if (hocr != null) {
				v.put(Columns.HOCR_TEXT, hocr);
			}
			if (plainText != null) {
				v.put(Columns.OCR_TEXT, plainText);
			}

			if (mParentId > -1) {
				v.put(Columns.PARENT_ID, mParentId);
			}
			client = getContentResolver().acquireContentProviderClient(DocumentContentProvider.CONTENT_URI);
			return client.insert(DocumentContentProvider.CONTENT_URI, v);
		} finally {
			if (client != null) {
				client.release();
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayShowTitleEnabled(false);

		mOCR = new OCR(this, mMessageReceiver);

		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		setContentView(R.layout.ocr_visualize);
		mImageView = (OCRImageView) findViewById(R.id.progress_image);

		mParentId = getIntent().getIntExtra(EXTRA_PARENT_DOCUMENT_ID, -1);
		int nativePix = getIntent().getExtras().getInt(DocumentGridActivity.EXTRA_NATIVE_PIX);

		Pix pixOrg = new Pix(nativePix);
		mOriginalHeight = pixOrg.getHeight();
		mOriginalWidth = pixOrg.getWidth();

		askUserAboutDocumentLayout(pixOrg);

		mButtonStartOCR = (Button) findViewById(R.id.column_pick_completed);
		initAppIcon(this, -1);

	}

	private void askUserAboutDocumentLayout(final Pix pixOrg) {
		AlertDialog alertDialog = LayoutQuestionDialog.createDialog(this, new LayoutChoseListener() {

			@Override
			public void onLayoutChosen(final LayoutKind layoutKind, final String ocrLanguage) {
				if (layoutKind == LayoutKind.DO_NOTHING) {
					saveDocument(pixOrg, null, null, false);
				} else {
					getSupportActionBar().show();
					getSupportActionBar().setDisplayShowTitleEnabled(true);
					// mFairyText.setText(R.string.progress_start);
					getSupportActionBar().setTitle(R.string.progress_start);
					if (layoutKind == LayoutKind.SIMPLE) {
						mOCR.startOCRForSimpleLayout(OCRActivity.this, ocrLanguage, pixOrg);
					} else if (layoutKind == LayoutKind.COMPLEX) {
						mOcrLanguage = ocrLanguage;
						mOCR.startLayoutAnalysis(pixOrg);
					}
				}
			}
		});
		alertDialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				finish();
			}
		});
		alertDialog.show();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mOcrLanguage != null) {
			outState.putString(OCR_LANGUAGE, mOcrLanguage);
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (mOcrLanguage == null) {
			mOcrLanguage = savedInstanceState.getString(OCR_LANGUAGE);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		if (mFinalPix != null) {
			mFinalPix.recycle();
			mFinalPix = null;
		}

		BitmapDrawable bd = (BitmapDrawable) mImageView.getDrawable();
		if (bd != null) {
			bd.getBitmap().recycle();
		}
		super.onDestroy();
	}
}
