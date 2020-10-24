package com.renard.ocr.analytics;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.renard.ocr.cropimage.image_processing.BlurDetectionResult;
import com.renard.ocr.documents.creation.ocr.LayoutQuestionDialog;
import com.renard.ocr.main_menu.language.OcrLanguage;

/**
 * @author renard
 */
class AnalyticsWithGoogle implements Analytics {
    static final String TRACKING_PREF_KEY = "tracking_key";


    public static final String CATEGORY_OCR = "Ocr";
    public static final String CATEGORY_LANGUAGE = "Language";
    public static final String CATEGORY_DOCUMENT_OPTIONS = "Document Options";
    private static final String LOG_TAG = AnalyticsWithGoogle.class.getSimpleName();
    public static final String START_TTS_SELECTED = "Start Tts selected";
    public static final String SHARE_TEXT_SELECTED = "Share text selected";
    public static final String COPY_TEXT_SELECTED = "Copy text selected";
    public static final String CREATE_PDF_SELECTED = "Create Pdf selected";
    public static final String ACTION_RESULT_DIALOG_SHOWN = "Result Dialog shown";
    public static final String SHOW_TIPS_SELECTED = "Show tips selected";
    public static final String SEND_FEEDBACK_SELECTED = "Send feedback selected";
    public static final String ACTION_BLUR_DIALOG = "Blur Dialog shown";
    public static final String CATEGORY_TEXT_TO_SPEECH = "Text to speech";
    private static final String CATEGORY_INSTALL = "Install";
    public static final String CATEGORY_SCAN_ACCURACY = "Scan accuracy";
    private final Context mApplicationContext;
    private final FirebaseAnalytics firebaseAnalytics;


    public AnalyticsWithGoogle(Context applicationContext) {
        mApplicationContext = applicationContext;
        firebaseAnalytics = FirebaseAnalytics.getInstance(applicationContext);
        listenForGoogleAnalyticsOptOut();
    }

