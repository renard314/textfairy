package com.renard.ocr.documents.viewing.single;

import com.renard.ocr.analytics.Analytics;
import com.renard.ocr.MonitoredActivity;
import com.renard.ocr.R;
import com.renard.ocr.main_menu.language.OcrLanguage;
import com.renard.ocr.util.ResourceUtils;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by renard on 05/12/13.
 */
public class TtsActionCallback implements ActionMode.Callback, TextToSpeech.OnInitListener, MonitoredActivity.LifeCycleListener {

    private final static String LOG_TAG = TtsActionCallback.class.getSimpleName();

    private TextToSpeech mTts;
    private final DocumentActivity activity;
    private boolean mTtsReady = false;
    private ActionMode mActionMode;
    final Map<String, String> hashMapResource;
    private final Analytics mAnalytics;

    TtsActionCallback(DocumentActivity activity) {
        mAnalytics = activity.getAnaLytics();
        hashMapResource = ResourceUtils.getHashMapResource(activity, R.xml.iso_639_mapping);
        this.activity = activity;
        this.activity.addLifeCycleListener(this);
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        if (mTtsReady) {
            //show play and stop button
            menu.findItem(R.id.item_play).setVisible(true);
            menu.findItem(R.id.item_stop).setVisible(false);
            menu.findItem(R.id.item_tts_settings).setVisible(true);
        } else {
            activity.setSupportProgressBarIndeterminateVisibility(true);
            menu.findItem(R.id.item_play).setVisible(false);
            menu.findItem(R.id.item_stop).setVisible(false);
            menu.findItem(R.id.item_tts_settings).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.item_play:
                startPlaying(actionMode);
                break;
            case R.id.item_stop:
                mAnalytics.ttsStop();
                stopPlaying(actionMode);
                break;
            case R.id.item_tts_settings:
                stopPlaying(actionMode);
                askForLocale();
                break;
        }
        return false;
    }

