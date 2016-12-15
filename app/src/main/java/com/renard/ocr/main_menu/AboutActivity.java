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
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AboutActivity extends MonitoredActivity {

    private boolean slideOutLeft = false;
    @BindView(R.id.version_name)
    protected TextView mVersionView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        ButterKnife.bind(this);
        initToolbar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setToolbarMessage(R.string.about);
        showVersionNumber();
    }

    private void showVersionNumber() {
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            mVersionView.setText(getString(R.string.app_version, versionName));
        } catch (PackageManager.NameNotFoundException e) {
            mVersionView.setVisibility(View.GONE);
        }
    }


    @OnClick(R.id.show_licences)
    public void clickOnLicense() {
        slideOutLeft = true;
        startActivity(new Intent(this, LicenseActivity.class));
    }

    @OnClick(R.id.show_contact)
    public void clickOnContact() {
        slideOutLeft = true;
        startActivity(new Intent(this, ContactActivity.class));
    }

    @OnClick(R.id.privacy_policy)
    public void clickOnPrivacyPolicy() {
        slideOutLeft = true;
        startActivity(new Intent(this, PrivacyPolicyActivity.class));
    }

    @Override
    protected int getHintDialogId() {
        return -1;
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
        return "About";
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
