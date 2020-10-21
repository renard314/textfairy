/*
 * Copyright (C) 2012,2013 Renard Wellnitz.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.renard.ocr.install;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentManager;

import com.renard.ocr.MonitoredActivity;
import com.renard.ocr.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * wrapper activity for the AssetsManager
 */
public class InstallActivity extends MonitoredActivity implements TaskFragment.TaskCallbacks {
    private static final String TAG_TASK_FRAGMENT = "task_fragment";
    @SuppressWarnings("unused")
    private static final String LOG_TAG = InstallActivity.class.getSimpleName();

    @BindView(R.id.button_start_app)
    protected TextView mButtonStartApp;
    @BindView(R.id.content_view)
    protected View mContentView;
    @BindView(R.id.fairy_container)
    protected View mFairyContainer;
    @BindView(R.id.imageView_fairy)
    protected ImageView mImageViewFairy;
    @BindView(R.id.fairy_text_bubble)
    protected View mFairySpeechBubble;
    @BindView(R.id.fairy_text)
    protected TextView mFairyText;

    @BindView(R.id.tip1)
    protected View mTip1;
    @BindView(R.id.tip2)
    protected View mTip2;
    @BindView(R.id.tip3)
    protected View mTip3;
    @BindView(R.id.promo)
    protected View mYoutube;

    private AnimationDrawable mFairyAnimation;

    @Override
    public String getScreenName() {
        return "Install Activity";
    }

    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install);
        ButterKnife.bind(this);

        mFairyAnimation = (AnimationDrawable) mImageViewFairy.getDrawable();
        FragmentManager fm = getSupportFragmentManager();
        TaskFragment mTaskFragment = (TaskFragment) fm.findFragmentByTag(TAG_TASK_FRAGMENT);

        // If the Fragment is non-null, then it is currently being
        // retained across a configuration change.
        if (mTaskFragment == null) {
            mTaskFragment = new TaskFragment();
            final FragmentManager supportFragmentManager = getSupportFragmentManager();
            supportFragmentManager.beginTransaction().add(mTaskFragment, TAG_TASK_FRAGMENT).commitAllowingStateLoss();
        } else {
            InstallResult result = mTaskFragment.getInstallResult();
            if (result != null) {
                markAsDone(result);
            } else {
                startInstallAnimation();
            }
        }

    }

    @OnClick(R.id.promo)
    public void clickOnYoutubeLink() {
        mAnalytics.sendClickYoutube();
        final String link = getString(R.string.youtube_promo_link);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        startActivity(intent);
    }


    @Override
    protected int getHintDialogId() {
        return 0;
    }

    private void startInstallAnimation() {
        mTip1.setAlpha(0);
        mTip2.setAlpha(0);
        mTip3.setAlpha(0);
        mYoutube.setAlpha(0);

        ObjectAnimator anim1 = ObjectAnimator.ofFloat(mTip1, "alpha", 1);
        ObjectAnimator anim2 = ObjectAnimator.ofFloat(mTip2, "alpha", 1);
        ObjectAnimator anim3 = ObjectAnimator.ofFloat(mTip3, "alpha", 1);
        ObjectAnimator anim4 = ObjectAnimator.ofFloat(mYoutube, "alpha", 1);
        AnimatorSet set = new AnimatorSet();
        set.setStartDelay(300);
        set.setDuration(600);
        set.playTogether(anim1, anim2, anim3, anim4);
        set.start();
        mFairyAnimation.start();

    }

    private void markAsDone(InstallResult result) {
        fadeInStartButton();
        mFairyAnimation.stop();
        switch (result.getResult()) {
            case OK:
                final View.OnClickListener onClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        setResult(RESULT_OK);
                        finish();
                    }
                };
                mButtonStartApp.setOnClickListener(onClickListener);
                mFairyContainer.setOnClickListener(onClickListener);
                mFairySpeechBubble.setVisibility(View.VISIBLE);
                mFairyText.setText(R.string.start_app);
                break;
            case NOT_ENOUGH_DISK_SPACE:
                String errorMsg = getString(R.string.install_error_disk_space);
                final long diff = result.getNeededSpace() - result.getFreeSpace();
                errorMsg = String.format(errorMsg, (diff / (1024 * 1024)));
                mFairyText.setText(errorMsg);
                mButtonStartApp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                });
                break;
            case MEDIA_NOT_MOUNTED:
                showError(R.string.no_sd_card);
                break;
            case UNSPECIFIED_ERROR:
                showError(R.string.install_error);
                break;
        }
    }

    private void showError(@StringRes int message) {
        mFairyText.setText(getString(message));
        mButtonStartApp.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }

    private void fadeInStartButton() {
        mButtonStartApp.setVisibility(View.VISIBLE);
        mButtonStartApp.setAlpha(0);
        mButtonStartApp.animate().alpha(1);
    }


    @Override
    public void onPreExecute() {
        startInstallAnimation();
    }

    @Override
    public void onProgressUpdate(int progress) {
        final float translateX = getTranslateX(progress);
        translateTextfairy((int) translateX);
    }

    private void translateTextfairy(int translateX) {
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mFairyContainer.getLayoutParams();
        lp.leftMargin = translateX;
        mFairyContainer.setLayoutParams(lp);
    }

    private float getTranslateX(float progress) {
        final int fairyEndX = mContentView.getWidth() / 2;
        final int fairyStartX = mImageViewFairy.getWidth() / 2;
        final int maxTravelDistance = Math.min(fairyEndX - fairyStartX, mContentView.getWidth() - mFairyContainer.getWidth());
        return maxTravelDistance * (progress / 100);
    }

    @Override
    public void onCancelled() {

    }

    @Override
    public void onPostExecute(InstallResult result) {
        markAsDone(result);
    }
}
