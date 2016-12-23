package com.renard.ocr.documents.viewing.single.tts;

import com.renard.ocr.R;
import com.renard.ocr.analytics.Analytics;

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

import java.util.Locale;

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
    private TextSpeaker mTextSpeaker;


    public void onCreateView(FragmentManager childFragmentManager, Analytics analytics) {
        mChildFragmentManager = childFragmentManager;
        mAnalytics = analytics;
        EventBus.getDefault().register(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(final TtsLanguageChoosen event) {
        Locale documentLocale = event.getLocale();
        setTtsLocale(documentLocale);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(final OnUtteranceDone event) {
        showPlayButton();
    }

    private void setTtsLocale(Locale locale) {
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
        showProgressSpinner();
        mButtonLanguage.setEnabled(false);
    }

    @OnClick(R.id.button_language)
    public void onClickLanguage() {
        askForLocale();
    }

    @OnClick(R.id.button_stop_speaking)
    public void onClickStopSpeaking() {
        mTextSpeaker.stopSpeaking();
        showPlayButton();
    }

    @OnClick(R.id.button_speak_text)
    public void onClickStartSpeaking() {
        Locale language = mTextSpeaker.getLanguageFromTts();
        if (language != null) {
            mAnalytics.ttsStart(language.getLanguage());
        } else {
            mAnalytics.ttsStart("no language");
        }

        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        final float streamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (streamVolume == 0) {
            Toast.makeText(getContext(), R.string.volume_is_off, Toast.LENGTH_SHORT).show();
            return;
        }

        final float result = mTextSpeaker.startSpeaking(mCurrentText);
        if (result == TextToSpeech.SUCCESS) {
            showStopButton();
        } else {
            Toast.makeText(getContext(), R.string.tts_init_error, Toast.LENGTH_LONG).show();
        }
    }

    public void onInitError() {
        setVisibility(View.GONE);
    }

    public void onInitSuccess(@Nullable TextSpeaker textSpeaker) {
        if (textSpeaker != null) {
            mTextSpeaker = textSpeaker;
            mButtonLanguage.setEnabled(true);
            setTtsLocale(mTextSpeaker.getTtsLocale());
            showPlayButton();
        } else {
            showProgressSpinner();
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
        PickTtsLanguageDialog.newInstance(mTextSpeaker.getAvailableLanguages(), getContext()).show(mChildFragmentManager, PickTtsLanguageDialog.TAG);
    }

    public void setCurrentText(CharSequence currentText) {
        mCurrentText = currentText;
    }
}
