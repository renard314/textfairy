package com.renard.ocr.main_menu;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.renard.ocr.HintDialog;
import com.renard.ocr.R;

public class ReleaseNoteDialog extends DialogFragment {

	public final static String TAG = ReleaseNoteDialog.class.getSimpleName();

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return HintDialog.createDialog(getActivity(), R.string.whats_new_title, R.raw.release_notes);
	}

}
