package com.renard.ocr.documents.creation;

import com.renard.ocr.R;
import com.renard.ocr.documents.viewing.single.TopDialogFragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

/**
 * @author renard
 */
public class MemoryWarningDialog extends TopDialogFragment {

    private static final String SCREEN_NAME = "Memory Warning Dialog";

    public static final String TAG = MemoryWarningDialog.class.getSimpleName();
    private static final String EXTRA_MEMORY = "extra_available_memory";
    private static final String EXTRA_DO_AFTER = "extra_do_after";

    public enum DoAfter {
        START_GALLERY, START_CAMERA;
    }

    public static MemoryWarningDialog newInstance(long availableMemory, DoAfter doAfter) {
        Bundle extra = new Bundle();
        extra.putLong(EXTRA_MEMORY, availableMemory);
        extra.putInt(EXTRA_DO_AFTER, doAfter.ordinal());
        final MemoryWarningDialog dialog = new MemoryWarningDialog();
        dialog.setArguments(extra);
        return dialog;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final DoAfter doAfter = DoAfter.values()[getArguments().getInt(EXTRA_DO_AFTER)];
        final long availableMegs = getArguments().getLong(EXTRA_MEMORY);
        getAnalytics().sendScreenView(SCREEN_NAME);
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_memory_warning, null);
        TextView description = (TextView) view.findViewById(R.id.memory_low_description);
        final String descriptionText = getActivity().getString(R.string.memory_low_description, availableMegs);
        description.setText(descriptionText);
        builder.setTitle(R.string.memory_low);
        builder.setView(view);
        builder.setPositiveButton(R.string.continue_ocr, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getAnalytics().sendIgnoreMemoryWarning(availableMegs);
                NewDocumentActivity activity = (NewDocumentActivity) getActivity();
                if (doAfter == DoAfter.START_CAMERA) {
                    activity.startCamera();
                } else if (doAfter == DoAfter.START_GALLERY) {
                    activity.startGallery();
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getAnalytics().sendHeedMemoryWarning(availableMegs);
                dialog.dismiss();
            }
        });

        final AlertDialog alertDialog = builder.create();
        setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setOnDismissListener(this);
        return alertDialog;
    }


}
