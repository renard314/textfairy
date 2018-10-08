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

import com.renard.ocr.util.Util;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class OCRLanguageInstallService extends JobIntentService {

    static final String ACTION_INSTALL_COMPLETED = "com.renard.ocr.ACTION_OCR_LANGUAGE_INSTALLED";
    static final String ACTION_INSTALL_FAILED = "com.renard.ocr.ACTION_INSTALL_FAILED";
    static final String EXTRA_OCR_LANGUAGE = "ocr_language";
    static final String EXTRA_OCR_LANGUAGE_DISPLAY = "ocr_language_display";

    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_FILE_NAME = "file_name";
    public static final String LOG_TAG = OCRLanguageInstallService.class.getSimpleName();


    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, OCRLanguageInstallService.class, 314, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.i(LOG_TAG, "onHandleIntent " + intent);
        if (!intent.hasExtra(DownloadManager.EXTRA_DOWNLOAD_ID)) {
            return;
        }
        final long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
        if (downloadId != 0) {
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            ParcelFileDescriptor file;
            BufferedInputStream in = null;
            FileInputStream fin = null;
            String langName = null;
            try {
                String fileName = intent.getStringExtra(EXTRA_FILE_NAME);
                final Uri fileUri = Uri.parse(fileName);
                langName = extractLanguageNameFromUri(fileUri);
                File tessDir = Util.getTrainingDataDir(this);
                if (tessDir.mkdirs() || tessDir.isDirectory() && langName != null) {

                    file = dm.openDownloadedFile(downloadId);
                    fin = new FileInputStream(file.getFileDescriptor());
                    in = new BufferedInputStream(fin);

                    copyInputStream(in, fileUri.getLastPathSegment(), tessDir);
                    Log.i(LOG_TAG, "Successfully installed " + fileName);

                    notifySuccess(langName);
                } else {
                    Log.i(LOG_TAG, "Failed to install " + fileName);
                    notifyError(langName);
                }
            } catch (SecurityException e) {
                notifyError(langName);
            } catch (IOException e) {
                notifyError(langName);
            } finally {
                dm.remove(downloadId);
                closeInputStream(in);
                closeInputStream(fin);
            }

        }
    }

    private void closeInputStream(InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void copyInputStream(InputStream inputStream, String fileName, File tessDir) throws IOException {
        final byte[] buffer = new byte[4096 * 8];

        File trainedData = new File(tessDir, fileName);
        FileOutputStream out = new FileOutputStream(trainedData);
        int n;
        while (-1 != (n = inputStream.read(buffer))) {
            out.write(buffer, 0, n);
        }
        out.close();
    }

    private String extractLanguageNameFromUri(final Uri fileName) {
        final String lastPathSegment = fileName.getLastPathSegment();

        int index = lastPathSegment.indexOf(".traineddata");
        if (index != -1) {
            return lastPathSegment.substring(0, index);
        }

        index = lastPathSegment.indexOf(".cube");
        if (index != -1) {
            return lastPathSegment.substring(0, index);
        }
        index = lastPathSegment.indexOf(".tesseract_cube.nn");
        if (index != -1) {
            return lastPathSegment.substring(0, index);
        }

        return null;
    }

    private void notifyError(String lang) {
        Intent resultIntent = new Intent(ACTION_INSTALL_FAILED);
        resultIntent.putExtra(EXTRA_OCR_LANGUAGE, lang);
        resultIntent.putExtra(EXTRA_STATUS, DownloadManager.STATUS_FAILED);
        sendBroadcast(resultIntent);
    }

    private void notifySuccess(String lang) {
        Intent resultIntent = new Intent(ACTION_INSTALL_COMPLETED);
        resultIntent.putExtra(EXTRA_OCR_LANGUAGE, lang);
        resultIntent.putExtra(EXTRA_STATUS, DownloadManager.STATUS_SUCCESSFUL);
        sendBroadcast(resultIntent);
    }
}
