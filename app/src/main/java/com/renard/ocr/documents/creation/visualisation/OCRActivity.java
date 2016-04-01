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
package com.renard.ocr.documents.creation.visualisation;

import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.Pixa;
import com.googlecode.tesseract.android.OCR;
import com.renard.ocr.MonitoredActivity;
import com.renard.ocr.PermissionGrantedEvent;
import com.renard.ocr.R;
import com.renard.ocr.documents.creation.visualisation.LayoutQuestionDialog.LayoutChoseListener;
import com.renard.ocr.documents.creation.visualisation.LayoutQuestionDialog.LayoutKind;
import com.renard.ocr.documents.viewing.DocumentContentProvider;
import com.renard.ocr.documents.viewing.DocumentContentProvider.Columns;
import com.renard.ocr.documents.viewing.grid.DocumentGridActivity;
import com.renard.ocr.documents.viewing.single.DocumentActivity;
import com.renard.ocr.util.Screen;
import com.renard.ocr.util.Util;

import android.Manifest;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

/**
 * this activity is shown during the ocr process
 *
 * @author renard
 */
public class OCRActivity extends MonitoredActivity implements LayoutChoseListener {

    @SuppressWarnings("unused")
    private static final String TAG = OCRActivity.class.getSimpleName();

    public static final String EXTRA_PARENT_DOCUMENT_ID = "parent_id";
    private static final String OCR_LANGUAGE = "ocr_language";
    public static final String EXTRA_USE_ACCESSIBILITY_MODE = "ACCESSIBILTY_MODE";


    @Bind(R.id.column_pick_completed)
    protected Button mButtonStartOCR;
    @Bind(R.id.progress_image)
    protected OCRImageView mImageView;

    private int mOriginalHeight = 0;
    private int mOriginalWidth = 0;
    private Pix mFinalPix;
    private String mOcrLanguage; // is set by dialog in
    private int mAccuracy;

    private OCR mOCR;
    // receives messages from background task
    private Messenger mMessageReceiver = new Messenger(new ProgressActivityHandler());
    // if >=0 its the id of the parent document to which the current page shall be added
    private int mParentId = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        long nativePix = getIntent().getLongExtra(DocumentGridActivity.EXTRA_NATIVE_PIX, -1);
        mParentId = getIntent().getIntExtra(EXTRA_PARENT_DOCUMENT_ID, -1);
        if (nativePix == -1) {
            Intent intent = new Intent(this, DocumentGridActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        mOCR = new OCR(this, mMessageReceiver);
        Screen.lockOrientation(this);
        setContentView(R.layout.activity_ocr);
        ButterKnife.bind(this);
        initToolbar();
        ensurePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, R.string.permission_explanation);

    }


    @SuppressWarnings("unused")
    public void onEventMainThread(final PermissionGrantedEvent event) {
        long nativePix = getIntent().getLongExtra(DocumentGridActivity.EXTRA_NATIVE_PIX, -1);
        final Pix pixOrg = new Pix(nativePix);
        mOriginalHeight = pixOrg.getHeight();
        mOriginalWidth = pixOrg.getWidth();
        askUserAboutDocumentLayout();
    }

    @Override
    public void onLayoutChosen(LayoutKind layoutKind, String ocrLanguage) {
        long nativePix = getIntent().getLongExtra(DocumentGridActivity.EXTRA_NATIVE_PIX, -1);
        final Pix pixOrg = new Pix(nativePix);
        if (layoutKind == LayoutKind.DO_NOTHING) {
            saveDocument(pixOrg, null, null, 0);
        } else {
            mOcrLanguage = ocrLanguage;

            setToolbarMessage(R.string.progress_start);

            if (layoutKind == LayoutKind.SIMPLE) {
                mOCR.startOCRForSimpleLayout(OCRActivity.this, ocrLanguage, pixOrg, mImageView.getWidth(), mImageView.getHeight());
            } else if (layoutKind == LayoutKind.COMPLEX) {
                mAccuracy = 0;
                mOCR.startLayoutAnalysis(OCRActivity.this, pixOrg, mImageView.getWidth(), mImageView.getHeight());
            }
        }

    }

