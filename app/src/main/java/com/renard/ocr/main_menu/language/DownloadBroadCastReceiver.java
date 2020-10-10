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

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class DownloadBroadCastReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = DownloadBroadCastReceiver.class.getSimpleName();
    static final String ACTION_INSTALL_COMPLETED = "com.renard.ocr.ACTION_OCR_LANGUAGE_INSTALLED";
    static final String ACTION_INSTALL_FAILED = "com.renard.ocr.ACTION_INSTALL_FAILED";
    static final String EXTRA_OCR_LANGUAGE = "ocr_language";
    static final String EXTRA_OCR_LANGUAGE_DISPLAY = "ocr_language_display";
    public static final String EXTRA_STATUS = "status";

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
            DownloadManager dm =
                    (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            Query query = new Query();
            query.setFilterById(downloadId);
            Cursor c = dm.query(query);
            if (c == null) {
                return;
            }
            if (c.moveToFirst()) {
                int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int status = c.getInt(columnIndex);
                columnIndex = c.getColumnIndex(DownloadManager.COLUMN_TITLE);
                String title = c.getString(columnIndex);
                columnIndex = c.getColumnIndex(DownloadManager.COLUMN_URI);
                String name = c.getString(columnIndex);
                final Uri fileUri = Uri.parse(name);
                final String lang = extractLanguageNameFromUri(fileUri);

                if (DownloadManager.STATUS_SUCCESSFUL == status) {
                    Log.i(LOG_TAG, "Download successful");
                    notifySuccess(context, lang);
                } else if (DownloadManager.STATUS_FAILED == status) {
                    Log.i(LOG_TAG, "Download failed");
                    notifyError(context, status, title);
                }
            }
            c.close();
        }
    }

    private void notifyError(Context context, int status, String title) {
        Intent resultIntent = new Intent(ACTION_INSTALL_FAILED);
        resultIntent.putExtra(EXTRA_STATUS, status);
        resultIntent.putExtra(EXTRA_OCR_LANGUAGE_DISPLAY, title);
        context.sendBroadcast(resultIntent);
    }

    private void notifySuccess(Context context, String lang) {
        Intent resultIntent = new Intent(ACTION_INSTALL_COMPLETED);
        resultIntent.putExtra(EXTRA_OCR_LANGUAGE, lang);
        resultIntent.putExtra(EXTRA_STATUS, DownloadManager.STATUS_SUCCESSFUL);
        context.sendBroadcast(resultIntent);
    }

    private String extractLanguageNameFromUri(final Uri fileName) {
        final String lastPathSegment = fileName.getLastPathSegment();
        int index = lastPathSegment.indexOf(".traineddata");
        return lastPathSegment.substring(0, index);
    }
}
