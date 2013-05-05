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