    /**
     * receives progress status messages from the background ocr task and
     * displays them in the current activity
     *
     * @author renard
     */
    private class ProgressActivityHandler extends Handler {

        private String hocrString;
        private String utf8String;
        //private long layoutPix;
        private int mPreviewWith;
        private int mPreviewHeight;

        private boolean mHasStartedOcr = false;

        public void handleMessage(Message msg) {
            switch (msg.what) {

                case OCR.MESSAGE_EXPLANATION_TEXT: {
                    setToolbarMessage(msg.arg1);
                    break;
                }
                case OCR.MESSAGE_TESSERACT_PROGRESS: {
                    if (!mHasStartedOcr) {
                        mAnalytics.sendScreenView("Ocr");
                        mHasStartedOcr = true;
                    }
                    int percent = msg.arg1;
                    Bundle data = msg.getData();
                    mImageView.setProgress(percent,
                            (RectF) data.getParcelable(OCR.EXTRA_WORD_BOX),
                            (RectF) data.getParcelable(OCR.EXTRA_OCR_BOX));
                    break;
                }
                case OCR.MESSAGE_PREVIEW_IMAGE: {
                    mImageView.setImageBitmapResetBase((Bitmap) msg.obj, true, 0);
                    break;
                }
                case OCR.MESSAGE_FINAL_IMAGE: {
                    long nativePix = (long) msg.obj;

                    if (nativePix != 0) {
                        mFinalPix = new Pix(nativePix);
                    }
                    break;
                }
                case OCR.MESSAGE_LAYOUT_PIX: {
                    Bitmap layoutPix = (Bitmap) msg.obj;
                    mPreviewHeight = layoutPix.getHeight();
                    mPreviewWith = layoutPix.getWidth();
                    mImageView.setImageBitmapResetBase(layoutPix, true, 0);
                    break;
                }

                case OCR.MESSAGE_LAYOUT_ELEMENTS: {
                    int nativePixaText = msg.arg1;
                    int nativePixaImages = msg.arg2;
                    final Pixa texts = new Pixa(nativePixaText, 0, 0);
                    final Pixa images = new Pixa(nativePixaImages, 0, 0);
                    ArrayList<Rect> boxes = images.getBoxRects();
                    ArrayList<RectF> scaledBoxes = new ArrayList<>(boxes.size());
                    float xScale = (1.0f * mPreviewWith) / mOriginalWidth;
                    float yScale = (1.0f * mPreviewHeight) / mOriginalHeight;
                    // scale the to the preview image space
                    for (Rect r : boxes) {
                        scaledBoxes.add(new RectF(r.left * xScale, r.top * yScale,
                                r.right * xScale, r.bottom * yScale));
                    }
                    mImageView.setImageRects(scaledBoxes);
                    boxes = texts.getBoxRects();
                    scaledBoxes = new ArrayList<>(boxes.size());
                    for (Rect r : boxes) {
                        scaledBoxes.add(new RectF(r.left * xScale, r.top * yScale,
                                r.right * xScale, r.bottom * yScale));
                    }
                    mImageView.setTextRects(scaledBoxes);

                    mButtonStartOCR.setVisibility(View.VISIBLE);
                    mButtonStartOCR.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {

                            int[] selectedTexts = mImageView
                                    .getSelectedTextIndexes();
                            int[] selectedImages = mImageView
                                    .getSelectedImageIndexes();
                            if (selectedTexts.length > 0
                                    || selectedImages.length > 0) {
                                mImageView.clearAllProgressInfo();


                                mOCR.startOCRForComplexLayout(OCRActivity.this,
                                        mOcrLanguage, texts,
                                        images, selectedTexts, selectedImages);
                                mButtonStartOCR.setVisibility(View.GONE);
                            } else {
                                Toast.makeText(getApplicationContext(),
                                        R.string.please_tap_on_column,
                                        Toast.LENGTH_LONG).show();
                            }

                        }
                    });
                    mAnalytics.sendScreenView("Pick Columns");

                    setToolbarMessage(R.string.progress_choose_columns);

                    break;
                }
                case OCR.MESSAGE_HOCR_TEXT: {
                    this.hocrString = (String) msg.obj;
                    mAccuracy = msg.arg1;
                    break;
                }
                case OCR.MESSAGE_UTF8_TEXT: {
                    this.utf8String = (String) msg.obj;
                    break;
                }
                case OCR.MESSAGE_END: {
                    saveDocument(mFinalPix, hocrString, utf8String, mAccuracy);
                    break;
                }
                case OCR.MESSAGE_ERROR: {
                    Toast.makeText(getApplicationContext(), getText(msg.arg1), Toast.LENGTH_LONG).show();
                    break;
                }
            }
        }

    }

    private void saveDocument(final Pix pix, final String hocrString, final String utf8String, final int accuracy) {

        Util.startBackgroundJob(OCRActivity.this, "",
                getText(R.string.saving_document).toString(), new Runnable() {

                    @Override
                    public void run() {
                        File imageFile = null;
                        Uri documentUri = null;

                        try {
                            imageFile = saveImage(pix);
                        } catch (IOException e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    Toast.makeText(
                                            getApplicationContext(),
                                            getText(R.string.error_create_file),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                        }

                        try {

                            documentUri = saveDocumentToDB(imageFile, hocrString, utf8String);
                            if (imageFile != null) {
                                Util.createThumbnail(OCRActivity.this, imageFile, Integer.valueOf(documentUri.getLastPathSegment()));
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();

                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    Toast.makeText(
                                            getApplicationContext(),
                                            getText(R.string.error_create_file),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                        } finally {
                            recycleResultPix(pix);
                            if (documentUri != null && !isFinishing()) {
                                Intent i;
                                i = new Intent(OCRActivity.this, DocumentActivity.class);
                                i.putExtra(DocumentActivity.EXTRA_ACCURACY, accuracy);
                                i.putExtra(DocumentActivity.EXTRA_LANGUAGE, mOcrLanguage);
                                i.setData(documentUri);
                                i.putExtra(DocumentGridActivity.EXTRA_NATIVE_PIX, pix.getNativePix());
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(i);
                                finish();
                                Screen.unlockOrientation(OCRActivity.this);
                            }
                        }
                    }
                }, new Handler());

    }

    private void recycleResultPix(Pix pix) {
        if (pix != null) {
            pix.recycle();
        }
    }


    private File saveImage(Pix p) throws IOException {
        CharSequence id = DateFormat.format("ssmmhhddMMyy", new Date(System.currentTimeMillis()));
        return Util.savePixToSD(p, id.toString());
    }

    private Uri saveDocumentToDB(File imageFile, String hocr, String plainText)
            throws RemoteException {
        ContentProviderClient client = null;
        try {
            ContentValues v = new ContentValues();
            if (imageFile != null) {
                v.put(DocumentContentProvider.Columns.PHOTO_PATH,
                        imageFile.getPath());
            }
            if (hocr != null) {
                v.put(Columns.HOCR_TEXT, hocr);
            }
            if (plainText != null) {
                v.put(Columns.OCR_TEXT, plainText);
            }
            v.put(Columns.OCR_LANG, mOcrLanguage);

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
    protected int getHintDialogId() {
        return -1;
    }

    private void askUserAboutDocumentLayout() {
        LayoutQuestionDialog dialog = LayoutQuestionDialog.newInstance();
        dialog.show(getSupportFragmentManager(), LayoutQuestionDialog.TAG);
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
    public String getScreenName() {
        return "";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        if (mFinalPix != null) {
            mFinalPix.recycle();
            mFinalPix = null;
        }
        mImageView.clear();

    }
}
