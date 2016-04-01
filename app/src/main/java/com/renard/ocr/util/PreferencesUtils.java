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
package com.renard.ocr.util;

import com.renard.ocr.R;
import com.renard.ocr.main_menu.language.OcrLanguage;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Pair;
import android.view.Gravity;
import android.widget.TextView;

public class PreferencesUtils {

    /* ids of the radio buttons pressed in the options dialogs */
    public final static String PREFERENCES_SPACING_KEY = "line_spacing";
    public final static String PREFERENCES_DESIGN_KEY = "text_design";
    public final static String PREFERENCES_ALIGNMENT_KEY = "text_alignment";
    public final static String PREFERENCES_TEXT_SIZE_KEY = "text_size";
    private final static String PREFERENCES_TRAINING_DATA_DIR = "training_data_dir";

    // actual language
    public final static String PREFERENCES_OCR_LANG = "ocr_language";
    private static final String PREFERENCES_OCR_LANG_DISPLAY = "ocr_language_display";

    public final static String PREFERENCES_KEY = "text_preferences";
    private static final String PREFERENCES_THUMBNAIL_HEIGHT = "thumbnail_width";
    private static final String PREFERENCES_THUMBNAIL_WIDTH = "thumbnail_height";
    private static final String PREFERENCES_HAS_ASKED_FOR_FEEDBACK = "has_asked_for_feedback";
    private static final String PREFERENCES_IS_FIRST_START = "is_first_start";
    private static final String PREFERENCES_IS_FIRST_SCAN = "is_first_scan";

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
        edit.apply();
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

    public static void saveOCRLanguage(final Context context, OcrLanguage language) {
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
        return new Pair<>(value, display);
    }

    public static void saveTessDir(Context appContext, final String value) {
        SharedPreferences prefs = getPreferences(appContext);
        Editor edit = prefs.edit();
        edit.putString(PREFERENCES_TRAINING_DATA_DIR, value);
        edit.apply();
    }

    public static void setNumberOfSuccessfulScans(Context appContext, final int value) {
        SharedPreferences prefs = getPreferences(appContext);
        Editor edit = prefs.edit();
        edit.putInt(PREFERENCES_HAS_ASKED_FOR_FEEDBACK, value);
        edit.apply();
    }

    public static int getNumberOfSuccessfulScans(Context appContext) {
        SharedPreferences prefs = getPreferences(appContext);
        return prefs.getInt(PREFERENCES_HAS_ASKED_FOR_FEEDBACK, 0);
    }


    public static String getTessDir(Context appContext) {
        SharedPreferences prefs = getPreferences(appContext);
        return prefs.getString(PREFERENCES_TRAINING_DATA_DIR, null);
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
        if (view == null) {
            return;
        }
        if (id == R.id.align_block) {
            view.setGravity(Gravity.CENTER_HORIZONTAL);
        } else if (id == R.id.align_left) {
            view.setGravity(Gravity.LEFT);
        } else if (id == R.id.spacing_1) {
            view.setLineSpacing(0, 1f);
        } else if (id == R.id.spacing_1_5) {
            view.setLineSpacing(0, 1.25f);
        } else if (id == R.id.spacing_2) {
            view.setLineSpacing(0, 1.5f);
        }
    }

    public static SharedPreferences getPreferences(Context applicationContext) {
        return applicationContext.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
    }

    public static int getThumbnailWidth(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return prefs.getInt(PREFERENCES_THUMBNAIL_WIDTH, 20);
    }

    public static int getThumbnailHeight(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return prefs.getInt(PREFERENCES_THUMBNAIL_HEIGHT, 20);
    }

    public static void saveThumbnailSize(Context context, int w, int h) {
        SharedPreferences prefs = getPreferences(context);
        Editor edit = prefs.edit();
        edit.putInt(PREFERENCES_THUMBNAIL_WIDTH, w);
        edit.putInt(PREFERENCES_THUMBNAIL_HEIGHT, h);
        edit.apply();
    }

    public static boolean isFirstStart(Context context) {
        return getPreferences(context).getBoolean(PREFERENCES_IS_FIRST_START, true);
    }

    public static void setFirstStart(Context context, boolean value) {
        SharedPreferences prefs = getPreferences(context);
        Editor edit = prefs.edit();
        edit.putBoolean(PREFERENCES_IS_FIRST_START, value);
        edit.apply();
    }

    public static void setFirstScan(Context context, boolean value) {
        SharedPreferences prefs = getPreferences(context);
        Editor edit = prefs.edit();
        edit.putBoolean(PREFERENCES_IS_FIRST_SCAN, value);
        edit.apply();
    }

    public static boolean isFirstScan(Context context) {
        return getPreferences(context).getBoolean(PREFERENCES_IS_FIRST_SCAN, true);
    }
}
