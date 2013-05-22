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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.renard.ocr.R;
import com.renard.util.PreferencesUtils;

public class DocumentFragment extends Fragment {
	private Spanned mSpanned;
	private EditText mEditText;
	
	public static DocumentFragment newInstance(final String text) {
		DocumentFragment f = new DocumentFragment();
		// Supply text input as an argument.
		Bundle args = new Bundle();
		args.putString("text", text);
		f.setArguments(args);
		return f;
	}
	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (mSpanned == null) {
			final String text = getArguments().getString("text");
			mSpanned = Html.fromHtml(text);
		}
		View view = inflater.inflate(R.layout.document_fragment, container,false);
		mEditText =(EditText) view.findViewById(R.id.editText_document);
		mEditText.setText(mSpanned);
		return view;
	}
		
	@Override
	public void onStart() {
		super.onStart();
		PreferencesUtils.applyTextPreferences(mEditText, getActivity());
	}
}
