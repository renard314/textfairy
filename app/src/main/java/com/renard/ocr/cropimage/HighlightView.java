package com.renard.ocr.cropimage;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.view.MotionEvent;

/**
 * Created by renard on 07/06/15.
 */
public interface HighLightView {
    public static final int GROW_NONE = 0;
    public static final int GROW_LEFT_EDGE = (1 << 1);
    public static final int GROW_RIGHT_EDGE = (1 << 2);
    public static final int GROW_TOP_EDGE = (1 << 3);
    public static final int GROW_BOTTOM_EDGE = (1 << 4);
    public static final int MOVE = (1 << 5);


    /**
     * @return Matrix that converts between image and screen space.
     */
    Matrix getMatrix();

    /**
     *
     * @return Drawing rect in screen space.
     */
    Rect getDrawRect();

    /**
     * @return vertical center in image space.
     */
    float centerY();

    /**
     * @return horizontal center in image space.
     */
    float centerX();


    int getHit(float x, float y, float scale);

    void handleMotion(int motionEdge, float dx, float dy);

    void draw(Canvas canvas);
}
