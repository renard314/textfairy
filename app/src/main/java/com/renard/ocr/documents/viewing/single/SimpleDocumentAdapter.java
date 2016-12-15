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

package com.renard.ocr.documents.viewing.single;

import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.renard.ocr.documents.viewing.DocumentContentProvider.Columns;
import com.renard.ocr.R;

public class SimpleDocumentAdapter extends CursorAdapter {

	public interface ViewBinder {
		void bind(final View v, final DocumentViewHolder holder, final String title, final CharSequence formattedDate, final String text, final int position, int documentId);
	}

	private LayoutInflater mInflater;
	private final int mLayoutId;
	private int mIndexCreated;
	private int mIndexTitle;
	private int mIndexOCRText;
	private int mIndexId;
	private final ViewBinder mBinder;

	public static class DocumentViewHolder {

		public TextView text;
		public TextView date;
		public TextView mPageNumber;
		public EditText edit;
		public int boundId = -1;
		public TextWatcher watcher;
		
		DocumentViewHolder(View v) {
			text = (TextView) v.findViewById(R.id.text);
			date = (TextView) v.findViewById(R.id.date);
			edit = (EditText) v.findViewById(R.id.editText_document);
			mPageNumber = (TextView) v.findViewById(R.id.page_number);
		}

	}

	@Override
	public Object getItem(int position) {
		Cursor c=  (Cursor) super.getItem(position);
		return c.getString(mIndexOCRText);
	}
	
	public SimpleDocumentAdapter(Activity context, final int layoutId, final Cursor cursor, final ViewBinder binder) {
		super(context, cursor,true);
		mInflater = LayoutInflater.from(context);
		final Cursor c = getCursor();
		mLayoutId = layoutId;
		mIndexCreated = c.getColumnIndex(Columns.CREATED);
		mIndexOCRText = c.getColumnIndex(Columns.OCR_TEXT);
		mIndexTitle = c.getColumnIndex(Columns.TITLE);
		mIndexId = c.getColumnIndex(Columns.ID);
		mBinder = binder;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		DocumentViewHolder holder = (DocumentViewHolder) view.getTag();
		final String title = cursor.getString(mIndexTitle);
		long created = cursor.getLong(mIndexCreated);
		final String text = cursor.getString(mIndexOCRText);
		CharSequence formattedDate = DateFormat.format("MMM dd, yyyy h:mmaa", new Date(created));
		final int id = cursor.getInt(mIndexId);
		mBinder.bind(view, holder, title, formattedDate, text, cursor.getPosition(),id);
	};

	@Override
	public long getItemId(int position) {
		if (getCursor().moveToPosition(position)) {
			int index = getCursor().getColumnIndex(Columns.ID);
			return getCursor().getLong(index);
		}
		return -1;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View v = null;
		DocumentViewHolder holder = null;
		v = mInflater.inflate(mLayoutId, null);
		holder = new DocumentViewHolder(v);
		v.setTag(holder);
		return v;
	}
}
