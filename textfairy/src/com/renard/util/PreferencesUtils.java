/*
 * Copyright (C) 2012,2013 Renard Wellnitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.renard.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Pair;
import android.view.Gravity;
import android.widget.TextView;

import com.renard.ocr.R;
import com.renard.ocr.help.OCRLanguageAdapter.OCRLanguage;

public class PreferencesUtils {

	/* ids of the radio buttons pressed in the options dialogs */
	public final static String PREFERENCES_SPACING_KEY = "line_spacing";
	public final static String PREFERENCES_DESIGN_KEY = "text_design";
	public final static String PREFERENCES_ALIGNMENT_KEY = "text_alignment";
	public final static String PREFERENCES_TEXT_SIZE_KEY = "text_size";

	// actual language
	public final static String PREFERENCES_OCR_LANG = "ocr_language";
	private static final String PREFERENCES_OCR_LANG_DISPLAY = "ocr_language_display";

	public final static String PREFERENCES_KEY = "text_preferences";

	public static void initPreferencesWithDefaultsIfEmpty(Context appContext) {
		SharedPreferences prefs = getPreferences(appContext);
		Editor edit = prefs.edit();
		setIfEmpty(edit, prefs, PREFERENCES_ALIGNMENT_KEY, R.id.align_left);
		// setIfEmpty(edit, prefs, PREFERENCES_DESIGN_KEY, R.id.design_day);
		setIfEmpty(edit, prefs, PREFERENCES_SPACING_KEY, R.id.spacing_1_5);
		final String defaultLanguage = appContext.getString(R.string.default_ocr_language);
		final String defaultLanguageDisplay = appContext.getString(R.string.default_ocr_display_language);
		setIfEmpty(edit, prefs, PREFERENCES_OCR_LANG, defaultLanguage);
		setIfEmpty(edit, prefs, PREFERENCES_OCR_LANG_DISPLAY, defaultLanguageDisplay);
		edit.commit();
	}

	private static void setIfEmpty(final Editor edit, final SharedPreferences prefs, final String id, final int value) {
		if (!prefs.contains(id)) {
			edit.putInt(id, value);
		}
	}

	private static void setIfEmpty(final Editor edit, final SharedPreferences prefs, final String id, final String value) {
		if (!prefs.contains(id)) {
			edit.putString(id, value);
		}
	}

	public static void saveOCRLanguage(final Context context, OCRLanguage language) {
		SharedPreferences prefs = getPreferences(context);
		Editor edit = prefs.edit();
		edit.putString(PREFERENCES_OCR_LANG, language.getValue());
		edit.putString(PREFERENCES_OCR_LANG_DISPLAY, language.getDisplayText());
		edit.apply();
	}

//	public static void pushDownloadId(Context context, long downloadId) {
//		SharedPreferences prefs = getPreferences(context);
//		Editor edit = prefs.edit();
//		edit.putLong("" + downloadId, downloadId);
//		edit.apply();
//	}

//	public static boolean isDownloadId(Context context, long downloadId) {
//		SharedPreferences prefs = getPreferences(context);
//		long savedId = prefs.getLong("" + downloadId, -1);
//		if (savedId != -1) {
//			Editor edit = prefs.edit();
//			edit.remove("" + downloadId);
//			edit.apply();
//			return true;
//		}
//		return false;
//	}

	public static Pair<String, String> getOCRLanguage(final Context context) {
		SharedPreferences prefs = getPreferences(context);
		String value = prefs.getString(PREFERENCES_OCR_LANG, null);
		String display = prefs.getString(PREFERENCES_OCR_LANG_DISPLAY, null);
		return new Pair<String, String>(value, display);
	}

	public static void saveTextSize(Context appContext, float size) {
		SharedPreferences prefs = getPreferences(appContext);
		Editor edit = prefs.edit();
		edit.putFloat(PREFERENCES_TEXT_SIZE_KEY, size);
		edit.apply();
	}

	public static void applyTextPreferences(TextView view, SharedPreferences prefs) {
		int id = prefs.getInt(PREFERENCES_ALIGNMENT_KEY, -1);
		applyById(view, id);
		id = prefs.getInt(PREFERENCES_DESIGN_KEY, -1);
		applyById(view, id);
		id = prefs.getInt(PREFERENCES_SPACING_KEY, -1);
		applyById(view, id);
		float size = prefs.getFloat(PREFERENCES_TEXT_SIZE_KEY, -1f);
		if (size != -1) {
			view.setTextSize(size);
		}
	}

	public static float getTextSize(Context appContext) {
		SharedPreferences prefs = getPreferences(appContext);
		return prefs.getFloat(PREFERENCES_TEXT_SIZE_KEY, 18f);
	}

	public static void applyTextPreferences(TextView view, Context appContext) {
		SharedPreferences prefs = getPreferences(appContext);
		applyTextPreferences(view, prefs);

	}

	private static void applyById(TextView view, int id) {
		switch (id) {
		case R.id.align_block:
			view.setGravity(Gravity.CENTER_HORIZONTAL);
			break;
		case R.id.align_left:
			view.setGravity(Gravity.LEFT);
			break;
		case R.id.spacing_1:
			view.setLineSpacing(0, 1f);
			break;
		case R.id.spacing_1_5:
			view.setLineSpacing(0, 1.25f);
			break;
		case R.id.spacing_2:
			view.setLineSpacing(0, 1.5f);
			break;
		// case R.id.design_night:
		// Drawable d = view.getBackground();
		// if (d != null) {
		// final ViewParent parent = view.getParent();
		// if (parent instanceof View) {
		// ((View) parent).setBackgroundDrawable(null);
		// }
		// d.setColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY);
		// view.invalidate();
		// } else {
		// view.setBackgroundColor(Color.BLACK);
		// }
		// view.setTextColor(Color.WHITE);
		//
		// break;
		// case R.id.design_day:
		// d = view.getBackground();
		// if (d != null) {
		// final ViewParent parent = view.getParent();
		// if (parent instanceof View) {
		// //((View)
		// parent).setBackgroundResource(R.color.bright_foreground_holo_dark);
		// }
		// // view.setBackgroundDrawable(null);
		// d.mutate();
		// // view.setBackgroundColor(Color. WHITE);
		// // d.getCurrent().setColorFilter(Color.RED,
		// // PorterDuff.Mode.MULTIPLY);
		// // d.getCurrent().setColorFilter(new
		// // LightingColorFilter(0xffffffff,0xff00ff00));
		// d.getCurrent().setColorFilter(Color.WHITE, PorterDuff.Mode.ADD);
		// // d.invalidateSelf();
		// // d.setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
		// view.invalidate();
		// } else {
		// view.setBackgroundColor(Color.WHITE);
		// }
		// view.setTextColor(Color.BLACK);
		// break;
		}
	}

	public static SharedPreferences getPreferences(Context applicationContext) {
		return applicationContext.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
	}

}
