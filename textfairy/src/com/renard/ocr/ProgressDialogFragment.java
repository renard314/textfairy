package com.renard.ocr;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NO_TITLE,getTheme());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.progress_dialog, container, false);

		Bundle arguments = getArguments();
		if (arguments != null) {
			int titleId = arguments.getInt(TITLE_ID);
			int messageId = arguments.getInt(MESSAGE_ID);
			TextView title = (TextView) view.findViewById(R.id.title);
			TextView message = (TextView) view.findViewById(R.id.message);
			title.setText(titleId);
			message.setText(messageId);
		}
		return view;
	}

}
