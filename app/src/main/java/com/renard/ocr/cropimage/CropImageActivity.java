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

package com.renard.ocr.cropimage;

import com.googlecode.leptonica.android.Box;
import com.googlecode.leptonica.android.Clip;
import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.Projective;
import com.googlecode.leptonica.android.Rotate;
import com.googlecode.leptonica.android.WriteFile;
import com.renard.image_processing.Blur;
import com.renard.image_processing.BlurDetectionResult;
import com.renard.ocr.DocumentGridActivity;
import com.renard.ocr.R;
import com.renard.ocr.help.HintDialog;
import com.renard.util.Util;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

/**
 * The activity can crop specific region of interest from an image.
 */
public class CropImageActivity extends MonitoredActivity implements ImageBlurredDialog.BlurDialogClickListener{
    public static final int RESULT_NEW_IMAGE = RESULT_FIRST_USER + 1;
    private static final int HINT_DIALOG_ID = 2;

    private final Handler mHandler = new Handler();

    private int mRotation = 0;

    boolean mSaving; // Whether the "save" button is already clicked.
    private Pix mPix; // original Picture
    private CropImageView mImageView;
    private CropImageScaler.ScaleResult mScaleResult;

    private Bitmap mBitmap;
    private CropHighlightView mCrop;
    private CheckImageAsyncTask mCheckImageAsyncTask;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getWindow().setFormat(PixelFormat.RGBA_8888);
        setContentView(R.layout.activity_cropimage);

        mImageView = (CropImageView) findViewById(R.id.image);
        mImageView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                //TODO not run on UI Thread
                Intent intent = getIntent();
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    mPix = new Pix(extras.getLong(DocumentGridActivity.EXTRA_NATIVE_PIX));
                    mRotation = extras.getInt(DocumentGridActivity.EXTRA_ROTATION) / 90;

                    mCheckImageAsyncTask = new CheckImageAsyncTask(mPix);
                    mCheckImageAsyncTask.execute();

