package com.renard.ocr;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

public class ProgressDialogFragment extends DialogFragment {
	
	private static final String MESSAGE_ID = "message_id";
	private static final String TITLE_ID = "title_id";

	public static ProgressDialogFragment newInstance(int titleId, int messageId) {
		ProgressDialogFragment progressDialogFragment = new ProgressDialogFragment();
		Bundle args = new Bundle();
		args.putInt(TITLE_ID, titleId);
		args.putInt(MESSAGE_ID, messageId);
		progressDialogFragment.setArguments(args);
		return progressDialogFragment;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (outState.isEmpty()) {
			outState.putBoolean("bug:fix", true);
		}
	}
	
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		FragmentActivity activity = getActivity();
	    final ProgressDialog dialog = new ProgressDialog(activity);
	    Bundle arguments = getArguments();
	    if (arguments!=null){
	    	int titleId = arguments.getInt(TITLE_ID);
	    	int messageId = arguments.getInt(MESSAGE_ID);
		    dialog.setTitle(titleId);	    	
		    dialog.setMessage(activity.getText(messageId));
	    }
	    dialog.setIndeterminate(true);
	    dialog.setCancelable(false);
	    return dialog;
	}
}
