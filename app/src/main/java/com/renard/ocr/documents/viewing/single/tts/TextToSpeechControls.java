package com.renard.ocr.documents.viewing.single.tts;

import com.renard.ocr.R;
import com.renard.ocr.analytics.Analytics;
import com.renard.ocr.util.ResourceUtils;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;

public class TextToSpeechControls extends RelativeLayout {

    @BindView(R.id.view_flipper_buttons)
    protected ViewFlipper mViewFlipper;
    @BindView(R.id.button_language)
    protected TextView mButtonLanguage;

    private FragmentManager mChildFragmentManager;
    private Analytics mAnalytics;
    private CharSequence mCurrentText;
    private Map<String, String> hashMapResource;
    private Locale mDocumentLocale;
    @Nullable
    private String mPageNo;
    private TextSpeaker mTextSpeaker;


    public void onCreateView(FragmentManager childFragmentManager, Analytics analytics, TextSpeaker textSpeaker) {
        mTextSpeaker = textSpeaker;
        mChildFragmentManager = childFragmentManager;
        mAnalytics = analytics;
        hashMapResource = ResourceUtils.getHashMapResource(getContext(), R.xml.iso_639_mapping);
        EventBus.getDefault().register(this);

    }

    public Locale mapTesseractLanguageToLocale(String ocrLanguage) {
        final String s = hashMapResource.get(ocrLanguage);
        if (s != null) {
            return new Locale(s);
        } else {
            return Locale.getDefault();
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(final TtsLanguageChoosen event) {
        Locale documentLocale = event.getLocale();
        setTtsLocale(documentLocale);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(final OnUtteranceStart event) {
        if (event.getUtteranceId().equals(mPageNo)) {
            showStopButton();
        } else {
            showPlayButton();
        }
    }


    @SuppressWarnings("unused")
    public void onEventMainThread(final OnUtteranceDone event) {
        if (event.getUtteranceId().equals(mPageNo)) {
            showPlayButton();
        }
    }

    private void setTtsLocale(Locale locale) {
        mDocumentLocale = locale;
        mButtonLanguage.setText(locale.getDisplayLanguage());
    }

    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
    }

    public TextToSpeechControls(Context context) {
        super(context);
        init();
    }

    public TextToSpeechControls(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TextToSpeechControls(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @SuppressWarnings("unused")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TextToSpeechControls(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        View view = LayoutInflater.from(this.getContext()).inflate(R.layout.view_text_to_speech_controls, this);
        ButterKnife.bind(view, this);
        showPlayButton();
        mButtonLanguage.setEnabled(false);
    }

    @OnClick(R.id.button_language)
    public void onClickLanguage() {
        if (mTextSpeaker.isInitialized()) {
            askForLocale();
        } else {
            showProgressSpinner();
            mTextSpeaker.createTts(getContext(), new TtsInitListener() {
                @Override
                public void onInitError() {
                    showPlayButton();
                    Toast.makeText(getContext(), R.string.tts_init_error, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onInitSuccess() {
                    showPlayButton();
                    askForLocale();

                }
            });
        }

    }

    @OnClick(R.id.button_stop_speaking)
    public void onClickStopSpeaking() {
        mTextSpeaker.stopSpeaking();
        showPlayButton();
    }

    @OnClick(R.id.button_speak_text)
    public void onClickStartSpeaking() {

        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        final float streamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (streamVolume == 0) {
            Toast.makeText(getContext(), R.string.volume_is_off, Toast.LENGTH_SHORT).show();
            return;
        }
        showProgressSpinner();
        if (mTextSpeaker.isInitialized()) {
            startSpeaking();
        } else {
            mTextSpeaker.createTts(getContext(), new TtsInitListener() {
                @Override
                public void onInitError() {
                    showPlayButton();
                    Toast.makeText(getContext(), R.string.tts_init_error, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onInitSuccess() {
                    startSpeaking();

                }
            });
        }

    }

    private void startSpeaking() {
        Locale language = mTextSpeaker.getLanguageFromTts();
        if (language != null) {
            mAnalytics.ttsStart(language.getLanguage());
        } else {
            mAnalytics.ttsStart("no language");
        }
        mTextSpeaker.setTtsLocale(mDocumentLocale);
        final int result = mTextSpeaker.startSpeaking(mCurrentText, mPageNo);
        if (result != TextToSpeech.SUCCESS) {
            Toast.makeText(getContext(), R.string.tts_init_error, Toast.LENGTH_LONG).show();
        } else if (Build.VERSION.SDK_INT < 15) {
            showStopButton();
        }

    }


    private void showProgressSpinner() {
        mViewFlipper.setDisplayedChild(1);
    }

    private void showStopButton() {
        mViewFlipper.setDisplayedChild(2);
    }

    private void showPlayButton() {
        mViewFlipper.setDisplayedChild(0);
    }

    private void askForLocale() {
        final Collection<Locale> availableLanguages = mTextSpeaker.getAvailableLanguages();
        if (availableLanguages.isEmpty()) {
            TtsLanguageInstallDialog.newInstance().show(mChildFragmentManager, TtsLanguageInstallDialog.TAG);
        } else {
            PickTtsLanguageDialog.newInstance(mTextSpeaker.getAvailableLanguages()).show(mChildFragmentManager, PickTtsLanguageDialog.TAG);
        }
    }

    public void setCurrentText(CharSequence currentText, String langOfCurrentlyShownDocument, int pageNo) {
        mCurrentText = currentText;
        if (mDocumentLocale == null) {
            mDocumentLocale = mapTesseractLanguageToLocale(langOfCurrentlyShownDocument);
            mButtonLanguage.setText(mDocumentLocale.getDisplayLanguage());
        }
        mPageNo = String.valueOf(pageNo);
        mButtonLanguage.setEnabled(true);

    }
}
