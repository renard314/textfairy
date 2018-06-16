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
package com.renard.ocr.documents.creation.visualisation;

import com.renard.ocr.R;
import com.renard.ocr.documents.creation.crop.ImageViewTouchBase;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * is used to show preview images and progress during ocr process
 *
 * @author renard
 */
public class OCRImageView extends ImageViewTouchBase {

    private final static float TEXT_SIZE = 60f;
    private static final String LOG_TAG = OCRImageView.class.getSimpleName();

    private final Paint mNumberStrokePaint = new Paint();
    private final Paint mNumberPaint = new Paint();
    private final Paint mWordPaint = new Paint();
    private final Paint mBackgroundPaint = new Paint();
    private final Paint mScanLinePaint = new Paint();
    private final Paint mImageRectPaint = new Paint();
    private final Paint mTextRectPaint = new Paint();
    private final Paint mTouchedImageRectPaint = new Paint();
    private final Paint mTouchedTextRectPaint = new Paint();
    private List<RectF> mImageRects;
    private List<RectF> mTextRects;

    private ArrayList<RectF> mTouchedImageRects = new ArrayList<>();
    private ArrayList<RectF> mTouchedTextRects = new ArrayList<>();

    private int mProgress;
    private RectF mWordBoundingBox = new RectF();
    private RectF mOCRBoundingBox = new RectF();
    private RectF mViewDrawingRect = new RectF();

    public OCRImageView(final Context context) {
        super(context);
        init(context);
    }

    public void clearAllProgressInfo() {
        this.mTouchedImageRects.clear();
        this.mTouchedTextRects.clear();
        this.mImageRects.clear();
        this.mTextRects.clear();
    }

    public int[] getSelectedImageIndexes() {
        int[] result = new int[mTouchedImageRects.size()];
        for (int i = 0; i < mTouchedImageRects.size(); i++) {
            int j = mImageRects.indexOf(mTouchedImageRects.get(i));
            result[i] = j;
        }
        return result;
    }

    public int[] getSelectedTextIndexes() {
        int[] result = new int[mTouchedTextRects.size()];
        for (int i = 0; i < mTouchedTextRects.size(); i++) {
            int j = mTextRects.indexOf(mTouchedTextRects.get(i));
            result[i] = j;
        }
        return result;
    }

    public void setImageRects(List<RectF> boxes) {
        mImageRects = boxes;
        this.invalidate();
    }

    public void setTextRects(List<RectF> boxes) {
        mTextRects = boxes;
        this.invalidate();
    }

    public void setProgress(int newProgress, RectF wordBoundingBox, RectF ocrBoundingBox) {
        this.mProgress = newProgress;
        this.mWordBoundingBox.set(wordBoundingBox);
        this.mOCRBoundingBox.set(ocrBoundingBox);
        this.invalidate();
    }

