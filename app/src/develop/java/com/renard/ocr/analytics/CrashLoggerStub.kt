package com.renard.ocr.analytics

import android.content.Context

object CrashLoggerFactory {
    fun createCrashLogger(context: Context): CrashLogger {
        return object : CrashLogger {
            override fun logException(exception: Exception) {
            }

            override fun logMessage(message: String) {
            }

            override fun setString(s: String, pageSegMode: String) {
            }

            override fun setLong(s: String, freeMemory: Long) {
            }
        }
    }
}