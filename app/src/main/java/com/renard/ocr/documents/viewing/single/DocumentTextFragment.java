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

import com.renard.ocr.R;
import com.renard.ocr.documents.creation.NewDocumentActivity;
import com.renard.ocr.documents.viewing.DocumentContentProvider;
import com.renard.ocr.documents.viewing.single.tts.TextSpeaker;
import com.renard.ocr.documents.viewing.single.tts.TextToSpeechControls;
import com.renard.ocr.util.PreferencesUtils;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ViewSwitcher;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DocumentTextFragment extends Fragment implements TextWatcher {

    private final static String IS_STATE_SAVED = "is_state_saved";
    public static final String EXTRA_LANG = "lang";
    private static final String EXTRA_POSITION = "position";

    @BindView(R.id.text_to_speech_controls)
    protected TextToSpeechControls mTextToSpeechControls;
    @BindView(R.id.editText_document)
    protected EditText mEditText;
    @BindView(R.id.viewSwitcher)
    protected ViewSwitcher mViewSwitcher;


    private int mDocumentId;
    private boolean mHasTextChanged;
    private HtmlToSpannedAsyncTask mHtmlTask;
    private boolean mIsInitialTextChange = true;

    public static DocumentTextFragment newInstance(final String text, Integer documentId, final String imagePath, String lang, int position) {
        DocumentTextFragment f = new DocumentTextFragment();
        // Supply text input as an argument.
        Bundle args = new Bundle();
        args.putString(EXTRA_LANG, lang);
        args.putInt(EXTRA_POSITION, position);
        args.putString("text", text);
        args.putInt("id", documentId);
        args.putString("image_path", imagePath);
        f.setArguments(args);
        return f;
    }

    public Spanned getDocumentText() {
        return mEditText.getText();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mDocumentId = getArguments().getInt("id");
        View view = inflater.inflate(R.layout.fragment_document_text, container, false);
        ButterKnife.bind(this, view);

        if (mHtmlTask != null) {
            mHtmlTask.cancel(true);
        }

        DocumentActivity documentActivity = (DocumentActivity) getActivity();
        DocumentPagerFragment pagerFragment = (DocumentPagerFragment) getParentFragment();
        TextSpeaker textSpeaker = pagerFragment.getTextSpeaker();
        mTextToSpeechControls.onCreateView(getChildFragmentManager(), documentActivity.getAnaLytics(), textSpeaker);
        PreferencesUtils.applyTextPreferences(mEditText, getActivity());

        return view;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mHtmlTask != null) {
            mHtmlTask.cancel(true);
        }
        mEditText.removeTextChangedListener(this);
        mTextToSpeechControls.onDestroyView();
    }

    @Override
    public void onPause() {
        super.onPause();
        saveIfTextHasChanged();
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        String text = getArguments().getString("text");
        if (savedInstanceState == null || !savedInstanceState.getBoolean(IS_STATE_SAVED)) {
            mHtmlTask = new HtmlToSpannedAsyncTask(mEditText, mViewSwitcher, this);
            mHtmlTask.execute(text);
        } else {
            mViewSwitcher.setDisplayedChild(1);
            mEditText.addTextChangedListener(this);
            mTextToSpeechControls.setCurrentText(mEditText.getText(), getLangOfCurrentlyShownDocument(), getIndexOfCurrentlyShownDocument());
        }
    }

    private int getIndexOfCurrentlyShownDocument() {
        return getArguments().getInt(EXTRA_POSITION);
    }

    private String getLangOfCurrentlyShownDocument() {
        return getArguments().getString(EXTRA_LANG);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(IS_STATE_SAVED, true);
    }


    void saveIfTextHasChanged() {
        if (mHasTextChanged) {
            mHasTextChanged = false;
            final Uri uri = Uri.withAppendedPath(DocumentContentProvider.CONTENT_URI, String.valueOf(mDocumentId));
            List<Uri> ids = new ArrayList<>();
            List<Spanned> texts = new ArrayList<>();
            ids.add(uri);
            texts.add(mEditText.getText());
            NewDocumentActivity.SaveDocumentTask saveTask = new NewDocumentActivity.SaveDocumentTask(getActivity(), ids, texts);
            saveTask.execute();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        PreferencesUtils.applyTextPreferences(mEditText, getActivity());
    }


    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (mIsInitialTextChange) {
            mIsInitialTextChange = false;
        } else {
            mHasTextChanged = true;
        }
        mTextToSpeechControls.setCurrentText(s, getLangOfCurrentlyShownDocument(), getIndexOfCurrentlyShownDocument());
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

}
