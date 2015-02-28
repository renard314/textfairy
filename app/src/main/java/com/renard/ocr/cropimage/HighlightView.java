/*
 * Copyright (C) 2007 The Android Open Source Project
 * Copyright (C) 2012,2013 Renard Wellnitz
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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.renard.ocr.R;

// This class is used by CropImage to display a highlighted cropping trapezoid
// overlayed with the image. There are two coordinate spaces in use. One is
// image, another is screen. computeLayout() uses mMatrix to map from image
// space to screen space.
class HighlightView {

    Matrix mMatrix;

    enum ModifyMode {
        None, Move, Grow
    }

    @SuppressWarnings("unused")
    private static final String LOG_TAG = HighlightView.class.getSimpleName();
    private View mContext; // The View displaying the image.

    /* used during onDraw */
    private final Rect mViewDrawingRect = new Rect();
    private final Rect mLeftRect = new Rect();
    private final Rect mRightRect = new Rect();
    private final Rect mTopRect = new Rect();
    private final Rect mBottomRect = new Rect();

    private ModifyMode mMode = ModifyMode.None;
    private final CroppingTrapezoid mTrapzoid;
    boolean mIsFocused;
    boolean mHidden = false;
    private float mPixelDensity;

    Rect mDrawRect; // in screen space


    private Drawable mResizeDrawableWidth;
    private Drawable mResizeDrawableHeight;

    private final Paint mFocusPaint = new Paint();
    private final Paint mNoFocusPaint = new Paint();
    private final Paint mOutlinePaint = new Paint();

    public static final int GROW_NONE = 0;
    public static final int GROW_LEFT_EDGE = (1 << 1);
    public static final int GROW_RIGHT_EDGE = (1 << 2);
    public static final int GROW_TOP_EDGE = (1 << 3);
    public static final int GROW_BOTTOM_EDGE = (1 << 4);
    public static final int MOVE = (1 << 5);


    public HighlightView(ImageView ctx, Rect imageRect, RectF cropRect, float density) {
        mContext = ctx;
        final int progressColor = mContext.getResources().getColor(R.color.progress_color);
        mMatrix = new Matrix(ctx.getImageMatrix());
        mPixelDensity = density;
        Log.i(LOG_TAG, "image = " +imageRect.toString() + " crop = " + cropRect.toString());
        mTrapzoid = new CroppingTrapezoid(cropRect,imageRect);

        mDrawRect = computeLayout();

        mFocusPaint.setARGB(125, 50, 50, 50);
        mNoFocusPaint.setARGB(125, 50, 50, 50);
        mOutlinePaint.setARGB(125, Color.red(progressColor), Color.green(progressColor), Color.blue(progressColor));
        mOutlinePaint.setStrokeWidth(3F);
        mOutlinePaint.setStyle(Paint.Style.STROKE);
        mOutlinePaint.setAntiAlias(true);

        mMode = ModifyMode.None;
        android.content.res.Resources resources = mContext.getResources();
        mResizeDrawableWidth = resources.getDrawable(R.drawable.camera_crop_width);
        mResizeDrawableHeight = resources.getDrawable(R.drawable.camera_crop_height);
    }


    public void setFocus(boolean f) {
        mIsFocused = f;
    }

    protected void draw(Canvas canvas) {
        if (mHidden) {
            return;
        }

//        mContext.getDrawingRect(mViewDrawingRect);
//
//        mTopRect.set(0, 0, mViewDrawingRect.right, mDrawRect.top);
//        mRightRect.set(0, mDrawRect.top, mDrawRect.left, mDrawRect.bottom);
//        mLeftRect.set(mDrawRect.right, mDrawRect.top, mViewDrawingRect.right, mDrawRect.bottom);
//        mBottomRect.set(0, mDrawRect.bottom, mViewDrawingRect.right, mViewDrawingRect.bottom);
//
//        canvas.drawRect(mTopRect, mFocusPaint);
//        canvas.drawRect(mRightRect, mFocusPaint);
//        canvas.drawRect(mLeftRect, mFocusPaint);
//        canvas.drawRect(mBottomRect, mFocusPaint);
//        canvas.drawRect(mDrawRect, mOutlinePaint);

        //drawResizeDrawables(canvas);
        drawEdges(canvas);

    }

    private void drawEdges(Canvas canvas) {
        final Point topLeft = mTrapzoid.getTopLeft();
        final Point topRight = mTrapzoid.getTopRight();
        final Point bottomRight = mTrapzoid.getBottomRight();
        final Point bottomLeft = mTrapzoid.getBottomLeft();
        canvas.drawLine(topLeft.x,topLeft.y,topRight.x,topRight.y,mOutlinePaint);
        canvas.drawLine(topRight.x,topRight.y,bottomRight.x,bottomRight.y,mOutlinePaint);
        canvas.drawLine(bottomRight.x,bottomRight.y,bottomLeft.x, bottomLeft.y,mOutlinePaint);
        canvas.drawLine(topLeft.x,topLeft.y,bottomLeft.x, bottomLeft.y,mOutlinePaint);
    }

    private void drawResizeDrawables(Canvas canvas) {
        int left = mDrawRect.left + 1;
        int right = mDrawRect.right + 1;
        int top = mDrawRect.top + 4;
        int bottom = mDrawRect.bottom + 3;

        int widthWidth = mResizeDrawableWidth.getIntrinsicWidth() / 2;
        int widthHeight = mResizeDrawableWidth.getIntrinsicHeight() / 2;
        int heightHeight = mResizeDrawableHeight.getIntrinsicHeight() / 2;
        int heightWidth = mResizeDrawableHeight.getIntrinsicWidth() / 2;

        int xMiddle = mDrawRect.left + ((mDrawRect.right - mDrawRect.left) / 2);
        int yMiddle = mDrawRect.top + ((mDrawRect.bottom - mDrawRect.top) / 2);

        mResizeDrawableWidth.setBounds(left - widthWidth, yMiddle - widthHeight, left + widthWidth, yMiddle + widthHeight);
        mResizeDrawableWidth.draw(canvas);

        mResizeDrawableWidth.setBounds(right - widthWidth, yMiddle - widthHeight, right + widthWidth, yMiddle + widthHeight);
        mResizeDrawableWidth.draw(canvas);

        mResizeDrawableHeight.setBounds(xMiddle - heightWidth, top - heightHeight, xMiddle + heightWidth, top + heightHeight);
        mResizeDrawableHeight.draw(canvas);

        mResizeDrawableHeight.setBounds(xMiddle - heightWidth, bottom - heightHeight, xMiddle + heightWidth, bottom + heightHeight);
        mResizeDrawableHeight.draw(canvas);


        final Point topLeft = mTrapzoid.getTopLeft();
        mResizeDrawableHeight.setBounds(topLeft.x - heightWidth, topLeft.y - heightHeight, topLeft.x + heightWidth, topLeft.y + heightHeight);
        mResizeDrawableHeight.draw(canvas);

        final Point topRight = mTrapzoid.getTopRight();
        mResizeDrawableHeight.setBounds(topRight.x - heightWidth, topRight.y - heightHeight, topRight.x + heightWidth, topRight.y + heightHeight);
        mResizeDrawableHeight.draw(canvas);

        final Point bottomRightt = mTrapzoid.getBottomRight();
        mResizeDrawableHeight.setBounds(bottomRightt.x - heightWidth, bottomRightt.y - heightHeight, bottomRightt.x + heightWidth, bottomRightt.y + heightHeight);
        mResizeDrawableHeight.draw(canvas);

        final Point bottomLeft = mTrapzoid.getBottomLeft();
        mResizeDrawableHeight.setBounds(bottomLeft.x - heightWidth, bottomLeft.y - heightHeight, bottomLeft.x + heightWidth, bottomLeft.y + heightHeight);
        mResizeDrawableHeight.draw(canvas);

    }

    public void setMode(ModifyMode mode) {
        if (mode != mMode) {
            mMode = mode;
            mContext.invalidate();
        }
    }


    // Determines which edges are hit by touching at (x, y).
    public int getHit(float x, float y, float scale) {
        // convert hysteresis to imagespace
        final float hysteresis = (20F * mPixelDensity) / scale;
        return mTrapzoid.getHit(x, y, hysteresis);
    }


    // Handles motion (dx, dy) in screen space.
    // The "edge" parameter specifies which edges the user is dragging.
    void handleMotion(int edge, float dx, float dy) {
        if (edge == GROW_NONE) {
            return;
        } else if (edge == MOVE) {
            mTrapzoid.moveBy(dx, dy);
        } else {
            mTrapzoid.growBy(edge, dx, dy);
        }
        mDrawRect = computeLayout();
        invalidate();
    }

    // Returns the cropping rectangle in image space.
    public Rect getCropRect() {
        return mTrapzoid.getBoundingRect();
    }

    // Maps the cropping rectangle from image space to screen space.
    private Rect computeLayout() {
        return mTrapzoid.getBoundingRect(mMatrix);
    }

    public void invalidate() {
        mContext.invalidate();
    }

}
