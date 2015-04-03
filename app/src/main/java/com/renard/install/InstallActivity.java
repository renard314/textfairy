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

package com.renard.install;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.renard.install.InstallActivity.InstallResult.Result;
import com.renard.ocr.R;
import com.renard.util.Util;

import java.io.File;
import java.util.concurrent.ExecutionException;

/**
 * wrapper activity for the AssetsManager
 */
public class InstallActivity extends Activity {
    public static class InstallResult {
        public enum Result {
            NOT_ENOUGH_DISK_SPACE, OK, UNSPEZIFIED_ERROR;
        }

        private long mNeededSpace;
        private long mFreeSpace;
        private Result mResult;

        public InstallResult(Result result) {
            mResult = result;
        }

        public InstallResult(Result result, long needed, long free) {
            mResult = result;
            mFreeSpace = free;
            mNeededSpace = needed;
        }

        public Result getResult() {
            return mResult;
        }

        public boolean isSuccessful() {
            return mResult == Result.OK;
        }

        public long getNeededSpace() {
            return mNeededSpace;
        }

        public long getFreeSpace() {
            return mFreeSpace;
        }
    }

    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = InstallActivity.class.getSimpleName();
    final static String TESSDATA_FILE_NAME = "tessdata.zip";

    private ProgressBar mProgressBar = null;
    private TextView mTextViewSpeechBubble = null;
    private ImageView mImageViewFairy;
    private View mFairyContainer = null;
    private AnimationDrawable mFairyAnimation;
    private TextView mButtonStartApp = null;
    private ViewSwitcher mSwitcher = null;
    private View mContentView = null;

    private InstallTask mTask = null;

    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        mTask = (InstallTask) getLastNonConfigurationInstance();

        mTextViewPromoLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                watchYoutubeVideo(getString(R.string.youtube_promo_link));
            }
        });

        if (mTask == null) {
            mTask = new InstallTask(this);
            mTask.execute();
        } else {
            mTask.attach(this);
            updateProgress(mTask.getProgress());
            if (mTask.getProgress() >= 100) {
                InstallResult result;
                try {
                    result = mTask.get();
                } catch (InterruptedException e) {
                    result = new InstallResult(Result.UNSPEZIFIED_ERROR);
                } catch (ExecutionException e) {
                    result = new InstallResult(Result.UNSPEZIFIED_ERROR);
                }
                markAsDone(result);
            }
        }

        mImageViewFairy.post(new Runnable() {

            @Override
            public void run() {
                mFairyAnimation.start();
            }
        });
    }

    public void watchYoutubeVideo(String id) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(id));
        startActivity(intent);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        mTask.detach();
        return (mTask);
    }

    /**
     * @return if the language assets are installed or not
     */
    public static boolean IsInstalled(Context appContext) {

        // check directories
        File tessDir = Util.getTrainingDataDir(appContext);

        if (!tessDir.exists()) {
            return false;
        }

        return true;
    }

    /**
     * @return the total size of the language-assets in the zip file
     */
    static long getTotalUnzippedSize(AssetManager manager) {
        /*
         * long ret = 0; FileInputStream in = null; try { AssetFileDescriptor fd
		 * = manager.openFd(TESSDATA_FILE_NAME); ret = fd.getLength(); if (ret
		 * == AssetFileDescriptor.UNKNOWN_LENGTH) { in = fd.createInputStream();
		 * ret = 0; byte[] buffer = new byte[1024]; int bytesRead = 0; while
		 * ((bytesRead = in.read(buffer)) != -1) { ret += bytesRead; } } } catch
		 * (IOException ioe) { Log.v(DEBUG_TAG, "exception:" + ioe.toString());
		 * return 0; } finally { if (in != null) { try { in.close(); } catch
		 * (IOException ignore) { } } } return ret;
		 */
        // return 5374633;
        return 24320801;
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
            case UNSPEZIFIED_ERROR:
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

    public void updateProgress(int progress) {
        final int fairyEndX = mContentView.getWidth() / 2;
        final int fairyStartX = mImageViewFairy.getWidth() / 2;
        final int maxTravelDistance = Math.min(fairyEndX - fairyStartX, mContentView.getWidth() - mFairyContainer.getWidth());
        final float translateX = maxTravelDistance * ((float) progress / 100);
        RelativeLayout.LayoutParams lp = (android.widget.RelativeLayout.LayoutParams) mFairyContainer.getLayoutParams();
        lp.leftMargin = (int) translateX;
        mFairyContainer.setLayoutParams(lp);
        mProgressBar.setProgress(progress);
    }

}
