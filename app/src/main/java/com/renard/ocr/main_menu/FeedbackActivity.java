/*
 * Copyright (C) 2012,2013 Renard Wellnitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.renard.ocr.main_menu;

import com.renard.ocr.MonitoredActivity;
import com.renard.ocr.R;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.core.app.NavUtils;
import android.view.MenuItem;
import android.view.View;

public class FeedbackActivity extends MonitoredActivity implements View.OnClickListener {
    public static final String MARKET_URL = "market://details?id=com.renard.ocr";
    private boolean slideOutLeft = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contribute);
        initToolbar();
        setToolbarMessage(R.string.contribute_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        findViewById(R.id.layout_enroll_beta_test).setOnClickListener(this);
        findViewById(R.id.layout_rate_app).setOnClickListener(this);
        findViewById(R.id.layout_send_feedback).setOnClickListener(this);
    }

    @Override
    protected int getHintDialogId() {
        return -1;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (slideOutLeft) {
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } else {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }

    @Override
    public String getScreenName() {
        return "Feedback";
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.layout_enroll_beta_test:
            case R.id.button_enroll_beta_test:
                slideOutLeft = true;
                intent = ContactActivity.getBetaTestIntent(this);
                startActivity(intent);
                break;
            case R.id.layout_send_feedback:
            case R.id.button_send_feedback:
                slideOutLeft = true;
                intent = ContactActivity.getFeedbackIntent(getString(R.string.feedback_subject), null);
                startActivity(Intent.createChooser(intent, getString(R.string.feedback_title)));
                break;
            case R.id.layout_rate_app:
            case R.id.button_rate_app:
                slideOutLeft = true;
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                intent = new Intent(Intent.ACTION_VIEW);
                Uri url = Uri.parse(MARKET_URL);
                intent.setData(url);
                startActivity(intent);
                break;
        }

    }

}
