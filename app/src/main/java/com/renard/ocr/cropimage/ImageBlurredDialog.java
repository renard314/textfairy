package com.renard.ocr.cropimage;

import com.renard.documentview.TopDialogFragment;
import com.renard.ocr.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * Created by renard on 12/11/13.
 */
public class ImageBlurredDialog extends TopDialogFragment implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {

    public static final String TAG = ImageBlurredDialog.class.getSimpleName();

    private final static String EXTRA_BLURRINES = "extra_blurrines";

    @Override
    public void onClick(DialogInterface dialog, int which) {
        BlurDialogClickListener listener = (BlurDialogClickListener) getActivity();
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                listener.onNewImageClicked();
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                listener.onContinueClicked();
                break;
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        BlurDialogClickListener listener = (BlurDialogClickListener) getActivity();
        listener.onContinueClicked();

    }

    interface BlurDialogClickListener {
        void onContinueClicked();

        void onNewImageClicked();
    }

    public static ImageBlurredDialog newInstance(float blurrines) {
        Bundle extra = new Bundle();
        extra.putFloat(EXTRA_BLURRINES, blurrines);
        final ImageBlurredDialog dialog = new ImageBlurredDialog();
        dialog.setArguments(extra);
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity(), R.style.DialogSlideAnim);
        builder.setCancelable(true);
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_blur_warning, null);
        final float blurriness = getArguments().getFloat(EXTRA_BLURRINES);
        TextView titleTextView = (TextView) view.findViewById(R.id.blur_warning_title);
        if (blurriness > .75) {
            titleTextView.setText(R.string.text_is_very_blurry);
        } else {
            titleTextView.setText(R.string.text_is_blurry);
        }
        builder.setView(view);
        builder.setOnCancelListener(this);
        builder.setNegativeButton(R.string.continue_ocr, this);
        builder.setPositiveButton(R.string.new_image, this);
        final AlertDialog alertDialog = builder.create();
        Window window = alertDialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.BOTTOM;
        window.setAttributes(wlp);
        setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);
        positionDialogAtTop(alertDialog);
        return alertDialog;
    }


}