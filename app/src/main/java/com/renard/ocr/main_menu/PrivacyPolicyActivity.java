package com.renard.ocr.main_menu;

import com.renard.ocr.HintDialog;
import com.renard.ocr.MonitoredActivity;
import com.renard.ocr.R;

import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.SwitchCompat;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;

/**
 * @author renard
 */
public class PrivacyPolicyActivity extends MonitoredActivity {

    @BindView(R.id.webview)
    protected WebView mWebView;
    @BindView(R.id.tracking_toggle)
    protected SwitchCompat mSwitchCompat;
    @BindView(R.id.tacking_label)
    protected TextView mTrackingLabel;


    @Override
    protected synchronized void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);
        ButterKnife.bind(this);
        initToolbar();
        setToolbarMessage(R.string.pref_title_privacy);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        initWebView();
        initTrackingToggle();
    }

    private void initTrackingToggle() {
        boolean isOptedOut = getAnaLytics().getAppOptOut();
        mSwitchCompat.setChecked(!isOptedOut);
        mTrackingLabel.setText(isOptedOut ? R.string.data_tracking_off : R.string.data_tracking_on);
    }

    @OnCheckedChanged(R.id.tracking_toggle)
    public void onDataTrackingToggled(boolean isChecked) {
        getAnaLytics().toggleTracking(!isChecked);
        mTrackingLabel.setText(!isChecked ? R.string.data_tracking_off : R.string.data_tracking_on);
    }

    private void initWebView() {
        final String path = HintDialog.convertResourceIdToPath(getApplication(), R.raw.privacy_policy);
        mWebView.loadUrl(path);
    }

    @Override
    public String getScreenName() {
        return "Privacy Policy";
    }

    @Override
    protected int getHintDialogId() {
        return -1;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
