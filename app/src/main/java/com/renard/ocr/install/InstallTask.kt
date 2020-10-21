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
package com.renard.ocr.install

import android.content.Context
import android.content.res.AssetManager
import android.os.AsyncTask
import android.util.Log
import com.renard.ocr.install.InstallResult.Result
import com.renard.ocr.install.InstallResult.Result.*
import com.renard.ocr.install.TaskFragment.TaskCallbacks
import com.renard.ocr.main_menu.language.OcrLanguageDataStore.getUserLocaleOcrLanguage
import com.renard.ocr.util.AppStorage.getFreeSpaceInBytes
import com.renard.ocr.util.AppStorage.getTrainingDataDir
import java.io.File
import java.io.IOException
import java.io.InputStream

private val DEBUG_TAG = InstallTask::class.java.simpleName

internal class InstallTask(
        private var mCallbacks: TaskCallbacks?,
        private val mAssetManager: AssetManager
) : AsyncTask<Context, Int, InstallResult>() {

    private var mBytesInstalled = 0L // bytes Installed
    private var mBytesToInstallTotal = 0L // total Install size in bytes

    fun setTaskCallbacks(callbacks: TaskCallbacks?) {
        mCallbacks = callbacks
    }

    override fun doInBackground(vararg contexts: Context): InstallResult {
        val context = contexts[0]
        Log.i(DEBUG_TAG, "start installation")
        val freeSpace = getFreeSpaceInBytes(context)
        mBytesToInstallTotal = totalUnzippedSize
        if (freeSpace < mBytesToInstallTotal) {
            return InstallResult(NOT_ENOUGH_DISK_SPACE, mBytesToInstallTotal, freeSpace)
        }
        publishProgress(0)
        val ret = copyLanguageAssets(context, mAssetManager)
        Log.i(DEBUG_TAG, "InstallLanguageAssets : $ret")
        maybeDownloadUserLocaleOcrLanguage(context)
        return ret
    }

    private fun maybeDownloadUserLocaleOcrLanguage(context: Context) {
        val userLocaleOcrLanguage = getUserLocaleOcrLanguage(context)
        if (userLocaleOcrLanguage?.isInstalled == false) {
            userLocaleOcrLanguage.installLanguage(context)
        }
    }

    /**
     * @return the total size of the language-assets in the zip file
     */
    private val totalUnzippedSize: Long
        get() = 31109429

    override fun onProgressUpdate(vararg values: Int?) {
        mCallbacks?.onProgressUpdate(values[0] ?: 0)
    }

    override fun onCancelled() {
        mCallbacks?.onCancelled()
    }

    override fun onPreExecute() {
        mCallbacks?.onPreExecute()
    }

    override fun onPostExecute(result: InstallResult?) {
        mCallbacks?.onPostExecute(result)
    }

    /**
     * @return the install status in procentages [0-100] %
     */
    private fun getInstallStatus(): Int {
        var percent = 0L // [0-100]
        if (mBytesToInstallTotal != 0L) {
            percent = mBytesInstalled * 100 / mBytesToInstallTotal
        }
        Log.v(DEBUG_TAG, "GetInstallStatus(): Installed ${mBytesInstalled}B [$percent%]")
        return percent.toInt()
    }

    /**
     * unzips all language-assets from the package
     */
    private fun copyLanguageAssets(context: Context, manager: AssetManager): InstallResult {
        val trainedDataDir = getTrainingDataDir(context) ?: return InstallResult(MEDIA_NOT_MOUNTED)
        try {
            manager.list("tessdata")?.forEach { trainedData ->
                manager.open("tessdata/$trainedData").use { inputStream ->
                    copyInputStream(inputStream, File(trainedDataDir, trainedData))
                }
            }
        } catch (ioe: IOException) {
            Log.v(DEBUG_TAG, "exception:$ioe")
            return InstallResult(Result.UNSPECIFIED_ERROR)
        }
        return InstallResult(OK)
    }

    /**
     * copy from the zip on the disk
     */
    private fun copyInputStream(inputStream: InputStream, targetFile: File) {
        val outputStream = targetFile.outputStream()
        val buffer = ByteArray(8 * 1024)
        var bytes = inputStream.read(buffer)
        while (bytes >= 0) {
            outputStream.write(buffer, 0, bytes)
            mBytesInstalled += bytes
            publishProgress(getInstallStatus())
            bytes = inputStream.read(buffer)
        }
        outputStream.close()
    }

}

