package com.renard.ocr.main_menu;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.renard.ocr.HintDialog;
import com.renard.ocr.R;

public class ReleaseNoteDialog extends DialogFragment {

	public final static String TAG = ReleaseNoteDialog.class.getSimpleName();

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final String htmlPath = "file:///android_res/raw/release_notes.html";
		return HintDialog.createDialog(getActivity(), R.string.whats_new_title, htmlPath);
	}

}
