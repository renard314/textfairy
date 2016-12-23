package com.renard.ocr.documents.viewing.single.tts;

import java.util.Locale;

public class TtsLanguageChoosen {
    private final Locale mLocale;

    TtsLanguageChoosen(Locale locale) {
        mLocale = locale;
    }

    public Locale getLocale() {
        return mLocale;
    }
}
