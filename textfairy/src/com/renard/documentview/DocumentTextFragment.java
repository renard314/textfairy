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

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

import com.renard.ocr.BaseDocumentActivitiy;
import com.renard.ocr.DocumentContentProvider;
import com.renard.ocr.R;
import com.renard.util.PreferencesUtils;
import com.renard.util.Util;

import java.util.ArrayList;
import java.util.List;

public class DocumentTextFragment extends Fragment implements TextWatcher {

    private final static String LOG_TAG = DocumentTextFragment.class.getSimpleName();

    private EditText mEditText;
    private int mDocumentId;
    private boolean mHasTextChanged;
    private ViewSwitcher mViewSwitcher;
    private HtmlToSpannedAsyncTask mHtmlTask;

    public static DocumentTextFragment newInstance(final String text, Integer documentId, final String imagePath) {
        DocumentTextFragment f = new DocumentTextFragment();
        // Supply text input as an argument.
        Bundle args = new Bundle();
        args.putString("text", text);
        args.putInt("id", documentId);
        args.putString("image_path", imagePath);
        f.setArguments(args);
        return f;
    }

    private static class HtmlToSpannedAsyncTask extends AsyncTask<String, Void, Spanned> {

        private final EditText mEditText;
        private final ViewSwitcher mViewSwitcher;
        private final TextWatcher mTextWatcher;

        private HtmlToSpannedAsyncTask(final EditText editText, ViewSwitcher viewSwitcher, TextWatcher textWatcher){
            mEditText = editText;
            mViewSwitcher = viewSwitcher;
            mTextWatcher = textWatcher;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mViewSwitcher.setDisplayedChild(0);
        }

        @Override
        protected Spanned doInBackground(String... params) {
            if (params!=null && params.length>0) {
                return Html.fromHtml(params[0]);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Spanned spanned) {
            super.onPostExecute(spanned);
            mEditText.setText(spanned);
            mEditText.addTextChangedListener(mTextWatcher);
            mViewSwitcher.setDisplayedChild(1);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final String text = getArguments().getString("text");
        final String imagePath= getArguments().getString("image_path");
        mDocumentId = getArguments().getInt("id");
        View view = inflater.inflate(R.layout.fragment_document, container, false);
        mEditText = (EditText) view.findViewById(R.id.editText_document);
        mViewSwitcher = (ViewSwitcher) view.findViewById(R.id.viewSwitcher);
        if (mHtmlTask!=null){
            mHtmlTask.cancel(true);
        }
            mHtmlTask = new HtmlToSpannedAsyncTask(mEditText,mViewSwitcher,this);
            mHtmlTask.execute(text);

        PreferencesUtils.applyTextPreferences(mEditText, getActivity());

        return view;
    }

    void saveIfTextHasChanged(){
        if(mHasTextChanged) {
            mHasTextChanged = false;
            final Uri uri = Uri.withAppendedPath(DocumentContentProvider.CONTENT_URI, String.valueOf(mDocumentId));
            List<Uri> ids = new ArrayList<Uri>();
            List<Spanned> texts = new ArrayList<Spanned>();
            ids.add(uri);
            texts.add(mEditText.getText());
            BaseDocumentActivitiy.SaveDocumentTask saveTask = new BaseDocumentActivitiy.SaveDocumentTask(getActivity(), ids, texts);
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
        mHasTextChanged = true;
    }

    @Override
    public void afterTextChanged(Editable s) {

    }
}
