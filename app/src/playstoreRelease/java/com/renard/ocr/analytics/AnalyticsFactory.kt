package com.renard.ocr.analytics

import android.content.Context

object AnalyticsFactory {
    @JvmStatic
    fun createAnalytics(context: Context): Analytics = AnalyticsWithGoogle(context)
}