package com.renard.ocr.documents.creation.crop;

import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.WriteFile;
import com.renard.ocr.cropimage.image_processing.Blur;
import com.renard.ocr.cropimage.image_processing.BlurDetectionResult;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

class CropData {
    private final Bitmap mBitmap;
    private final CropImageScaler.ScaleResult mScaleResult;
    private final BlurDetectionResult mBlurriness;

    CropData(Bitmap bitmap, CropImageScaler.ScaleResult scaleFactor, BlurDetectionResult blurriness) {
        mBitmap = bitmap;
        this.mScaleResult = scaleFactor;
        this.mBlurriness = blurriness;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public CropImageScaler.ScaleResult getScaleResult() {
        return mScaleResult;
    }

    public BlurDetectionResult getBlurriness() {
        return mBlurriness;
    }

    public void recylce() {
        mScaleResult.getPix().recycle();
        mBlurriness.destroy();
    }
}


public class PreparePixForCropTask extends AsyncTask<Void, Void, CropData> {
    private static final String TAG = PreparePixForCropTask.class.getName();


    private final Pix mPix;
    private final int mWidth;
    private final int mHeight;

    public PreparePixForCropTask(Pix pix, int width, int height) {
        if (width == 0 || height == 0) {
            Log.e(TAG, "scaling to 0 value: (" + width + "," + height + ")");
        }
        mPix = pix;
        mWidth = width;
        mHeight = height;
    }

    @Override
    protected void onCancelled(CropData cropData) {
        super.onCancelled(cropData);
        if (cropData != null) {
            cropData.recylce();
        }
    }

    @Override
    protected void onPostExecute(CropData cropData) {
        super.onPostExecute(cropData);
        de.greenrobot.event.EventBus.getDefault().post(cropData);
    }

    @Override
    protected CropData doInBackground(Void... params) {
        Log.d(TAG, "scaling to (" + mWidth + "," + mHeight + ")");
        BlurDetectionResult blurDetectionResult = Blur.blurDetect(mPix);
        CropImageScaler scaler = new CropImageScaler();
        CropImageScaler.ScaleResult scaleResult;
        // scale it so that it fits the screen
        if (isCancelled()) {
            blurDetectionResult.destroy();
            return null;
        }
        scaleResult = scaler.scale(mPix, mWidth, mHeight);
        if (isCancelled()) {
            scaleResult.getPix().recycle();
            blurDetectionResult.destroy();
            return null;
        }
        Bitmap bitmap = WriteFile.writeBitmap(scaleResult.getPix());
        if (isCancelled()) {
            scaleResult.getPix().recycle();
            blurDetectionResult.destroy();
            if (bitmap != null) {
                bitmap.recycle();
            }
            return null;
        }
        if (bitmap != null) {
            Log.d(TAG, "scaling result (" + bitmap.getWidth() + "," + bitmap.getHeight() + ")");
        } else {
            //TODO LOG to GA along with with and height as well as pix pointer
        }
        return new CropData(bitmap, scaleResult, blurDetectionResult);

    }


}