    public OCRImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
        mTouchedTextRects = new ArrayList<>();
    }

    @Override
    public void onZoomFinished() {

    }

    private void init(final Context c) {
        final int progressColor = c.getResources().getColor(R.color.progress_color);
        mBackgroundPaint.setARGB(125, 50, 50, 50);
        mScanLinePaint.setColor(progressColor);
        mScanLinePaint.setStrokeWidth(3F);
        mScanLinePaint.setAntiAlias(true);
        mScanLinePaint.setStyle(Style.STROKE);
        mWordPaint.setARGB(125, Color.red(progressColor), Color.green(progressColor), Color.blue(progressColor));

        mImageRectPaint.setColor(progressColor);
        mImageRectPaint.setStrokeWidth(3F);
        mImageRectPaint.setAntiAlias(true);
        mImageRectPaint.setStyle(Style.STROKE);

        mTouchedImageRectPaint.setARGB(125, Color.red(progressColor), Color.green(progressColor), Color.blue(progressColor));

        mTouchedImageRectPaint.setStrokeWidth(3F);
        mTouchedImageRectPaint.setAntiAlias(true);
        mTouchedImageRectPaint.setStyle(Style.FILL);

        mTextRectPaint.setColor(0xFF002AFF);
        mTextRectPaint.setStrokeWidth(3F);
        mTextRectPaint.setAntiAlias(true);
        mTextRectPaint.setStyle(Style.STROKE);

        mTouchedTextRectPaint.setARGB(125, 0x00, 0x2A, 0xFF);
        mTouchedTextRectPaint.setStrokeWidth(3F);
        mTouchedTextRectPaint.setAntiAlias(true);
        mTouchedTextRectPaint.setStyle(Style.FILL);
        mNumberPaint.setARGB(0xff, 0x33, 0xb5, 0xe5);
        mNumberPaint.setTextAlign(Paint.Align.CENTER);
        mNumberPaint.setTextSize(TEXT_SIZE * getResources().getDisplayMetrics().density);
        Typeface tf = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
        mNumberPaint.setTypeface(tf);
        mNumberPaint.setAntiAlias(true);
        mNumberPaint.setMaskFilter(new BlurMaskFilter(3, Blur.SOLID));

        mNumberStrokePaint.setARGB(255, 0, 0, 0);
        mNumberStrokePaint.setTextAlign(Paint.Align.CENTER);
        mNumberStrokePaint.setTextSize(TEXT_SIZE * getResources().getDisplayMetrics().density);
        mNumberStrokePaint.setTypeface(tf);
        mNumberStrokePaint.setStyle(Paint.Style.STROKE);
        mNumberStrokePaint.setStrokeWidth(2);
        mNumberStrokePaint.setAntiAlias(true);

        // mNumberPaint.setMaskFilter(new EmbossMaskFilter(new float[] { 1, 1,
        // 1},0.8f, 10, 4f));
        mProgress = -1;
    }

    private void updateTouchedBoxesByPoint(float x, float y, List<RectF> boxes, List<RectF> touchedBoxes) {
        if (boxes != null) {
            for (RectF r : boxes) {
                if (r.contains(x, y)) {
                    if (touchedBoxes.contains(r)) {
                        touchedBoxes.remove(r);
                    } else {
                        touchedBoxes.add(r);
                    }
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                float[] pts = {event.getX(), event.getY()};
                Matrix inverse = new Matrix();
                getImageViewMatrix().invert(inverse);
                inverse.mapPoints(pts);
                updateTouchedBoxesByPoint(pts[0], pts[1], mImageRects, mTouchedImageRects);
                updateTouchedBoxesByPoint(pts[0], pts[1], mTextRects, mTouchedTextRects);
                this.invalidate();
                break;
            }
        }

        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getDrawable() == null) {
            return;
        }
        if (mProgress >= 0) {
            drawBoxAroundCurrentWord(canvas);
            /* draw progress rectangle */
            mViewDrawingRect.set(mOCRBoundingBox.left, mOCRBoundingBox.top, mOCRBoundingBox.right, mOCRBoundingBox.bottom);
            getImageViewMatrix().mapRect(mViewDrawingRect);
            canvas.drawRect(mViewDrawingRect, mScanLinePaint);
            float centerx = mViewDrawingRect.centerX();
            float centery = mViewDrawingRect.centerY();

            int pos = (int) (mViewDrawingRect.height() * (mProgress / 100f));
            mViewDrawingRect.top += pos;
            canvas.drawRect(mViewDrawingRect, mBackgroundPaint);
            canvas.drawLine(mViewDrawingRect.left, mViewDrawingRect.top, mViewDrawingRect.right, mViewDrawingRect.top, mScanLinePaint);

            canvas.drawText(String.valueOf(mProgress) + "%", centerx, centery, mNumberPaint);
            canvas.drawText(String.valueOf(mProgress) + "%", centerx, centery, mNumberStrokePaint);
        }
        /* draw boxes around text/images */
        drawRects(canvas, mImageRects, mImageRectPaint);
        drawRects(canvas, mTextRects, mTextRectPaint);
        /* draw special boxes around text/images selected by the user */
        drawRects(canvas, mTouchedImageRects, mTouchedImageRectPaint);
        drawRectsWithIndex(canvas, mTouchedTextRects, mTouchedTextRectPaint);
    }

    private void drawBoxAroundCurrentWord(Canvas canvas) {
        if (!mWordBoundingBox.isEmpty()) {
            getImageViewMatrix().mapRect(mWordBoundingBox);
            canvas.drawRect(mWordBoundingBox, mWordPaint);
        }
    }

    private void drawRectsWithIndex(Canvas canvas, ArrayList<RectF> rects, Paint paint) {
        if (rects != null) {
            RectF mappedRect = new RectF();
            for (int i = 0; i < rects.size(); i++) {
                RectF r = rects.get(i);
                mappedRect.set(r);
                getImageViewMatrix().mapRect(mappedRect);
                canvas.drawRect(mappedRect, paint);
                canvas.drawText(String.valueOf(i + 1), mappedRect.centerX(), mappedRect.centerY(), mNumberPaint);
                canvas.drawText(String.valueOf(i + 1), mappedRect.centerX(), mappedRect.centerY(), mNumberStrokePaint);
            }
        }
    }

    private void drawRects(Canvas canvas, List<RectF> rects, Paint paint) {
        if (rects != null) {
            RectF mappedRect = new RectF();
            for (RectF r : rects) {
                mappedRect.set(r);
                getImageViewMatrix().mapRect(mappedRect);
                canvas.drawRect(mappedRect, paint);
            }
        }
    }

}
