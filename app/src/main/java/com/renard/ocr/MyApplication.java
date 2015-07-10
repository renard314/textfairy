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
package com.renard.ocr;

import com.crashlytics.android.Crashlytics;
import com.renard.util.PreferencesUtils;
import com.squareup.leakcanary.LeakCanary;

import android.app.Application;
import android.os.StrictMode;
import android.view.ViewConfiguration;

import java.lang.reflect.Field;

import io.fabric.sdk.android.Fabric;

public class MyApplication extends Application {

    public void onCreate() {
        super.onCreate();
        final Fabric fabric = new Fabric.Builder(this).kits(new Crashlytics()).debuggable(BuildConfig.DEBUG).build();
        Fabric.with(fabric);

        PreferencesUtils.initPreferencesWithDefaultsIfEmpty(getApplicationContext());

        if (BuildConfig.DEBUG) {
            LeakCanary.install(this);
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build();
            StrictMode.setThreadPolicy(policy);
        }

        // force overflow button for actionbar for devices with hardware option
        // button
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // Ignore
        }

    }

}
