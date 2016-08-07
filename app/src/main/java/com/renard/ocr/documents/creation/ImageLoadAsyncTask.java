package com.renard.ocr.documents.creation;

import com.crashlytics.android.Crashlytics;
import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.leptonica.android.Scale;
import com.renard.ocr.TextFairyApplication;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;


/**
 * @author renard
 */
public class ImageLoadAsyncTask extends AsyncTask<Void, Void, ImageLoadAsyncTask.LoadResult> {

    public class LoadResult {
        private final Pix mPix;
        private final PixLoadStatus mStatus;

        public LoadResult(PixLoadStatus status) {
            mStatus = status;
            mPix = null;
        }

        public LoadResult(Pix p) {
            mStatus = PixLoadStatus.SUCCESS;
            mPix = p;
        }

    }

    final static String EXTRA_PIX = "pix";
    final static String EXTRA_STATUS = "status";
    final static String EXTRA_SKIP_CROP = "skip_crop";
    final static String ACTION_IMAGE_LOADED = ImageLoadAsyncTask.class.getName() + ".image.loaded";
    final static String ACTION_IMAGE_LOADING_START = ImageLoadAsyncTask.class.getName() + ".image.loading.start";
    public static final int MIN_PIXEL_COUNT = 3 * 1024 * 1024;
    private final boolean skipCrop;
    private final Context context;
    private final Uri cameraPicUri;

    ImageLoadAsyncTask(NewDocumentActivity activity, boolean skipCrop, Uri cameraPicUri) {
        context = activity.getApplicationContext();
        this.skipCrop = skipCrop;
        this.cameraPicUri = cameraPicUri;
    }


    private static final String LOG_TAG = ImageLoadAsyncTask.class.getSimpleName();


    @Override
    protected void onPreExecute() {
        Log.i(LOG_TAG, "onPreExecute");
        Intent intent = new Intent(ACTION_IMAGE_LOADING_START);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    protected void onPostExecute(LoadResult result) {
        Log.i(LOG_TAG, "onPostExecute");
        Intent intent = new Intent(ACTION_IMAGE_LOADED);
        if (result.mStatus == PixLoadStatus.SUCCESS) {
            intent.putExtra(EXTRA_PIX, result.mPix.getNativePix());
        }
        intent.putExtra(EXTRA_STATUS, result.mStatus.ordinal());
        intent.putExtra(EXTRA_SKIP_CROP, skipCrop);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    protected LoadResult doInBackground(Void... params) {
        if (isCancelled()) {
            Log.i(LOG_TAG, "isCancelled");
            return null;
        }
        Pix p = ReadFile.loadWithPicasso(context, cameraPicUri);
        if (p == null) {
            if (TextFairyApplication.isRelease()) {
                Crashlytics.setString("image uri", cameraPicUri.toString());
                Crashlytics.logException(new IOException("could not load image."));
            }
            return new LoadResult(PixLoadStatus.IMAGE_FORMAT_UNSUPPORTED);
        }

        final long pixPixelCount = p.getWidth() * p.getHeight();
        if (pixPixelCount < MIN_PIXEL_COUNT) {
            double scale = Math.sqrt(((double) MIN_PIXEL_COUNT) / pixPixelCount);
            Pix scaledPix = Scale.scale(p, (float) scale);
            if (scaledPix.getNativePix() == 0) {
                if (TextFairyApplication.isRelease()) {
                    Crashlytics.log("pix = (" + p.getWidth() + ", " + p.getHeight() + ")");
                    Crashlytics.logException(new IllegalStateException("scaled pix is 0"));
                }
            } else {
                p.recycle();
                p = scaledPix;
            }
        }


        return new LoadResult(p);

    }

}
