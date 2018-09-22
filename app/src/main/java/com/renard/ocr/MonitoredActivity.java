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

package com.renard.ocr;

import com.renard.ocr.analytics.Analytics;
import com.renard.ocr.analytics.CrashLogger;
import com.renard.ocr.documents.creation.crop.BaseActivityInterface;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import de.greenrobot.event.EventBus;


public abstract class MonitoredActivity extends AppCompatActivity implements BaseActivityInterface, OnGlobalLayoutListener {

    private static final String LOG_TAG = MonitoredActivity.class.getSimpleName();
    static final int MY_PERMISSIONS_REQUEST = 232;

    private final ArrayList<LifeCycleListener> mListeners = new ArrayList<>();
    private int mDialogId = -1;
    private final Handler mHandler = new Handler();
    private ImageView mAppIcon = null;
    private TextView mToolbarMessage;
    private AlertDialog mPermissionDialog;
    protected Analytics mAnalytics;
    protected  CrashLogger mCrashLogger;

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
    protected synchronized void onPause() {
        super.onPause();
        final ArrayList<LifeCycleListener> lifeCycleListeners = copyListeners();
        for (LifeCycleListener listener : lifeCycleListeners) {
            listener.onActivityPaused(this);
        }
    }

    private ArrayList<LifeCycleListener> copyListeners() {
        final ArrayList<LifeCycleListener> lifeCycleListeners = new ArrayList<>(mListeners.size());
        lifeCycleListeners.addAll(mListeners);
        return lifeCycleListeners;
    }

    public abstract String getScreenName();

    @Override
    protected synchronized void onResume() {
        super.onResume();
        for (LifeCycleListener listener : mListeners) {
            listener.onActivityResumed(this);
        }
        final String screenName = getScreenName();
        if (!TextUtils.isEmpty(screenName)) {
            mAnalytics.sendScreenView(screenName);
        }
    }

    @Override
    protected synchronized void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ArrayList<LifeCycleListener> lifeCycleListeners = copyListeners();

        for (LifeCycleListener listener : lifeCycleListeners) {
            listener.onActivityCreated(this);
        }
        TextFairyApplication application = (TextFairyApplication) getApplication();
        mAnalytics = application.getAnalytics();
        mCrashLogger = application.getCrashLogger();

        Log.i(LOG_TAG, "onCreate: " + this.getClass());
    }

    public Analytics getAnaLytics() {
        return mAnalytics;
    }

    public CrashLogger getCrashLogger() {
        return mCrashLogger;
    }

    protected void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbarMessage = (TextView) toolbar.findViewById(R.id.toolbar_text);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        initAppIcon(getHintDialogId(), (ImageView) toolbar.findViewById(R.id.app_icon));
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
        if (mPermissionDialog != null) {
            mPermissionDialog.cancel();
        }
        final ArrayList<LifeCycleListener> lifeCycleListeners = copyListeners();
        for (LifeCycleListener listener : lifeCycleListeners) {
            listener.onActivityDestroyed(this);
        }
    }

    @Override
    protected synchronized void onStart() {
        super.onStart();
        final ArrayList<LifeCycleListener> lifeCycleListeners = copyListeners();
        for (LifeCycleListener listener : lifeCycleListeners) {
            listener.onActivityStarted(this);
        }
    }

    @Override
    protected synchronized  void onStop() {
        super.onStop();
        final ArrayList<LifeCycleListener> lifeCycleListeners = copyListeners();
        for (LifeCycleListener listener : lifeCycleListeners) {
            listener.onActivityStopped(this);
        }
        Log.i(LOG_TAG, "onStop: " + this.getClass());
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
            final int delayMillis = (int) ((Math.random() * 15 + 15) * 1000);
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
            IconAnimationRunnable runnable = new IconAnimationRunnable(animation, mHandler);
            mHandler.removeCallbacksAndMessages(null);
            mHandler.post(runnable);
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

    public void ensurePermission(String permission, @StringRes int explanation) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                explainPermission(permission, explanation);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{permission}, MY_PERMISSIONS_REQUEST);

            }
        } else {
            EventBus.getDefault().post(new PermissionGrantedEvent(permission));
        }
    }


    private void explainPermission(final String permission, int explanation) {
        //PermissionExplanationDialog.newInstance(R.string.permission_explanation_title, explanation, permission);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(explanation);
        builder.setTitle(R.string.permission_explanation_title);
        builder.setNegativeButton(R.string.close_app, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ActivityCompat.requestPermissions(MonitoredActivity.this, new String[]{permission}, MY_PERMISSIONS_REQUEST);

            }
        });
        mPermissionDialog = builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    EventBus.getDefault().post(new PermissionGrantedEvent(permissions[0]));
                } else {
                    finish();
                }
            }
        }
    }

}
