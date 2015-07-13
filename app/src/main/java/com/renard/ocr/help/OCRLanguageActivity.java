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

import com.renard.ocr.R;
import com.renard.ocr.cropimage.MonitoredActivity;
import com.renard.ocr.help.OCRLanguageAdapter.OCRLanguage;
import com.renard.util.Util;

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
import android.support.v4.app.NavUtils;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class OCRLanguageActivity extends MonitoredActivity {

    public final static String ACTION_OCR_LANGUAGE_READY = "com.renard.ocr.language_ready";
    public final static String EXTRA_OCR_LANGUAGE = "com.renard.ocr.language";
    public static final String DOWNLOADED_TRAINING_DATA = "downloaded_training_data.tmp";

    private BroadcastReceiver mDownloadReceiver;
    private ListView mList;
    private OCRLanguageAdapter mAdapter;
    private ViewSwitcher mSwitcher;
    private BroadcastReceiver mFailedReceiver;
    private boolean mReceiverRegistered;

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

                    OCRLanguage language = (OCRLanguage) mAdapter.getItem(position);
                    if (!language.mDownloaded) {
                        final DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                        Uri uri = getDownloadUri(language);
                        Request request = new Request(uri);
                        request.setTitle(language.mDisplayText);
                        String tessDir = Util.getDownloadTempDir(OCRLanguageActivity.this);
                        File targetFile = new File(tessDir, DOWNLOADED_TRAINING_DATA);
                        request.setDestinationUri(Uri.fromFile(targetFile));
                        @SuppressWarnings("unused")
                        long downloadId = dm.enqueue(request);
                        language.mDownloading = true;
                        mAdapter.notifyDataSetChanged();

                    } else {
                        deleteLanguage(position);
                    }

                }
            });

        }

        protected void deleteLanguage(int position) {
            final OCRLanguage language = (OCRLanguage) mAdapter.getItem(position);
            AlertDialog.Builder b = new Builder(OCRLanguageActivity.this);
            String msg = getString(R.string.delete_language_message);
            String title = getString(R.string.delete_language_title);
            title = String.format(title, language.mDisplayText);
            msg = String.format(msg, language.mSize / 1024);
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
                    if (deleteLanguage(language)) {
                        language.mDownloaded = false;
                        mAdapter.notifyDataSetChanged();
                    }

                }
            });
            b.show();

        }

        private boolean deleteLanguage(final OCRLanguage language) {
            File tessDir = Util.getTrainingDataDir(OCRLanguageActivity.this);
            if (!tessDir.exists()) {
                return false;
            }

            File lang = new File(tessDir, language.mValue + ".traineddata");
            final String[] list = getCubeFilesForLanguage(language.mValue, OCRLanguageActivity.this);
            for (String fileName : list) {
                File f = new File(tessDir, fileName);
                f.delete();
            }
            if (lang.delete()) {
                return true;
            }
            return false;
        }

        @Override
        protected OCRLanguageAdapter doInBackground(Void... params) {
            return initLanguageList();
        }

    }

    private Uri getDownloadUri(OCRLanguage language) {
        final String part1;
        final String part2;
        if ("deu-frak".equalsIgnoreCase(language.getValue())) {
            part1 = "https://tesseract-ocr.googlecode.com/files/";
            part2 = ".traineddata.gz";
        } else if ("guj".equalsIgnoreCase(language.getValue())) {
            part1 = "https://parichit.googlecode.com/files/";
            part2 = ".traineddata";

        } else {
            part1 = "http://tesseract-ocr.googlecode.com/files/tesseract-ocr-3.02.";
            part2 = ".tar.gz";
        }

        return Uri.parse(part1 + language.mValue + part2);
    }

    private static String[] getCubeFilesForLanguage(String language, final Context context) {
        final String prefix = language + ".cube";
        File tessDir = Util.getTrainingDataDir(context);
        if (!tessDir.exists()) {
            return new String[0];
        }
        final String[] list = tessDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                if (filename != null && filename.startsWith(prefix)) {
                    return true;
                }
                return false;
            }
        });
        if (list == null) {
            return new String[0];
        } else {
            return list;
        }
    }

    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr_language);
        mList = (ListView) findViewById(R.id.list_ocr_languages);
        mSwitcher = (ViewSwitcher) findViewById(R.id.viewSwitcher_language_list);
        initAppIcon(-1);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        new LoadListAsyncTask().execute();
    }

    ;

    private OCRLanguageAdapter initLanguageList() {
        OCRLanguageAdapter adapter = new OCRLanguageAdapter(getApplicationContext(), false);
        List<OCRLanguage> languages = getAllOCRLanguages(this);
        adapter.addAll(languages);
        updateLanguageListWithDownloadManagerStatus(adapter);
        return adapter;
    }

    public static boolean isLanguageInstalled(final String ocrLang, Context appContext) {
        final File tessDir = Util.getTrainingDataDir(appContext);
        if (!tessDir.exists()) {
            return false;
        }
        final File[] languageFiles = tessDir.listFiles(new FileFilter() {


            @Override
            public boolean accept(File pathname) {
                if (pathname.getName().equalsIgnoreCase(ocrLang + ".traineddata") && pathname.isFile()) {
                    return true;
                }
                return false;
            }
        });

        return languageFiles != null && languageFiles.length >= 1;

    }

    private static final List<Pair<String, Long>> getInstalledLanguages(Context appContext) {
        final List<Pair<String, Long>> result = new ArrayList<Pair<String, Long>>();
        final File tessDir = Util.getTrainingDataDir(appContext);
        if (!tessDir.exists()) {
            return result;
        }
        final File[] languageFiles = tessDir.listFiles(new FileFilter() {


            @Override
            public boolean accept(File pathname) {
                if (pathname.getName().endsWith(".traineddata") && pathname.isFile()) {
                    return true;
                }
                return false;
            }
        });

        for (final File val : languageFiles) {
            final int dotIndex = val.getName().indexOf('.');
            if (dotIndex > -1) {
                result.add(Pair.create(val.getName().substring(0, dotIndex), val.length()));
            }
        }
        return result;
    }

    private static List<OCRLanguage> getAllOCRLanguages(Context context) {
        List<OCRLanguage> languages = new ArrayList<OCRLanguage>();
        // actual values uses by tesseract
        final String[] languageValues = context.getResources().getStringArray(R.array.ocr_languages);
        // values shown to the user
        final String[] languageDisplayValues = new String[languageValues.length];
        for (int i = 0; i < languageValues.length; i++) {
            final String val = languageValues[i];
            final int firstSpace = val.indexOf(' ');
            languageDisplayValues[i] = languageValues[i].substring(firstSpace + 1, languageValues[i].length());
            languageValues[i] = languageValues[i].substring(0, firstSpace);
        }
        final List<Pair<String, Long>> installedLanguages = getInstalledLanguages(context);
        for (int i = 0; i < languageValues.length; i++) {
            boolean downloaded = false;
            long size = 0;
            for (Pair<String, Long> installedLang : installedLanguages) {
                if (installedLang.first.equalsIgnoreCase(languageValues[i])) {
                    downloaded = true;
                    size = installedLang.second;
                    break;
                }
            }
            OCRLanguage language = new OCRLanguage(languageValues[i], languageDisplayValues[i], downloaded, size);
            if (language.needsCubeData && getCubeFilesForLanguage(language.mValue, context).length == 0) {
                language.mDownloaded = false;
            }

            languages.add(language);
        }
        return languages;
    }


    public static final List<OCRLanguage> getInstalledOCRLanguages(Context appContext) {
        final List<OCRLanguage> ocrLanguages = getAllOCRLanguages(appContext);
        final List<OCRLanguage> result = new ArrayList<OCRLanguage>();
        for (OCRLanguage lang : ocrLanguages) {
            if (lang.mDownloaded) {
                result.add(lang);
            }
        }
        return result;
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

    // @Override
    // protected void onResume() {
    // super.onResume();
    // updateLanguageListWithDownloadManagerStatus(mAdapter);
    // registerDownloadReceiver();
    // }

    private void updateLanguageListWithDownloadManagerStatus(OCRLanguageAdapter adapter) {
        if (adapter != null) {
            // find languages that are currently beeing downloaded
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
            final OCRLanguage language = (OCRLanguage) mAdapter.getItem(i);
            if (language.getDisplayText().equalsIgnoreCase(displayValue)) {
                updateLanguage(language, status);
                return;
            }
        }
    }

    protected void updateLanguageList(String lang, int status) {
        for (int i = 0; i < mAdapter.getCount(); i++) {
            final OCRLanguage language = (OCRLanguage) mAdapter.getItem(i);
            if (language.getValue().equalsIgnoreCase(lang)) {
                updateLanguage(language, status);
                return;
            }
        }
    }

    private void updateLanguage(final OCRLanguage language, int status) {
        language.mDownloading = false;
        if (status == DownloadManager.STATUS_SUCCESSFUL) {
            language.mDownloaded = true;
        } else {
            language.mDownloaded = false;
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    String msg = getString(R.string.download_failed);
                    msg = String.format(msg, language.mDisplayText);
                    Toast.makeText(OCRLanguageActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            });
        }
        mAdapter.notifyDataSetChanged();
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


}
