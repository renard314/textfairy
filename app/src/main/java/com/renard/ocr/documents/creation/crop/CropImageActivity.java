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

package com.renard.ocr.documents.creation.crop;

import com.googlecode.leptonica.android.Box;
import com.googlecode.leptonica.android.Clip;
import com.googlecode.leptonica.android.Convert;
import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.Projective;
import com.googlecode.leptonica.android.Rotate;
import com.googlecode.tesseract.android.OCR;
import com.renard.ocr.HintDialog;
import com.renard.ocr.MonitoredActivity;
import com.renard.ocr.R;
import com.renard.ocr.documents.viewing.grid.DocumentGridActivity;
import com.renard.ocr.util.PreferencesUtils;
import com.renard.ocr.util.Util;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import javax.annotation.Nullable;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;

/**
 * The activity can crop specific region of interest from an image.
 */
public class CropImageActivity extends MonitoredActivity implements BlurWarningDialog.BlurDialogClickListener {
    public static final int RESULT_NEW_IMAGE = RESULT_FIRST_USER + 1;
    private static final int HINT_DIALOG_ID = 2;
    public static final String SCREEN_NAME = "Crop Image";
    private final Handler mHandler = new Handler();

    private int mRotation = 0;
    boolean mSaving;
    private Pix mPix;
    @BindView(R.id.toolbar)
    protected Toolbar mToolbar;
    @BindView(R.id.cropImageView)
    protected CropImageView mImageView;
    @BindView(R.id.crop_layout)
    protected ViewSwitcher mViewSwitcher;
    @BindView(R.id.item_rotate_left)
    protected ImageView mRotateLeft;
    @BindView(R.id.item_rotate_right)
    protected ImageView mRotateRight;
    @BindView(R.id.item_save)
    protected ImageView mSave;


    private CropHighlightView mCrop;
    @Nullable
    private CropData mCropData = null;
    @Nullable
    private PreparePixForCropTask mPrepareTask = null;

    @Override
    public String getScreenName() {
        return "";
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        EventBus.getDefault().register(this);
        setContentView(R.layout.activity_cropimage);
        ButterKnife.bind(this);
        initToolbar();
        setToolbarMessage(R.string.crop_title);
        initNavigationAsUp();
        startCropping();
    }

