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
package com.renard.ocr.help;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.renard.ocr.R;
import com.renard.ocr.cropimage.MonitoredActivity;

public class ContributeActivity extends MonitoredActivity implements View.OnClickListener {
    private static final String MARKET_URL = "market://details?id=com.renard.ocr";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contribute);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        initAppIcon(this, -1);
        TextView email = (TextView) findViewById(R.id.button_send_feedback);
        email.setOnClickListener(this);

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
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.layout_enroll_beta_test:
            case R.id.button_enroll_beta_test:
                break;
            case R.id.layout_send_feedback:
            case R.id.button_send_feedback:
                intent = ContactActivity.getFeedbackIntent();
                startActivity(intent);
                break;
            case R.id.layout_rate_app:
            case R.id.button_rate_app:
                intent = new Intent(Intent.ACTION_VIEW);
                Uri url = Uri.parse(MARKET_URL);
                intent.setData(url);
                startActivity(intent);
                break;
        }

    }

}
