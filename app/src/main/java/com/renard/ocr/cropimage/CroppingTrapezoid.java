package com.renard.ocr.cropimage;

import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import static com.renard.ocr.cropimage.HighlightView.*;

/**
 * Created by renard on 26/02/15.
 */
public class CroppingTrapezoid {

    private final static String LOG_TAG = CroppingTrapezoid.class.getSimpleName();
    private final float[] mPoints = new float[8];
    private final float[] mMappedPoints = new float[8];
    private final Rect mImageRect;

    public CroppingTrapezoid(RectF cropRect, Rect imageRect) {
        mImageRect = new Rect(imageRect);
        mPoints[0] = cropRect.left;
        mPoints[1] = cropRect.top;
        mPoints[2] = cropRect.right;
        mPoints[3] = cropRect.top;
        mPoints[4] = cropRect.right;
        mPoints[5] = cropRect.bottom;
        mPoints[6] = cropRect.left;
        mPoints[7] = cropRect.bottom;
    }


    public Rect getBoundingRect() {
        return getBoundingRect(mPoints);
    }

    public Rect getBoundingRect(Matrix matrix) {
        matrix.mapPoints(mMappedPoints, mPoints);
        return getBoundingRect(mMappedPoints);
    }


    public Point getTopLeft() {
        return new Point((int) mPoints[0], (int) mPoints[1]);
    }

    public Point getTopRight() {
        return new Point((int) mPoints[2], (int) mPoints[3]);
    }

    public Point getBottomRight() {
        return new Point((int) mPoints[4], (int) mPoints[5]);
    }

    public Point getBottomLeft() {
        return new Point((int) mPoints[6], (int) mPoints[7]);
    }

    // moves the cropping trpezoid by (dx, dy) in image space.
    public void moveBy(float dx, float dy) {
        for (int i = 0; i < 4; i += 2) {
            mPoints[i] += dx;
            mPoints[i + 1] += dy;
        }
        capPoints();
    }


    // Grows the cropping trapezoid by (dx, dy) in image space.
    public void growBy(int edge, float dx, float dy) {

        // Don't let the cropping rectangle grow too fast.
        // Grow at most half of the difference between the image rectangle and
        // the cropping rectangle.
        Rect r = getBoundingRect();
        if (dx > 0F && r.width() + 2 * dx > mImageRect.width()) {
            float adjustment = (mImageRect.width() - r.width()) / 2F;
            dx = adjustment;
        }
        if (dy > 0F && r.height() + 2 * dy > mImageRect.height()) {
            float adjustment = (mImageRect.height() - r.height()) / 2F;
            dy = adjustment;
        }

        if ((GROW_LEFT_EDGE | GROW_TOP_EDGE) == edge) {
            mPoints[0] += dx;
            mPoints[1] += dy;
            return;
        }
        if ((GROW_RIGHT_EDGE | GROW_TOP_EDGE) == edge) {
            mPoints[2] += dx;
            mPoints[3] += dy;
            return;
        }
        if ((GROW_RIGHT_EDGE | GROW_BOTTOM_EDGE) == edge) {
            mPoints[4] += dx;
            mPoints[5] += dy;
            return;
        }
        if ((GROW_LEFT_EDGE | GROW_BOTTOM_EDGE) == edge) {
            mPoints[6] += dx;
            mPoints[7] += dy;
            return;
        }
        if ((GROW_LEFT_EDGE) == edge) {
            mPoints[0] += dx;
            mPoints[1] += dy;
            mPoints[6] += dx;
            mPoints[7] += dy;
        }
        if ((GROW_RIGHT_EDGE) == edge) {
            mPoints[2] += dx;
            mPoints[3] += dy;
            mPoints[4] += dx;
            mPoints[5] += dy;
        }
        if ((GROW_TOP_EDGE) == edge) {
            mPoints[0] += dx;
            mPoints[1] += dy;
            mPoints[2] += dx;
            mPoints[3] += dy;
        }
        if ((GROW_BOTTOM_EDGE) == edge) {
            mPoints[4] += dx;
            mPoints[5] += dy;
            mPoints[6] += dx;
            mPoints[7] += dy;
        }

        // TODO Don't let the cropping rectangle shrink too fast.
        final float widthCap = 25F;
        r = getBoundingRect();
        if (r.width() < widthCap) {
            //r.inset(-(widthCap - r.width()) / 2F, 0F);
        }
        float heightCap = widthCap;
        if (r.height() < heightCap) {
            //r.inset(0F, -(heightCap - r.height()) / 2F);
        }
        capPoints();
    }


