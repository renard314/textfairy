/*
 * Copyright (C) 2008 The Android Open Source Project, Romain Guy
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

package com.renard.ocr.documents.viewing.grid;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.SystemClock;


public class CrossFadeDrawable extends Drawable {

	private static final int TRANSITION_STARTING = 0;
	private static final int TRANSITION_RUNNING = 1;
	private static final int TRANSITION_NONE = 2;

	private int mTransitionState = TRANSITION_NONE;

	private boolean mCrossFade;
	private boolean mReverse;
	private long mStartTimeMillis;
	private int mFrom;
	private int mTo;
	private int mDuration;
	private int mOriginalDuration;
	private int mAlpha;

	private Bitmap mStart;
	private Bitmap mEnd;

	private final Paint mStartPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
	private final Paint mEndPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

	private float mStartX;
	private float mStartY;
	private float mEndX;
	private float mEndY;

	private final Handler mHandler;
	private final Runnable mInvalidater;

	public CrossFadeDrawable(Bitmap start, Bitmap end) {
		mStart = start;
		mEnd = end;
		mHandler = new Handler();
		mInvalidater = new Runnable() {
			public void run() {
				invalidateSelf();
			}
		};
	}

	/**
	 * Begin the second layer on top of the first layer.
	 * 
	 * @param durationMillis
	 *            The length of the transition in milliseconds
	 */
	public void startTransition(int durationMillis) {
		mFrom = 0;
		mTo = 255;
		mAlpha = 0;
		mOriginalDuration = mDuration = durationMillis;
		mReverse = false;
		mTransitionState = mStart != mEnd ? TRANSITION_STARTING : TRANSITION_NONE;
		invalidateSelf();
	}

	/**
	 * Show only the first layer.
	 */
	public void resetTransition() {
		mAlpha = 0;
		mTransitionState = TRANSITION_NONE;
		invalidateSelf();
	}

	/**
	 * Reverses the transition, picking up where the transition currently is. If
	 * the transition is not currently running, this will start the transition
	 * with the specified duration. If the transition is already running, the
	 * last known duration will be used.
	 * 
	 * @param duration
	 *            The duration to use if no transition is running.
	 */
	public void reverseTransition(int duration) {
		final long time = SystemClock.uptimeMillis();

		if (time - mStartTimeMillis > mOriginalDuration) {
			if (mAlpha == 0) {
				mFrom = 0;
				mTo = 255;
				mAlpha = 0;
				mReverse = false;
			} else {
				mFrom = 255;
				mTo = 0;
				mAlpha = 255;
				mReverse = true;
			}
			mDuration = mOriginalDuration = duration;
			mTransitionState = TRANSITION_STARTING;
			mHandler.post(mInvalidater);
			return;
		}

		mReverse = !mReverse;
		mFrom = mAlpha;
		mTo = mReverse ? 0 : 255;
		mDuration = (int) (mReverse ? time - mStartTimeMillis : mOriginalDuration - (time - mStartTimeMillis));
		mTransitionState = TRANSITION_STARTING;
	}

	@Override
	public void draw(Canvas canvas) {
		boolean done = true;

		switch (mTransitionState) {
		case TRANSITION_STARTING:
			mStartTimeMillis = SystemClock.uptimeMillis();
			done = false;
			mTransitionState = TRANSITION_RUNNING;
			break;
		case TRANSITION_RUNNING:
			if (mStartTimeMillis >= 0) {
				float normalized = (float) (SystemClock.uptimeMillis() - mStartTimeMillis) / mDuration;

				done = normalized >= 1.0f;
				mAlpha = (int) (mFrom + (mTo - mFrom) * Math.min(normalized, 1.0f));

				if (done) {
					mTransitionState = TRANSITION_NONE;
					mHandler.post(mInvalidater);
				}
			}
			break;
		}

		final int alpha = mAlpha;
		final boolean crossFade = mCrossFade;

		Bitmap bitmap = mStart;
		Paint paint = mStartPaint;

		if (!crossFade || 255 - alpha > 0) {
			if (crossFade) {
				paint.setAlpha(255 - alpha);
			}
			canvas.drawBitmap(bitmap, mStartX, mStartY, paint);
			if (crossFade) {
				paint.setAlpha(0xFF);
			}
		}

		if (alpha > 0) {
			bitmap = mEnd;
			paint = mEndPaint;
			paint.setAlpha(alpha);
			canvas.drawBitmap(bitmap, mEndX, mEndY, paint);
			paint.setAlpha(0xFF);
		}

		if (!done) {			
			mHandler.post(mInvalidater);
		}
	}

	Bitmap getStart() {
		return mStart;
	}

	void setStart(Bitmap start) {
		mStart = start;
		invalidateSelf();
	}

	Bitmap getEnd() {
		return mEnd;
	}

	public void setEnd(Bitmap end) {
		mEnd = end;
		invalidateSelf();
	}

	@Override
	public void setBounds(int left, int top, int right, int bottom) {
		super.setBounds(left, top, right, bottom);

		final int width = right - left;
		final int height = bottom - top;

		mStartX = (width - mStart.getWidth()) / 2.0f;
		mStartY = height - mStart.getHeight();

		mEndX = (width - mEnd.getWidth()) / 2.0f;
		mEndY = height - mEnd.getHeight();
	}

	@Override
	public int getIntrinsicWidth() {
		return Math.max(mStart.getWidth(), mEnd.getWidth());
	}

	@Override
	public int getIntrinsicHeight() {
		return Math.max(mStart.getHeight(), mEnd.getHeight());
	}

	@Override
	public int getMinimumWidth() {
		return Math.max(mStart.getWidth(), mEnd.getWidth());
	}

	@Override
	public int getMinimumHeight() {
		return Math.max(mStart.getHeight(), mEnd.getHeight());
	}

	@Override
	public void setDither(boolean dither) {
		mStartPaint.setDither(true);
		mEndPaint.setDither(true);
	}

	@Override
	public void setFilterBitmap(boolean filter) {
		mStartPaint.setFilterBitmap(true);
		mEndPaint.setFilterBitmap(true);
	}

	/**
	 * Ignored.
	 */
	public void setAlpha(int alpha) {
	}

	public void setColorFilter(ColorFilter cf) {
		mStartPaint.setColorFilter(cf);
		mEndPaint.setColorFilter(cf);
	}

	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}

	/**
	 * Enables or disables the cross fade of the drawables. When cross fade is
	 * disabled, the first drawable is always drawn opaque. With cross fade
	 * enabled, the first drawable is drawn with the opposite alpha of the
	 * second drawable.
	 * 
	 * @param enabled
	 *            True to enable cross fading, false otherwise.
	 */
	public void setCrossFadeEnabled(boolean enabled) {
		mCrossFade = enabled;
	}

	/**
	 * Indicates whether the cross fade is enabled for this transition.
	 * 
	 * @return True if cross fading is enabled, false otherwise.
	 */
	boolean isCrossFadeEnabled() {
		return mCrossFade;
	}
}
