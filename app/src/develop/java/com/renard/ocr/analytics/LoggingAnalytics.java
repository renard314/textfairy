package com.renard.ocr.analytics;

import com.renard.ocr.cropimage.image_processing.BlurDetectionResult;
import com.renard.ocr.documents.creation.visualisation.LayoutQuestionDialog;
import com.renard.ocr.main_menu.language.OcrLanguage;

import android.util.Log;

public class LoggingAnalytics implements Analytics {

    private static final String LOG_TAG = LoggingAnalytics.class.getSimpleName();

    @Override
    public boolean getAppOptOut() {
        return false;
    }

    @Override
    public void toggleTracking(boolean optOut) {
        Log.i(LOG_TAG, "toggleTracking " + optOut);
    }

    @Override
    public void sendScreenView(String screenName) {
        Log.i(LOG_TAG, "sendScreenView " + screenName);
    }

    @Override
    public void sendStartDownload(OcrLanguage language) {
        Log.i(LOG_TAG, "sendStartDownload " + language.getValue());

    }

    @Override
    public void sendDeleteLanguage(OcrLanguage language) {
        Log.i(LOG_TAG, "sendDeleteLanguage " + language.getValue());
    }

    @Override
    public void sendOcrResult(String language, int accuracy) {
        Log.i(LOG_TAG, "sendOcrResult " + language + ", " + accuracy);
    }

    @Override
    public void sendOcrLanguageChanged(OcrLanguage language) {
        Log.i(LOG_TAG, "sendOcrLanguageChanged " + language.getValue());
    }

    @Override
    public void sendLayoutDialogCancelled() {
        Log.i(LOG_TAG, "sendLayoutDialogCancelled");
    }

    @Override
    public void sendOcrCancelled() {
        Log.i(LOG_TAG, "sendOcrCancelled");
    }

    @Override
    public void sendOcrStarted(String language, LayoutQuestionDialog.LayoutKind layout) {
        Log.i(LOG_TAG, "sendOcrStarted " + language + ", " + layout.name());
    }

    @Override
    public void optionDocumentViewMode(boolean showingText) {
        Log.i(LOG_TAG, "optionDocumentViewMode " + showingText);
    }

    @Override
    public void optionTextSettings() {
        Log.i(LOG_TAG, "optionTextSettings");
    }

    @Override
    public void optionTableOfContents() {
        Log.i(LOG_TAG, "optionTableOfContents");
    }

    @Override
    public void optionsDeleteDocument() {
        Log.i(LOG_TAG, "optionsDeleteDocument");
    }

    @Override
    public void optionsCreatePdf() {
        Log.i(LOG_TAG, "optionsCreatePdf");
    }

    @Override
    public void optionsCopyToClipboard() {
        Log.i(LOG_TAG, "optionsCopyToClipboard");
    }

    @Override
    public void optionsStartTts() {
        Log.i(LOG_TAG, "optionsStartTts");
    }

    @Override
    public void optionsShareText() {
        Log.i(LOG_TAG, "optionsShareText");
    }

    @Override
    public void ttsLanguageChanged(OcrLanguage lang) {
        Log.i(LOG_TAG, "ttsLanguageChanged " + lang.getValue());
    }

    @Override
    public void ttsStart(String language) {
        Log.i(LOG_TAG, "ttsStart " + language);
    }

    @Override
    public void ttsStop() {
        Log.i(LOG_TAG, "ttsStop");
    }

    @Override
    public void startGallery() {
        Log.i(LOG_TAG, "startGallery");
    }

    @Override
    public void startCamera() {
        Log.i(LOG_TAG, "startCamera");
    }

    @Override
    public void sendCropError() {
        Log.i(LOG_TAG, "sendCropError");
    }

    @Override
    public void sendBlurResult(BlurDetectionResult blurriness) {
        Log.i(LOG_TAG, "sendBlurResult " + blurriness.getBlurValue());
    }

    @Override
    public void newImageBecauseOfBlurWarning(float blurriness) {
        Log.i(LOG_TAG, "newImageBecauseOfBlurWarning " + blurriness);
    }

    @Override
    public void continueDespiteOfBlurWarning(float blurriness) {
        Log.i(LOG_TAG, "continueDespiteOfBlurWarning " + blurriness);
    }

    @Override
    public void ocrResultSendFeedback() {
        Log.i(LOG_TAG, "ocrResultSendFeedback");
    }

    @Override
    public void ocrResultStartTts() {
        Log.i(LOG_TAG, "ocrResultStartTts");
    }

    @Override
    public void ocrResultCopyToClipboard() {
        Log.i(LOG_TAG, "ocrResultCopyToClipboard");
    }

    @Override
    public void ocrResultShowTips() {
        Log.i(LOG_TAG, "ocrResultShowTips");
    }

    @Override
    public void ocrResultCreatePdf() {
        Log.i(LOG_TAG, "ocrResultCreatePdf");
    }

    @Override
    public void ocrResultShareText() {
        Log.i(LOG_TAG, "ocrResultShareText");
    }

    @Override
    public void sendClickYoutube() {
        Log.i(LOG_TAG, "sendClickYoutube");
    }

    @Override
    public void sendIgnoreMemoryWarning(long availableMegs) {
        Log.i(LOG_TAG, "sendIgnoreMemoryWarning(" + availableMegs + ")");
    }

    @Override
    public void sendHeedMemoryWarning(long availableMegs) {
        Log.i(LOG_TAG, "sendHeedMemoryWarning(" + availableMegs + ")");
    }

    @Override
    public void optionTranslateText() {
        Log.i(LOG_TAG, "optionTranslateText()");
    }

}
