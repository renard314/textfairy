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

import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

public class LicenseActivity extends MonitoredActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);
        initToolbar();
        setToolbarMessage(R.string.pref_title_license);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        TextView leptonica = (TextView) findViewById(R.id.textView_leptonica);
        TextView tesseract = (TextView) findViewById(R.id.textView_tesseract);
        TextView hocr2pdf = (TextView) findViewById(R.id.textView_hocr2pdf);
        leptonica.setMovementMethod(new LinkMovementMethod());
        tesseract.setMovementMethod(new LinkMovementMethod());
        hocr2pdf.setMovementMethod(new LinkMovementMethod());
    }

    @Override
    protected int getHintDialogId() {
        return -1;
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    public String getScreenName() {
        return "Licenses";
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

}
