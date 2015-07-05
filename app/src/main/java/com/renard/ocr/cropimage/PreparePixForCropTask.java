package com.renard.ocr.cropimage;

import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.WriteFile;
import com.renard.image_processing.Blur;
import com.renard.image_processing.BlurDetectionResult;

import android.graphics.Bitmap;
import android.os.AsyncTask;

class CropData {
    private final Bitmap mBitmap;
    private final CropImageScaler.ScaleResult mScaleResult;
    private final BlurDetectionResult mBlurrines;

    CropData(Bitmap bitmap, CropImageScaler.ScaleResult scaleFactor, BlurDetectionResult blurrines) {
        mBitmap = bitmap;
        this.mScaleResult = scaleFactor;
        this.mBlurrines = blurrines;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public CropImageScaler.ScaleResult getScaleResult() {
        return mScaleResult;
    }

    public BlurDetectionResult getBlurrines() {
        return mBlurrines;
    }

    public void recylce() {
        mBitmap.recycle();
        mScaleResult.getPix().recycle();
        mBlurrines.getPixBlur().recycle();
    }
}


public class PreparePixForCropTask extends AsyncTask<Void, Void, CropData> {
    private static final String TAG = PreparePixForCropTask.class.getName();


    private final Pix mPix;
    private final int mWidth;
    private final int mHeight;

    public PreparePixForCropTask(Pix pix, int width, int height) {
        mPix = pix;
        mWidth = width;
        mHeight = height;
    }

    @Override
    protected void onCancelled(CropData cropData) {
        super.onCancelled(cropData);
        cropData.recylce();
    }

    @Override
    protected void onPostExecute(CropData cropData) {
        super.onPostExecute(cropData);
        de.greenrobot.event.EventBus.getDefault().post(cropData);
    }

    @Override
    protected CropData doInBackground(Void... params) {
        BlurDetectionResult blurDetectionResult = Blur.blurDetect(mPix);
        CropImageScaler scaler = new CropImageScaler();
        CropImageScaler.ScaleResult scaleResult;
        // scale it so that it fits the screen
        scaleResult = scaler.scale(mPix, mWidth, mHeight);
        Bitmap bitmap = WriteFile.writeBitmap(scaleResult.getPix());
        return new CropData(bitmap, scaleResult, blurDetectionResult);

    }


}