package com.renard.ocr.analytics

import android.content.Context

object CrashLoggerFactory {
    fun createCrashLogger(context: Context): CrashLogger {
        return object : CrashLogger {
            override fun logException(exception: Exception) {
            }

            override fun logMessage(message: String) {
            }

            override fun setString(tag: String, value: String) {
            }

            override fun setLong(tag: String, value: Long) {
            }
        }
    }
}