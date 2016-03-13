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
        sendEvent(CATEGORY_DOCUMENT_OPTIONS, CREATE_PDF_SELECTED, "", 1);
    }

    public void optionsCopyToClipboard() {
        sendEvent(CATEGORY_DOCUMENT_OPTIONS, COPY_TEXT_SELECTED, "", 1);
    }

    public void optionsStartTts() {
        sendEvent(CATEGORY_DOCUMENT_OPTIONS, START_TTS_SELECTED, "", 1);
    }

    public void optionsShareText() {
        sendEvent(CATEGORY_DOCUMENT_OPTIONS, SHARE_TEXT_SELECTED, "", 1);
    }

    public void ttsLanguageChanged(OcrLanguage lang) {
        sendEvent(CATEGORY_TEXT_TO_SPEECH, "Language changed", lang.getValue(), 1);
    }

    public void ttsStart(String language) {
        sendEvent(CATEGORY_TEXT_TO_SPEECH, "Started speaking", language, 1);
    }

    public void ttsStop() {
        sendEvent(CATEGORY_TEXT_TO_SPEECH, "Stopped speaking", "", 1);
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
        sendEvent(CATEGORY_OCR, ACTION_BLUR_DIALOG, "New image requested", 1);

    }

    public void continueDespiteOfBlurWarning() {
        sendEvent(CATEGORY_OCR, ACTION_BLUR_DIALOG, "Continue with blurry image", 1);
    }

    public void ocrResultSendFeedback() {
        sendEvent(CATEGORY_OCR, ACTION_RESULT_DIALOG_SHOWN, SEND_FEEDBACK_SELECTED, 1);
    }

    public void ocrResultStartTts() {
        sendEvent(CATEGORY_OCR, ACTION_RESULT_DIALOG_SHOWN, START_TTS_SELECTED, 1);
    }

    public void ocrResultCopyToClipboard() {
        sendEvent(CATEGORY_OCR, ACTION_RESULT_DIALOG_SHOWN, COPY_TEXT_SELECTED, 1);
    }

    public void ocrResultShowTips() {
        sendEvent(CATEGORY_OCR, ACTION_RESULT_DIALOG_SHOWN, SHOW_TIPS_SELECTED, 1);
    }

    public void ocrResultCreatePdf() {
        sendEvent(CATEGORY_OCR, ACTION_RESULT_DIALOG_SHOWN, CREATE_PDF_SELECTED, 1);
    }

    public void ocrResultShareText() {
        sendEvent(CATEGORY_OCR, ACTION_RESULT_DIALOG_SHOWN, SHARE_TEXT_SELECTED, 1);

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
