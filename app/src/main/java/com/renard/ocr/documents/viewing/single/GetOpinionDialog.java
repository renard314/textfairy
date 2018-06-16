package com.renard.ocr.documents.viewing.single;

import com.googlecode.tesseract.android.OCR;
import com.renard.ocr.R;
import com.renard.ocr.main_menu.ContactActivity;
import com.renard.ocr.main_menu.FeedbackActivity;

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

import java.io.File;

/**
 * @author renard
 */
public class GetOpinionDialog extends TopDialogFragment implements DialogInterface.OnClickListener {

    private static final String SCREEN_NAME = "Ocr Opinion Dialog";
    private static final String SCREEN_NAME_LOVE_IT = "Ocr Opinion Dialog - Love it";
    private static final String SCREEN_NAME_COULD_BE_BETTER = "Ocr Opinion Dialog - Could be better";
    private static final String SCREEN_NAME_RATE_ON_PLAY_STORE = "Ocr Opinion Dialog - Rate on Play Store";


    public interface FeedbackDialogClickListener {
        void onContinueClicked();
    }


    public static final String TAG = GetOpinionDialog.class.getSimpleName();
    private static final String EXTRA_LANGUAGE = "extra_language";

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


    public static GetOpinionDialog newInstance(String language) {
        Bundle extra = new Bundle();
        extra.putString(EXTRA_LANGUAGE, language);
        final GetOpinionDialog dialog = new GetOpinionDialog();
        dialog.setArguments(extra);
        return dialog;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        getAnalytics().sendScreenView(SCREEN_NAME);
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity(), R.style.DialogSlideAnim);
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_feedback, null);
        mButtonDivider = view.findViewById(R.id.button_divider);
        mCouldBeBetter = (Button) view.findViewById(R.id.could_be_better);
        mLoveIt = (Button) view.findViewById(R.id.love_it);
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
        builder.setView(view);
        builder.setPositiveButton(R.string.continue_editing, this);
        final AlertDialog alertDialog = builder.create();
        positionDialogAtTop(alertDialog);
        setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setOnDismissListener(this);
        return alertDialog;
    }


    private void showFeedbackButton() {
        getAnalytics().sendScreenView(SCREEN_NAME_COULD_BE_BETTER);
        final String language = getArguments().getString(EXTRA_LANGUAGE);
        final String body = getString(R.string.document_scanned_as, language);
        mLoveIt.setVisibility(View.GONE);
        mButtonDivider.setVisibility(View.GONE);
        mCouldBeBetter.setText(R.string.feedback_title);
        mCouldBeBetter.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                File lastOriginalImage = OCR.Companion.getLastOriginalImageFromCache(getActivity());
                Intent intent = ContactActivity.getFeedbackIntent(getActivity(), getString(R.string.feedback_subject), lastOriginalImage, body);
                startActivity(intent);
                dismiss();
                notifyListener();
            }
        });
        final Drawable drawable = getResources().getDrawable(R.drawable.ic_action_feedback);
        mCouldBeBetter.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
    }

    private void showPlayStoreRatingButton() {
        getAnalytics().sendScreenView(SCREEN_NAME_LOVE_IT);
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
        getAnalytics().sendScreenView(SCREEN_NAME_RATE_ON_PLAY_STORE);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri url = Uri.parse(FeedbackActivity.MARKET_URL);
        intent.setData(url);
        startActivity(intent);
    }

}
