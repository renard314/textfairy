package com.renard.ocr;

import java.lang.reflect.Field;

import android.app.Application;
import android.view.ViewConfiguration;

import com.renard.util.PreferencesUtils;

//@ReportsCrashes(formKey = "dDZFZXFpU1NocWUwZ0x0aURsVUhNSXc6MQ", socketTimeout = 10000)
public class MyApplication extends Application {

	public void onCreate() {
		PreferencesUtils.initPreferencesWithDefaultsIfEmpty(getApplicationContext());

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
		// ACRA.init(this);

	};

}
