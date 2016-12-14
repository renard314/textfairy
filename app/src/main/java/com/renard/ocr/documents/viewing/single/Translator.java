package com.renard.ocr.documents.viewing.single;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;

import java.util.Locale;

import static android.net.Uri.parse;

/**
 * Triggers translation of a text by the Google Translate app.
 */
class Translator {

    private static final String TRANSLATE_PACKAGE_NAME = "com.google.android.apps.translate";
    private static final String TRANSLATE_ACTIVITY_NAME = "com.google.android.apps.translate.TranslateActivity";
    private static final String TRANSLATE_POP_OVER_ACTIVITY_NAME = "com.google.android.apps.translate.copydrop.CopyDropActivity";
    private static final String EXTRA_TO_LANGUAGE = "to";

    void startTranslation(Activity activity, String text) {
        if (!isGoogleTranslateInstalled(activity)) {
            openPlayStore(activity);
        } else {
            translateWithGoogleTranslate(activity, text);
        }
    }

    private void translateWithGoogleTranslate(Activity activity, String text) {
        if (supportsPopOver(activity)) {
            translateInPopOver(activity, text);
        } else {
            openGoogleTranslateApp(activity, text);
        }
    }

    private boolean supportsPopOver(Activity activity) {
        final Intent intent = createPopOverIntent();
        return !activity.getPackageManager().queryIntentActivities(intent, 0).isEmpty();
    }

    private Intent createPopOverIntent() {
        return new Intent()
                .setAction("android.intent.action.PROCESS_TEXT")
                .setType("text/plain")
                .setComponent(new ComponentName(TRANSLATE_PACKAGE_NAME, TRANSLATE_POP_OVER_ACTIVITY_NAME));
    }

    private void translateInPopOver(Activity activity, String text) {
        final Intent popOverIntent = createPopOverIntent();
        Locale current = Locale.getDefault();
        popOverIntent.putExtra("key_text_to_be_translated", text);
        popOverIntent.putExtra(EXTRA_TO_LANGUAGE, current.getLanguage());
        activity.startActivity(popOverIntent);
    }

    private boolean isGoogleTranslateInstalled(Activity activity) {
        PackageManager packageManager = activity.getPackageManager();
        return !packageManager.queryIntentActivities(createGoogleTranslateIntent(), 0).isEmpty();
    }

    private void openPlayStore(Activity activity) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, parse("market://details?id=" + TRANSLATE_PACKAGE_NAME)));
        } catch (android.content.ActivityNotFoundException anfe) {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, parse("https://play.google.com/store/apps/details?id=" + TRANSLATE_PACKAGE_NAME)));
        }
    }

    private void openGoogleTranslateApp(Activity activity, String text) {
        Locale current = Locale.getDefault();
        final Intent intent = createGoogleTranslateIntent().
                putExtra(EXTRA_TO_LANGUAGE, current.getLanguage()).
                putExtra(Intent.EXTRA_TEXT, text);

        activity.startActivity(intent);
    }

    private Intent createGoogleTranslateIntent() {
        return new Intent()
                .setAction(Intent.ACTION_SEND)
                .setComponent(new ComponentName(TRANSLATE_PACKAGE_NAME, TRANSLATE_ACTIVITY_NAME));
    }

}