    private void startPlaying(ActionMode actionMode) {
        final Locale language = mTts.getLanguage();
        if (language != null) {
            mAnalytics.ttsStart(language.getLanguage());
        } else {
            mAnalytics.ttsStart("no language");
        }
        HashMap<String, String> alarm = new HashMap<String, String>();
        alarm.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(TextToSpeech.Engine.DEFAULT_STREAM));
        alarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, LOG_TAG);
        final String plainDocumentText = activity.getCurrentDocumentText();
        int result = mTts.speak(plainDocumentText, TextToSpeech.QUEUE_FLUSH, alarm);
        actionMode.getMenu().findItem(R.id.item_play).setVisible(false);
        actionMode.getMenu().findItem(R.id.item_stop).setVisible(true);
    }

    private void stopPlaying(ActionMode actionMode) {
        mTts.stop();
        actionMode.getMenu().findItem(R.id.item_play).setVisible(true);
        actionMode.getMenu().findItem(R.id.item_stop).setVisible(false);
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        mActionMode = actionMode;

        activity.getMenuInflater().inflate(R.menu.tts_action_mode, menu);
        if (mTts == null) {
            Intent checkIntent = new Intent();
            checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
            ResolveInfo resolveInfo = activity.getPackageManager().resolveActivity(checkIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfo != null) {
                activity.startActivityForResult(checkIntent, DocumentActivity.REQUEST_CODE_TTS_CHECK);
                return true;
            } else {
                Toast.makeText(activity, R.string.tts_not_available, Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        if (mTtsReady && mTts != null) {
            mTts.stop();
        }

    }

    @Override
    public void onInit(int status) {
        if (mActionMode == null) {
            return;
        }
        activity.setSupportProgressBarIndeterminateVisibility(false);
        // status can be either TextToSpeech.SUCCESS or TextToSpeech.ERROR.
        if (status == TextToSpeech.ERROR) {
            Log.e(LOG_TAG, "Could not initialize TextToSpeech.");
            Toast.makeText(activity, R.string.tts_init_error, Toast.LENGTH_LONG).show();
            mActionMode.finish();
        } else {
            registerForSpeechFinished();
            mActionMode.getMenu().findItem(R.id.item_tts_settings).setVisible(true);
            String ocrLanguage = activity.getLanguageOfDocument();
            Locale documentLocale = mapTesseractLanguageToLocale(ocrLanguage);
            if (documentLocale == null) {
                askForLocale();
            } else {
                if (isLanguageAvailable(new OcrLanguage(ocrLanguage, null, true, 0))) {
                    mTts.setLanguage(documentLocale);
                    mActionMode.getMenu().findItem(R.id.item_play).setVisible(true);
                    mTtsReady = true;
                } else {
                    askForLocale(ocrLanguage, true);
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private void registerForSpeechFinished() {
        final Handler handler = new Handler();
        if (Build.VERSION.SDK_INT >= 15) {
            mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {

                }

                @Override
                public void onDone(String utteranceId) {
                    onUtteranceDone(utteranceId, handler);
                }

                @Override
                public void onError(String utteranceId) {

                }
            });

        } else {
            final TextToSpeech.OnUtteranceCompletedListener onUtteranceCompletedListener = new TextToSpeech.OnUtteranceCompletedListener() {

                @Override
                public void onUtteranceCompleted(String utteranceId) {
                    onUtteranceDone(utteranceId, handler);
                }
            };
            mTts.setOnUtteranceCompletedListener(onUtteranceCompletedListener);
        }
    }

    private void onUtteranceDone(String utteranceId, Handler handler) {
        if (utteranceId.equalsIgnoreCase(LOG_TAG)) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    mActionMode.getMenu().findItem(R.id.item_play).setVisible(true);
                    mActionMode.getMenu().findItem(R.id.item_stop).setVisible(false);
                }
            });

        }
    }


    private void askForLocale(final String documentLanguage, boolean languageSupported) {
        PickTtsLanguageDialog.newInstance(documentLanguage, languageSupported, activity).show(activity.getSupportFragmentManager(), PickTtsLanguageDialog.TAG);
    }

    private void askForLocale() {
        PickTtsLanguageDialog.newInstance(activity).show(activity.getSupportFragmentManager(), PickTtsLanguageDialog.TAG);
    }

    /**
     * user has picked a language for tts
     */
    public void onTtsLanguageChosen(OcrLanguage lang) {
        mAnalytics.ttsLanguageChanged(lang);
        Locale documentLocale = mapTesseractLanguageToLocale(lang.getValue());
        int result = mTts.setLanguage(documentLocale);
        switch (result) {
            case TextToSpeech.LANG_COUNTRY_AVAILABLE:
            case TextToSpeech.LANG_AVAILABLE:
            case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
                Log.i(LOG_TAG, "language ok");
                break;
            default:
                Log.i(LOG_TAG, "language not supported");
                break;
        }
        mActionMode.getMenu().findItem(R.id.item_play).setVisible(true);
        mActionMode.getMenu().findItem(R.id.item_stop).setVisible(false);
        mTtsReady = true;
    }

    public void onTtsCancelled() {
        if (mTts != null) {
            mTts.shutdown();
            mTts = null;
        }
        mTtsReady = false;
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    public boolean isLanguageAvailable(OcrLanguage lang) {
        if (mTts != null) {
            final Locale locale = mapTesseractLanguageToLocale(lang.getValue());
            if (locale == null) {
                return false;
            }
            Log.i(LOG_TAG, "Checking " + locale.toString());
            final int result = mTts.isLanguageAvailable(locale);
            switch (result) {
                case TextToSpeech.LANG_NOT_SUPPORTED:
                    Log.i(LOG_TAG, "LANG_NOT_SUPPORTED");
                    return false;
                case TextToSpeech.LANG_MISSING_DATA:
                    Log.i(LOG_TAG, "LANG_MISSING_DATA");
                    return false;
                case TextToSpeech.LANG_COUNTRY_AVAILABLE:
                    Log.w(LOG_TAG, "LANG_COUNTRY_AVAILABLE");
                    mTtsReady = true;
                    return true;
                case TextToSpeech.LANG_AVAILABLE:
                    Log.w(LOG_TAG, "LANG_AVAILABLE");
                    mTtsReady = true;
                    return true;
                default:
                    return true;
            }
        }
        return false;
    }


    private Locale mapTesseractLanguageToLocale(String ocrLanguage) {
        final String s = hashMapResource.get(ocrLanguage);
        if (s != null) {
            return new Locale(s);
        } else {
            return null;
        }
    }

    void onTtsCheck(int resultCode) {

        if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
            // success, create the TTS instance
            mTts = new TextToSpeech(activity, this);
        } else {
            if (mActionMode != null) {
                mActionMode.finish();
            }
            // missing data, install it
            Intent installIntent = new Intent();
            installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
            activity.startActivity(installIntent);
        }

    }

    @Override
    public void onActivityCreated(MonitoredActivity activity) {

    }

    @Override
    public void onActivityDestroyed(MonitoredActivity activity) {
        if (mTtsReady) {
            mTts.shutdown();
            mTtsReady = false;
            mTts = null;
        }
    }

    @Override
    public void onActivityPaused(MonitoredActivity activity) {

    }

    @Override
    public void onActivityResumed(MonitoredActivity activity) {

    }

    @Override
    public void onActivityStarted(MonitoredActivity activity) {

    }

    @Override
    public void onActivityStopped(MonitoredActivity activity) {

    }
}
