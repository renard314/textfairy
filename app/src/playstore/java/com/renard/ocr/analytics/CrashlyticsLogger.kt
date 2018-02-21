package com.renard.ocr.analytics

import android.content.Context
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.ndk.CrashlyticsNdk
import com.googlecode.tesseract.android.TessBaseAPI
import com.renard.ocr.BuildConfig
import io.fabric.sdk.android.Fabric

object CrashLoggerFactory {
    fun createCrashLogger(context: Context): CrashLogger {
        return CrashlyticsLogger(context)
    }
}

private class CrashlyticsLogger(val context: Context) : CrashLogger {

    init {
        val fabric = Fabric.Builder(context).kits(Crashlytics(), CrashlyticsNdk()).debuggable(BuildConfig.DEBUG).build()
        Fabric.with(fabric)
        TessBaseAPI.initCrashlytics()

    }

    override fun logException(exception: Exception) {
        Crashlytics.logException(exception)
    }

    override fun logMessage(message: String) {
        Crashlytics.log(message)
    }

    override fun setString(tag: String, value: String) {
        Crashlytics.setString(tag, value)
    }

    override fun setLong(tag: String, value: Long) {
        Crashlytics.setLong(tag, value)
    }

}