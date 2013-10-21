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

package com.renard.documentview;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.renard.documentview.DocumentActivity.DocumentContainerFragment;
import com.renard.ocr.DocumentContentProvider;
import com.renard.ocr.R;
import com.renard.ocr.DocumentContentProvider.Columns;

import fi.harism.curl.CurlView;

public class DocumentCurlFragment extends Fragment implements DocumentContainerFragment {

	private CurlView mCurlView;
	private Cursor mCursor;
	private BackgroundLoadingBitmapProvider mBitmapProvider;
	private boolean mIsNewCursor;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.document_curl_fragment, container, false);
		mCurlView = (CurlView) v.findViewById(R.id.curl);
		initCurlView();
		return v;
	}

	public void setDisplayedPage(final int pageno) {
		mCurlView.setCurrentIndex(pageno);
	}



	private void initCurlView(){
		if (mIsNewCursor) {
			mCurlView.setBitmapProvider(null);

			if (mBitmapProvider != null) {
				mBitmapProvider.clearCache();
				mBitmapProvider.setCursor(mCursor);
			} else {
				mBitmapProvider = new BackgroundLoadingBitmapProvider(mCursor, getActivity());
			}
			mCurlView.setIndexChangedObserver(mBitmapProvider);
			mCurlView.setSizeChangedObserver(mBitmapProvider);
			mCurlView.setBitmapProvider(mBitmapProvider);
			mIsNewCursor = false;
		}
	}
	
	@Override
	public void setCursor(final Cursor cursor) {
		mCursor = cursor;
		mIsNewCursor = true;
		if(isInLayout()){
			initCurlView();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mBitmapProvider != null) {
			mBitmapProvider.clearCache();
		}
		mCurlView.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		mCurlView.onResume();
	}

	@Override
	public String getTextofCurrentlyShownDocument() {
		int index = mCurlView.getCurrentIndex();
		Cursor cursor = mBitmapProvider.getCursor();
		final int columIndex = cursor.getColumnIndex(DocumentContentProvider.Columns.OCR_TEXT);
		boolean success = mCursor.moveToPosition(index);
		if (success){
			return mCursor.getString(columIndex);
		}
		return null;
	}
}
