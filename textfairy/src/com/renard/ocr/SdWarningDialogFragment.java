package com.renard.ocr;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SdWarningDialogFragment extends DialogFragment {
	
	private SdReadyListener listener;

	interface SdReadyListener {
		void onSdReady();
		void onSdNotReady();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		return super.onCreateView(inflater, container, savedInstanceState);
	}
}
