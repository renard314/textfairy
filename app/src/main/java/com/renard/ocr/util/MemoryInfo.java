package com.renard.ocr.util;

import android.app.ActivityManager;
import android.content.Context;

public class MemoryInfo {

    public static final long MINIMUM_RECOMMENDED_RAM = 120;

    private MemoryInfo() {

    }

    public static long getFreeMemory(Context context) {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        return mi.availMem / 1048576L;
    }
}
