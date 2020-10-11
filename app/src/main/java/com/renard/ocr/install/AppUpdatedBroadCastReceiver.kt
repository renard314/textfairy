package com.renard.ocr.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.renard.ocr.main_menu.language.OcrLanguageDataStore
import com.renard.ocr.util.AppStorage
import com.renard.ocr.util.AppStorage.getOldTextFairyCacheDirectory
import com.renard.ocr.util.AppStorage.getOldTextFairyImageDirectory

private val LOG_TAG = AppUpdatedBroadCastReceiver::class.simpleName!!

class AppUpdatedBroadCastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val numCacheFiles = getOldTextFairyCacheDirectory().listFiles()?.size ?: 0
            val numImageFiles = getOldTextFairyImageDirectory().listFiles()?.size ?: 0
            if (numCacheFiles > 1 || numImageFiles > 0) {
                CopyAppFilesService.enqueueWork(context)
            }
            val installedOCRLanguages = OcrLanguageDataStore.getOldInstalledOCRLanguages(context)
            val existingLanguages = context.assets.list("tessdata")
                    ?.map { it.removeSuffix(".traineddata") }
                    ?: emptyList()
            AppStorage.getOldTrainingDataDir().deleteRecursively()
            installedOCRLanguages
                    .filterNot { existingLanguages.contains(it.value) }
                    .forEach {
                        Log.d(LOG_TAG, "Starting download of $it")
                        it.installLanguage(context)
                    }
        }
    }

}