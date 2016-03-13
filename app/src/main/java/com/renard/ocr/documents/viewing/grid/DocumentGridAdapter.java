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

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.renard.ocr.documents.viewing.DocumentContentProvider;
import com.renard.ocr.R;
import com.renard.ocr.documents.viewing.grid.CheckableGridElement.OnCheckedChangeListener;
import com.renard.ocr.documents.viewing.DocumentContentProvider.Columns;
import com.renard.ocr.util.Util;

/**
 * adapter for the document grid view
 * @author renard
 *
 */
public class DocumentGridAdapter extends CursorAdapter implements OnCheckedChangeListener {

	public interface OnCheckedChangeListener {
		void onCheckedChanged(final Set<Integer> checkedIds);
	}

	private final static String[] PROJECTION = { Columns.ID, Columns.TITLE, Columns.OCR_TEXT, Columns.CREATED, Columns.PHOTO_PATH,Columns.CHILD_COUNT };

	private Set<Integer> mSelectedDocuments = new HashSet<Integer>();
	private LayoutInflater mInflater;
	private final DocumentGridActivity mActivity;
	private int mElementLayoutId;

	private int mIndexCreated;
	private int mIndexTitle;
	private int mIndexID;
	private int mChildCountID;
	private OnCheckedChangeListener mCheckedChangeListener = null;

	static class DocumentViewHolder {

		public CheckableGridElement gridElement;
		private TextView date;
		private TextView mPageNumber;
		public int documentId;
		public boolean updateThumbnail;
		CrossFadeDrawable transition;

		DocumentViewHolder(View v) {
			gridElement = (CheckableGridElement) v;
			date = (TextView) v.findViewById(R.id.date);
			mPageNumber = (TextView) v.findViewById(R.id.page_number);
		}

	}

	public void clearAllSelection() {
		mSelectedDocuments.clear();
	}

	public DocumentGridAdapter(DocumentGridActivity activity, int elementLayout, OnCheckedChangeListener listener) {
		super(activity, activity.getContentResolver().query(DocumentContentProvider.CONTENT_URI, PROJECTION, DocumentContentProvider.Columns.PARENT_ID + "=-1", null, null), true);
		mElementLayoutId = elementLayout;
		mActivity = activity;
		mInflater = LayoutInflater.from(activity);
		final Cursor c = getCursor();
		mIndexCreated = c.getColumnIndex(Columns.CREATED);
		mIndexID = c.getColumnIndex(Columns.ID);
		mIndexTitle = c.getColumnIndex(Columns.TITLE);
		mChildCountID = c.getColumnIndex(Columns.CHILD_COUNT);
		mCheckedChangeListener = listener;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		final DocumentViewHolder holder = (DocumentViewHolder) view.getTag();
		final int documentId = cursor.getInt(mIndexID);
		final int childCount = cursor.getInt(mChildCountID);
		final boolean isSelected = mSelectedDocuments.contains(documentId);
		holder.documentId = documentId;

		String title = cursor.getString(mIndexTitle);
		if (title != null && title.length() > 0) {
			holder.date.setText(title);
		} else {
			long created = cursor.getLong(mIndexCreated);
			CharSequence formattedDate = DateFormat.format("MMM dd, yyyy h:mmaa", new Date(created));
			holder.date.setText(formattedDate);
		}

		if (holder.mPageNumber != null) {
			holder.mPageNumber.setText(String.valueOf(childCount+1));
		}
		if (holder.gridElement != null) {

			if (mActivity.getScrollState() == AbsListView.OnScrollListener.SCROLL_STATE_FLING || mActivity.isPendingThumbnailUpdate()) {
				holder.gridElement.setImage(Util.sDefaultDocumentThumbnail);
				holder.updateThumbnail = true;
			} else {
				final Drawable d = Util.getDocumentThumbnail(documentId);
				holder.gridElement.setImage(d);
				holder.updateThumbnail = false;
			}
		}
		holder.gridElement.setCheckedNoAnimate(isSelected);
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
		v = mInflater.inflate(mElementLayoutId,null, false);
		int index = cursor.getColumnIndex(Columns.ID);
		int documentId = cursor.getInt(index);
		holder = new DocumentViewHolder(v);
		holder.documentId = documentId;
		holder.gridElement.setChecked(mSelectedDocuments.contains(documentId));
		holder.gridElement.setOnCheckedChangeListener(this);
		v.setTag(holder);
		FastBitmapDrawable start = Util.sDefaultDocumentThumbnail;
		Bitmap startBitmap = null;
		if(start!=null){
			startBitmap = start.getBitmap();
		}
		final CrossFadeDrawable transition = new CrossFadeDrawable(startBitmap, null);
		transition.setCallback(v);
		transition.setCrossFadeEnabled(true);
		holder.transition = transition;
		return v;
	}
	
	public void setSelectedDocumentIds(List<Integer> selection) {
		mSelectedDocuments.addAll(selection);
		if (mCheckedChangeListener!=null){
			mCheckedChangeListener.onCheckedChanged(mSelectedDocuments);
		}
	}

	public Set<Integer> getSelectedDocumentIds() {
		return mSelectedDocuments;
	}

	@Override
	public void onCheckedChanged(View documentView, boolean isChecked) {
		DocumentViewHolder holder = (DocumentViewHolder) documentView.getTag();
		if (isChecked) {
			mSelectedDocuments.add(holder.documentId);
		} else {
			mSelectedDocuments.remove(holder.documentId);
		}
		mCheckedChangeListener.onCheckedChanged(mSelectedDocuments);
	}
}
