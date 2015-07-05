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

import com.renard.ocr.R;

import android.annotation.TargetApi;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.ArrayList;


public class MonitoredActivity extends ActionBarActivity implements BaseActivityInterface {

    private final ArrayList<LifeCycleListener> mListeners = new ArrayList<LifeCycleListener>();
    private int mDialogId = -1;

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

    /**
     * position the app icon at the bottom of the action bar and start animation
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void initAppIcon(final BaseActivityInterface activity, final int dialogId) {
        activity.setDialogId(dialogId);
        // ActionBar bar = getSupportActionBar();
        // final ImageView appIcon = (ImageView) findViewById(16908332);
        // final ImageView appIcon1 = (ImageView) findViewById(16908858);
        // if (appIcon!=null){
        // appIcon.setVisibility(View.INVISIBLE);
        // }

        ImageView nativeAppIcon = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            nativeAppIcon = (ImageView) activity.findViewById(android.R.id.home);
        }
        if (nativeAppIcon == null) {
            nativeAppIcon = (ImageView) activity.findViewById(android.support.v7.appcompat.R.id.home);
        }
        ImageView sherlockAppIcon = null;//(ImageView) activity.findViewById(com.actionbarsherlock.R.id.abs__home);
        final ImageView appIcon = nativeAppIcon != null ? nativeAppIcon : sherlockAppIcon;
        if (appIcon == null) {
            return;
        }
        ViewTreeObserver viewTreeObserver = appIcon.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    FrameLayout.LayoutParams llp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                    appIcon.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    final int iconHeight = appIcon.getHeight() + 1;
                    final int barHeight = ((FrameLayout) appIcon.getParent()).getHeight();
                    int leftMargin = dialogId == -1 ? 0 : (int) appIcon.getContext().getResources().getDimension(R.dimen.home_icon_margin);
                    llp.setMargins(leftMargin, barHeight - iconHeight, 0, 0);
                    appIcon.setLayoutParams(llp);
                    appIcon.invalidate();

                    // start fairy animation at random intervalls
                    if (appIcon.getDrawable() instanceof AnimationDrawable) {
                        final AnimationDrawable animation = (AnimationDrawable) appIcon.getDrawable();
                        final Runnable runnable = new Runnable() {

                            @Override
                            public void run() {
                                animation.setVisible(false, true);
                                animation.start();
                                final int delayMillis = (int) ((Math.random() * 5 + 5) * 1000);
                                appIcon.postDelayed(this, delayMillis);

                            }
                        };
                        appIcon.post(runnable);
                    }
                    if (dialogId != -1) {
                        // show hint dialog when user clicks on the app icon
                        appIcon.setOnClickListener(new OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                activity.showDialog(dialogId);
                            }
                        });
                    }
                }
            });

        }
    }

    @Override
    public void setDialogId(int dialogId) {
        mDialogId = dialogId;
    }

}
