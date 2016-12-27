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

package com.renard.ocr.install;

import com.renard.ocr.install.InstallResult.Result;
import com.renard.ocr.util.Util;

import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class InstallTask extends AsyncTask<Void, Integer, InstallResult> {

    private static final String DEBUG_TAG = InstallTask.class.getSimpleName();
    private static final int PROGRESS_STEP = 100; // when to give feedback to the parent progress dialog
    private static final String TESSDATA_FILE_NAME = "tessdata.zip";

    private int mProgress = 0;
    private final AssetManager mAssetManager;
    private long mBytesInstalled; // bytes Installed
    private long mBytesToInstallTotal; // total Install size in bytes
    private TaskFragment.TaskCallbacks mCallbacks;

    InstallTask(TaskFragment.TaskCallbacks callbacks, AssetManager assetManager) {
        mCallbacks = callbacks;
        mAssetManager = assetManager;
    }

    public void setTaskCallbacks(TaskFragment.TaskCallbacks callbacks) {
        mCallbacks = callbacks;
    }

    @Override
    protected InstallResult doInBackground(Void... unused) {
        Log.i(DEBUG_TAG, "start installation");
        long freeSpace = Util.GetFreeSpaceB();
        mBytesToInstallTotal = getTotalUnzippedSize();
        if (freeSpace < mBytesToInstallTotal) {
            return new InstallResult(Result.NOT_ENOUGH_DISK_SPACE, mBytesToInstallTotal, freeSpace);
        }

        publishProgress(0);
        InstallResult ret = unzipLanguageAssets(mAssetManager);
        Log.i(DEBUG_TAG, "InstallLanguageAssets : " + ret);
        return ret;
    }

    /**
     * @return the total size of the language-assets in the zip file
     */
    private long getTotalUnzippedSize() {
        return 51402414;
    }


    @Override
    protected void onProgressUpdate(Integer... progress) {
        if (mCallbacks != null) {
            mCallbacks.onProgressUpdate(progress[0]);
        }
    }

    @Override
    protected void onCancelled() {
        if (mCallbacks != null) {
            mCallbacks.onCancelled();
        }
    }

    @Override
    protected void onPreExecute() {
        if (mCallbacks != null) {
            mCallbacks.onPreExecute();
        }
    }

    @Override
    protected void onPostExecute(InstallResult result) {
        if (mCallbacks != null) {
            mCallbacks.onPostExecute(result);
        }
    }

    /**
     * @return the install status in procentages [0-100] %
     */
    private int getInstallStatus() {
        long percent = 0; // [0-100]
        if (mBytesToInstallTotal != 0) {
            percent = mBytesInstalled * 100 / mBytesToInstallTotal;
        }

        Log.v(DEBUG_TAG, "GetInstallStatus(): Installed " + mBytesInstalled + "B [" + percent + "%]");
        return (int) percent;
    }

    /**
     * unzips all language-assets from the package
     */
    private InstallResult unzipLanguageAssets(AssetManager manager) {
        ZipInputStream zipStream = null;
        File externalStorageDir = new File(Environment.getExternalStorageDirectory(), Util.EXTERNAL_APP_DIRECTORY);
        if (!externalStorageDir.exists()) {
            externalStorageDir.mkdirs();
        }
        InstallResult result = null;

        try {

            InputStream in = manager.open(TESSDATA_FILE_NAME);
            zipStream = new ZipInputStream(in);

            ZipEntry entry = null;
            while ((entry = zipStream.getNextEntry()) != null) {

                String filename = entry.getName();
                File file = new File(externalStorageDir, entry.getName());
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();
                    Log.v(DEBUG_TAG, "Extracting asset file: " + filename);
                    result = copyInputStream(zipStream, entry.getSize(), entry.getName(), file);
                    publishProgress(getInstallStatus());
                }
            }
            zipStream.closeEntry();
        } catch (IOException ioe) {
            Log.v(DEBUG_TAG, "exception:" + ioe.toString());
            return new InstallResult(Result.UNSPECIFIED_ERROR);
        } finally {
            if (zipStream != null) {
                try {
                    zipStream.close();
                } catch (IOException ignore) {
                }
            }
        }
        return result;
    }


    /**
     * copy from the zip on the disk
     */
    private InstallResult copyInputStream(final InputStream in, final long in_size, final String outname, final File outfile) {
        byte[] buffer = new byte[1024];
        int len;
        DataOutputStream out = null;
        Log.v(DEBUG_TAG, "Asset " + outfile.getName() + " installing on disk.");
        try {
            out = new DataOutputStream(new FileOutputStream(outfile));

            while ((len = in.read(buffer)) >= 0) {
                mProgress++;
                if (mProgress >= PROGRESS_STEP) {
                    publishProgress(getInstallStatus());
                    mProgress = 0;
                }
                out.write(buffer, 0, len);
                mBytesInstalled += len;
            }
        } catch (IOException e) {
            return new InstallResult(Result.UNSPECIFIED_ERROR);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignore) {
                }
            }
        }
        return new InstallResult(Result.OK);
    }

}
