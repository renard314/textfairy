/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.renard.ocr.cropimage;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.googlecode.leptonica.android.Box;
import com.googlecode.leptonica.android.Clip;
import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.Rotate;
import com.googlecode.leptonica.android.Scale;
import com.googlecode.leptonica.android.WriteFile;
import com.renard.ocr.DocumentGridActivity;
import com.renard.ocr.R;
import com.renard.ocr.help.HintDialog;
import com.renard.util.Util;

/**
 * The activity can crop specific region of interest from an image.
 */
public class CropImage extends MonitoredActivity {
	private static final int HINT_DIALOG_ID = 2;

	private final int SCALE_FACTOR = 4;
	private int mAspectX, mAspectY;
	private final Handler mHandler = new Handler();

	private int mRotation = 0;

	boolean mSaving; // Whether the "save" button is already clicked.
	private Pix mPix; // original Picture
	private Pix mPixScaled; // scaled Picture
	private CropImageView mImageView;

	private Bitmap mBitmap;
	HighlightView mCrop;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		getWindow().setFormat(PixelFormat.RGBA_8888);
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.cropimage_activity);

		// getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		// getSupportActionBar().setDisplayShowHomeEnabled(true);

		mImageView = (CropImageView) findViewById(R.id.image);
		mImageView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

			@Override
			public void onGlobalLayout() {
				Intent intent = getIntent();
				Bundle extras = intent.getExtras();
				if (extras != null) {
					// mImagePath = extras.getString("image-path");
					// mSaveUri = Uri.fromFile(new File(mImagePath));
					// mBitmap = getBitmap(mImagePath);
					mPix = new Pix(extras.getInt(DocumentGridActivity.EXTRA_NATIVE_PIX));

					// scale it so that it fits the screen
					
					float scaleFactor = getScaleFactorToFitScreen(mPix, mImageView.getWidth(), mImageView.getHeight());
					mPixScaled = Scale.scale(mPix, scaleFactor);

					mBitmap = WriteFile.writeBitmap(mPixScaled);
					mAspectX = extras.getInt("aspectX");
					mAspectY = extras.getInt("aspectY");
					mRotation = extras.getInt(DocumentGridActivity.EXTRA_ROTATION) / 90;
				}

				if (mBitmap == null) {
					finish();
					return;
				}

				mImageView.setImageBitmapResetBase(mBitmap, true, mRotation * 90);
				makeDefault();
				mImageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}

		});

		initAppIcon(this, HINT_DIALOG_ID);
	}

	private float getScaleFactorToFitScreen(Pix mPix, int vwidth, int vheight) {
		float scale;
		float dx;
		float dy;
		int dwidth = mPix.getWidth();
		int dheight = mPix.getHeight();
		if (dwidth <= vwidth && dheight <= vheight) {
			scale = 1.0f;
		} else {
			scale = Math.min((float) vwidth / (float) dwidth, (float) vheight / (float) dheight);
		}

		//dx = (vwidth - dwidth * scale) * 0.5f;
		//dy = (vheight - dheight * scale) * 0.5f;

		//mDrawMatrix.setScale(scale, scale);
		//mDrawMatrix.postTranslate(dx, dy);
		return scale;
	}

	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		switch (id) {
		case HINT_DIALOG_ID:
			return HintDialog.createDialog(this, R.string.crop_help_title, "file:///android_res/raw/crop_help.html");
		}
		return super.onCreateDialog(id, args);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.item_save) {
			onSaveClicked();
			return true;
		} else if (itemId == R.id.item_rotate_right) {
			onRotateClicked(1);
			return true;
		} else if (itemId == R.id.item_rotate_left) {
			onRotateClicked(-1);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.crop_image_options, menu);

		/*
		 * menu.add(R.string.continue_ocr) .setIcon(R.drawable.ic_action_save)
		 * .setOnMenuItemClickListener(new OnMenuItemClickListener() {
		 * 
		 * @Override public boolean onMenuItemClick(MenuItem item) {
		 * onSaveClicked(); return true; } })
		 * .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS |
		 * MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		 * 
		 * menu.add("") .setIcon(R.drawable.ic_action_rotate_right)
		 * .setOnMenuItemClickListener(new OnMenuItemClickListener() {
		 * 
		 * @Override public boolean onMenuItemClick(MenuItem item) {
		 * onRotateClicked(1); return true; } })
		 * .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		 * 
		 * menu.add("") .setIcon(R.drawable.ic_action_rotate_left)
		 * .setOnMenuItemClickListener(new OnMenuItemClickListener() {
		 * 
		 * @Override public boolean onMenuItemClick(MenuItem item) {
		 * onRotateClicked(-1); return true; } })
		 * .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		 */
		return true;
	}

	private void onRotateClicked(int delta) {
		if (delta < 0) {
			delta = -delta * 3;
		}
		mRotation += delta;
		mRotation = mRotation % 4;
		mImageView.setImageBitmapResetBase(mBitmap, false, mRotation * 90);
		makeDefault();
	}

	private Box adjustBoundsToMultipleOf4(int left, int top, int width, int height) {
		int newLeft = left;
		int newTop = top;
		int newRight = left + width;
		int newBottom = top + height;

		int wDiff = width % 4;
		int hDiff = height % 4;
		for (int i = 0; i < wDiff; i++) {
			if (i % 2 == 0) {
				newLeft++;
			} else {
				newRight--;
			}
		}
		for (int i = 0; i < hDiff; i++) {
			if (i % 2 == 0) {
				newTop++;
			} else {
				newBottom--;
			}
		}
		return new Box(newLeft, newTop, newRight - newLeft, newBottom - newTop);

	}

	private void onSaveClicked() {
		if (mSaving)
			return;

		if (mCrop == null) {
			return;
		}

		mSaving = true;

		Util.startBackgroundJob(this, null, getText(R.string.cropping_image).toString(), new Runnable() {
			public void run() {
				try {
					Rect r = mCrop.getCropRect();
					/*
					 * during image analysing image will be scaled to 1/4 of its
					 * size to compute the halftone mask
					 */
					Box boundingBox = adjustBoundsToMultipleOf4(r.left * SCALE_FACTOR, r.top * SCALE_FACTOR, (r.right - r.left) * SCALE_FACTOR, (r.bottom - r.top) * SCALE_FACTOR);
					Pix croppedPix = Clip.clipRectangle(mPix, boundingBox);
					if (croppedPix == null) {
						throw new IllegalStateException();
					}
					if (mRotation != 0 && mRotation != 4) {
						Pix rotatedPix = Rotate.rotateOrth(croppedPix, mRotation);
						croppedPix.recycle();
						croppedPix = rotatedPix;
					}
					if (croppedPix == null) {
						throw new IllegalStateException();
					}
					Intent result = new Intent();
					result.putExtra(DocumentGridActivity.EXTRA_NATIVE_PIX, croppedPix.getNativePix());
					setResult(RESULT_OK, result);
				} catch (IllegalStateException e) {
					setResult(RESULT_CANCELED);
				} finally {
					mPix.recycle();
					finish();
				}
			}
		}, mHandler);

	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		setResult(RESULT_CANCELED);
		mPix.recycle();
	}

	@Override
	protected void onDestroy() {
		mPixScaled.recycle();
		super.onDestroy();
	}

	// Create a default HightlightView if we found no face in the picture.
	private void makeDefault() {
		HighlightView hv = new HighlightView(mImageView);

		int width = mBitmap.getWidth();
		int height = mBitmap.getHeight();

		Rect imageRect = new Rect(0, 0, width, height);

		// make the default size about 4/5 of the width or height
		int cropWidth = Math.min(width, height) * 4 / 5;
		int cropHeight = cropWidth;

		if (mAspectX != 0 && mAspectY != 0) {
			if (mAspectX > mAspectY) {
				cropHeight = cropWidth * mAspectY / mAspectX;
			} else {
				cropWidth = cropHeight * mAspectX / mAspectY;
			}
		}

		int x = (width - cropWidth) / 2;
		int y = (height - cropHeight) / 2;

		RectF cropRect = new RectF(x, y, x + cropWidth, y + cropHeight);
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

		hv.setup(mImageView.getImageViewMatrix(), imageRect, cropRect, mAspectX != 0 && mAspectY != 0, metrics.density);
		mImageView.add(hv);
		mImageView.invalidate();
		mCrop = hv;
		mCrop.setFocus(true);
	}

}