    public int getHit(float x, float y, float hysteresis) {
        int retval = GROW_NONE;

        final boolean touchesTopLeft = calculateDistanceToPoint(mPoints[0], mPoints[1], x, y) <= hysteresis;
        if (touchesTopLeft) {
            Log.i(LOG_TAG, "top left");
            return GROW_LEFT_EDGE | GROW_TOP_EDGE;
        }
        final boolean touchesTopRight = calculateDistanceToPoint(mPoints[2], mPoints[3], x, y) <= hysteresis;
        if (touchesTopRight) {
            Log.i(LOG_TAG, "top right");
            return GROW_RIGHT_EDGE | GROW_TOP_EDGE;
        }
        final boolean touchesBottomRight = calculateDistanceToPoint(mPoints[4], mPoints[5], x, y) <= hysteresis;
        if (touchesBottomRight) {
            Log.i(LOG_TAG, "bottom right");
            return GROW_RIGHT_EDGE | GROW_BOTTOM_EDGE ;
        }
        final boolean touchesBottomLeft = calculateDistanceToPoint(mPoints[6], mPoints[7], x, y) <= hysteresis;
        if (touchesBottomLeft) {
            Log.i(LOG_TAG, "bottom left");
            return GROW_LEFT_EDGE | GROW_BOTTOM_EDGE;
        }

        final double topDistance = calculcateDistanceToLine(mPoints[0], mPoints[1], mPoints[2], mPoints[3], x, y);
        if (topDistance <= hysteresis) {
            Log.i(LOG_TAG, "top");
            retval |= GROW_TOP_EDGE;
        }
        final double rightDistance = calculcateDistanceToLine(mPoints[2], mPoints[3], mPoints[4], mPoints[5], x, y);
        if (rightDistance <= hysteresis) {
            Log.i(LOG_TAG, "right");
            retval |= GROW_RIGHT_EDGE;
        }
        final double bottomDistance = calculcateDistanceToLine(mPoints[6], mPoints[7], mPoints[4], mPoints[5], x, y);
        if (bottomDistance <= hysteresis) {
            Log.i(LOG_TAG, "bottom");
            retval |= GROW_BOTTOM_EDGE;
        }
        final double leftDistance = calculcateDistanceToLine(mPoints[0], mPoints[1], mPoints[6], mPoints[7], x, y);
        if (leftDistance <= hysteresis) {
            Log.i(LOG_TAG, "left");
            retval |= GROW_LEFT_EDGE;
        }

        // Not near any edge but maybe inside the trapezoid
        if (retval == GROW_NONE) {
            //check if it is inside the trapezoid
            float xCross = calculateXCrossing(mPoints[2], mPoints[3], mPoints[4], mPoints[5], x, y);
            if (xCross < (x-hysteresis)) {
                //point is outside to right of trapezoid
                return retval;
            }
            xCross = calculateXCrossing(mPoints[0], mPoints[1], mPoints[6], mPoints[7], x, y);
            if (xCross > (x+hysteresis)) {
                //point is outside to left of trapezoid
                return retval;
            }
            float yCross = calculateYCrossing(mPoints[0], mPoints[1], mPoints[2], mPoints[3], x, y);
            if (yCross > (y+hysteresis)) {
                //point is outside to the top of trapezoid
                return retval;
            }
            yCross = calculateYCrossing(mPoints[4], mPoints[5], mPoints[6], mPoints[7], x, y);
            if (yCross < (y-hysteresis)) {
                //point is outside to the bottom of trapezoid
                return retval;
            }
            Log.i(LOG_TAG, "move");
            retval = MOVE;
        }

        return retval;
    }

    private Rect getBoundingRect(float[] points) {
        //return new Rect(Math.round(r.left), Math.round(r.top), Math.round(r.right), Math.round(r.bottom));
        int left = (int) Math.min(points[0], points[6]);
        int right = (int) Math.max(points[2], points[4]);
        int top = (int) Math.min(points[1], points[3]);
        int bottom = (int) Math.max(points[5], points[7]);
        //int width = right-left;
        //int height = bottom-top;
        return new Rect(left, top, right, bottom);
    }

    private void capPoints() {
        mPoints[0] = Math.max(0, mPoints[0]); //left
        mPoints[1] = Math.max(0, mPoints[0]); //top
        mPoints[2] = Math.min(mImageRect.right, mPoints[2]); //right
        mPoints[3] = Math.max(0, mPoints[3]); //top
        mPoints[4] = Math.min(mImageRect.right, mPoints[4]); //right
        mPoints[5] = Math.min(mImageRect.bottom, mPoints[5]); //bottom
        mPoints[6] = Math.max(0, mPoints[6]); //left
        mPoints[7] = Math.min(mImageRect.bottom, mPoints[7]); //bottom
    }

    private double calculcateDistanceToLine(float x1, float y1, float x2, float y2, float x, float y) {
        if ((x > x1 && x < x2) || (y > y1 && y < y2)) {
            if (x1 == x2) {
                return Math.abs(x1 - x);
            }
            float mTop = getRiseOfLine(x1, y1, x2, y2);
            float nTop = getYCrossingOfLine(x1, y1, mTop);
            if (mTop == 0) {
                return Math.abs(nTop - y);
            }
            float mCross = -1/mTop;
            float nCross = getYCrossingOfLine(x, y, mCross);
            float xCross = (nTop-nCross)/(mCross-mTop);
            float yCross = mCross * xCross + nCross;
            return calculateDistanceToPoint(xCross, yCross, x, y);
        } else {
            return Double.MAX_VALUE;
        }
    }

    private double calculateDistanceToPoint(float x1, float y1, float x, float y) {
        return Math.sqrt((x - x1) * (x - x1) + (y - y1) * (y - y1));
    }

    private float calculateXCrossing(float x1, float y1, float x2, float y2, float x, float y) {
        if (x1 == x2) {
            return x1;
        }
        float mTop = getRiseOfLine(x1, y1, x2, y2);
        float nTop = getYCrossingOfLine(x1, y1, mTop);
        float xCross = getXOnLine(y, mTop, nTop);
        return xCross;
    }

    private float getXOnLine(float y, float mTop, float nTop) {
        return (y - nTop) / mTop;
    }

    private float calculateYCrossing(float x1, float y1, float x2, float y2, float x, float y) {
        if(y1==y2){
            return y1;
        }
        float mTop = getRiseOfLine(x1, y1, x2, y2);
        float nTop = getYCrossingOfLine(x1, y1, mTop);
        float xCross = getXOnLine(y, mTop, nTop);
        return mTop * xCross + nTop;
    }

    private float getYCrossingOfLine(float x1, float y1, float mTop) {
        return y1 - mTop * x1;
    }

    private float getRiseOfLine(float x1, float y1, float x2, float y2) {
        return (y1 - y2) / (x1 - x2);
    }

}
