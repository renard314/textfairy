package com.renard.ocr;

import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.leptonica.android.Rotate;
import com.renard.util.Util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;


enum PixLoadStatus {
    IMAGE_FORMAT_UNSUPPORTED, IMAGE_NOT_32_BIT, IMAGE_COULD_NOT_BE_READ, MEDIA_STORE_RETURNED_NULL, IMAGE_DOES_NOT_EXIST, SUCCESS, IO_ERROR, CAMERA_APP_NOT_FOUND, CAMERA_APP_ERROR, CAMERA_NO_IMAGE_RETURNED

}


/**
 * @author renard
 */
public class ImageLoadAsyncTask extends AsyncTask<Void, Void, Pair<Pix, PixLoadStatus>> {

    final static String EXTRA_PIX = "pix";
    final static String EXTRA_STATUS = "status";
    final static String EXTRA_SKIP_CROP = "skip_crop";
    final static String ACTION_IMAGE_LOADED = ImageLoadAsyncTask.class.getName() + ".image.loaded";
    final static String ACTION_IMAGE_LOADING_START = ImageLoadAsyncTask.class.getName() + ".image.loading.start";
    final private static String TMP_FILE_NAME = "loadfiletmp";
    private final boolean skipCrop;
    private final WeakReference<ContentResolver> mContentResolver;
    private final Context context;
    private int rotateXDegrees;
    private final Uri cameraPicUri;

    ImageLoadAsyncTask(BaseDocumentActivitiy activity, boolean skipCrop, int rotateXDegrees, Uri cameraPicUri) {
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
    protected void onPostExecute(Pair<Pix, PixLoadStatus> p) {
        Log.i(LOG_TAG, "onPostExecute");
        Intent intent = new Intent(ACTION_IMAGE_LOADED);
        if (p.second == PixLoadStatus.SUCCESS) {
            intent.putExtra(EXTRA_PIX, p.first.getNativePix());
        }
        intent.putExtra(EXTRA_STATUS, p.second.ordinal());
        intent.putExtra(EXTRA_SKIP_CROP, skipCrop);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    protected Pair<Pix, PixLoadStatus> doInBackground(Void... params) {
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
                final ContentResolver contentResolver = this.mContentResolver.get();
                if (contentResolver != null) {
                    Bitmap b = MediaStore.Images.Media.getBitmap(contentResolver, cameraPicUri);
                    if (b != null) {
                        if (b.getConfig() != Bitmap.Config.ARGB_8888) {
                            return Pair.create(null, PixLoadStatus.IMAGE_NOT_32_BIT);
                        }
                        p = ReadFile.readBitmap(b);
                        b.recycle();
                    } else {
                        return Pair.create(null, PixLoadStatus.MEDIA_STORE_RETURNED_NULL);
                    }
                } else {
                    return Pair.create(null, PixLoadStatus.IMAGE_COULD_NOT_BE_READ);
                }
            } else if (pathForUri != null) {
                File imageFile = new File(pathForUri);
                if (imageFile.exists()) {
                    if (rotateXDegrees == -1) {
                        rotateXDegrees = getRotationFromFile(pathForUri);
                    }
                    p = ReadFile.readFile(context, imageFile);

                    if (p == null) {
                        return Pair.create(null, PixLoadStatus.IMAGE_FORMAT_UNSUPPORTED);
                    }
                } else {
                    return Pair.create(null, PixLoadStatus.IMAGE_DOES_NOT_EXIST);
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
                        return Pair.create(null, PixLoadStatus.IMAGE_COULD_NOT_BE_READ);
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
                    return Pair.create(null, PixLoadStatus.IMAGE_FORMAT_UNSUPPORTED);
                }
            } else {
                return Pair.create(null, PixLoadStatus.IO_ERROR);
            }

            if (skipCrop && rotateXDegrees > 0 && rotateXDegrees != 360) {
                final Pix pix = Rotate.rotateOrth(p, rotateXDegrees / 90);
                p.recycle();
                p = pix;
                rotateXDegrees = 0;
            }
            return Pair.create(p, PixLoadStatus.SUCCESS);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return Pair.create(null, PixLoadStatus.IMAGE_DOES_NOT_EXIST);
        } catch (IOException e) {
            e.printStackTrace();
            return Pair.create(null, PixLoadStatus.IO_ERROR);
        }
    }

}