class CropImageView extends ImageViewTouchBase {
	HighlightView mHighlightView;
	HighlightView mMotionHighlightView = null;
	float mLastX, mLastY;
	int mMotionEdge;

	private Context mContext;

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (mBitmapDisplayed.getBitmap() != null) {
			mHighlightView.mMatrix.set(getImageMatrix());
			mHighlightView.invalidate();
			if (mHighlightView.mIsFocused) {
				centerBasedOnHighlightView(mHighlightView);
			}
		}
	}

	public CropImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.mContext = context;
	}

	@Override
	protected void zoomTo(float scale, float centerX, float centerY) {
		super.zoomTo(scale, centerX, centerY);
		mHighlightView.mMatrix.set(getImageMatrix());
		mHighlightView.invalidate();
	}

	@Override
	protected void zoomIn() {
		super.zoomIn();
		mHighlightView.mMatrix.set(getImageMatrix());
		mHighlightView.invalidate();
	}

	@Override
	protected void zoomOut() {
		super.zoomOut();
		mHighlightView.mMatrix.set(getImageMatrix());
		mHighlightView.invalidate();
	}

	@Override
	protected void postTranslate(float deltaX, float deltaY) {
		super.postTranslate(deltaX, deltaY);
		mHighlightView.mMatrix.postTranslate(deltaX, deltaY);
		mHighlightView.invalidate();
	}

	private float[] mapPointToImageSpace(float x, float y) {
		float[] p = new float[2];
		Matrix m = getImageViewMatrix();
		Matrix m2 = new Matrix();
		m.invert(m2);
		p[0] = x;
		p[1] = y;
		m2.mapPoints(p);
		return p;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		CropImage cropImage = (CropImage) mContext;
		if (cropImage.mSaving) {
			return false;
		}
		float[] mappedPoint = mapPointToImageSpace(event.getX(), event.getY());

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			int edge = mHighlightView.getHit(mappedPoint[0], mappedPoint[1], getScale());
			if (edge != HighlightView.GROW_NONE) {
				mMotionEdge = edge;
				mMotionHighlightView = mHighlightView;
				mLastX = mappedPoint[0];
				mLastY = mappedPoint[1];
				mMotionHighlightView.setMode((edge == HighlightView.MOVE) ? HighlightView.ModifyMode.Move : HighlightView.ModifyMode.Grow);
				break;
			}
			break;
		case MotionEvent.ACTION_UP:
			if (mMotionHighlightView != null) {
				centerBasedOnHighlightView(mMotionHighlightView);
				mMotionHighlightView.setMode(HighlightView.ModifyMode.None);
			}
			mMotionHighlightView = null;
			break;
		case MotionEvent.ACTION_MOVE:
			if (mMotionHighlightView != null) {
				mMotionHighlightView.handleMotion(mMotionEdge, mappedPoint[0] - mLastX, mappedPoint[1] - mLastY);
				mLastX = mappedPoint[0];
				mLastY = mappedPoint[1];

				if (true) {
					// This section of code is optional. It has some user
					// benefit in that moving the crop rectangle against
					// the edge of the screen causes scrolling but it means
					// that the crop rectangle is no longer fixed under
					// the user's finger.
					ensureVisible(mMotionHighlightView);
				}
			}
			break;
		}

		switch (event.getAction()) {
		case MotionEvent.ACTION_UP:
			center(true, true);
			break;
		case MotionEvent.ACTION_MOVE:
			// if we're not zoomed then there's no point in even allowing
			// the user to move the image around. This call to center puts
			// it back to the normalized location (with false meaning don't
			// animate).
			if (getScale() == 1F) {
				center(true, true);
			}
			break;
		}

		return true;
	}

	// Pan the displayed image to make sure the cropping rectangle is visible.
	private void ensureVisible(HighlightView hv) {
		Rect r = hv.mDrawRect;

		int panDeltaX1 = Math.max(0, mLeft - r.left);
		int panDeltaX2 = Math.min(0, mRight - r.right);

		int panDeltaY1 = Math.max(0, mTop - r.top);
		int panDeltaY2 = Math.min(0, mBottom - r.bottom);

		int panDeltaX = panDeltaX1 != 0 ? panDeltaX1 : panDeltaX2;
		int panDeltaY = panDeltaY1 != 0 ? panDeltaY1 : panDeltaY2;

		if (panDeltaX != 0 || panDeltaY != 0) {
			panBy(panDeltaX, panDeltaY);
		}
	}

	// If the cropping rectangle's size changed significantly, change the
	// view's center and scale according to the cropping rectangle.
	private void centerBasedOnHighlightView(HighlightView hv) {
		Rect drawRect = hv.mDrawRect;

		float width = drawRect.width();
		float height = drawRect.height();

		float thisWidth = getWidth();
		float thisHeight = getHeight();

		float z1 = thisWidth / width * .6F;
		float z2 = thisHeight / height * .6F;

		float zoom = Math.min(z1, z2);
		zoom = zoom * this.getScale();
		zoom = Math.max(1F, zoom);
		if ((Math.abs(zoom - getScale()) / zoom) > .1) {
			float[] coordinates = new float[] { hv.mCropRect.centerX(), hv.mCropRect.centerY() };
			getImageMatrix().mapPoints(coordinates);
			zoomTo(zoom, coordinates[0], coordinates[1], 300F);
		}

		ensureVisible(hv);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (!isInEditMode()) {
			mHighlightView.draw(canvas);
		}
	}

	public void add(HighlightView hv) {
		mHighlightView = hv;
		invalidate();
	}
}