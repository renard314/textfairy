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
import com.renard.ocr.documents.viewing.single.tts.TtsInitError;
import com.renard.ocr.documents.viewing.single.tts.TtsInitListener;
import com.renard.ocr.documents.viewing.single.tts.TtsInitSuccess;
import com.renard.ocr.documents.viewing.single.tts.TtsLanguageChoosen;
import com.renard.ocr.util.PreferencesUtils;
import com.renard.ocr.util.ResourceUtils;
import com.viewpagerindicator.CirclePageIndicator;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Locale;
import java.util.Map;

import de.greenrobot.event.EventBus;

public class DocumentPagerFragment extends Fragment implements DocumentContainerFragment {


    private ViewPager mPager;
    private CirclePageIndicator mTitleIndicator;
    private boolean mIsTitleIndicatorVisible = false;
    private boolean mIsNewCursor;
    private Cursor mCursor;
    private DocumentAdapter mAdapter;
    private int mLastPosition = -1;
    static final int REQUEST_CODE_TTS_CHECK = 6;
    private TextSpeaker mTextSpeaker;
    private boolean mHasTts;
    private Map<String, String> hashMapResource;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_document_pager, container, false);
        mPager = (ViewPager) v.findViewById(R.id.document_pager);
        mTitleIndicator = (CirclePageIndicator) v.findViewById(R.id.titles);
        mLastPosition = 0;
        mTextSpeaker = new TextSpeaker();
        hashMapResource = ResourceUtils.getHashMapResource(getContext(), R.xml.iso_639_mapping);
        mHasTts = checkTtsData();
        initPager();
        EventBus.getDefault().register(this);
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mTextSpeaker.onDestroyView();
        EventBus.getDefault().unregister(this);
    }

    private boolean checkTtsData() {
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        ResolveInfo resolveInfo = getContext().getPackageManager().resolveActivity(checkIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo != null) {
            getActivity().startActivityForResult(checkIntent, REQUEST_CODE_TTS_CHECK);
            return true;
        } else {
            Toast.makeText(getContext(), R.string.tts_not_available, Toast.LENGTH_LONG).show();
            return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_TTS_CHECK) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                mTextSpeaker.createTts(getContext(), new TtsInitListener() {
                    @Override
                    public void onInitError() {
                        EventBus.getDefault().post(new TtsInitError());
                    }

                    @Override
                    public void onInitSuccess() {
                        final String langOfCurrentlyShownDocument = getLangOfCurrentlyShownDocument();
                        Locale documentLocale = mapTesseractLanguageToLocale(langOfCurrentlyShownDocument);

                        final boolean localeSupported = mTextSpeaker.isLocaleSupported(documentLocale);
                        if (localeSupported) {
                            mTextSpeaker.setTtsLocale(documentLocale);
                        }
                        EventBus.getDefault().post(new TtsInitSuccess());

                    }
                });
            } else {
                Intent installIntent = new Intent();
                installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        }

    }

    public Locale mapTesseractLanguageToLocale(String ocrLanguage) {
        final String s = hashMapResource.get(ocrLanguage);
        if (s != null) {
            return new Locale(s);
        } else {
            return null;
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(final TtsLanguageChoosen event) {
        Locale documentLocale = event.getLocale();
        mTextSpeaker.setTtsLocale(documentLocale);
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
    public String getLangOfCurrentlyShownDocument() {
        DocumentAdapter adapter = (DocumentAdapter) mPager.getAdapter();
        int currentItem = mPager.getCurrentItem();
        return adapter.getLanguage(currentItem);
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

    public boolean hasTts() {
        return mHasTts;
    }

    public TextSpeaker getTextSpeaker() {
        return mTextSpeaker;
    }
}
