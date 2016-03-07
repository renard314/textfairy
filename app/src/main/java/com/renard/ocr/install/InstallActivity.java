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

import com.renard.ocr.MonitoredActivity;
import com.renard.ocr.PermissionGrantedEvent;
import com.renard.ocr.R;
import com.renard.ocr.util.Util;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import java.io.File;

import de.greenrobot.event.EventBus;

/**
 * wrapper activity for the AssetsManager
 */
public class InstallActivity extends MonitoredActivity implements TaskFragment.TaskCallbacks {
    private static final String TAG_TASK_FRAGMENT = "task_fragment";
    @SuppressWarnings("unused")
    private static final String LOG_TAG = InstallActivity.class.getSimpleName();

    private ProgressBar mProgressBar = null;
    private TextView mTextViewSpeechBubble = null;
    private ImageView mImageViewFairy;
    private View mFairyContainer = null;
    private AnimationDrawable mFairyAnimation;
    private TextView mButtonStartApp = null;
    private ViewSwitcher mSwitcher = null;
    private View mContentView = null;
    private TaskFragment mTaskFragment;

    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        setContentView(R.layout.activity_install);
        mImageViewFairy = (ImageView) findViewById(R.id.imageView_fairy);
        mFairyContainer = findViewById(R.id.fairy_container);
        mProgressBar = (ProgressBar) findViewById(R.id.installactivity_progressbar);
        mTextViewSpeechBubble = (TextView) findViewById(R.id.fairy_text);
        mFairyAnimation = (AnimationDrawable) mImageViewFairy.getDrawable();
        mButtonStartApp = (TextView) findViewById(R.id.button_start_app);
        mSwitcher = (ViewSwitcher) findViewById(R.id.viewSwitcher_progress);
        mContentView = findViewById(R.id.content_view);
        TextView mTextViewPromoLink = (TextView) findViewById(R.id.promo);


        android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
        mTaskFragment = (TaskFragment) fm.findFragmentByTag(TAG_TASK_FRAGMENT);

        // If the Fragment is non-null, then it is currently being
        // retained across a configuration change.
        if (mTaskFragment == null) {
            Log.i(LOG_TAG, "ensuring permission for: " + this);
            ensurePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, R.string.permission_explanation_install);
        } else {
            InstallResult result = mTaskFragment.getInstallResult();
            if (result != null) {
                markAsDone(result);
            } else {
                startInstallAnimation();
            }
        }

        mTextViewPromoLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                watchYoutubeVideo(getString(R.string.youtube_promo_link));
            }
        });

    }

    @SuppressWarnings("unused")
    public void onEventMainThread(final PermissionGrantedEvent event) {
        Log.i(LOG_TAG, "PermissionGrantedEvent : " + this);
        mTaskFragment = new TaskFragment();
        final FragmentManager supportFragmentManager = getSupportFragmentManager();
        supportFragmentManager.beginTransaction().add(mTaskFragment, TAG_TASK_FRAGMENT).commitAllowingStateLoss();
    }

    @Override
    protected int getHintDialogId() {
        return 0;
    }

    private void startInstallAnimation() {
        mImageViewFairy.post(new Runnable() {

            @Override
            public void run() {
                mFairyAnimation.start();
            }
        });
    }

    private void watchYoutubeVideo(String id) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(id));
        startActivity(intent);
    }

    /**
     * @return if the language assets are installed or not
     */
    public static boolean IsInstalled(Context appContext) {
        File tessDir = Util.getTrainingDataDir(appContext);
        return tessDir.exists();

    }


    @Override
    protected synchronized void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void markAsDone(InstallResult result) {
        mSwitcher.setDisplayedChild(1);
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
                mTextViewSpeechBubble.setText(R.string.start_app);
                break;
            case NOT_ENOUGH_DISK_SPACE:
                String errorMsg = getString(R.string.install_error_disk_space);
                final long diff = result.getNeededSpace() - result.getFreeSpace();
                errorMsg = String.format(errorMsg, (diff / (1024 * 1024)));
                mTextViewSpeechBubble.setText(errorMsg);
                mButtonStartApp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                });
                break;
            case UNSPECIFIED_ERROR:
                errorMsg = getString(R.string.install_error);
                mTextViewSpeechBubble.setText(errorMsg);
                mButtonStartApp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                });
                break;
        }
    }


    @Override
    public void onPreExecute() {
        startInstallAnimation();
    }

    @Override
    public void onProgressUpdate(int progress) {
        final int fairyEndX = mContentView.getWidth() / 2;
        final int fairyStartX = mImageViewFairy.getWidth() / 2;
        final int maxTravelDistance = Math.min(fairyEndX - fairyStartX, mContentView.getWidth() - mFairyContainer.getWidth());
        final float translateX = maxTravelDistance * ((float) progress / 100);
        RelativeLayout.LayoutParams lp = (android.widget.RelativeLayout.LayoutParams) mFairyContainer.getLayoutParams();
        lp.leftMargin = (int) translateX;
        mFairyContainer.setLayoutParams(lp);
        mProgressBar.setProgress(progress);
    }

    @Override
    public void onCancelled() {

    }

    @Override
    public void onPostExecute(InstallResult result) {
        markAsDone(result);
    }
}
