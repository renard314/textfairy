package com.renard.ocr.cropimage;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by renard on 13/11/14.
 */
public class CropImageView extends ImageViewTouchBase {
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
		CropImageActivity cropImage = (CropImageActivity) mContext;
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
			float[] coordinates = new float[] { hv.getCropRect().centerX(), hv.getCropRect().centerY() };
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
