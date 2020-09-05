package com.renard.ocr.documents.viewing.single.tts;

import com.renard.ocr.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import androidx.annotation.Nullable;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

import de.greenrobot.event.EventBus;

public class TextSpeaker {

    private TextToSpeech mTts;
    private boolean mIsInitialized;

    public void onDestroyView() {
        if (mTts != null) {
            mTts.shutdown();
            mTts = null;
        }
    }


    void setTtsLocale(Locale ttsLocale) {
        final boolean localeSupported = isLocaleSupported(ttsLocale);
        if (localeSupported) {
            try {
                mTts.setLanguage(ttsLocale);
            } catch (IllegalArgumentException ignored) {

            }
        }
    }

    void stopSpeaking() {
        mTts.stop();
    }

    int startSpeaking(CharSequence text, String utteranceId) {
        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bundle params = new Bundle();
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, TextToSpeech.Engine.DEFAULT_STREAM);
            result = mTts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId);
        } else {
            HashMap<String, String> alarm = new HashMap<>();
            alarm.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(TextToSpeech.Engine.DEFAULT_STREAM));
            alarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
            result = mTts.speak(text.toString(), TextToSpeech.QUEUE_FLUSH, alarm);
        }
        return result;
    }

    Locale getLanguageFromTts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                if (mTts.getVoice() != null) {
                    return mTts.getVoice().getLocale();
                }
            } catch (Exception why) {
                return Locale.getDefault();
            }
        }
        return mTts.getLanguage();
    }

    void createTts(final Context context, final TtsInitListener listener) {
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
                    onUtteranceStart(utteranceId);
                }

                @Override
                public void onDone(String utteranceId) {
                    onUtteranceDone(utteranceId);
                }

                @Override
                public void onError(String utteranceId) {

                }
            });

        } else {
            final TextToSpeech.OnUtteranceCompletedListener onUtteranceCompletedListener = new TextToSpeech.OnUtteranceCompletedListener() {

                @Override
                public void onUtteranceCompleted(String utteranceId) {
                    onUtteranceDone(utteranceId);
                }
            };
            mTts.setOnUtteranceCompletedListener(onUtteranceCompletedListener);
        }
    }


    private void onUtteranceStart(String utteranceId) {
        EventBus.getDefault().post(new OnUtteranceStart(utteranceId));
    }

    private void onUtteranceDone(String utteranceId) {
        EventBus.getDefault().post(new OnUtteranceDone(utteranceId));
    }

    boolean isLocaleSupported(@Nullable Locale locale) {
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
            try {
                final Set<Locale> availableLanguages = mTts.getAvailableLanguages();
                if (availableLanguages == null) {
                    return new ArrayList<>();
                } else {
                    return availableLanguages;
                }
            } catch (IllegalArgumentException exception) {
                return getValidTtsLocales();
            } catch (NullPointerException npe) {
                return getValidTtsLocales();
            }
        } else {
            return getValidTtsLocales();
        }
    }

    private Collection<Locale> getValidTtsLocales() {
        HashMap<String, Locale> localeMap = new HashMap<>();
        Locale[] allLocales = Locale.getAvailableLocales();
        for (Locale locale : allLocales) {
            if (isLocaleSupported(locale)) {
                localeMap.put(locale.getLanguage(), locale);
            }
        }
        return localeMap.values();
    }


    boolean isInitialized() {
        return mIsInitialized;
    }

}
