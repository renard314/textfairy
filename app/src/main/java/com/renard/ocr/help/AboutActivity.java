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
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.renard.ocr.R;
import com.renard.ocr.cropimage.MonitoredActivity;

public class AboutActivity extends MonitoredActivity implements View.OnClickListener {

    private boolean slideOutLeft = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        initAppIcon(-1);
        findViewById(R.id.show_licences).setOnClickListener(this);
        findViewById(R.id.show_contact).setOnClickListener(this);
        TextView version = (TextView) findViewById(R.id.version_name);
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            version.setText(getString(R.string.app_version,versionName));
        } catch (PackageManager.NameNotFoundException e) {
            version.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.show_licences:
                slideOutLeft = true;
                startActivity(new Intent(this, LicenseActivity.class));
                break;
            case R.id.show_contact:
                slideOutLeft = true;
                startActivity(new Intent(this, ContactActivity.class));
                break;
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (slideOutLeft){
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } else {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
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