    private void listenForGoogleAnalyticsOptOut() {
        SharedPreferences userPrefs = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
        userPrefs.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
            if (key.equals(TRACKING_PREF_KEY)) {
                final boolean optOut = sharedPreferences.getBoolean(key, false);
                Log.i(LOG_TAG, "tracking preference was changed. setting app opt out to: " + optOut);
                firebaseAnalytics.setAnalyticsCollectionEnabled(optOut);
            }
        });
    }

    @Override
    public boolean getAppOptOut() {
        SharedPreferences userPrefs = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
        return userPrefs.getBoolean(TRACKING_PREF_KEY, false);
    }

    @Override
    public void toggleTracking(boolean optOut) {
        Log.i(LOG_TAG, "toggleTracking(" + optOut + ")");
        SharedPreferences userPrefs = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
        userPrefs.edit().putBoolean(TRACKING_PREF_KEY, optOut).apply();
    }

    @Override
    public void sendScreenView(String screenName) {
        Log.i(LOG_TAG, "Setting screen name: " + screenName);
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName);
    }


    @Override
    public void sendStartDownload(OcrLanguage language) {
        sendEvent(CATEGORY_LANGUAGE, "Start download", language.getValue(), 1);
    }

    @Override
    public void sendDeleteLanguage(OcrLanguage language) {
        sendEvent(CATEGORY_LANGUAGE, "Delete language", language.getValue(), 1);
    }

    @Override
    public void sendOcrResult(String language, int accuracy) {
        int rem = accuracy % 10;
        final int bracket = accuracy - rem;
        sendEvent(CATEGORY_SCAN_ACCURACY, language, bracket + "", accuracy);
        sendEvent(CATEGORY_OCR, "Scan completed", language, accuracy);
    }

    @Override
    public void sendOcrLanguageChanged(OcrLanguage language) {
        sendEvent(CATEGORY_OCR, "Picked Language", language.getValue(), 1);
    }

    @Override
    public void sendLayoutDialogCancelled() {
        sendEvent(CATEGORY_OCR, "Scan Cancelled", "Layout Dialog", 1);
    }

    @Override
    public void sendOcrCancelled() {
        sendEvent(CATEGORY_OCR, "Scan Cancelled", "Back Button", 1);
    }

    @Override
    public void sendOcrStarted(String language, LayoutQuestionDialog.LayoutKind layout) {
        sendEvent(CATEGORY_OCR, "Scan started", language, 1);
        sendEvent(CATEGORY_OCR, "Layout chosen", layout.name(), 1);
    }

    @Override
    public void optionTranslateText() {
        sendEvent(CATEGORY_DOCUMENT_OPTIONS, "Translate started", "", 1);
    }

    @Override
    public void noLanguageInstalled() {
        firebaseAnalytics.logEvent("no_Language_installed",null);
    }


    @Override
    public void optionDocumentViewMode(boolean showingText) {
        String label = showingText ? "Text" : "Image";
        sendEvent(CATEGORY_DOCUMENT_OPTIONS, "View mode changed", label, 1);
    }

    @Override
    public void optionTextSettings() {
        sendEvent(CATEGORY_DOCUMENT_OPTIONS, "Text settings selected", "", 1);

    }

    @Override
    public void optionTableOfContents() {
        sendEvent(CATEGORY_DOCUMENT_OPTIONS, "Table of contents selected", "", 1);
    }

    @Override
    public void optionsDeleteDocument() {
        sendEvent(CATEGORY_DOCUMENT_OPTIONS, "Delete document selected", "", 1);
    }

    @Override
    public void optionsCreatePdf() {
        sendEvent(CATEGORY_DOCUMENT_OPTIONS, CREATE_PDF_SELECTED, "", 1);
    }

    @Override
    public void optionsCopyToClipboard() {
        sendEvent(CATEGORY_DOCUMENT_OPTIONS, COPY_TEXT_SELECTED, "", 1);
    }

    @Override
    public void optionsStartTts() {
        sendEvent(CATEGORY_DOCUMENT_OPTIONS, START_TTS_SELECTED, "", 1);
    }

    @Override
    public void optionsShareText() {
        sendEvent(CATEGORY_DOCUMENT_OPTIONS, SHARE_TEXT_SELECTED, "", 1);
    }

    @Override
    public void ttsLanguageChanged(OcrLanguage lang) {
        sendEvent(CATEGORY_TEXT_TO_SPEECH, "Language changed", lang.getValue(), 1);
    }

    @Override
    public void ttsStart(String language) {
        sendEvent(CATEGORY_TEXT_TO_SPEECH, "Started speaking", language, 1);
    }

    @Override
    public void ttsStop() {
        sendEvent(CATEGORY_TEXT_TO_SPEECH, "Stopped speaking", "", 1);
    }

    @Override
    public void startGallery() {
        sendEvent(CATEGORY_OCR, "Gallery started", "", 1);
    }

    @Override
    public void startCamera() {
        sendEvent(CATEGORY_OCR, "Camera started", "", 1);
    }

    @Override
    public void sendCropError() {
        sendEvent(CATEGORY_OCR, "Crop error occurred", "", 1);
    }

    @Override
    public void sendBlurResult(BlurDetectionResult blurriness) {
        final int blurValue = (int) (blurriness.getBlurValue() * 100);
        sendEvent(CATEGORY_OCR, "Blur test completed", blurriness.getBlurriness().name(), blurValue);
    }

    @Override
    public void newImageBecauseOfBlurWarning(float blurriness) {
        sendEvent(CATEGORY_OCR, ACTION_BLUR_DIALOG, "New image requested", (int) (blurriness * 100));

    }

    @Override
    public void continueDespiteOfBlurWarning(float blurriness) {
        sendEvent(CATEGORY_OCR, ACTION_BLUR_DIALOG, "Continue with blurry image", (int) (blurriness * 100));
    }

    @Override
    public void ocrResultSendFeedback() {
        sendEvent(CATEGORY_OCR, ACTION_RESULT_DIALOG_SHOWN, SEND_FEEDBACK_SELECTED, 1);
    }

    @Override
    public void ocrResultStartTts() {
        sendEvent(CATEGORY_OCR, ACTION_RESULT_DIALOG_SHOWN, START_TTS_SELECTED, 1);
    }

    @Override
    public void ocrResultCopyToClipboard() {
        sendEvent(CATEGORY_OCR, ACTION_RESULT_DIALOG_SHOWN, COPY_TEXT_SELECTED, 1);
    }

    @Override
    public void ocrResultShowTips() {
        sendEvent(CATEGORY_OCR, ACTION_RESULT_DIALOG_SHOWN, SHOW_TIPS_SELECTED, 1);
    }

    @Override
    public void ocrResultCreatePdf() {
        sendEvent(CATEGORY_OCR, ACTION_RESULT_DIALOG_SHOWN, CREATE_PDF_SELECTED, 1);
    }

    @Override
    public void ocrResultShareText() {
        sendEvent(CATEGORY_OCR, ACTION_RESULT_DIALOG_SHOWN, SHARE_TEXT_SELECTED, 1);

    }

    private void sendEvent(String category, String action, String label, int value) {
        Log.i(LOG_TAG, "sendEvent(" + category + ", " + action + ", " + label + ", " + value + ")");
        Bundle params = new Bundle();
        params.putString("category", category);
        params.putString("action", action);
        params.putString("label", label);
        params.putInt("value", value);
        firebaseAnalytics.logEvent(category, params);
    }

    @Override
    public void sendClickYoutube() {
        sendEvent(CATEGORY_INSTALL, "Youtube link clicked", "", 1);
    }

    @Override
    public void sendIgnoreMemoryWarning(long availableMegs) {
        sendEvent(CATEGORY_OCR, "Memory Warning", "ignored", (int) availableMegs);
    }

    @Override
    public void sendHeedMemoryWarning(long availableMegs) {
        sendEvent(CATEGORY_OCR, "Memory Warning", "heeded", (int) availableMegs);
    }


}
