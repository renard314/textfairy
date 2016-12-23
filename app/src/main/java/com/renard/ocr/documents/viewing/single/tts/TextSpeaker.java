package com.renard.ocr.documents.viewing.single.tts;

import com.renard.ocr.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import de.greenrobot.event.EventBus;

public class TextSpeaker {

    private static final String LOG_TAG = "TextSpeaker";

    private TextToSpeech mTts;
    private boolean mIsInitialized;


    public void onDestroyView() {
        if (mTts != null) {
            mTts.shutdown();
            mTts = null;
        }
    }


    public void setTtsLocale(Locale ttsLocale) {
        mTts.setLanguage(ttsLocale);
    }

    public Locale getTtsLocale() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return mTts.getVoice().getLocale();
        } else {
            return mTts.getLanguage();
        }
    }

    public void stopSpeaking() {
        mTts.stop();
    }

    int startSpeaking(CharSequence text) {
        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bundle params = new Bundle();
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, TextToSpeech.Engine.DEFAULT_STREAM);
            result = mTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, LOG_TAG);
        } else {
            HashMap<String, String> alarm = new HashMap<>();
            alarm.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(TextToSpeech.Engine.DEFAULT_STREAM));
            alarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, LOG_TAG);
            result = mTts.speak(text.toString(), TextToSpeech.QUEUE_FLUSH, alarm);
        }
        return result;
    }

    Locale getLanguageFromTts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mTts.getVoice() != null) {
                return mTts.getVoice().getLocale();
            }
        }
        return mTts.getLanguage();
    }

    public void createTts(final Context context, final TtsInitListener listener) {
        mTts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.ERROR) {
                    Toast.makeText(context, R.string.tts_init_error, Toast.LENGTH_LONG).show();
                    listener.onInitError();
                } else {
                    mIsInitialized = true;
                    listener.onInitSuccess();
                    registerForSpeechFinished();
                }

            }
        });
    }

    @SuppressLint("NewApi")
    private void registerForSpeechFinished() {
        if (Build.VERSION.SDK_INT >= 15) {
            mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {

                }

                @Override
                public void onDone(String utteranceId) {
                    onUtteranceDone();
                }

                @Override
                public void onError(String utteranceId) {

                }
            });

        } else {
            final TextToSpeech.OnUtteranceCompletedListener onUtteranceCompletedListener = new TextToSpeech.OnUtteranceCompletedListener() {

                @Override
                public void onUtteranceCompleted(String utteranceId) {
                    onUtteranceDone();
                }
            };
            mTts.setOnUtteranceCompletedListener(onUtteranceCompletedListener);
        }
    }

    private void onUtteranceDone() {
        EventBus.getDefault().post(new OnUtteranceDone());
    }

    public boolean isLocaleSupported(@Nullable Locale locale) {
        boolean result;
        try {
            if (locale == null) {
                return false;
            }
            int res = mTts.isLanguageAvailable(locale);
            boolean hasVariant = (null != locale.getVariant() && locale.getVariant().length() > 0);
            boolean hasCountry = (null != locale.getCountry() && locale.getCountry().length() > 0);

            result = !hasVariant && !hasCountry && res == TextToSpeech.LANG_AVAILABLE ||
                    !hasVariant && hasCountry && res == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                    res == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE;
        } catch (IllegalArgumentException sillySamsungIsSilly) {
            result = false;
        }
        return result;
    }


    Collection<Locale> getAvailableLanguages() {
        if (Build.VERSION.SDK_INT >= 21) {
            return mTts.getAvailableLanguages();
        } else {
            HashMap<String, Locale> localeMap = new HashMap<>();
            Locale[] allLocales = Locale.getAvailableLocales();
            for (Locale locale : allLocales) {
                if (isLocaleSupported(locale)) {
                    localeMap.put(locale.getLanguage(), locale);
                }
            }
            return localeMap.values();
        }
    }


    public boolean isInitialized() {
        return mIsInitialized;
    }

    public boolean isSpeaking() {
        return mTts.isSpeaking();
    }
}
