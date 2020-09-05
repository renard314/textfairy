package com.renard.ocr.documents.creation.crop;

import com.renard.ocr.R;
import com.renard.ocr.documents.viewing.single.TopDialogFragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * @author renard
 */
public class BlurWarningDialog extends TopDialogFragment implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {

    public static final String TAG = BlurWarningDialog.class.getSimpleName();

    private final static String EXTRA_BLURRINES = "extra_blurrines";
    private static final String SCREEN_NAME = "Blur Warning Dialog";

    @Override
    public void onClick(DialogInterface dialog, int which) {
        BlurDialogClickListener listener = (BlurDialogClickListener) getActivity();
        final float blurriness = getArguments().getFloat(EXTRA_BLURRINES);
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                getAnalytics().newImageBecauseOfBlurWarning(blurriness);
                listener.onNewImageClicked();
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                getAnalytics().continueDespiteOfBlurWarning(blurriness);
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

    public static BlurWarningDialog newInstance(float blurrines) {
        Bundle extra = new Bundle();
        extra.putFloat(EXTRA_BLURRINES, blurrines);
        final BlurWarningDialog dialog = new BlurWarningDialog();
        dialog.setArguments(extra);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        getAnalytics().sendScreenView(SCREEN_NAME);
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