    private void showCropOnBoarding(final CropData cropData) {
        PreferencesUtils.setFirstScan(this, false);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.crop_onboarding_title);
        builder.setMessage(R.string.crop_onboarding_message);
        builder.setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                handleBlurResult(cropData);
            }
        });
        builder.show();
    }

    private boolean isFirstStart() {
        return PreferencesUtils.isFirstScan(this);
    }


    @OnClick(R.id.item_rotate_left)
    public void onRotateLeft() {
        onRotateClicked(-1);
    }

    @OnClick(R.id.item_rotate_right)
    public void onRotateRight() {
        onRotateClicked(1);
    }

    private void onRotateClicked(int delta) {
        if (mCropData != null) {
            if (delta < 0) {
                delta = -delta * 3;
            }
            mRotation += delta;
            mRotation = mRotation % 4;
            mImageView.setImageBitmapResetBase(mCropData.getBitmap(), false, mRotation * 90);
            showDefaultCroppingRectangle(mCropData.getBitmap());
        }
    }

    private void startCropping() {
        mImageView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                Bundle extras = getIntent().getExtras();
                final long nativePix = extras.getLong(DocumentGridActivity.EXTRA_NATIVE_PIX);
                final float margin = getResources().getDimension(R.dimen.crop_margin);
                final int width = (int) (mViewSwitcher.getWidth() - 2 * margin);
                final int height = (int) (mViewSwitcher.getHeight() - 2 * margin);
                mPix = new Pix(nativePix);
                mRotation = 0;
                mPrepareTask = new PreparePixForCropTask(mPix, width, height);
                mPrepareTask.execute();
                mImageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }

        });
    }


    private void initNavigationAsUp() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected int getHintDialogId() {
        return HINT_DIALOG_ID;
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(final CropData cropData) {
        if (cropData.getBitmap() == null) {
            //should not happen. Scaling of the original document failed some how. Maybe out of memory?
            mAnalytics.sendCropError();
            Toast.makeText(this, R.string.could_not_load_image, Toast.LENGTH_LONG).show();
            onNewImageClicked();
            return;
        }
        mAnalytics.sendBlurResult(cropData.getBlurriness());

        mCropData = cropData;
        adjustOptionsMenu();
        mViewSwitcher.setDisplayedChild(1);

        mImageView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                mImageView.setImageBitmapResetBase(cropData.getBitmap(), true, mRotation * 90);

                if (isFirstStart()) {
                    showCropOnBoarding(cropData);
                } else {
                    handleBlurResult(cropData);
                }


                mImageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }

        });

    }

    private void handleBlurResult(CropData cropData) {
        switch (cropData.getBlurriness().getBlurriness()) {
            case NOT_BLURRED:
                mAnalytics.sendScreenView(SCREEN_NAME);
                showDefaultCroppingRectangle(cropData.getBitmap());
                break;
            case MEDIUM_BLUR:
            case STRONG_BLUR:
                setTitle(R.string.image_is_blurred);
                BlurWarningDialog dialog = BlurWarningDialog.newInstance((float) cropData.getBlurriness().getBlurValue());
                final FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.add(dialog, BlurWarningDialog.TAG).commitAllowingStateLoss();
                break;
        }
    }


    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case HINT_DIALOG_ID:
                return HintDialog.createDialog(this, R.string.crop_help_title, R.raw.crop_help);
        }
        return super.onCreateDialog(id, args);
    }


    private void adjustOptionsMenu() {
        if (mCropData != null) {
            mRotateLeft.setVisibility(View.VISIBLE);
            mRotateRight.setVisibility(View.VISIBLE);
            mSave.setVisibility(View.VISIBLE);
        } else {
            mRotateLeft.setVisibility(View.GONE);
            mRotateRight.setVisibility(View.GONE);
            mSave.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.item_save)
    void onSaveClicked() {
        if (mCropData == null || mSaving || (mCrop == null)) {
            return;
        }
        mSaving = true;

        Util.startBackgroundJob(this, null, getText(R.string.cropping_image).toString(), new Runnable() {
            public void run() {
                try {
                    float scale = 1f / mCropData.getScaleResult().getScaleFactor();
                    Matrix scaleMatrix = new Matrix();
                    scaleMatrix.setScale(scale, scale);

                    final float[] trapezoid = mCrop.getTrapezoid();
                    final RectF perspectiveCorrectedBoundingRect = new RectF(mCrop.getPerspectiveCorrectedBoundingRect());
                    scaleMatrix.mapRect(perspectiveCorrectedBoundingRect);
                    Box bb = new Box((int) perspectiveCorrectedBoundingRect.left, (int) perspectiveCorrectedBoundingRect.top, (int) perspectiveCorrectedBoundingRect.width(), (int) perspectiveCorrectedBoundingRect.height());

                    Pix pix8 = Convert.convertTo8(mPix);
                    mPix.recycle();

                    Pix croppedPix = Clip.clipRectangle2(pix8, bb);
                    if (croppedPix == null) {
                        throw new IllegalStateException();
                    }
                    pix8.recycle();

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
                    OCR.Companion.savePixToCacheDir(CropImageActivity.this, bilinear.copy());
                    result.putExtra(DocumentGridActivity.EXTRA_NATIVE_PIX, bilinear.getNativePix());
                    setResult(RESULT_OK, result);
                } catch (IllegalStateException e) {
                    setResult(RESULT_CANCELED);
                } finally {
                    finish();
                }
            }
        }, mHandler);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_CANCELED);
        mPix.recycle();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        unbindDrawables(findViewById(android.R.id.content));
        mImageView.clear();
        if (mPrepareTask != null) {
            mPrepareTask.cancel(true);
            mPrepareTask = null;
        }
        if (mCropData != null) {
            mCropData.recylce();
            mCropData = null;
        }

    }

    private void unbindDrawables(View view) {
        if (view.getBackground() != null) {
            view.getBackground().setCallback(null);
        }
        if (view instanceof ViewGroup && !(view instanceof AdapterView)) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                unbindDrawables(((ViewGroup) view).getChildAt(i));
            }
            ((ViewGroup) view).removeAllViews();
        }
    }

    private void zoomToBlurredRegion(CropData data) {
        float width = data.getBlurriness().getPixBlur().getWidth();
        float height = data.getBlurriness().getPixBlur().getHeight();
        float widthScale = width / data.getBitmap().getWidth();
        float heightScale = height / data.getBitmap().getHeight();
        final Point c = data.getBlurriness().getMostBlurredRegion().getCenter();
        c.set((int) (c.x / widthScale), (int) (c.y / heightScale));
        float[] pts = {c.x, c.y};
        mImageView.getImageMatrix().mapPoints(pts);
        /*
        int w = (Math.min(mBitmap.getWidth(), mBitmap.getHeight())) / 25;

        Rect focusArea = new Rect((int) (Math.max(c.x-w,0)*widthScale), (int) (Math.max(c.y-w,0)*heightScale), (int) (Math.min(c.x+w,mBitmap.getWidth())*widthScale), (int) (Math.min(c.y+w,mBitmap.getHeight())*heightScale));

        //final int progressColor = getResources().getColor(R.color.progress_color);
        //final int edgeWidth = getResources().getDimensionPixelSize(R.dimen.crop_edge_width);
        Clip.clipRectangle2();

        BlurHighLightView hv = new BlurHighLightView(focusArea,progressColor,edgeWidth, mImageView.getImageMatrix());
        mImageView.add(hv);
        */
        mImageView.setMaxZoom(3);
        mImageView.zoomTo(3, pts[0], pts[1], 2000);
    }


    private void showDefaultCroppingRectangle(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Rect imageRect = new Rect(0, 0, width, height);

        // make the default size about 4/5 of the width or height
        int cropWidth = Math.min(width, height) * 4 / 5;


        int x = (width - cropWidth) / 2;
        int y = (height - cropWidth) / 2;

        RectF cropRect = new RectF(x, y, x + cropWidth, y + cropWidth);

        CropHighlightView hv = new CropHighlightView(mImageView, imageRect, cropRect);

        mImageView.resetMaxZoom();
        mImageView.add(hv);
        mCrop = hv;
        mCrop.setFocus(true);
        mImageView.invalidate();
    }

    @Override
    public void onContinueClicked() {
        if (mCropData != null) {
            mAnalytics.sendScreenView(SCREEN_NAME);
            showDefaultCroppingRectangle(mCropData.getBitmap());
            setToolbarMessage(R.string.crop_title);
            mImageView.zoomTo(1, 500);
        }
    }

    @Override
    public void onNewImageClicked() {
        setResult(RESULT_NEW_IMAGE);
        mPix.recycle();
        finish();
    }
}

