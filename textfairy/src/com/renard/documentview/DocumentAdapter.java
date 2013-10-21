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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Pair;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.renard.ocr.DocumentContentProvider;
import com.renard.ocr.DocumentContentProvider.Columns;
import com.renard.ocr.R;
import com.renard.util.PreferencesUtils;

public class DocumentAdapter extends PagerAdapter {
	private Set<Integer> mChangedDocuments = new HashSet<Integer>();
	private SparseArray<Spanned> mSpannedTexts = new SparseArray<Spanned>();
	private SparseArray<CharSequence> mChangedTexts = new SparseArray<CharSequence>();

	private int mIndexTitle;
	private int mIndexOCRText;
	private int mIndexId;
	private LayoutInflater mInflater;
	private Context mContext;

	final Cursor mCursor;

	public DocumentAdapter(FragmentActivity activity, final Cursor cursor) {
		mCursor = cursor;
		mContext = activity.getApplicationContext();
		mIndexOCRText = mCursor.getColumnIndex(Columns.OCR_TEXT);
		// mIndexCreated = mCursor.getColumnIndex(Columns.CREATED);
		mIndexTitle = mCursor.getColumnIndex(Columns.TITLE);
		mIndexId = mCursor.getColumnIndex(Columns.ID);
		mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

	}

	@Override
	public Object instantiateItem(final View collection, final int position) {
		View view = null;
		if (mCursor.moveToPosition(position)) {
			final int documentId = mCursor.getInt(mIndexId);
			Spanned spanned = mSpannedTexts.get(documentId);

			if (spanned == null) {
				final String text = mCursor.getString(mIndexOCRText);
				if (text == null) {
					spanned = SpannableStringBuilder.valueOf("");
				} else {
					spanned = Html.fromHtml(text);
				}
				mSpannedTexts.put(documentId, spanned);
			}
			view = mInflater.inflate(R.layout.document_fragment, null);
			EditText edit = (EditText) view.findViewById(R.id.editText_document);
			edit.setText(spanned);
			TextWatcher watcher = new TextWatcher() {

				public void afterTextChanged(Editable s) {
				}

				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				public void onTextChanged(CharSequence s, int start, int before, int count) {
					mChangedDocuments.add(documentId);
					mChangedTexts.put(documentId, s);
				}
			};
			edit.addTextChangedListener(watcher);

			((ViewPager) collection).addView(view);
			PreferencesUtils.applyTextPreferences(edit, mContext);
		}
		return view;
	}

	public Pair<List<Uri>, List<Spanned>> getTextsToSave() {
		List<Uri> documentIds = new ArrayList<Uri>();
		List<Spanned> texts = new ArrayList<Spanned>();

		for (Integer id : mChangedDocuments) {
			documentIds.add(Uri.withAppendedPath(DocumentContentProvider.CONTENT_URI, String.valueOf(id)));
			final CharSequence text = mChangedTexts.get(id);
			texts.add((Spanned) text);
		}
		return new Pair<List<Uri>, List<Spanned>>(documentIds, texts);
	}

	public void destroyItem(View collection, int position, Object view) {

		((ViewPager) collection).removeView((View) view);
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == ((View) object);
	}

	// @Override
	// public Fragment getItem(int position) {
	// if (mCursor.moveToPosition(position)) {
	// final String text = mCursor.getString(mIndexOCRText);
	// Fragment fragment = DocumentFragment.newInstance(text);
	// return fragment;
	// }
	// return null;
	// }

	@Override
	public int getCount() {
		return mCursor.getCount();
	}

	public String getLongTitle(int position) {
		if (mCursor.moveToPosition(position)) {
			return mCursor.getString(mIndexTitle);
		}
		return null;
	}

	public String getTextByPosition(int position){
		boolean success = mCursor.moveToPosition(position);
		if (success) {
			return mCursor.getString(mIndexOCRText);
		}
		return null;
	}
}