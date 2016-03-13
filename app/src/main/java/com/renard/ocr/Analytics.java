package com.renard.ocr;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import com.renard.ocr.cropimage.image_processing.BlurDetectionResult;
import com.renard.ocr.language.OcrLanguage;
import com.renard.ocr.visualisation.LayoutQuestionDialog;

import android.util.Log;

/**
 * @author renard
 */
public class Analytics {

    public static final String CATEGORY_OCR = "Ocr";
    public static final String CATEGORY_LANGUAGE = "Language";
    public static final String CATEGORY_DOCUMENT_OPTIONS = "Document Options";
    private static final String LOG_TAG = Analytics.class.getSimpleName();
    private final Tracker mTracker;

    public Analytics(Tracker tracker) {
        mTracker = tracker;
    }

    public void sendScreenView(String screenName) {
        Log.i(LOG_TAG, "Setting screen name: " + screenName);
        mTracker.setScreenName(screenName);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }


    public void sendStartDownload(OcrLanguage language) {
        sendEvent(CATEGORY_LANGUAGE, "Start download", language.getValue(), 1);
    }

    public void sendDeleteLanguage(OcrLanguage language) {
        sendEvent(CATEGORY_LANGUAGE, "Delete language", language.getValue(), 1);
    }

    public void sendOcrResult(String language, int accuracy) {
        sendEvent(CATEGORY_OCR, "Scan completed", language, accuracy);
    }

    public void sendOcrLanguageChanged(OcrLanguage language) {
        sendEvent(CATEGORY_OCR, "Picked Language", language.getValue(), 1);
    }

    public void sendLayoutDialogCancelled() {
        sendEvent(CATEGORY_OCR, "Scan Cancelled", "Layout Dialog", 1);
    }

    public void sendOcrCancelled() {
        sendEvent(CATEGORY_OCR, "Scan Cancelled", "Back Button", 1);
    }

    public void sendOcrStarted(String language, LayoutQuestionDialog.LayoutKind layout) {
        sendEvent(CATEGORY_OCR, "Scan started", language, 1);
        sendEvent(CATEGORY_OCR, "Layout chosen", layout.name(), 1);
    }

    public void optionDocumentViewMode(boolean showingText) {
        String label = showingText ? "Text" : "Image";
        sendEvent(CATEGORY_DOCUMENT_OPTIONS, "View mode changed", label, 1);
    }

    public void optionTextSettings() {
        sendEvent(CATEGORY_DOCUMENT_OPTIONS, "Text settings selected", "", 1);

    }

    public void optionTableOfContents() {
        sendEvent(CATEGORY_DOCUMENT_OPTIONS, "Table of contents selected", "", 1);
    }

    public void optionsDeleteDocument() {
        sendEvent(CATEGORY_DOCUMENT_OPTIONS, "Delete document selected", "", 1);
    }

    public void optionsCreatePdf() {
        sendEvent(CATEGORY_DOCUMENT_OPTIONS, "Create Pdf selected", "", 1);
    }

    public void optionsCopyToClipboard() {
        sendEvent(CATEGORY_DOCUMENT_OPTIONS, "Copy text selected", "", 1);
    }

    public void optionsStartTts() {
        sendEvent(CATEGORY_DOCUMENT_OPTIONS, "Start Tts selected", "", 1);
    }

    public void optionsShareText() {
        sendEvent(CATEGORY_DOCUMENT_OPTIONS, "Share text selected", "", 1);

    }

    public void ttsLanguageChanged(OcrLanguage lang) {
        sendEvent("Text to speech", "Language changed", lang.getValue(), 1);
    }

    public void ttsStart(String language) {
        sendEvent("Text to speech", "Started speaking", language, 1);
    }

    public void ttsStop() {
        sendEvent("Text to speech", "Stopped speaking", "", 1);
    }

    public void startGallery() {
        sendEvent(CATEGORY_OCR, "Gallery started", "", 1);
    }

    public void startCamera() {
        sendEvent(CATEGORY_OCR, "Camera started", "", 1);
    }

    public void sendCropError() {
        sendEvent(CATEGORY_OCR, "Crop error occurred", "", 1);
    }

    public void sendBlurResult(BlurDetectionResult blurriness) {
        final int blurValue = (int) (blurriness.getBlurValue() * 100);
        sendEvent(CATEGORY_OCR, "Blur test completed", blurriness.getBlurriness().name(), blurValue);
    }

    public void newImageBecauseOfBlurWarning() {
        sendEvent(CATEGORY_OCR, "Blur", "New image requested", 1);

    }

    public void continueDespiteOfBlurWarning() {
        sendEvent(CATEGORY_OCR, "Blur", "Continue with blurry image", 1);
    }

    private void sendEvent(String category, String action, String label, int value) {
        Log.i(LOG_TAG, "sendEvent(" + category + ", " + action + ", " + label + ", " + value + ")");
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
                .setValue(value)
                .build());
    }

    public void sendClickYoutubte() {
        sendEvent(CATEGORY_INSTALL, "Youtube link clicked", "", 1);
    }
}
