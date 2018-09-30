package com.renard.ocr.documents.creation;


import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.leptonica.android.Scale;
import com.renard.ocr.analytics.CrashLogger;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;


/**
 * @author renard
 */
class ImageLoadAsyncTask extends AsyncTask<Void, Void, ImageLoadAsyncTask.LoadResult> {

    class LoadResult {
        private final Pix mPix;
        private final PixLoadStatus mStatus;

        LoadResult(PixLoadStatus status) {
            mStatus = status;
            mPix = null;
        }

        LoadResult(Pix p) {
            mStatus = PixLoadStatus.SUCCESS;
            mPix = p;
        }

    }

    final static String EXTRA_PIX = "pix";
    final static String EXTRA_STATUS = "status";
    final static String EXTRA_SKIP_CROP = "skip_crop";
    final static String ACTION_IMAGE_LOADED = ImageLoadAsyncTask.class.getName() + ".image.loaded";
    final static String ACTION_IMAGE_LOADING_START = ImageLoadAsyncTask.class.getName() + ".image.loading.start";
    private static final int MIN_PIXEL_COUNT = 3 * 1024 * 1024;
    private final boolean skipCrop;
    private final Context context;
    private final Uri cameraPicUri;
    private final CrashLogger mCrashLogger;

    ImageLoadAsyncTask(NewDocumentActivity activity, boolean skipCrop, Uri cameraPicUri) {
        context = activity.getApplicationContext();
        mCrashLogger = activity.getCrashLogger();
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
        Pix p;
        String type = context.getContentResolver().getType(cameraPicUri);

        if (cameraPicUri.getPath().endsWith(".pdf") || "application/pdf".equalsIgnoreCase(type)) {
            p = loadAsPdf(cameraPicUri);
            if (p == null) {
                return new LoadResult(PixLoadStatus.IMAGE_FORMAT_UNSUPPORTED);
            }
        } else {
            p = ReadFile.load(context, cameraPicUri);
            if (p == null) {
                mCrashLogger.setString("image uri", cameraPicUri.toString());
                mCrashLogger.logException(new IOException("could not load image."));
                return new LoadResult(PixLoadStatus.IMAGE_FORMAT_UNSUPPORTED);
            }
        }


        final long pixPixelCount = p.getWidth() * p.getHeight();
        if (pixPixelCount < MIN_PIXEL_COUNT)

        {
            double scale = Math.sqrt(((double) MIN_PIXEL_COUNT) / pixPixelCount);
            Pix scaledPix = Scale.scale(p, (float) scale);
            if (scaledPix.getNativePix() == 0) {
                mCrashLogger.logMessage("pix = (" + p.getWidth() + ", " + p.getHeight() + ")");
                mCrashLogger.logException(new IllegalStateException("scaled pix is 0"));
            } else {
                p.recycle();
                p = scaledPix;
            }
        }

        return new LoadResult(p);
    }

    @Nullable
    private Pix loadAsPdf(Uri cameraPicUri) {
        Pix p = null;
        Bitmap bitmap = null;
        int pageNum = 0;
        PdfiumCore pdfiumCore = new PdfiumCore(context);
        try {
            mCrashLogger.logMessage(cameraPicUri.toString());
            final String replace = cameraPicUri.toString().replace("/file/file", "/file");
            final Uri fixedUri = Uri.parse(replace);

            final ParcelFileDescriptor fd = context.getContentResolver().openFileDescriptor(fixedUri, "r");
            PdfDocument pdfDocument = pdfiumCore.newDocument(fd);

            pdfiumCore.openPage(pdfDocument, pageNum);

            int width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNum);
            int height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNum);
            int widthPixels = (int) (width / (72.0 / 300.0));
            int heightPixels = (int) (height / (72.0 / 300.0));

            bitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888);
            pdfiumCore.renderPageBitmap(pdfDocument, bitmap, pageNum, 0, 0, widthPixels, heightPixels);
            p = ReadFile.readBitmap(bitmap);
            pdfiumCore.closeDocument(pdfDocument);

        } catch (IOException ex) {
            mCrashLogger.logException(ex);
            ex.printStackTrace();
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
        return p;
    }


}
