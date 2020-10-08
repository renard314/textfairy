package com.renard.ocr.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.renard.ocr.util.AppStorage.getOldTextFairyCacheDirectory
import com.renard.ocr.util.AppStorage.getOldTextFairyImageDirectory


class AppUpdatedBroadCastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val numCacheFiles = getOldTextFairyCacheDirectory().listFiles()?.size ?: 0
            val numImageFiles = getOldTextFairyImageDirectory().listFiles()?.size ?: 0
            if (numCacheFiles > 1 || numImageFiles > 0) {
                CopyAppFilesService.enqueueWork(context)
            }
        }
    }

}