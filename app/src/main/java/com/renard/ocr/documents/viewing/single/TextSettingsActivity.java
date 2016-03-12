/*
 * Copyright (C) 2012,2013 Renard Wellnitz.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.renard.ocr.documents.viewing.single;

import com.renard.ocr.R;
import com.renard.ocr.MonitoredActivity;
import com.renard.ocr.util.PreferencesUtils;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

public class TextSettingsActivity extends MonitoredActivity {

    private SharedPreferences mPreferences;
    private RadioGroup mTextAlignmentRadioGroup;
    private RadioGroup mLineSpacingRadioGroup;
    //private RadioGroup mDesignRadioGroup;
    private TextView mPreviewText;

    private static class MyOnCheckedChangedListener implements OnCheckedChangeListener {

        private String mPrefKey;
        private SharedPreferences mPreferences;
        private TextView mPreviewText;

        MyOnCheckedChangedListener(String prefKey, SharedPreferences prefs, TextView view) {
            mPrefKey = prefKey;
            mPreferences = prefs;
            mPreviewText = view;
        }

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            Editor edit = mPreferences.edit();
            edit.putInt(mPrefKey, checkedId);
            edit.apply();
            PreferencesUtils.applyTextPreferences(mPreviewText, mPreferences);
        }

    }

    @Override
    public String getScreenName() {
        return "Text Settings";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_text_options);
        initToolbar();
        setToolbarMessage(R.string.text_options_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPreferences = PreferencesUtils.getPreferences(getApplicationContext());

        mTextAlignmentRadioGroup = (RadioGroup) findViewById(R.id.text_alignment);
        mLineSpacingRadioGroup = (RadioGroup) findViewById(R.id.line_spacing);
        //mDesignRadioGroup = (RadioGroup) findViewById(R.id.text_designs);
        mPreviewText = (TextView) findViewById(R.id.preview_text);

        mLineSpacingRadioGroup.setOnCheckedChangeListener(new MyOnCheckedChangedListener(PreferencesUtils.PREFERENCES_SPACING_KEY, mPreferences, mPreviewText));
        //mDesignRadioGroup.setOnCheckedChangeListener(new MyOnCheckedChangedListener(PreferencesUtils.PREFERENCES_DESIGN_KEY, mPreferences, mPreviewText));
        mTextAlignmentRadioGroup.setOnCheckedChangeListener(new MyOnCheckedChangedListener(PreferencesUtils.PREFERENCES_ALIGNMENT_KEY, mPreferences, mPreviewText));
        // PreferencesUtils.applyTextPreferences(mPreviewText,
        // getApplicationContext());

        int id = mPreferences.getInt(PreferencesUtils.PREFERENCES_SPACING_KEY, -1);
        mLineSpacingRadioGroup.check(id);
        id = mPreferences.getInt(PreferencesUtils.PREFERENCES_DESIGN_KEY, -1);
        //mDesignRadioGroup.check(id);
        id = mPreferences.getInt(PreferencesUtils.PREFERENCES_ALIGNMENT_KEY, -1);
        mTextAlignmentRadioGroup.check(id);

        Button okButton = (Button) findViewById(R.id.button_ok);
        okButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected int getHintDialogId() {
        return -1;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
