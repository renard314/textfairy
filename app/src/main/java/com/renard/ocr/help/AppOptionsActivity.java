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
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;

import com.lamerman.FileDialog;
import com.lamerman.SelectionMode;
import com.renard.ocr.R;
import com.renard.ocr.cropimage.MonitoredActivity;
import com.renard.util.PreferencesUtils;

/**
 * preferences dialog for app wide settings or info stuff
 *
 * @author renard
 */

public class AppOptionsActivity extends MonitoredActivity implements View.OnClickListener {

    protected static final int REQUEST_LOAD_FILE = 474;
    private boolean slideOutLeft = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        initAppIcon(this, -1);

        findViewById(R.id.language_settings).setOnClickListener(this);
        findViewById(R.id.tessdata_directory).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.language_settings:
                slideOutLeft=true;
                startActivity(new Intent(AppOptionsActivity.this, OCRLanguageActivity.class));
                break;
            case R.id.tessdata_directory: {
                Intent intent = new Intent(getBaseContext(), FileDialog.class);
                intent.putExtra(FileDialog.START_PATH, Environment.getExternalStorageDirectory().getPath());

                //can user select directories or not
                intent.putExtra(FileDialog.CAN_SELECT_DIR, true);
                intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);
                //alternatively you can set file filter
                intent.putExtra(FileDialog.FORMAT_FILTER, new String[]{""});
                slideOutLeft=true;
                startActivityForResult(intent, REQUEST_LOAD_FILE);
                break;
            }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_LOAD_FILE) {
            if (resultCode == RESULT_OK) {
                String filePath = data.getStringExtra(FileDialog.RESULT_PATH);
                if (filePath != null) {
                    if (filePath.endsWith("tessdata")) {
                        filePath = filePath.substring(0, filePath.length() - "tessdata".length());
                    } else {
                        filePath += "/";
                    }
                    PreferencesUtils.saveTessDir(this, filePath);
                }
            }

        }
    }

}
