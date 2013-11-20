package com.renard.documentview;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.renard.ocr.R;

/**
 * Created by renard on 12/11/13.
 */
public class OCRResultDialog extends SherlockDialogFragment {

    public static final String TAG = OCRResultDialog.class.getSimpleName();

    public static OCRResultDialog newInstance() {
        return new OCRResultDialog();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
        builder.setCancelable(true);
        View view = getActivity().getLayoutInflater().inflate(R.layout.ocr_result_dialog, null);
        builder.setView(view);
        builder.setTitle("TITLE");
        return builder.create();
    }

}