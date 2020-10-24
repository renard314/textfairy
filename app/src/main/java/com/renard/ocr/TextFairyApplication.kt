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
package com.renard.ocr

import android.content.Context
import android.os.StrictMode
import android.util.Log
import android.view.ViewConfiguration
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.getkeepsafe.relinker.ReLinker
import com.renard.ocr.analytics.Analytics
import com.renard.ocr.analytics.AnalyticsFactory
import com.renard.ocr.analytics.CrashLogger
import com.renard.ocr.analytics.CrashLoggerFactory.createCrashLogger
import com.renard.ocr.main_menu.language.OcrLanguageDataStore.deleteLanguage
import com.renard.ocr.util.PreferencesUtils
import com.squareup.leakcanary.LeakCanary

class TextFairyApplication : MultiDexApplication() {
    lateinit var analytics: Analytics
        private set
    lateinit var crashLogger: CrashLogger
        private set

    var nativePix: Long? = null

    override fun attachBaseContext(base: Context) {
        MultiDex.install(this);
        super.attachBaseContext(base);
    }

    override fun onCreate() {
        super.onCreate()
        loadLibaries()
        createAnalytics()
        createCrashLogger()
        initTextPreferences()
        enableStrictMode()
        alwaysShowOverflowButton()
        startLeakCanary()
        deleteLanguage("Latin", this)
    }

    private fun loadLibaries() {
        val reLinker = ReLinker.log { message: String -> Log.d("ReLinker", message) }
        listOf("c++_shared", "pngo", "jpeg", "lept", "tess", "image_processing", "hocr2pdf")
                .forEach { reLinker.loadLibrary(this@TextFairyApplication, it) }
    }

    private fun startLeakCanary() {
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }
        LeakCanary.install(this)
    }

    private fun initTextPreferences() {
        PreferencesUtils.initPreferencesWithDefaultsIfEmpty(applicationContext)
    }

    private fun createAnalytics() {
        analytics = AnalyticsFactory.createAnalytics(this)
    }

    private fun createCrashLogger() {
        crashLogger = createCrashLogger(applicationContext)
    }

    private fun enableStrictMode() {
//        if (BuildConfig.DEBUG) {
//            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
//        }
    }

    private fun alwaysShowOverflowButton() {
        // force overflow button for actionbar for devices with hardware option
        // button
        try {
            val config = ViewConfiguration.get(this)
            val menuKeyField = ViewConfiguration::class.java.getDeclaredField("sHasPermanentMenuKey")
            menuKeyField.isAccessible = true
            menuKeyField.setBoolean(config, false)
        } catch (ex: Exception) {
            // Ignore
        }
    }
}