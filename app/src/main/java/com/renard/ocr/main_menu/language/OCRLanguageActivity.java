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
package com.renard.ocr.main_menu.language;

import com.renard.ocr.MonitoredActivity;
import com.renard.ocr.R;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import androidx.core.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import java.util.Iterator;
import java.util.List;

public class OCRLanguageActivity extends MonitoredActivity {

    private BroadcastReceiver mDownloadReceiver;
    private ListView mList;
    private OCRLanguageAdapter mAdapter;
    private ViewSwitcher mSwitcher;
    private BroadcastReceiver mFailedReceiver;
    private boolean mReceiverRegistered;
    private final DownloadManagerResolver mDownloadManagerResolver = new DownloadManagerResolver();

    private class LoadListAsyncTask extends AsyncTask<Void, Void, OCRLanguageAdapter> {

        @Override
        protected void onPostExecute(OCRLanguageAdapter result) {
            super.onPostExecute(result);
            registerDownloadReceiver();
            mAdapter = result;
            mList.setAdapter(result);
            mSwitcher.setDisplayedChild(1);
            mList.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    OcrLanguage language = (OcrLanguage) mAdapter.getItem(position);
                    if (!language.isInstalled()) {
                        if (mDownloadManagerResolver.resolve(OCRLanguageActivity.this)) {
                            startDownload(language);
                        }

                    } else {
                        deleteLanguage(position);
                    }

                }
            });

        }

        private void startDownload(OcrLanguage language) {
            mAnalytics.sendStartDownload(language);

            final DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            List<Uri> uris = language.getDownloadUris();
            for (Uri uri : uris) {
                Request request = new Request(uri);
                request.setTitle(language.getDisplayText());
                dm.enqueue(request);
            }

            language.setDownloading(true);
            mAdapter.notifyDataSetChanged();
        }

        protected void deleteLanguage(int position) {
            final OcrLanguage language = (OcrLanguage) mAdapter.getItem(position);

            AlertDialog.Builder b = new Builder(OCRLanguageActivity.this);
            String msg = getString(R.string.delete_language_message);
            String title = getString(R.string.delete_language_title);
            title = String.format(title, language.getDisplayText());
            msg = String.format(msg, language.getSize() / 1024);
            b.setTitle(title);
            b.setMessage(msg);
            b.setCancelable(true);
            b.setNegativeButton(R.string.cancel, new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            b.setPositiveButton(R.string.ocr_language_delete, new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    OcrLanguageDataStore.deleteLanguage(language, OCRLanguageActivity.this);
                    mAdapter.notifyDataSetChanged();
                    mAnalytics.sendDeleteLanguage(language);

                }
            });
            b.show();

        }

        @Override
        protected OCRLanguageAdapter doInBackground(Void... params) {
            return initLanguageList();
        }

    }

    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr_language);
        mList = (ListView) findViewById(R.id.list_ocr_languages);
        mSwitcher = (ViewSwitcher) findViewById(R.id.viewSwitcher_language_list);
        initToolbar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setToolbarMessage(R.string.ocr_language_title);
        new LoadListAsyncTask().execute();
    }

    @Override
    protected int getHintDialogId() {
        return -1;
    }


    private OCRLanguageAdapter initLanguageList() {
        OCRLanguageAdapter adapter = new OCRLanguageAdapter(getApplicationContext(), false);
        List<OcrLanguage> languages = OcrLanguageDataStore.getAvailableOcrLanguages(this);
        hideArabicDownload(languages);
        adapter.addAll(languages);
        updateLanguageListWithDownloadManagerStatus(adapter);
        return adapter;
    }

    private void hideArabicDownload(List<OcrLanguage> languages) {
        Iterator<OcrLanguage> it = languages.iterator();
        while (it.hasNext()) {
            final OcrLanguage lang = it.next();
            if (lang.getValue().equalsIgnoreCase("ara")) {
                it.remove();
                return;
            }
        }
    }


    @Override
    protected synchronized void onDestroy() {
        super.onDestroy();
        if (mReceiverRegistered) {
            unregisterReceiver(mDownloadReceiver);
            unregisterReceiver(mFailedReceiver);
            mReceiverRegistered = false;
        }
    }


    private void updateLanguageListWithDownloadManagerStatus(OCRLanguageAdapter adapter) {
        if (adapter != null) {
            // find languages that are currently being downloaded
            Query query = new Query();
            query.setFilterByStatus(DownloadManager.STATUS_RUNNING | DownloadManager.STATUS_PENDING | DownloadManager.STATUS_PAUSED);
            final DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            Cursor c = dm.query(query);
            if (c == null) {
                return;
            }
            int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_TITLE);
            while (c.moveToNext()) {
                final String title = c.getString(columnIndex);
                adapter.setDownloading(title, true);
            }
            adapter.notifyDataSetChanged();
            c.close();
        }
    }

    private void registerDownloadReceiver() {
        mDownloadReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String lang = intent.getStringExtra(OCRLanguageInstallService.EXTRA_OCR_LANGUAGE);
                int status = intent.getIntExtra(OCRLanguageInstallService.EXTRA_STATUS, -1);
                updateLanguageList(lang, status);
            }

        };
        mFailedReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String lang = intent.getStringExtra(OCRLanguageInstallService.EXTRA_OCR_LANGUAGE_DISPLAY);
                int status = intent.getIntExtra(OCRLanguageInstallService.EXTRA_STATUS, -1);
                updateLanguageListByDisplayValue(lang, status);
            }
        };
        registerReceiver(mFailedReceiver, new IntentFilter(OCRLanguageInstallService.ACTION_INSTALL_FAILED));
        registerReceiver(mDownloadReceiver, new IntentFilter(OCRLanguageInstallService.ACTION_INSTALL_COMPLETED));
        mReceiverRegistered = true;
    }

    protected void updateLanguageListByDisplayValue(String displayValue, int status) {
        for (int i = 0; i < mAdapter.getCount(); i++) {
            final OcrLanguage language = (OcrLanguage) mAdapter.getItem(i);
            if (language.getDisplayText().equalsIgnoreCase(displayValue)) {
                updateLanguage(language, status);
                return;
            }
        }
    }

    protected void updateLanguageList(String lang, int status) {
        for (int i = 0; i < mAdapter.getCount(); i++) {
            final OcrLanguage language = (OcrLanguage) mAdapter.getItem(i);
            if (language.getValue().equalsIgnoreCase(lang)) {
                updateLanguage(language, status);
                return;
            }
        }
    }

    private void updateLanguage(final OcrLanguage language, int status) {
        if (status == DownloadManager.STATUS_SUCCESSFUL) {
            final InstallStatus installStatus = OcrLanguageDataStore.isLanguageInstalled(language.getValue(), OCRLanguageActivity.this);
            language.setInstallStatus(installStatus);
            if (installStatus.isInstalled()) {
                language.setDownloading(false);
                mAdapter.notifyDataSetChanged();
            }
        } else {
            language.setDownloading(false);
            mAdapter.notifyDataSetChanged();
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    String msg = getString(R.string.download_failed);
                    msg = String.format(msg, language.getDisplayText());
                    Toast.makeText(OCRLanguageActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            });
        }
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
    public String getScreenName() {
        return "Ocr Languages";
    }

}