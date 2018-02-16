package com.googlecode.tesseract.android;

/**
 * Created by renard on 09/01/15.
 */
public interface OcrProgressListener {
    void onProgressValues(int percent, int left, int right, int top, int bottom, int left2, int right2, int top2, int bottom2);
}
