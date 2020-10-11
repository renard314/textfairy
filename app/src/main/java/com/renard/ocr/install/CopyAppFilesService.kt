package com.renard.ocr.install

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import com.renard.ocr.main_menu.language.OcrLanguageDataStore
import com.renard.ocr.util.AppStorage.getCacheDirectory
import com.renard.ocr.util.AppStorage.getImageDirectory
import com.renard.ocr.util.AppStorage.getOldTextFairyCacheDirectory
import com.renard.ocr.util.AppStorage.getOldTextFairyImageDirectory
import com.renard.ocr.util.AppStorage.getOldTrainingDataDir
import java.io.File
import java.io.IOException
import kotlin.io.OnErrorAction.SKIP

private val LOG_TAG = CopyAppFilesService::class.simpleName!!

class CopyAppFilesService : JobIntentService() {

    override fun onHandleWork(intent: Intent) {
        getOldTextFairyCacheDirectory().moveRecursively(to = getCacheDirectory(this))
        getOldTextFairyImageDirectory().moveRecursively(to = getImageDirectory(this))
    }

    private fun File.moveRecursively(to: File) {
        Log.d(LOG_TAG, "source: $this contains ${this.list()}.")
        Log.d(LOG_TAG, "target: $to contains ${this.list()}.")
        Log.d(LOG_TAG, "start copying")
        copyRecursively(to, false, skip())
        Log.d(LOG_TAG, "finished copying")
        Log.d(LOG_TAG, "target $to contains ${this.list()} now.")
        deleteRecursively()
    }

    private fun skip() = { _: File, _: IOException -> SKIP }

    companion object {
        fun enqueueWork(context: Context) {
            enqueueWork(context, CopyAppFilesService::class.java, 3141, Intent())
        }
    }
}