package com.renard.ocr.documents.creation;

import com.crashlytics.android.Crashlytics;
import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.leptonica.android.Rotate;
import com.googlecode.leptonica.android.Scale;
import com.renard.ocr.util.Util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;


/**
 * @author renard
 */
public class ImageLoadAsyncTask extends AsyncTask<Void, Void, ImageLoadAsyncTask.LoadResult> {

    public class LoadResult {
        private final Pix mPix;
        private final int mRotation;
        private final PixLoadStatus mStatus;

        public LoadResult(PixLoadStatus status) {
            mStatus = status;
            mPix = null;
            mRotation = 0;
        }

        public LoadResult(Pix pix, int rotation) {
            mStatus = PixLoadStatus.SUCCESS;
            mPix = pix;
            mRotation = rotation;
        }
    }

    final static String EXTRA_PIX = "pix";
    final static String EXTRA_STATUS = "status";
    public final static String EXTRA_ROTATION = "rotation";
    final static String EXTRA_SKIP_CROP = "skip_crop";
    final static String ACTION_IMAGE_LOADED = ImageLoadAsyncTask.class.getName() + ".image.loaded";
    final static String ACTION_IMAGE_LOADING_START = ImageLoadAsyncTask.class.getName() + ".image.loading.start";
    final private static String TMP_FILE_NAME = "loadfiletmp";
    public static final int MIN_PIXEL_COUNT = 3 * 1024 * 1024;
    private final boolean skipCrop;
    private final WeakReference<ContentResolver> mContentResolver;
    private final Context context;
    private int rotateXDegrees;
    private final Uri cameraPicUri;

    ImageLoadAsyncTask(NewDocumentActivity activity, boolean skipCrop, int rotateXDegrees, Uri cameraPicUri) {
        mContentResolver = new WeakReference<>(activity.getContentResolver());
        context = activity.getApplicationContext();
        this.skipCrop = skipCrop;
        this.rotateXDegrees = rotateXDegrees;
        this.cameraPicUri = cameraPicUri;
    }


    private static final String LOG_TAG = ImageLoadAsyncTask.class.getSimpleName();

    private int getRotationFromFile(String pathForUri) {
        int orientation = 0;
        try {
            ExifInterface exif = new ExifInterface(pathForUri);
            int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (exifOrientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    orientation = 270;

                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    orientation = 180;

                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    orientation = 90;

                    break;

                case ExifInterface.ORIENTATION_NORMAL:
                    orientation = 0;

                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return orientation;
    }

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
            intent.putExtra(EXTRA_ROTATION, result.mRotation);
        }
        intent.putExtra(EXTRA_STATUS, result.mStatus.ordinal());
        intent.putExtra(EXTRA_SKIP_CROP, skipCrop);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    protected LoadResult doInBackground(Void... params) {
        Log.i(LOG_TAG, "doInBackground");
        if (isCancelled()) {
            Log.i(LOG_TAG, "isCancelled");
            return null;
        }

        try {
            Pix p = null;
            String pathForUri = Util.getPathForUri(context, cameraPicUri);
            // MediaStore loves to crash with an oom exception. So we
            // try to load bitmap nativly if it is on internal storage
            if (pathForUri != null && pathForUri.startsWith("http")) {
                p = downloadFile(pathForUri);
                if (p == null) {
                    return new LoadResult(PixLoadStatus.IMAGE_FORMAT_UNSUPPORTED);
                }
            } else if (pathForUri != null) {
                File imageFile = new File(pathForUri);
                if (imageFile.exists()) {
                    if (rotateXDegrees == -1) {
                        rotateXDegrees = getRotationFromFile(pathForUri);
                    }
                    p = ReadFile.readFile(context, imageFile);

                    if (p == null) {
                        return new LoadResult(PixLoadStatus.IMAGE_FORMAT_UNSUPPORTED);
                    }
                } else {
                    return new LoadResult(PixLoadStatus.IMAGE_DOES_NOT_EXIST);
                }
            } else if (cameraPicUri.toString().startsWith("content")) {
                InputStream stream = null;
                FileOutputStream fileOut = null;
                try {
                    final ContentResolver contentResolver = this.mContentResolver.get();
                    if (contentResolver != null) {
                        stream = contentResolver.openInputStream(cameraPicUri);
                        if (stream != null) {
                            fileOut = context.openFileOutput(TMP_FILE_NAME, Context.MODE_PRIVATE);
                            Util.copy(stream, fileOut);
                            File file = context.getFileStreamPath(TMP_FILE_NAME);
                            p = ReadFile.readFile(context, file);
                        }
                    } else {
                        return new LoadResult(PixLoadStatus.IMAGE_COULD_NOT_BE_READ);
                    }
                } finally {
                    if (stream != null) {
                        stream.close();
                    }
                    if (fileOut != null) {
                        fileOut.close();
                    }
                    context.deleteFile(TMP_FILE_NAME);
                }
                if (p == null) {
                    return new LoadResult(PixLoadStatus.IMAGE_FORMAT_UNSUPPORTED);
                }
            } else {
                return new LoadResult(PixLoadStatus.IO_ERROR);
            }

            if (skipCrop && rotateXDegrees > 0 && rotateXDegrees != 360) {
                final Pix pix = Rotate.rotateOrth(p, rotateXDegrees / 90);
                p.recycle();
                p = pix;
                rotateXDegrees = 0;
            }
            if (p == null) {
                return new LoadResult(PixLoadStatus.IMAGE_FORMAT_UNSUPPORTED);
            }
            final long pixPixelCount = p.getWidth() * p.getHeight();
            if (pixPixelCount < MIN_PIXEL_COUNT) {
                final double scale = ((double) MIN_PIXEL_COUNT) / pixPixelCount;
                Pix scaledPix = Scale.scale(p, (float) scale);
                if (scaledPix != null) {
                    p.recycle();
                    p = scaledPix;
                } else {
                    Crashlytics.log("pix = (" + p.getWidth() + ", " + p.getHeight() + ")");
                    Crashlytics.logException(new IllegalStateException("scaled pix is null"));
                }
            }


            return new LoadResult(p, rotateXDegrees);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return new LoadResult(PixLoadStatus.IMAGE_DOES_NOT_EXIST);
        } catch (IOException e) {
            e.printStackTrace();
            return new LoadResult(PixLoadStatus.IO_ERROR);
        }
    }


    private Pix downloadFile(String pathForUri) {
        InputStream inputStream = null;
        try {
            URL url = new URL(pathForUri);
            inputStream = url.openStream();
            return getPixFromStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private Pix getPixFromStream(InputStream stream) throws IOException {
        FileOutputStream fileOut = null;
        Pix p = null;
        try {
            if (stream != null) {
                fileOut = context.openFileOutput(TMP_FILE_NAME, Context.MODE_PRIVATE);
                Util.copy(stream, fileOut);
                File file = context.getFileStreamPath(TMP_FILE_NAME);
                p = ReadFile.readFile(context, file);
            }
        } finally {
            if (stream != null) {
                stream.close();
            }
            if (fileOut != null) {
                fileOut.close();
            }
            context.deleteFile(TMP_FILE_NAME);
        }
        return p;
    }
}
