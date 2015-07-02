package com.renard.documentview;

import com.googlecode.tesseract.android.OCR;
import com.renard.ocr.R;
import com.renard.ocr.help.ContactActivity;
import com.renard.ocr.help.ContributeActivity;
import com.renard.ocr.help.HelpActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;

/**
 * @author renard
 */
public class FeedbackDialog extends TopDialogFragment implements DialogInterface.OnClickListener {

    public static final String TAG = FeedbackDialog.class.getSimpleName();

    private final static String EXTRA_ACCURACY = "extra_accuracy";
    private Button mLoveIt;
    private Button mCouldBeBetter;
    private View mButtonDivider;

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        FeedbackDialogClickListener listener = (FeedbackDialogClickListener) getActivity();
        listener.onContinueClicked();

    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        notifyListener();
    }

    public interface FeedbackDialogClickListener {
        void onContinueClicked();
    }

    public static FeedbackDialog newInstance(float accuracy) {
        Bundle extra = new Bundle();
        extra.putInt(EXTRA_ACCURACY, (int) accuracy);
        final FeedbackDialog dialog = new FeedbackDialog();
        dialog.setArguments(extra);
        return dialog;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity(), R.style.DialogSlideAnim);
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_feedback, null);
        final int accuracy = getArguments().getInt(EXTRA_ACCURACY);
        mButtonDivider = view.findViewById(R.id.button_divider);
        mCouldBeBetter = (Button) view.findViewById(R.id.could_be_better);
        mLoveIt = (Button) view.findViewById(R.id.love_it);
        if (accuracy <= OCRResultDialog.LOW_ACCURACY) {
            final TextView title = (TextView) view.findViewById(R.id.fairy_text);
            title.setText(R.string.first_scan_unsuccessful);
            showFeedbackButton();
            showTipsButton();
            TextView explanationTextView = (TextView) view.findViewById(R.id.explanation_text);
            explanationTextView.setText(R.string.first_time_bad_result);
        } else {
            mLoveIt.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    showPlayStoreRatingButton();
                }
            });
            mCouldBeBetter.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    showFeedbackButton();
                }
            });
        }
        builder.setView(view);
        builder.setPositiveButton(R.string.continue_editing, this);
        final AlertDialog alertDialog = builder.create();

        positionDialogAtTop(alertDialog);

        setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setOnDismissListener(this);
        return alertDialog;
    }


    private void showTipsButton() {
        mLoveIt.setVisibility(View.VISIBLE);
        mButtonDivider.setVisibility(View.VISIBLE);
        mLoveIt.setText(R.string.show_tips);
        final Drawable drawable = getResources().getDrawable(R.drawable.ic_action_tips_light);
        mLoveIt.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
        mLoveIt.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), HelpActivity.class));
                dismiss();
                notifyListener();
            }
        });
    }

    private void showFeedbackButton() {
        mLoveIt.setVisibility(View.GONE);
        mButtonDivider.setVisibility(View.GONE);
        mCouldBeBetter.setText(R.string.feedback_title);
        mCouldBeBetter.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                File lastOriginalImage = OCR.getLastOriginalImageFromCache(getActivity());
                Intent intent = ContactActivity.getFeedbackIntent(getActivity(), getString(R.string.feedback_subject), lastOriginalImage);
                startActivity(intent);
                dismiss();
                notifyListener();
            }
        });
        final Drawable drawable = getResources().getDrawable(R.drawable.ic_action_feedback);
        mCouldBeBetter.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
    }

    private void showPlayStoreRatingButton() {
        mCouldBeBetter.setVisibility(View.GONE);
        mButtonDivider.setVisibility(View.GONE);
        mLoveIt.setText(R.string.rating_title);
        mLoveIt.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                rateOnPlayStore();
                dismiss();
                notifyListener();
            }
        });
        final Drawable drawable = getResources().getDrawable(R.drawable.ic_rating);
        mLoveIt.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
    }

    private void notifyListener() {
        FeedbackDialogClickListener listener = (FeedbackDialogClickListener) getActivity();
        listener.onContinueClicked();
    }

    private void rateOnPlayStore() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri url = Uri.parse(ContributeActivity.MARKET_URL);
        intent.setData(url);
        startActivity(intent);
    }

}
