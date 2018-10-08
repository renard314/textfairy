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

import android.app.AlarmManager;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

public class DownloadBroadCastReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = DownloadBroadCastReceiver.class.getSimpleName();

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
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


                if (DownloadManager.STATUS_SUCCESSFUL == status) {
                    Log.i(LOG_TAG, "Download successful");
                    //start service to extract language file
                    Intent serviceIntent = new Intent();
                    serviceIntent.putExtra(DownloadManager.EXTRA_DOWNLOAD_ID, downloadId);
                    serviceIntent.putExtra(OCRLanguageInstallService.EXTRA_FILE_NAME, name);
                    OCRLanguageInstallService.enqueueWork(context, serviceIntent);
                } else if (DownloadManager.STATUS_FAILED == status) {
                    Log.i(LOG_TAG, "Download failed");
                    Intent resultIntent = new Intent(OCRLanguageInstallService.ACTION_INSTALL_FAILED);
                    resultIntent.putExtra(OCRLanguageInstallService.EXTRA_STATUS, status);
                    resultIntent.putExtra(OCRLanguageInstallService.EXTRA_OCR_LANGUAGE_DISPLAY, title);
                    context.sendBroadcast(resultIntent);
                }
            }
            c.close();

        }
    }

}
