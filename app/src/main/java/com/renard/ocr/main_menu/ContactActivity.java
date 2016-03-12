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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.FileProvider;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import java.io.File;

public class ContactActivity extends MonitoredActivity {

    private static final String FEEDBACK_MAIL = "textfairyapp@gmail.com";

    public static Intent getBetaTestIntent(Context context) {
        return new Intent(context, BetaTestActivity.class);
    }

    public static Intent getFeedbackIntent(String subject, String body) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        if (subject != null) {
            intent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        }
        if (body != null) {
            intent.putExtra(Intent.EXTRA_TEXT, body);
        }
        intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{FEEDBACK_MAIL});
        return intent;
    }

    public static Intent getFeedbackIntent(Context context, String subject, File file, String body) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        if (subject != null) {
            intent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        }
        if (file.exists()) {
            final Uri uriForFile = FileProvider.getUriForFile(context, context.getString(R.string.config_share_file_auth), file);
            intent.putExtra(Intent.EXTRA_STREAM, uriForFile);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        if (body != null) {
            intent.putExtra(Intent.EXTRA_TEXT, body);
        }

        intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{FEEDBACK_MAIL});
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);
        initToolbar();
        setToolbarMessage(R.string.pref_title_contact);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        TextView email = (TextView) findViewById(R.id.textView_send_mail);
        email.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = getFeedbackIntent(getString(R.string.feedback_subject), null);
                startActivity(Intent.createChooser(intent, getString(R.string.feedback_title)));
            }
        });
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
        return "Contact";
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
