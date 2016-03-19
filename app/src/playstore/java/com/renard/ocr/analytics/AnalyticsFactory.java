package com.renard.ocr.analytics;

import android.content.Context;

public class AnalyticsFactory {

    public static Analytics createAnalytics(Context context) {
        return new AnalyticsWithGoogle(context);
    }
}
