package com.renard.documentview;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.renard.documentview.DocumentActivity.DocumentContainerFragment;
import com.renard.ocr.R;

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
}
