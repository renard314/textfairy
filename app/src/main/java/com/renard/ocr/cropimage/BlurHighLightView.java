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

    private Matrix mMatrix;

    BlurHighLightView(Rect blurredRegion, int progressColor, int edgeWidth) {
        mBlurredRegion = new RectF(blurredRegion);
        mOutlinePaint.setARGB(0xFF, Color.red(progressColor), Color.green(progressColor), Color.blue(progressColor));
        mOutlinePaint.setStrokeWidth(edgeWidth);
        mOutlinePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mOutlinePaint.setAntiAlias(true);

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
        mMatrix.mapRect(mBlurredRegion, mDrawRect);
        mViewDrawingRect.set((int) mDrawRect.left, (int) mDrawRect.top, (int) mDrawRect.right, (int) mDrawRect.bottom);
        canvas.drawRect(mDrawRect, mOutlinePaint);
    }
}