                    final BlurDetectionResult blurDetectionResult = Blur.blurDetect(mPix);
                    CropImageScaler scaler = new CropImageScaler();
                    switch (blurDetectionResult.getBlurriness()) {

                        case NOT_BLURRED:
                            // scale it so that it fits the screen
                            mScaleResult = scaler.scale(mPix, mImageView.getWidth(), mImageView.getHeight());
                            mBitmap = WriteFile.writeBitmap(mScaleResult.getPix());
                            mImageView.setImageBitmapResetBase(mBitmap, true, mRotation * 90);
                            showDefaultCroppingRectangle();
                            break;
                        case MEDIUM_BLUR:
                        case STRONG_BLUR:
                            mScaleResult = scaler.scale(mPix, mImageView.getWidth(), mImageView.getHeight());
                            //mScaleResult = scaler.scale(blurDetectionResult.getPixBlur(), mImageView.getWidth(), mImageView.getHeight());
                            mBitmap = WriteFile.writeBitmap(mScaleResult.getPix());
                            mImageView.setImageBitmapResetBase(mBitmap, true, mRotation * 90);
                            supportInvalidateOptionsMenu();
                            showBlurRectangle(blurDetectionResult);
                            setTitle(R.string.image_is_blurred);
                            ImageBlurredDialog dialog = ImageBlurredDialog.newInstance((float) blurDetectionResult.getBlurValue());
                            dialog.show(getSupportFragmentManager(), ImageBlurredDialog.TAG);
                            supportInvalidateOptionsMenu();
                            break;
                    }
                    mImageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);


                }

                if (mBitmap == null) {
                    finish();
                }

            }

        });

        initAppIcon(this, HINT_DIALOG_ID);
    }


    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case HINT_DIALOG_ID:
                return HintDialog.createDialog(this, R.string.crop_help_title, "file:///android_res/raw/crop_help.html");
        }
        return super.onCreateDialog(id, args);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.item_save) {
            onSaveClicked();
            return true;
        } else if (itemId == R.id.item_rotate_right) {
            onRotateClicked(1);
            return true;
        } else if (itemId == R.id.item_rotate_left) {
            onRotateClicked(-1);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.crop_image_options, menu);
        return true;
    }

    private void onRotateClicked(int delta) {
        if (delta < 0) {
            delta = -delta * 3;
        }
        mRotation += delta;
        mRotation = mRotation % 4;
        mImageView.setImageBitmapResetBase(mBitmap, false, mRotation * 90);
        showDefaultCroppingRectangle();
    }

    private void onSaveClicked() {
        if (mSaving)
            return;

        if (mCrop == null) {
            return;
        }

        mSaving = true;

        Util.startBackgroundJob(this, null, getText(R.string.cropping_image).toString(), new Runnable() {
            public void run() {
                try {
                    float scale = 1f / mScaleResult.getScaleFactor();
                    Matrix scaleMatrix = new Matrix();
                    scaleMatrix.setScale(scale, scale);

                    final float[] trapezoid = mCrop.getTrapezoid();
                    final RectF perspectiveCorrectedBoundingRect = new RectF(mCrop.getPerspectiveCorrectedBoundingRect());
                    scaleMatrix.mapRect(perspectiveCorrectedBoundingRect);
                    Box bb = new Box((int) perspectiveCorrectedBoundingRect.left, (int) perspectiveCorrectedBoundingRect.top, (int) perspectiveCorrectedBoundingRect.width(), (int) perspectiveCorrectedBoundingRect.height());
                    Pix croppedPix = Clip.clipRectangle2(mPix, bb);
                    if (croppedPix == null) {
                        throw new IllegalStateException();
                    }

                    scaleMatrix.postTranslate(-bb.getX(), -bb.getY());
                    scaleMatrix.mapPoints(trapezoid);

                    final float[] dest = new float[]{0, 0, bb.getWidth(), 0, bb.getWidth(), bb.getHeight(), 0, bb.getHeight()};
                    Pix bilinear = Projective.projectiveTransform(croppedPix, dest, trapezoid);
                    if (bilinear == null) {
                        bilinear = croppedPix;
                    } else {
                        croppedPix.recycle();
                    }


                    if (mRotation != 0 && mRotation != 4) {
                        Pix rotatedPix = Rotate.rotateOrth(bilinear, mRotation);
                        bilinear.recycle();
                        bilinear = rotatedPix;
                    }
                    if (bilinear == null) {
                        throw new IllegalStateException();
                    }
                    Intent result = new Intent();
                    result.putExtra(DocumentGridActivity.EXTRA_NATIVE_PIX, bilinear.getNativePix());
                    setResult(RESULT_OK, result);
                } catch (IllegalStateException e) {
                    setResult(RESULT_CANCELED);
                } finally {
                    mPix.recycle();
                    finish();
                }
            }
        }, mHandler);

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_CANCELED);
        mPix.recycle();
    }

    @Override
    protected void onDestroy() {
        mScaleResult.getPix().recycle();
        super.onDestroy();
    }

    private void showBlurRectangle(BlurDetectionResult blurDetectionResult) {
        float width = blurDetectionResult.getPixBlur().getWidth();
        float height = blurDetectionResult.getPixBlur().getHeight();
        float widthScale = width / mBitmap.getWidth();
        float heightScale = height / mBitmap.getHeight();
        final Point c = blurDetectionResult.getMostBlurredRegion().getCenter();
        c.set((int) (c.x / widthScale), (int) (c.y / heightScale));
        float[] pts = {c.x, c.y};
        mImageView.getImageMatrix().mapPoints(pts);
        //int w = (Math.min(mBitmap.getWidth(), mBitmap.getHeight())) / 25;
        //Rect focusArea = new Rect((int) (Math.max(c.x-w,0)*widthScale), (int) (Math.max(c.y-w,0)*heightScale), (int) (Math.min(c.x+w,mBitmap.getWidth())*widthScale), (int) (Math.min(c.y+w,mBitmap.getHeight())*heightScale));

        //final int progressColor = getResources().getColor(R.color.progress_color);
        //final int edgeWidth = getResources().getDimensionPixelSize(R.dimen.crop_edge_width);

        //BlurHighLightView hv = new BlurHighLightView(focusArea,progressColor,edgeWidth, mImageView.getImageMatrix());
        //mImageView.add(hv);
        mImageView.setMaxZoom(6);
        mImageView.zoomTo(6, pts[0], pts[1], 1000);
    }


    private void showDefaultCroppingRectangle() {

        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight();

        Rect imageRect = new Rect(0, 0, width, height);

        // make the default size about 4/5 of the width or height
        int cropWidth = Math.min(width, height) * 4 / 5;
        int cropHeight = cropWidth;


        int x = (width - cropWidth) / 2;
        int y = (height - cropHeight) / 2;

        RectF cropRect = new RectF(x, y, x + cropWidth, y + cropHeight);

        CropHighlightView hv = new CropHighlightView(mImageView, imageRect, cropRect);

        mImageView.resetMaxZoom();
        mImageView.add(hv);
        mImageView.invalidate();
        mCrop = hv;
        mCrop.setFocus(true);
    }

    @Override
    public void onContinueClicked() {
        showDefaultCroppingRectangle();
        setTitle(R.string.crop_title);
        mImageView.zoomTo(1,500);
    }

    @Override
    public void onNewImageClicked() {
        setResult(RESULT_NEW_IMAGE);
        mPix.recycle();
        finish();
    }
}

