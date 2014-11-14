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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
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
import android.view.ViewGroup;
import android.widget.EditText;

import com.renard.ocr.DocumentContentProvider;
import com.renard.ocr.DocumentContentProvider.Columns;
import com.renard.ocr.R;
import com.renard.util.PreferencesUtils;

public class DocumentAdapter extends FragmentStatePagerAdapter {
    private int mIndexLanguage;
    private int mIndexImagePath;
	private int mIndexTitle;
	private int mIndexOCRText;
	private int mIndexId;

	Cursor mCursor;
    private Map<Integer, DocumentTextFragment> mPageReferenceMap = new HashMap<Integer, DocumentTextFragment>();
    private boolean mShowText = true;

    public DocumentAdapter(FragmentManager fm, final Cursor cursor) {
        super(fm);
        mCursor = cursor;
        mIndexOCRText = mCursor.getColumnIndex(Columns.OCR_TEXT);
        mIndexImagePath = mCursor.getColumnIndex(Columns.PHOTO_PATH);
        // mIndexCreated = mCursor.getColumnIndex(Columns.CREATED);
        mIndexTitle = mCursor.getColumnIndex(Columns.TITLE);
        mIndexId = mCursor.getColumnIndex(Columns.ID);
        mIndexLanguage = mCursor.getColumnIndex(Columns.OCR_LANG);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        final Object o = super.instantiateItem(container, position);
        if (o instanceof  DocumentTextFragment) {
            mPageReferenceMap.put(position, (DocumentTextFragment) o);
        } else {
            mPageReferenceMap.put(position,null);
        }
        return o;

    }

    public void setShowText(boolean text) {
        mShowText = text;
        notifyDataSetChanged();

    }

    @Override
    public Fragment getItem(int position) {
        String text = null;
        Integer documentId = -1;
        String imagePath = null;
        if (mCursor.moveToPosition(position)) {
            text = mCursor.getString(mIndexOCRText);
            documentId = mCursor.getInt(mIndexId);
            imagePath = mCursor.getString(mIndexImagePath);
        }
        if (mShowText) {
            return DocumentTextFragment.newInstance(text, documentId, imagePath);
        }else {
            return DocumentImageFragment.newInstance(imagePath);

        }
    }

    public DocumentTextFragment getFragment(int key) {
        return mPageReferenceMap.get(key);
    }

    public void destroyItem(View container, int position, Object object) {
        super.destroyItem(container, position, object);
        mPageReferenceMap.remove(position);
    }

	@Override
	public int getCount() {
		return mCursor.getCount();
	}


    public String getLanguage(int position){
        return mCursor.getString(mIndexLanguage);
    }

	public String getLongTitle(int position) {
		if (mCursor.moveToPosition(position)) {
			return mCursor.getString(mIndexTitle);
		}
		return null;
	}

    public String getText(int position){
        boolean success = mCursor.moveToPosition(position);
        if (success) {
            return mCursor.getString(mIndexOCRText);
        }
        return null;
    }

    @Override
    public int getItemPosition(Object object) {
        if(object instanceof  DocumentTextFragment && !mShowText){
            return POSITION_NONE;
        }else if(object instanceof  DocumentImageFragment && mShowText){
            return POSITION_NONE;
        }
        return POSITION_UNCHANGED;
    }

    public void setCursor(Cursor cursor) {
        mCursor = cursor;
        notifyDataSetChanged();
    }

    public boolean getShowText() {
        return mShowText;
    }
}