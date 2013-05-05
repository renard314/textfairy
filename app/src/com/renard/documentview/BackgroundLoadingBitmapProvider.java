package com.renard.documentview;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.util.Log;

import com.renard.ocr.DocumentContentProvider;
import com.renard.util.LruCache;
import com.renard.util.Util;

import fi.harism.curl.CurlView;
import fi.harism.curl.CurlView.IndexChangedObserver;
import fi.harism.curl.CurlView.SizeChangedObserver;

public class BackgroundLoadingBitmapProvider implements CurlView.BitmapProvider, SizeChangedObserver, IndexChangedObserver {

	private static final String DEBUG_TAG = BackgroundLoadingBitmapProvider.class.getSimpleName();

	private Cursor mPageCursor;
	private final Context mContext;
	private ExecutorService mBitmapLoader = Executors.newSingleThreadExecutor();
	private int mWidth;
	private int mHeight;
	private BitmapCache mCache = new BitmapCache();

	private static class BitmapCache extends LruCache<Integer, Future<Bitmap>> {

		final private static int cacheSize = 6 * 1024 * 1024;

		public BitmapCache() {
			super(cacheSize);
		}

		@Override
		protected int sizeOf(Integer key, Future<Bitmap> value) {
			try {
				if (value != null) {
					return value.get().getRowBytes() * value.get().getHeight();
				}
				return 0;
			} catch (InterruptedException e) {
				return 0;
			} catch (ExecutionException e) {
				return 0;
			}
		}

		@Override
		protected void entryRemoved(boolean evicted, Integer key, Future<Bitmap> oldValue, Future<Bitmap> newValue) {
			try {
				if (oldValue != null) {
					Bitmap b = oldValue.get();
					if (b != null) {
						b.recycle();
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
	}

	private class BitmapLoadCallable implements Callable<Bitmap> {
		private final int mBitmapWidth;
		private final int mBitmapHeight;
		private final String mImagePath;

		private BitmapLoadCallable(final int width, final int height, final String imagePath) {
			mImagePath = imagePath;
			mBitmapHeight = height;
			mBitmapWidth = width;
		}

		@Override
		public Bitmap call() throws Exception {
			final float start = System.nanoTime();
			Log.i(DEBUG_TAG, "starting loading of " + mImagePath);
			Bitmap b = Util.loadBitmap(mContext, mImagePath, mBitmapWidth, mBitmapHeight);
			Log.i(DEBUG_TAG, "loading of " + mImagePath + " finished after: " + (System.nanoTime() - start));
			return b;
		}
	}

	private String getImagePathFromIndex(int index) {
		if (!mPageCursor.isClosed() && mPageCursor.moveToPosition(index)) {
			final int columIndex = mPageCursor.getColumnIndex(DocumentContentProvider.Columns.PHOTO_PATH);
			return mPageCursor.getString(columIndex);
		}
		return null;
	}

	public BackgroundLoadingBitmapProvider(Cursor pageCursor, Context c) {
		this.mPageCursor = pageCursor;
		this.mContext = c;
	}

	@Override
	public synchronized Bitmap getBitmap(int width, int height, int index) {
		if (height > 0 && width > 0) {
			try {
				Future<Bitmap> f = mCache.get(index);
				if (f != null) {
					return f.get();
				}
				final String path = getImagePathFromIndex(index);
				BitmapLoadCallable task = new BitmapLoadCallable(width, height, path);
				f = mBitmapLoader.submit(task);
				mCache.put(index, f);
				return f.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public int getBitmapCount() {
		return mPageCursor.getCount();
	}

	@Override
	public synchronized void close() {
		mBitmapLoader.shutdownNow();
		mCache.evictAll();
	}

	@Override
	public synchronized void onIndexChanged(int index) {
		for (int i = 1; i > -2; i--) {
			final int realIndex = index + i;
			if (realIndex >= 0 && realIndex < getBitmapCount()) {
				Future<Bitmap> f = mCache.get(index + i);
				if (f == null) {
					final String path = getImagePathFromIndex(realIndex);
					BitmapLoadCallable task = new BitmapLoadCallable(mWidth, mHeight, path);
					f = mBitmapLoader.submit(task);
					mCache.put(realIndex, f);
				}
			}
		}
	}

	public synchronized void clearCache() {
		mCache.evictAll();
	}

	public synchronized void setCursor(final Cursor cursor) {
		mBitmapLoader.shutdownNow();
		mBitmapLoader = Executors.newSingleThreadExecutor();
		mCache.evictAll();
		mPageCursor = cursor;
	}

	@Override
	public synchronized void onSizeChanged(int width, int height) {
		if (width != mWidth && height != mHeight && (width > 0 && height > 0)) {
			mWidth = width;
			mHeight = height;
			mBitmapLoader.shutdownNow();
			mBitmapLoader = Executors.newSingleThreadExecutor();
			mCache.evictAll();
		}
	}

}
