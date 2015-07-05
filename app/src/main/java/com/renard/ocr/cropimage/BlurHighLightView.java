package com.renard.ocr.cropimage;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 * Created by renard on 07/06/15.
 */
public class BlurHighLightView implements HighLightView {

    private final RectF mBlurredRegion;
    private final RectF mDrawRect = new RectF(); // in screen space
    private final Rect mViewDrawingRect = new Rect();
    private final Paint mOutlinePaint = new Paint();
    private final Paint mFocusPaint = new Paint();
    private final Rect mLeftRect = new Rect();
    private final Rect mRightRect = new Rect();
    private final Rect mTopRect = new Rect();
    private final Rect mBottomRect = new Rect();

    private Matrix mMatrix;

    BlurHighLightView(Rect blurredRegion, int progressColor, int edgeWidth, Matrix imageMatrix) {
        mBlurredRegion = new RectF(blurredRegion);
        mOutlinePaint.setARGB(0xFF, Color.red(progressColor), Color.green(progressColor), Color.blue(progressColor));
        mOutlinePaint.setStrokeWidth(edgeWidth);
        mOutlinePaint.setStyle(Paint.Style.STROKE);
        mOutlinePaint.setAntiAlias(true);
        mMatrix = new Matrix(imageMatrix);
        mFocusPaint.setARGB(125, 50, 50, 50);
        mFocusPaint.setStyle(Paint.Style.FILL);


    }

    @Override
    public Matrix getMatrix() {
        return mMatrix;
    }

    @Override
    public Rect getDrawRect() {
        return mViewDrawingRect;
    }

    @Override
    public float centerY() {
        return mBlurredRegion.centerY();
    }

    @Override
    public float centerX() {
        return mBlurredRegion.centerX();
    }

    @Override
    public int getHit(float x, float y, float scale) {
        return GROW_NONE;
    }

    @Override
    public void handleMotion(int motionEdge, float dx, float dy) {

    }

    @Override
    public void draw(Canvas canvas) {
        //set draw rect by mapping the points
        mMatrix.mapRect(mDrawRect,mBlurredRegion);
        mViewDrawingRect.set((int) mDrawRect.left, (int) mDrawRect.top, (int) mDrawRect.right, (int) mDrawRect.bottom);
        canvas.drawRect(mDrawRect, mOutlinePaint);

        mTopRect.set(0, 0, canvas.getWidth(), (int) mDrawRect.top);
        mLeftRect.set(0, (int) mDrawRect.top, (int) mDrawRect.left, (int) mDrawRect.bottom);
        mRightRect.set((int) mDrawRect.right, (int) mDrawRect.top, canvas.getWidth(), (int) mDrawRect.bottom);
        mBottomRect.set(0, (int) mDrawRect.bottom, canvas.getWidth(), canvas.getHeight());


        canvas.drawRect(mTopRect, mFocusPaint);
        canvas.drawRect(mRightRect, mFocusPaint);
        canvas.drawRect(mLeftRect, mFocusPaint);
        canvas.drawRect(mBottomRect, mFocusPaint);

    }
}
