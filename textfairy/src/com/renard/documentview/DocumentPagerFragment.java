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

import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Spanned;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.renard.documentview.DocumentActivity.DocumentContainerFragment;
import com.renard.ocr.R;
import com.renard.util.PreferencesUtils;
import com.viewpagerindicator.CirclePageIndicator;

import java.util.List;

public class DocumentPagerFragment extends Fragment implements DocumentContainerFragment {

    private ViewPager mPager;
    private CirclePageIndicator mTitleIndicator;
    private boolean mIsTitleIndicatorVisible = false;
    private boolean mIsNewCursor;
    private Cursor mCursor;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.document_pager_fragment, container, false);
        mPager = (ViewPager) v.findViewById(R.id.document_pager);
        mTitleIndicator = (CirclePageIndicator) v.findViewById(R.id.titles);
        initPager();
        return v;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        boolean isKeyboardHidden = Configuration.KEYBOARDHIDDEN_YES == newConfig.hardKeyboardHidden || Configuration.HARDKEYBOARDHIDDEN_YES == newConfig.hardKeyboardHidden;
        boolean isKeyboardShown = Configuration.KEYBOARDHIDDEN_NO == newConfig.hardKeyboardHidden || Configuration.HARDKEYBOARDHIDDEN_NO == newConfig.hardKeyboardHidden;
        if (isKeyboardShown) {
            showTitleIndicator(false);
        } else if (isKeyboardHidden) {
            showTitleIndicator(true);
        }
    }

    private void showTitleIndicator(final boolean show) {
        if (mIsTitleIndicatorVisible == true) {
            if (show) {
                mTitleIndicator.setVisibility(View.VISIBLE);
            } else {
                mTitleIndicator.setVisibility(View.GONE);
            }
        }
    }

    public void applyTextPreferences() {
        final int count = mPager.getChildCount();
        for (int i = 0; i < count; i++) {
            View v = mPager.getChildAt(i);
            EditText e = (EditText) v.findViewById(R.id.editText_document);
            PreferencesUtils.applyTextPreferences(e, getActivity());
        }
    }

    public void setDisplayedPage(final int pageno) {
        mPager.setCurrentItem(pageno, true);
    }

    private void initPager() {

        if (mIsNewCursor && mPager != null) {
            final DocumentAdapter adapter = new DocumentAdapter(getActivity(), mCursor);

            Log.i(DocumentPagerFragment.class.getSimpleName(), mCursor.getCount() + "");
            mPager.setAdapter(adapter);
            // mTitleIndicator.setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_bg_black));
            if (adapter.getCount() > 1) {
                mTitleIndicator.setViewPager(mPager);
                mIsTitleIndicatorVisible = true;
            } else {
                mTitleIndicator.setVisibility(View.GONE);
            }

            mTitleIndicator.setOnPageChangeListener(new OnPageChangeListener() {

                @Override
                public void onPageSelected(int position) {
                    final String title = adapter.getLongTitle(position);
                    getActivity().getActionBar().setTitle(title);
                    getActivity().getActionBar().setDisplayShowTitleEnabled(true);
                }

                @Override
                public void onPageScrolled(int arg0, float arg1, int arg2) {
                }

                @Override
                public void onPageScrollStateChanged(int arg0) {
                }
            });
            mIsNewCursor = false;
        }
    }

    public Pair<List<Uri>, List<Spanned>> getTextsToSave() {
        PagerAdapter adapter = mPager.getAdapter();
        if (adapter instanceof DocumentAdapter) {
            return ((DocumentAdapter) adapter).getTextsToSave();
        }
        return null;
    }

    @Override
    public String getLangOfCurrentlyShownDocument() {
        DocumentAdapter adapter = (DocumentAdapter) mPager.getAdapter();
        int currentItem = mPager.getCurrentItem();
        return adapter.getLanguage(currentItem);
    }

    @Override
    public void setCursor(Cursor cursor) {
        mIsNewCursor = true;
        mCursor = cursor;
        initPager();
    }

    @Override
    public String getTextOfCurrentlyShownDocument() {
        DocumentAdapter adapter = (DocumentAdapter) mPager.getAdapter();
        int currentItem = mPager.getCurrentItem();
        return adapter.getText(currentItem);
    }
}
