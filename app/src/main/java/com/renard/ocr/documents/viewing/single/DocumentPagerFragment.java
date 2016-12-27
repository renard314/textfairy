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

import com.renard.ocr.MonitoredActivity;
import com.renard.ocr.R;
import com.renard.ocr.documents.viewing.single.DocumentActivity.DocumentContainerFragment;
import com.renard.ocr.documents.viewing.single.tts.TextSpeaker;
import com.renard.ocr.util.PreferencesUtils;
import com.viewpagerindicator.CirclePageIndicator;

import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class DocumentPagerFragment extends Fragment implements DocumentContainerFragment {


    private ViewPager mPager;
    private CirclePageIndicator mTitleIndicator;
    private boolean mIsTitleIndicatorVisible = false;
    private boolean mIsNewCursor;
    private Cursor mCursor;
    private DocumentAdapter mAdapter;
    private int mLastPosition = -1;
    private TextSpeaker mTextSpeaker = new TextSpeaker();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_document_pager, container, false);
        mPager = (ViewPager) v.findViewById(R.id.document_pager);
        mTitleIndicator = (CirclePageIndicator) v.findViewById(R.id.titles);
        mLastPosition = 0;
        initPager();
        return v;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        boolean isKeyboardHidden = Configuration.KEYBOARDHIDDEN_YES == newConfig.hardKeyboardHidden;
        boolean isKeyboardShown = Configuration.KEYBOARDHIDDEN_NO == newConfig.hardKeyboardHidden;
        if (isKeyboardShown) {
            showTitleIndicator(false);
        } else if (isKeyboardHidden) {
            showTitleIndicator(true);
        }
    }

    private void showTitleIndicator(final boolean show) {
        if (mIsTitleIndicatorVisible) {
            if (show) {
                mTitleIndicator.setVisibility(View.VISIBLE);
            } else {
                mTitleIndicator.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mTextSpeaker.onDestroyView();
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

    public void setDisplayedPageByDocumentId(final int documentId) {
        final int count = mAdapter.getCount();
        for (int i = 0; i < count; i++) {
            final int id = mAdapter.getId(i);
            if (documentId == id) {
                mPager.setCurrentItem(i, false);
                return;
            }
        }
    }

    private void initPager() {

        if (mIsNewCursor && mPager != null) {


            if (mAdapter != null) {
                mAdapter.setCursor(mCursor);
            } else {
                mAdapter = new DocumentAdapter(getChildFragmentManager(), mCursor);
                mPager.setAdapter(mAdapter);
            }


            if (mAdapter.getCount() > 1) {
                mTitleIndicator.setViewPager(mPager);
                mIsTitleIndicatorVisible = true;
                mTitleIndicator.setVisibility(View.VISIBLE);
            } else {
                mTitleIndicator.setVisibility(View.GONE);
            }

            mTitleIndicator.setOnPageChangeListener(new OnPageChangeListener() {

                @Override
                public void onPageSelected(int position) {
                    final DocumentTextFragment fragment = mAdapter.getFragment(mLastPosition);
                    if (fragment != null) {
                        fragment.saveIfTextHasChanged();
                    }
                    mLastPosition = position;
                    final String title = mAdapter.getLongTitle(position);
                    ((MonitoredActivity) getActivity()).setToolbarMessage(title);
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


    @Override
    public String getTextOfCurrentlyShownDocument() {
        int currentItem = mPager.getCurrentItem();
        final String htmlText = mAdapter.getText(currentItem);
        if (htmlText != null) {
            return Html.fromHtml(htmlText).toString();
        } else {
            return null;
        }
    }

    @Override
    public int getDocumentCount() {
        if (mCursor != null) {
            return mCursor.getCount();
        } else {
            return 0;
        }
    }

    @Override
    public void setCursor(Cursor cursor) {
        mIsNewCursor = true;
        mCursor = cursor;
        initPager();
    }

    @Override
    public String getTextOfAllDocuments() {
        final DocumentTextFragment fragment = mAdapter.getFragment(mLastPosition);

        DocumentAdapter adapter = (DocumentAdapter) mPager.getAdapter();
        final int count = adapter.getCount();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            String text = null;
            if (i == mLastPosition && fragment != null) {
                final Spanned documentText = fragment.getDocumentText();
                if (!TextUtils.isEmpty(documentText)) {
                    text = Html.toHtml(documentText);
                }
            } else {
                text = adapter.getText(i);
            }
            if (text != null) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(text);
            }
        }
        return sb.toString();
    }

    @Override
    public void setShowText(boolean text) {
        mAdapter.setShowText(text);
    }

    @Override
    public boolean getShowText() {
        return mAdapter.getShowText();
    }

    public TextSpeaker getTextSpeaker() {
        return mTextSpeaker;
    }
}
