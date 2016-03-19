package com.renard.ocr.documents.viewing.single;

import com.renard.ocr.analytics.Analytics;
import com.renard.ocr.MonitoredActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.TypedArray;
import android.support.v4.app.DialogFragment;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

/**
 * @author renard
 */
public class TopDialogFragment extends DialogFragment {

    private Analytics mAnalytics;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        MonitoredActivity monitoredActivity = (MonitoredActivity) getActivity();
        mAnalytics = monitoredActivity.getAnaLytics();
    }

    public Analytics getAnalytics() {
        if (mAnalytics == null && getActivity() != null) {
            MonitoredActivity activity = (MonitoredActivity) getActivity();
            return activity.getAnaLytics();
        }
        return mAnalytics;
    }

    protected void positionDialogAtTop(AlertDialog alertDialog) {
        Window window = alertDialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        final TypedArray styledAttributes = getActivity().getTheme().obtainStyledAttributes(new int[]{android.R.attr.actionBarSize});
        int actionBarSize = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();
        wlp.y = actionBarSize;
        wlp.gravity = Gravity.TOP;
        window.setAttributes(wlp);
    }

}
