package com.renard.ocr.analytics

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics

object CrashLoggerFactory {
    fun createCrashLogger(context: Context): CrashLogger {
        return CrashlyticsLogger(context)
    }
}

private class CrashlyticsLogger(val context: Context) : CrashLogger {

    override fun logException(exception: Exception) {
        FirebaseCrashlytics.getInstance().recordException(exception)
    }

    override fun logMessage(message: String) 


    override fun setString(tag: String, value: String) {
        FirebaseCrashlytics.getInstance().setCustomKey(tag, value)
    }

    override fun setLong(tag: String, value: Long) {
        FirebaseCrashlytics.getInstance().setCustomKey(tag, value)
    }

}