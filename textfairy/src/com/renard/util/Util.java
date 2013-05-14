/*
 * Copyright (C) 2012,2013 Renard Wellnitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.renard.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;

import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.WriteFile;
import com.renard.drawable.FastBitmapDrawable;
import com.renard.ocr.R;
import com.renard.ocr.cropimage.MonitoredActivity;

public class Util {

	public final static String EXTERNAL_APP_DIRECTORY = "textfee";
	private final static String CACHE_DIRECTORY = EXTERNAL_APP_DIRECTORY + "/thumbnails";
	private final static String IMAGE_DIRECTORY = EXTERNAL_APP_DIRECTORY + "/pictures";
	private final static String PDF_DIRECTORY = EXTERNAL_APP_DIRECTORY + "/pdfs";
	public final static String OCR_DATA_DIRECTORY = EXTERNAL_APP_DIRECTORY + "/tessdata";
	
	private final static String THUMBNAIL_SUFFIX = "png";
	public final static int MAX_THUMB_WIDTH = 512;
	public final static int MAX_THUMB_HEIGHT = 512;
	private static final FastBitmapDrawable NULL_DRAWABLE = new FastBitmapDrawable(null);
	public static FastBitmapDrawable sDefaultDocumentThumbnail;

	
	public static int sThumbnailHeight = 0;
	public static int sThumbnailWidth = 0;
	
	public static int determineThumbnailSize(final Activity context, final int[] outNum ){
		DisplayMetrics metrics = new DisplayMetrics();
		context.getWindowManager().getDefaultDisplay().getMetrics(metrics);

		
		final int spacing = context.getResources().getDimensionPixelSize(R.dimen.grid_spacing);
		int minSize = context.getResources().getDimensionPixelSize(R.dimen.min_grid_size)+spacing;
		final int h = metrics.heightPixels;
		final int w = metrics.widthPixels;
		final int maxSize = Math.min(h, w) - spacing;
		minSize = Math.min(maxSize, minSize);
		final int screenWidth = (w-spacing);
		final int num = (int) Math.max(2, Math.floor((double) (screenWidth) / minSize));		//i want at least 2 columns. if more than 2 columns are possible each document must be minSize pixels wide
		
		
		int columnWidth = (screenWidth-num*spacing)/num;
		if (columnWidth > (screenWidth-spacing)){
			columnWidth = screenWidth-spacing;
		}
		
		if (outNum!=null){
			outNum[0] = num;
		}
		return columnWidth;
	}
	
		
	public static void setThumbnailSize(final int w, final int h, final Context c){
		 Drawable drawable = c.getResources().getDrawable(R.drawable.default_thumbnail);
		 drawable.setBounds(0,0,w,h);
		 Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		 final Canvas canvas = new Canvas(b);
		 drawable.draw(canvas);
		 sDefaultDocumentThumbnail = new FastBitmapDrawable(b);
		sThumbnailHeight = h;
		sThumbnailWidth = w;
	}

	private static class ThumbnailCache extends LruCache<Integer, FastBitmapDrawable> {

		final private static int cacheSize = 10 * 1024 * 1024;

		public ThumbnailCache() {
			super(cacheSize);
		}

		@Override
		protected int sizeOf(Integer key, FastBitmapDrawable value) {
			if (value.getBitmap() != null) {
				return value.getBitmap().getRowBytes() * value.getBitmap().getHeight();
			}
			return 0;
		}

		@Override
		protected void entryRemoved(boolean evicted, Integer key, FastBitmapDrawable oldValue, FastBitmapDrawable newValue) {
			oldValue.getBitmap().recycle();
		}
	}

	private static ThumbnailCache mCache = new ThumbnailCache();

	private static Bitmap loadDocumentThumbnail(int documentId) {
		Log.i("cache", "loadDocumentThumbnail " + documentId);

		File thumbDir = new File(Environment.getExternalStorageDirectory(), CACHE_DIRECTORY);
		File thumbFile = new File(thumbDir, String.valueOf(documentId)+ "." + THUMBNAIL_SUFFIX);
		if (thumbFile.exists()) {
			InputStream stream = null;
			try {
				stream = new FileInputStream(thumbFile);
				BitmapFactory.Options opts = new Options();
				opts.inPreferredConfig = Bitmap.Config.RGB_565;
				return BitmapFactory.decodeStream(stream, null, opts);
			} catch (FileNotFoundException e) {
				// Ignore
			} finally {
				try {
					if (stream != null) {
						stream.close();
					}
				} catch (IOException ignore) {
				}
			}
		}
		return null;
	}

	public static FastBitmapDrawable getDocumentThumbnail(int documentId) {
		FastBitmapDrawable drawable = null;

		drawable = mCache.get(documentId);

		if (drawable == null) {
			final Bitmap bitmap = loadDocumentThumbnail(documentId);
			if (bitmap != null) {
				drawable = new FastBitmapDrawable(bitmap);
			} else {
				drawable = NULL_DRAWABLE;
			}
			mCache.put(documentId, drawable);
		}
		return drawable == NULL_DRAWABLE ? sDefaultDocumentThumbnail : drawable;
	}

	public static Bitmap loadBitmap(final Context c, final String imagePath, final int desiredWith, final int desiredHeight){
		int w = Math.min(1024, desiredWith);
		int h = Math.min(1024, desiredHeight);
		Bitmap orgBitmap =  Util.decodeFile(imagePath,w, h);
		return Util.adjustBitmapSize(desiredWith, desiredHeight, orgBitmap);
	}
	
	/**
	 * recycles orgBitmap
	 * @param width
	 * @param height
	 * @param orgBitmap
	 * @return
	 */
	public static Bitmap adjustBitmapSize(int width, int height, Bitmap orgBitmap) {
		Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		b.eraseColor(Color.TRANSPARENT);
		Canvas c = new Canvas(b);

		int border = 2;
		Rect r = new Rect(0, 0, width, height);

		int imageWidth = r.width() - (border * 2);
		int imageHeight = imageWidth * orgBitmap.getHeight() / orgBitmap.getWidth();
		if (imageHeight > r.height() - (border * 2)) {
			imageHeight = r.height() - (border * 2);
			imageWidth = imageHeight * orgBitmap.getWidth() / orgBitmap.getHeight();
		}

		r.left += ((r.width() - imageWidth) / 2) - border;
		r.right = r.left + imageWidth + border + border;
		r.top += ((r.height() - imageHeight) / 2) - border;
		r.bottom = r.top + imageHeight + border + border;

		Paint p = new Paint();
		p.setColor(0xFFC0C0C0);
		c.drawRect(r, p);
		r.left += border;
		r.right -= border;
		r.top += border;
		r.bottom -= border;

		c.drawBitmap(orgBitmap, null, r, null);
		orgBitmap.recycle();
		return b;
	}

	/**
	 * reads the specified Bitmap from the SD card and scales it down so that
	 * resulting bitmap width nor height exceeds maxSize see also:
	 * http://stackoverflow
	 * .com/questions/477572/android-strange-out-of-memory-issue/823966#823966
	 * 
	 * @param f
	 * @param maxSize
	 * @return
	 */
	public static Bitmap decodeFile(String imagePath, int maxWidth, int maxHeight) {
		Bitmap b = null;
		// Decode image size
		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;

		BitmapFactory.decodeFile(imagePath, o);
		int scale = determineScaleFactor(o.outWidth, o.outHeight, maxWidth, maxHeight);

		// Decode with inSampleSize
		o.inSampleSize = scale;
		o.inJustDecodeBounds = false;
		o.inPreferredConfig = Bitmap.Config.RGB_565;
		b = BitmapFactory.decodeFile(imagePath, o);
		return b;
	}

	public static int determineScaleFactor(int w, int h, int maxWidth, int maxHeight) {
		int scale = 1;
		if (w > maxWidth || h > maxHeight) {
			scale = (int) Math.pow(2, (int) Math.round(Math.log(Math.max(maxWidth, maxHeight) / (double) Math.max(h, w)) / Math.log(0.5)));
		}
		return scale;
	}

	/***
	 * returns file path for the image at the given uri
	 * 
	 * @param context
	 * @param uri
	 * @return
	 */
	public static String getPathForUri(Context context, Uri uri) {
		final String scheme = uri.getScheme();
		if (scheme == null){
			return uri.getPath();
		}
		if (scheme.equals("content")) {
			ContentResolver resolver = context.getContentResolver();
			if (resolver==null){
				return null;
			}
			Cursor cursor = resolver.query(uri, null, null, null, null);
			try {
				if (cursor.moveToFirst()) {
					final int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
					if(idx!=-1){
						String absoluteFilePath = cursor.getString(idx);
						return absoluteFilePath;
					}
				}
				return null;
			} finally {
				cursor.close();
			}
		} else if (scheme.equals("file")){
			return uri.getPath();
		}
		return null;

	}

	/**
	 * reads the orientiation from the exif data
	 * 
	 * @param filepath
	 * @return
	 */
	public static int getExifOrientation(String filepath) {
		int degree = 0;
		ExifInterface exif = null;
		try {
			exif = new ExifInterface(filepath);
		} catch (IOException ex) {
			Log.e("", "cannot read exif", ex);
		}
		if (exif != null) {
			int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
			if (orientation != -1) {
				// We only recognize a subset of orientation tag values.
				switch (orientation) {
				case ExifInterface.ORIENTATION_ROTATE_90:
					degree = 90;
					break;
				case ExifInterface.ORIENTATION_ROTATE_180:
					degree = 180;
					break;
				case ExifInterface.ORIENTATION_ROTATE_270:
					degree = 270;
					break;
				}
			}
		}
		return degree;
	}

	public static File savePixToSD(final Pix pix,final String name) throws IOException {
		final String fileName = name + ".jpg";

		File picdir = new File(Environment.getExternalStorageDirectory(), IMAGE_DIRECTORY);
		if (!picdir.exists()) {
			if (!picdir.mkdirs()) {
				throw new IOException();
			}
			createNoMediaFile(picdir);
		}
		File image = new File(picdir, fileName);

		try {
			WriteFile.writeImpliedFormat(pix, image, 85, true);
		} catch (Exception e) {
			throw new IOException(e);
		}
		
		return image;
	}
	
	private static void createNoMediaFile(final File parentDir){
		File noMedia = new File(parentDir, ".nomedia");
		try {
			noMedia.createNewFile();
		} catch (IOException ignore) {
		}
	}
	
	
	public static String getTessDir(){
		return new File(Environment.getExternalStorageDirectory(),EXTERNAL_APP_DIRECTORY ).getPath() + "/";				
	}
	public static File getPDFDir(){
		File dir = new File(Environment.getExternalStorageDirectory(), PDF_DIRECTORY);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}

	/**
	 * creates a thumbnail file and puts it into the in memory cache
	 * 
	 * @param image
	 * @param documentId
	 */
	public static void createThumbnail(final Context context, final File image, final int documentId) {
		Bitmap source = Util.decodeFile(image.getPath(), MAX_THUMB_WIDTH, MAX_THUMB_HEIGHT);
		//Bitmap thumb =  Util.extractMiniThumb(source, width, height);
		//final int color = context.getResources().getColor(R.color.document_element_background);
		Bitmap thumb = Util.adjustBitmapSize(sThumbnailWidth, sThumbnailHeight, source);

		if (thumb != null) {

			FastBitmapDrawable drawable = new FastBitmapDrawable(thumb);
			mCache.put(documentId, drawable);
			File thumbDir = new File(Environment.getExternalStorageDirectory(), CACHE_DIRECTORY);
			if (!thumbDir.exists()) {
				thumbDir.mkdirs();
				createNoMediaFile(thumbDir);
			}
			File thumbFile = new File(thumbDir, String.valueOf(documentId) + "." + THUMBNAIL_SUFFIX);
			FileOutputStream out;
			try {
				out = new FileOutputStream(thumbFile);
				thumb.compress(Bitmap.CompressFormat.PNG, 85, out);
			} catch (FileNotFoundException ignore) {
			}
		}
	}

	// public static File saveImageToSD(Context context, byte[] jpegData, String
	// id) {
	// final String fileName = "Scan" + id + ".jpg";
	// Toast errortToast = Toast.makeText(context,
	// context.getText(R.string.error_create_file), Toast.LENGTH_LONG);
	//
	// // FileOutputStream out = openFileOutput(fileName,
	// // MODE_WORLD_WRITEABLE);
	// File dir =
	// Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES
	// + "/ScanDroid");
	// if (!dir.exists()) {
	// if (!dir.mkdir()) {
	// errortToast.sho// public static void updateDocumentWithImageUri(Context
	// context, Uri docUri, Uri imageUri) {
	// ContentValues v = new ContentValues();
	// v.put(Columns.PHOTO_URI, imageUri.toString());
	// context.getContentResolver().update(docUri, v, null, null);
	// }

	// public static Uri addImageToGallery(Context context, File imageFile/*
	// * ,
	// * int[]
	// * degree
	// */) {
	//
	// // int degree[] = new int[1];
	// // degree[0]= getExifOrientation(imageFile.getPath());
	// long size = imageFile.length();
	// ContentValues values = new ContentValues(7);
	//
	// // That filename is what will be handed to Gmail when a user shares a
	// // photo. Gmail gets the name of the picture attachment from the
	// // "DISPLAY_NAME" field.
	// values.put(Images.Media.DISPLAY_NAME, imageFile.getName());
	// values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());
	// values.put(Images.Media.MIME_TYPE, "image/jpeg");
	// // values.put(Images.Media.ORIENTATION, degree[0]);
	// values.put(Images.Media.DATA, imageFile.getPath());
	// values.put(Images.Media.SIZE, size);
	// return
	// context.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI,
	// values);
	// }w();
	// return null;
	// }
	// }
	// File image = new File(dir, fileName);
	// OutputStream outputStream = null;
	//
	// try {
	// outputStream = new FileOutputStream(image);
	// outputStream.write(jpegData);
	// return image;
	// } catch (FileNotFoundException exc) {
	// errortToast.show();
	// exc.printStackTrace();
	// } catch (IOException exc) {
	// errortToast.show();
	// exc.printStackTrace();
	// } finally {
	// if (outputStream != null) {
	// try {
	// outputStream.close();
	// } catch (IOException exc) {
	// // ignore
	// }
	// }
	// }
	// return null;
	// }

	// public static void updateDocumentWithImageUri(Context context, Uri
	// docUri, Uri imageUri) {
	// ContentValues v = new ContentValues();
	// v.put(Columns.PHOTO_URI, imageUri.toString());
	// context.getContentResolver().update(docUri, v, null, null);
	// }

	// public static Uri addImageToGallery(Context context, File imageFile/*
	// * ,
	// * int[]
	// * degree
	// */) {
	//
	// // int degree[] = new int[1];
	// // degree[0]= getExifOrientation(imageFile.getPath());
	// long size = imageFile.length();
	// ContentValues values = new ContentValues(7);
	//
	// // That filename is what will be handed to Gmail when a user shares a
	// // photo. Gmail gets the name of the picture attachment from the
	// // "DISPLAY_NAME" field.
	// values.put(Images.Media.DISPLAY_NAME, imageFile.getName());
	// values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());
	// values.put(Images.Media.MIME_TYPE, "image/jpeg");
	// // values.put(Images.Media.ORIENTATION, degree[0]);
	// values.put(Images.Media.DATA, imageFile.getPath());
	// values.put(Images.Media.SIZE, size);
	// return
	// context.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI,
	// values);
	// }

	// /**
	// * saves the image on the sd card and adds it to the gallery
	// *
	// * @param context
	// * @param jpegData
	// * @param docUri
	// * uri to the document
	// * @param degree
	// * return value for the orientation of the picture
	// * @return
	// */
	// public static Pix saveImage(Context context, byte[] jpegData, Uri docUri,
	// int[] degree) {
	// File imageFile = saveImageToSD(context, jpegData,
	// docUri.getLastPathSegment());
	// Uri imageUri = addImageToGallery(context, imageFile);
	// updateDocumentWithImageUri(context, docUri, imageUri);
	// return ReadFile.readMem(jpegData);
	// }
	// ContentResolver crThumb = context.getContentResolver();
	// BitmapFactory.Options options = new BitmapFactory.Options();
	// options.inSampleSize = 2;
	// long id = Long.valueOf(uri.getLastPathSegment());
	// Bitmap thumb = MediaStore.Images.Thumbnails.getThumbnail(crThumb, id,
	// MediaStore.Images.Thumbnails.MINI_KIND, null);
	// if (thumb != null) {
	// return ImageUtils.adjustBitmapSize(150, 150, thumb);
	// } else {
	// return null;
	// }
	// public static Bitmap loadBitmap(int width, int height, Uri imageUri,
	// Context c) {
	// String imagePath = getPathForUri(c, imageUri);
	// return loadBitmap(width, height, imagePath);
	// }
    /*
     * Compute the sample size as a function of minSideLength
     * and maxNumOfPixels.
     * minSideLength is used to specify that minimal width or height of a bitmap.
     * maxNumOfPixels is used to specify the maximal size in pixels that are tolerable
     * in terms of memory usage.
     *
     * The function returns a sample size based on the constraints.
     * Both size and minSideLength can be passed in as IImage.UNCONSTRAINED,
     * which indicates no care of the corresponding constraint.
     * The functions prefers returning a sample size that
     * generates a smaller bitmap, unless minSideLength = IImage.UNCONSTRAINED.
     */
    

    private static Bitmap transform(Matrix scaler,
                                   Bitmap source,
                                   int targetWidth,
                                   int targetHeight,
                                   boolean scaleUp) {
        int deltaX = source.getWidth() - targetWidth;
        int deltaY = source.getHeight() - targetHeight;
        if (!scaleUp && (deltaX < 0 || deltaY < 0)) {
            /*
             * In this case the bitmap is smaller, at least in one dimension,
             * than the target.  Transform it by placing as much of the image
             * as possible into the target and leaving the top/bottom or
             * left/right (or both) black.
             */
            Bitmap b2 = Bitmap.createBitmap(targetWidth, targetHeight,
                    Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b2);

            int deltaXHalf = Math.max(0, deltaX / 2);
            int deltaYHalf = Math.max(0, deltaY / 2);
            Rect src = new Rect(
                    deltaXHalf,
                    deltaYHalf,
                    deltaXHalf + Math.min(targetWidth, source.getWidth()),
                    deltaYHalf + Math.min(targetHeight, source.getHeight()));
            int dstX = (targetWidth  - src.width())  / 2;
            int dstY = (targetHeight - src.height()) / 2;
            Rect dst = new Rect(
                    dstX,
                    dstY,
                    targetWidth - dstX,
                    targetHeight - dstY);
            c.drawBitmap(source, src, dst, null);
            return b2;
        }
        float bitmapWidthF = source.getWidth();
        float bitmapHeightF = source.getHeight();

        float bitmapAspect = bitmapWidthF / bitmapHeightF;
        float viewAspect   = (float) targetWidth / targetHeight;

        if (bitmapAspect > viewAspect) {
            float scale = targetHeight / bitmapHeightF;
            if (scale < .9F || scale > 1F) {
                scaler.setScale(scale, scale);
            } else {
                scaler = null;
            }
        } else {
            float scale = targetWidth / bitmapWidthF;
            if (scale < .9F || scale > 1F) {
                scaler.setScale(scale, scale);
            } else {
                scaler = null;
            }
        }

        Bitmap b1;
        if (scaler != null) {
            // this is used for minithumb and crop, so we want to filter here.
            b1 = Bitmap.createBitmap(source, 0, 0,
                    source.getWidth(), source.getHeight(), scaler, true);
        } else {
            b1 = source;
        }

        int dx1 = Math.max(0, b1.getWidth() - targetWidth);
        int dy1 = Math.max(0, b1.getHeight() - targetHeight);

        Bitmap b2 = Bitmap.createBitmap(
                b1,
                dx1 / 2,
                dy1 / 2,
                targetWidth,
                targetHeight);

        if (b1 != source) {
            b1.recycle();
        }

        return b2;
    }

    /**
     * Creates a centered bitmap of the desired size. Recycles the input.
     * @param source
     */
    public static Bitmap extractMiniThumb(
            Bitmap source, int width, int height) {
        return Util.extractMiniThumb(source, width, height, true);
    }

    public static Bitmap extractMiniThumb(
            Bitmap source, int width, int height, boolean recycle) {
        if (source == null) {
            return null;
        }

        float scale;
        if (source.getWidth() < source.getHeight()) {
            scale = width / (float) source.getWidth();
        } else {
            scale = height / (float) source.getHeight();
        }
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        Bitmap miniThumbnail = transform(matrix, source, width, height, true);

        if (recycle && miniThumbnail != source) {
            source.recycle();
        }
        return miniThumbnail;
    }

 
    private static class BackgroundJob extends MonitoredActivity.LifeCycleAdapter implements Runnable {

        private final MonitoredActivity mActivity;
        private final ProgressDialog mDialog;
        private final Runnable mJob;
        private final Handler mHandler;
        private final Runnable mCleanupRunner = new Runnable() {
            public void run() {
                mActivity.removeLifeCycleListener(BackgroundJob.this);
                if (mDialog.getWindow() != null) mDialog.dismiss();
            }
        };

        public BackgroundJob(MonitoredActivity activity, Runnable job,
                ProgressDialog dialog, Handler handler) {
            mActivity = activity;
            mDialog = dialog;
            mJob = job;
            mActivity.addLifeCycleListener(this);
            mHandler = handler;
        }

        public void run() {
            try {
                mJob.run();
            } finally {
                mHandler.post(mCleanupRunner);
            }
        }


        @Override
        public void onActivityDestroyed(MonitoredActivity activity) {
            // We get here only when the onDestroyed being called before
            // the mCleanupRunner. So, run it now and remove it from the queue
            mCleanupRunner.run();
            mHandler.removeCallbacks(mCleanupRunner);
        }

        @Override
        public void onActivityStopped(MonitoredActivity activity) {
            mDialog.hide();
        }

        @Override
        public void onActivityStarted(MonitoredActivity activity) {
            mDialog.show();
        }
    }
    
	/**
	 * @return the free space on sdcard in bytes
	 */
	public static long GetFreeSpaceB() {
		try {
			String storageDirectory = Environment.getExternalStorageDirectory().toString();
			StatFs stat = new StatFs(storageDirectory);
			long availableBlocks = stat.getAvailableBlocks();
			return availableBlocks * stat.getBlockSize();
		} catch (Exception ex) {
			return -1;
		}
	}

    public static void startBackgroundJob(MonitoredActivity activity,
            String title, String message, Runnable job, Handler handler) {
        // Make the progress dialog uncancelable, so that we can gurantee
        // the thread will be done before the activity getting destroyed.
        ProgressDialog dialog = ProgressDialog.show(
                activity, title, message, true, false);
        new Thread(new BackgroundJob(activity, job, dialog, handler)).start();
    }  

}
