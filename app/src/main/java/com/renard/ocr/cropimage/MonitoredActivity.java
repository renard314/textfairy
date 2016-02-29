/*
 * Copyright (C) 2009 The Android Open Source Project
 * Copyright (C) 2012,2013,2014,2015 Renard Wellnitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.renard.ocr.cropimage;

import com.google.common.base.Optional;

import com.renard.ocr.R;

import android.app.Activity;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;


public abstract class MonitoredActivity extends AppCompatActivity implements BaseActivityInterface, OnGlobalLayoutListener {

    private static final String LOG_TAG = MonitoredActivity.class.getSimpleName();

    private final ArrayList<LifeCycleListener> mListeners = new ArrayList<LifeCycleListener>();
    private int mDialogId = -1;
    private final Handler mHandler = new Handler();
    private ImageView mAppIcon = null;
    private Optional<IconAnimationRunnable> mRunnable = Optional.absent();
    private Toolbar mToolbar;
    private TextView mToolbarMessage;


    public interface LifeCycleListener {
        void onActivityCreated(MonitoredActivity activity);

        void onActivityDestroyed(MonitoredActivity activity);

        void onActivityPaused(MonitoredActivity activity);

        void onActivityResumed(MonitoredActivity activity);

        void onActivityStarted(MonitoredActivity activity);

        void onActivityStopped(MonitoredActivity activity);
    }

    public static class LifeCycleAdapter implements LifeCycleListener {
        public void onActivityCreated(MonitoredActivity activity) {
        }

        public void onActivityDestroyed(MonitoredActivity activity) {
        }

        public void onActivityPaused(MonitoredActivity activity) {
        }

        public void onActivityResumed(MonitoredActivity activity) {
        }

        public void onActivityStarted(MonitoredActivity activity) {
        }

        public void onActivityStopped(MonitoredActivity activity) {
        }
    }

    public synchronized void addLifeCycleListener(LifeCycleListener listener) {
        if (mListeners.contains(listener))
            return;
        mListeners.add(listener);
    }

    public synchronized void removeLifeCycleListener(LifeCycleListener listener) {
        mListeners.remove(listener);
    }

    @Override
    protected synchronized void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        for (LifeCycleListener listener : mListeners) {
            listener.onActivityCreated(this);
        }
    }

    protected void initToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbarMessage = (TextView) mToolbar.findViewById(R.id.toolbar_text);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        initAppIcon(getHintDialogId(), (ImageView) mToolbar.findViewById(R.id.app_icon));
    }

    public void setToolbarMessage(@StringRes int stringId) {
        mToolbarMessage.setVisibility(View.VISIBLE);
        mToolbarMessage.setText(stringId);
    }

    public void setToolbarMessage(String message) {
        mToolbarMessage.setText(message);
    }


    protected abstract int getHintDialogId();

    @Override
    protected synchronized void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
        for (LifeCycleListener listener : mListeners) {
            listener.onActivityDestroyed(this);
        }
    }

    @Override
    protected synchronized void onStart() {
        super.onStart();
        for (LifeCycleListener listener : mListeners) {
            listener.onActivityStarted(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        for (LifeCycleListener listener : mListeners) {
            listener.onActivityStopped(this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mDialogId != -1) {
                    showDialog(mDialogId);
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private static class IconAnimationRunnable implements Runnable {
        final AnimationDrawable animation;
        final Handler mHandler;

        private IconAnimationRunnable(AnimationDrawable animation, Handler handler) {
            this.animation = animation;
            mHandler = handler;
        }

        @Override
        public void run() {
            animation.setVisible(false, true);
            animation.start();
            final int delayMillis = (int) ((Math.random() * 5 + 15) * 1000);
            Log.i(LOG_TAG, "IconAnimationRunnable:run() in " + delayMillis + " ms.");
            mHandler.postDelayed(this, delayMillis);
        }
    }

    private static class AppIconClickListener implements View.OnClickListener {
        private final WeakReference<Activity> mActivityWeakReference;
        private final int mDialogId;

        private AppIconClickListener(Activity activity, int dialogId) {
            mDialogId = dialogId;
            mActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void onClick(View v) {
            final Activity activity = mActivityWeakReference.get();
            if (activity != null) {
                activity.showDialog(mDialogId);
            }
        }
    }

    @Override
    public void onGlobalLayout() {
        // start fairy animation at random intervals
        if (mAppIcon.getDrawable() instanceof AnimationDrawable) {
            final AnimationDrawable animation = (AnimationDrawable) mAppIcon.getDrawable();
            mRunnable = Optional.of(new IconAnimationRunnable(animation, mHandler));
            mHandler.removeCallbacksAndMessages(null);
            mHandler.post(mRunnable.get());
        }
        if (mDialogId != -1) {
            // show hint dialog when user clicks on the app icon
            mAppIcon.setOnClickListener(new AppIconClickListener(this, mDialogId));
        }
        if (mAppIcon.getViewTreeObserver().isAlive()) {
            mAppIcon.getViewTreeObserver().removeGlobalOnLayoutListener(this);
        }
    }

    /**
     * position the app icon at the bottom of the action bar and start animation
     */
    private void initAppIcon(final int dialogId, ImageView appIcon) {
        setDialogId(dialogId);
        mAppIcon = appIcon;
        final ViewTreeObserver viewTreeObserver = appIcon.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(this);

        }
    }

    @Override
    public void setDialogId(int dialogId) {
        mDialogId = dialogId;
    }

}
