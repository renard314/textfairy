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
import com.squareup.leakcanary.LeakCanary;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;


public class MonitoredActivity extends ActionBarActivity implements BaseActivityInterface, OnGlobalLayoutListener {

    private final ArrayList<LifeCycleListener> mListeners = new ArrayList<LifeCycleListener>();
    private int mDialogId = -1;
    private final Handler mHandler = new Handler();
    private ImageView appIcon = null;
    private Optional<IconAnimationRunnable> mRunnable = Optional.absent();


    public static interface LifeCycleListener {
        public void onActivityCreated(MonitoredActivity activity);

        public void onActivityDestroyed(MonitoredActivity activity);

        public void onActivityPaused(MonitoredActivity activity);

        public void onActivityResumed(MonitoredActivity activity);

        public void onActivityStarted(MonitoredActivity activity);

        public void onActivityStopped(MonitoredActivity activity);
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

    @Override
    protected synchronized void onDestroy() {
        super.onDestroy();
        if (mRunnable.isPresent()) {
            mHandler.removeCallbacks(mRunnable.get());
        }
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
            final int delayMillis = (int) ((Math.random() * 5 + 5) * 1000);
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
            if(activity !=null) {
                activity.showDialog(mDialogId);
            }
        }
    }

    @Override
    public void onGlobalLayout() {
        FrameLayout.LayoutParams llp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        appIcon.getViewTreeObserver().removeGlobalOnLayoutListener(this);
        final int iconHeight = appIcon.getHeight() + 1;
        final int barHeight = ((FrameLayout) appIcon.getParent()).getHeight();
        int leftMargin = mDialogId == -1 ? 0 : (int) appIcon.getContext().getResources().getDimension(R.dimen.home_icon_margin);
        llp.setMargins(leftMargin, barHeight - iconHeight, 0, 0);
        appIcon.setLayoutParams(llp);
        appIcon.invalidate();

        // start fairy animation at random intervals
        if (appIcon.getDrawable() instanceof AnimationDrawable) {
            final AnimationDrawable animation = (AnimationDrawable) appIcon.getDrawable();
            mRunnable = Optional.of(new IconAnimationRunnable(animation, mHandler));
            mHandler.post(mRunnable.get());
        }
        if (mDialogId != -1) {
            // show hint dialog when user clicks on the app icon
            appIcon.setOnClickListener(new AppIconClickListener(this, mDialogId));
        }
    }

    /**
     * position the app icon at the bottom of the action bar and start animation
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void initAppIcon(final int dialogId) {
        setDialogId(dialogId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            appIcon = (ImageView) findViewById(android.R.id.home);
        }
        if (appIcon == null) {
            appIcon = (ImageView) findViewById(android.support.v7.appcompat.R.id.home);
        }
        if (appIcon == null) {
            return;
        }
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
