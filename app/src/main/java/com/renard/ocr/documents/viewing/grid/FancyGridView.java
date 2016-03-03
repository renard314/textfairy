/*
 * Copyright (C) 2012,2013 Renard Wellnitz.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.renard.ocr.documents.viewing.grid;

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
			mLastTouchedChild = touchedChild;
			break;
		}
		}

		return super.onTouchEvent(ev);
	}

}
