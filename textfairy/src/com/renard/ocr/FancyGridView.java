package com.renard.ocr;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.GridView;

/**
 * a gridview that applies 3d effects to its children
 * 
 * @author renard
 * 
 */
public class FancyGridView extends GridView {

	public FancyGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	private CheckableGridElement mLastTouchedChild;

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		final int action = ev.getAction();
		final int x = (int) ev.getX();
		final int y = (int) ev.getY();
		final int motionPosition = pointToPosition(x, y);
		CheckableGridElement touchedChild = null;
		if (motionPosition >= 0) {
			touchedChild = (CheckableGridElement) getChildAt(motionPosition - getFirstVisiblePosition());
		}

		// if (mLastTouchedChild != null &&
		// !mLastTouchedChild.equals(touchedChild)) {
		// mLastTouchedChild.startTouchUpAnimation();
		// mLastTouchedChild=null;
		// }

		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN: {
			if (touchedChild != null) {
				touchedChild.startTouchDownAnimation();
				mLastTouchedChild = touchedChild;
			} else {
				mLastTouchedChild = null;
			}
			break;
		}
		case MotionEvent.ACTION_UP: {
			if (mLastTouchedChild != null) {
				mLastTouchedChild.startTouchUpAnimation();
				mLastTouchedChild = null;
			}
			break;
		}
		case MotionEvent.ACTION_MOVE: {

			if (mLastTouchedChild != null && !mLastTouchedChild.equals(touchedChild)) {
				mLastTouchedChild.startTouchUpAnimation();

			}
			// if (touchedChild != null &&
			// !touchedChild.equals(mLastTouchedChild)) {
			// touchedChild.startTouchDownAnimation();
			// }
			mLastTouchedChild = touchedChild;
			break;
		}
		}

		return super.onTouchEvent(ev);
	}

}
