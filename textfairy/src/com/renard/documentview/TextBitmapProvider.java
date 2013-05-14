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

import java.util.ArrayList;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.FloatMath;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import com.renard.ocr.DocumentContentProvider.Columns;
import com.renard.util.PreferencesUtils;

import fi.harism.curl.CurlView.BitmapProvider;
import fi.harism.curl.CurlView.SizeChangedObserver;

public class TextBitmapProvider implements BitmapProvider, SizeChangedObserver {

	
	private final static String TAG = TextBitmapProvider.class.getSimpleName();
	private int mPageIndex = - 1;
	private float mTextSize;
	private int mWidth = 0;
	private int mHeight = 0;
	private Cursor mPageCursor;
	//private String mText;
	private ArrayList<Spanned> mText;
	
	private ContentObserver observer = new DocumentContentObserver();
	private TextView mTextView;

	private class DocumentContentObserver extends ContentObserver {

		public DocumentContentObserver() {
			super(new Handler());
		}

		public void onChange(boolean selfChange) {
			mText = getWholeText(mPageCursor);
			doLayout(mWidth, mHeight);
		};
	}

	@Override
	public void close() {
		mPageCursor.unregisterContentObserver(observer);
	}

	public TextBitmapProvider(Cursor pageCursor, Context appContext) {
		this.mPageCursor = pageCursor;
		//mTextPaint = new Paint();
//		mTextPaint.setStyle(Paint.Style.FILL);
//		mTextPaint.setColor(Color.BLACK);
//		mTextPaint.setTextAlign(Paint.Align.LEFT);
//		mTextPaint.setTextSize(mTextSize* density);
//		mTextPaint.setAntiAlias(true);
		mText = getWholeText(mPageCursor);
		mTextView =  new TextView(appContext);
		mTextView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));		
		//mTextView.setTextSize(mTextSize);
		mTextSize = PreferencesUtils.getTextSize(appContext);
		PreferencesUtils.applyTextPreferences(mTextView, appContext);
		SpannableStringBuilder ssb = new SpannableStringBuilder();
		for (Spanned st: mText){
			ssb.append(st);			
		}
		mTextView.setText(ssb);
		mPageCursor.registerContentObserver(observer);
	}
	
	public float getTextSize(){
		return mTextSize;
	}

	public void setTextSize(float size) {
		this.mTextSize=size;
		mTextView.setTextSize(size);
		doLayout(mWidth, mHeight);		
	}
	
	public void applyTextPreferences(Context appContext) {
		PreferencesUtils.applyTextPreferences(mTextView, appContext);
		doLayout(mWidth, mHeight);
		
	}

	public ArrayList<Spanned> getWholeText(Cursor c) {
		ArrayList<Spanned> result = new ArrayList<Spanned>(c.getCount());
		c.moveToPosition(-1);
//		StringBuilder sb = new StringBuilder();
		String pageText;
		int index = c.getColumnIndex(Columns.OCR_TEXT);
		while (c.moveToNext()) {
			pageText = sanitizeText(c.getString(index));
			result.add(Html.fromHtml(pageText));
			//sb.append(sanitizeText(pageText));
		}
		//return sb.toString();
		return result;
	}

	private String sanitizeText(String text) {
		return text.replace("\n", " ");
	}

	private void doLayout(int width, int height){
		mHeight = height;
		mWidth =width;

		mTextView.setWidth(width);
		mTextView.setHeight(height);
		long start = System.currentTimeMillis();
		mTextView.measure(MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
		Log.i(TAG,"layout: " + (System.currentTimeMillis()-start));
	}

	public int getTextOffsetForCurrentFirstLine(){
		int offset = (mPageIndex)*mHeight;		
		int firstline = mTextView.getLayout().getLineForVertical(offset);
		return mTextView.getLayout().getLineStart(firstline);
	}
	public int getPageForTextOffset(int offset){
		int line = mTextView.getLayout().getLineForOffset(offset);
		int top = mTextView.getLayout().getLineTop(line);
		return top /mHeight;
	}
	
	public int getPageIndexFromDocumentIndex(int documentIndex) {
		//get text index
		int textindex = 0;
		for (int i = 0; i<documentIndex;i++){
			textindex+=mText.get(documentIndex).length();
		}
		int firstline = mTextView.getLayout().getLineForOffset(textindex);
		float linePos = mTextView.getLayout().getLineTop(firstline);
		return (int) FloatMath.ceil(linePos/mHeight);
	
	}
	
	@Override
	public Bitmap getBitmap(int width, int height, int index) {
		if (width != mWidth || height != mHeight) {
			doLayout(width, height);
		}
		
		mPageIndex = index;
		Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		b.eraseColor(0xFFFFFFFF);
		Canvas c = new Canvas(b);
		if (mTextView.getBackground()!=null) {
			mTextView.getBackground().draw(c);
		}

		int offset = (index)*height;
		
		int firstline = mTextView.getLayout().getLineForVertical(offset);
		int upperBound = mTextView.getLayout().getLineTop(firstline);		
		int lastline = mTextView.getLayout().getLineForVertical(upperBound + height);		
		int offsetLastLine = mTextView.getLayout().getLineBottom(lastline);
		int offsetBeforeLastLine  = mTextView.getLayout().getLineBottom(lastline-1);		
		int lowerBound;
		if((offsetLastLine - (upperBound))>height){
			lowerBound = offsetBeforeLastLine;
		} else {
			lowerBound = offsetLastLine;
		}
		c.clipRect(0, 0, width, lowerBound-upperBound);
		c.translate(0, -upperBound);
		mTextView.getPaint().setColor(mTextView.getCurrentTextColor());
		mTextView.getLayout().draw(c);
		

		return b;
	}

	@Override
	public int getBitmapCount() {
		if (mTextView.getLayout()==null){
			doLayout(mWidth, mHeight);
		}
		float totalHeight = mTextView.getLayout().getHeight();
		int count = (int) FloatMath.ceil( totalHeight / mHeight);
		return count;
	}

	@Override
	public void onSizeChanged(int width, int height) {
		if (width != mWidth || height != mHeight) {
			doLayout(width, height);
		}
		
	}
}
