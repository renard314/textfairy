package com.googlecode.tesseract.android;

import com.googlecode.leptonica.android.Pix;
import com.renard.ocr.R;

import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;

public class NativeBinding {


    public interface ProgressCallBack {
        void onProgressImage(long nativePix);

        void onProgressText(@StringRes int message);

        void onLayoutAnalysed(long nativePixaText, long nativePixaImages);
    }

    private static final String TAG = "NativeBinding";

    static {
        System.loadLibrary("pngo");
        System.loadLibrary("lept");
        System.loadLibrary("tess");
        System.loadLibrary("image_processing_jni");
        nativeClassInit();
    }

    @Nullable
    private ProgressCallBack mCallBack;

     //Used by the native code
    @SuppressWarnings("unused")
    private long mNativeData;


    public synchronized void setProgressCallBack(@Nullable  ProgressCallBack callBack) {
        mCallBack = callBack;
    }

    public NativeBinding() {
        nativeConstruct();
    }

    public void destroy() {
        Log.d(TAG, "destroying native data");
        nativeDestruct();
    }

    public long[] combinePixa(long nativePixaText, long nativePixaImages, int[] selectedTexts, int[] selectedImages) {
        return combineSelectedPixa(nativePixaText, nativePixaImages, selectedTexts, selectedImages);
    }

    public void analyseLayout(Pix pixs) {
        nativeAnalyseLayout(pixs.getNativePix());
    }

    public long convertBookPage(Pix pixs) {
        return nativeOCRBook(pixs.getNativePix());
    }

    /**
     * called from native code
     */
    @SuppressWarnings("unused")
    private synchronized void onProgressImage(final long nativePix) {
        if (mCallBack != null) {
            mCallBack.onProgressImage(nativePix);
        }
    }

    /**
     * called from native
     * static const int MESSAGE_IMAGE_DETECTION = 0; static const int
     * MESSAGE_IMAGE_DEWARP = 1; static const int MESSAGE_OCR = 2; static const
     * int MESSAGE_ASSEMBLE_PIX = 3; static const int MESSAGE_ANALYSE_LAYOUT =
     * 4;
     */
    @SuppressWarnings("unused")
    private void onProgressText(int id) {
        int messageId = 0;
        switch (id) {
            case 0:
                messageId = R.string.progress_image_detection;
                break;
            case 1:
                messageId = R.string.progress_dewarp;
                break;
            case 2:
                messageId = R.string.progress_ocr;
                break;
            case 3:
                messageId = R.string.progress_assemble_pix;
                break;
            case 4:
                messageId = R.string.progress_analyse_layout;
                break;

        }
        if (messageId != 0 && mCallBack!=null) {
            mCallBack.onProgressText(messageId);
        }
    }

    @SuppressWarnings("unused")
    private void onLayoutElements(long nativePixaText, long nativePixaImages) {
        if(mCallBack!=null){
            mCallBack.onLayoutAnalysed(nativePixaText, nativePixaImages);
        }
    }


    private static native void nativeClassInit();

    private native void nativeConstruct();

    private native void nativeDestruct();

    private native long nativeOCRBook(long nativePix);

    private native long[] combineSelectedPixa(long nativePixaTexts, long nativePixaImages, int[] selectedTexts, int[] selectedImages);

    private native long nativeAnalyseLayout(long nativePix);

}
