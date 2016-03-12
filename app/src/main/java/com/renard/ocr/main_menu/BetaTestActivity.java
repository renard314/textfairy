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

import com.renard.ocr.R;
import com.renard.ocr.MonitoredActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;

public class BetaTestActivity extends MonitoredActivity implements View.OnClickListener {

    private boolean slideOutLeft = false;
    private final static String BETA_TEST_GROUP_URL = "https://plus.google.com/communities/105320277782726490448";
    private final static String GOOGLE_PLAY_TEST_URL = "https://play.google.com/apps/testing/com.renard.ocr";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beta_test);
        initToolbar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setToolbarMessage(R.string.beta_test_title);
        findViewById(R.id.textView_become_tester).setOnClickListener(this);
        findViewById(R.id.textView_join_community).setOnClickListener(this);
    }

    @Override
    protected int getHintDialogId() {
        return -1;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.textView_become_tester:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLE_PLAY_TEST_URL)));
                break;
            case R.id.textView_join_community:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(BETA_TEST_GROUP_URL)));
                break;
        }

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
        return "Beta Test";
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setDialogId(int dialogId) {
        // ignored
    }

}
