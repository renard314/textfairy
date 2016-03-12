package com.renard.ocr;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import com.renard.ocr.language.OcrLanguage;
import com.renard.ocr.visualisation.LayoutQuestionDialog;

/**
 * @author renard
 */
public class Analytics {

    private final Tracker mTracker;

    public Analytics(Tracker tracker) {
        mTracker = tracker;
    }

    public void sendScreenView(String screenName) {
        mTracker.setScreenName(screenName);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }


    public void sendStartDownload(OcrLanguage language) {
        sendEvent("Language", "Start download", language.getValue(), 1);
    }


    public void sendDeleteLanguage(OcrLanguage language) {
        sendEvent("Language", "Delete language", language.getValue(), 1);
    }

    public void sendOcrResult(String language, int accuracy) {
        sendEvent("Ocr", "Scan completed", language, accuracy);
    }

    public void sendOcrLanguageChanged(OcrLanguage language) {
        sendEvent("Ocr", "Picked Language", language.getValue(), 1);
    }

    public void sendLayoutDialogCancelled() {
        sendEvent("Ocr", "Scan Cancelled", "Layout Dialog", 1);
    }

    public void sendOcrCancelled() {
        sendEvent("Ocr", "Scan Cancelled", "Back Button", 1);
    }


    public void sendOcrStarted(String language, LayoutQuestionDialog.LayoutKind layout) {
        sendEvent("Ocr", "Scan started", language, 1);
        sendEvent("Ocr", "Layout chosen", layout.name(), 1);
    }

    private void sendEvent(String category, String action, String label, int value) {
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
                .setValue(value)
                .build());
    }

}
