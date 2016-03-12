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

package com.renard.ocr.documents.viewing.grid;

import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.renard.ocr.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/**
 * custom view for children of the document grid view TODO remove tight
 * coupling with DocumentGridActivity
 *
 * @author renard
 */
public class CheckableGridElement extends RelativeLayout implements Checkable {

    private boolean mIsChecked = false;
    private OnCheckedChangeListener mListener;
    private ImageView mThumbnailImageView;
    private final Transformation mSelectionTransformation;
    private final Transformation mAlphaTransformation;
    private AnimatorSet mAnimatorSet;

    private float mTargetAlpha = 1;
    private float mCurrentAlpha = 1;

    private float mTargetScale = 1;
    private float mCurrentScale = 1;

    private static final float SELECTED_ALPHA = 1.0f;
    private static final float NOT_SELECTED_ALPHA = .35f;

    private static final float SELECTED_SCALE = 1.0f;
    private static final float NOT_SELECTED_SCALE = 0.95f;

    private final static int ANIMATION_DURATION = 200;
    private final static long TAP_ANIMATION_DURATION = ViewConfiguration.getLongPressTimeout();

    @SuppressWarnings("unused")
    private final String LOG_TAG = CheckableGridElement.class.getSimpleName();

    public interface OnCheckedChangeListener {
        void onCheckedChanged(View documentView, boolean isChecked);
    }

    public CheckableGridElement(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setWillNotDraw(false);
        if (!isInEditMode()) {
            setStaticTransformationsEnabled(true);
            mSelectionTransformation = new Transformation();
            mAlphaTransformation = new Transformation();
            mSelectionTransformation.setTransformationType(Transformation.TYPE_MATRIX);
            mAlphaTransformation.setTransformationType(Transformation.TYPE_ALPHA);
        } else {
            mAlphaTransformation = null;
            mSelectionTransformation = null;
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mThumbnailImageView = (ImageView) findViewById(R.id.thumb);
        this.setFocusable(false);
    }

    public void setImage(Drawable d) {
        mThumbnailImageView.setImageDrawable(d);
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        mListener = listener;
    }

    /**
     * sets checked state without starting an animation
     */
    public void setCheckedNoAnimate(final boolean checked) {
        mIsChecked = checked;
        // stop any running animation
        if (mAnimatorSet != null && mAnimatorSet.isRunning()) {
            mAnimatorSet.removeAllListeners();
            mAnimatorSet.cancel();
        }
        setCurrentAlpha(getDesiredAlpha());
        setCurrentScale(getDesiredScale());
        if (mListener != null) {
            mListener.onCheckedChanged(this, mIsChecked);
        }

    }

    /**
     * sets checked state and starts animation if necessary
     */
    @Override
    public void setChecked(boolean checked) {
        mIsChecked = checked;

        initAnimationProperties(AnimationType.CHECK);

        if (mListener != null) {
            mListener.onCheckedChanged(this, mIsChecked);
        }
    }

    @Override
    public boolean isChecked() {
        return mIsChecked;
    }

    private float getDesiredScale() {
        if (mIsChecked && DocumentGridActivity.isInSelectionMode()) {
            return SELECTED_SCALE;
        } else if (!mIsChecked && DocumentGridActivity.isInSelectionMode()) {
            return NOT_SELECTED_SCALE;
        } else {
            return SELECTED_SCALE;
        }
    }

    private float getDesiredAlpha() {
        if (mIsChecked && DocumentGridActivity.isInSelectionMode()) {
            return SELECTED_ALPHA;
        } else if (!mIsChecked && DocumentGridActivity.isInSelectionMode()) {
            return NOT_SELECTED_ALPHA;
        } else {
            return SELECTED_ALPHA;
        }
    }

    private enum AnimationType {
        TAP_UP, TAP_DOWN, CHECK
    }

    private void initAnimationProperties(final AnimationType type) {
        final long duration;
        switch (type) {

            case CHECK: {
                duration = ANIMATION_DURATION;
                mTargetAlpha = getDesiredAlpha();
                mTargetScale = getDesiredScale();
                break;
            }
            case TAP_DOWN: {
                duration = TAP_ANIMATION_DURATION;
                mTargetScale = NOT_SELECTED_SCALE;
                break;
            }
            case TAP_UP: {
                duration = ANIMATION_DURATION;
                mTargetScale = SELECTED_SCALE;
                break;
            }
            default:
                duration = ANIMATION_DURATION;
        }

        startAnimation(duration);
    }

    private void setCurrentScale(final float value) {
        mCurrentScale = value;
        getChildAt(0).invalidate();
    }

    private void setCurrentAlpha(final float value) {
        mCurrentAlpha = value;
    }

    private void startAnimation(final long anmationDuration) {
        final long duration = (long) (anmationDuration * (Math.abs(mCurrentScale - mTargetScale) / (SELECTED_SCALE - NOT_SELECTED_SCALE)));

        ObjectAnimator scale = ObjectAnimator.ofFloat(this, "currentScale", mCurrentScale, mTargetScale);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(this, "currentAlpha", mCurrentAlpha, mTargetAlpha);
        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.setDuration(duration);
        mAnimatorSet.setInterpolator(new DecelerateInterpolator(1.2f));
        mAnimatorSet.playTogether(scale, alpha);
        mAnimatorSet.start();
    }

    @Override
    public void toggle() {
        mIsChecked = !mIsChecked;
        initAnimationProperties(AnimationType.CHECK);
        if (mListener != null) {
            mListener.onCheckedChanged(this, mIsChecked);
        }
    }

    public void startTouchDownAnimation() {
        if (!DocumentGridActivity.isInSelectionMode()) {
            initAnimationProperties(AnimationType.TAP_DOWN);
        }
    }

    public void startTouchUpAnimation() {
        if (!DocumentGridActivity.isInSelectionMode()) {
            initAnimationProperties(AnimationType.TAP_UP);
        }
    }

    @Override
    protected boolean getChildStaticTransformation(View child, Transformation t) {

        boolean hasChanged = false;
        if (mTargetAlpha != mCurrentAlpha || mTargetAlpha != SELECTED_ALPHA) {
            mAlphaTransformation.setAlpha(mCurrentAlpha);
            t.compose(mAlphaTransformation);
            hasChanged = true;
        }
        if (mTargetScale != mCurrentScale || mTargetScale != SELECTED_SCALE) {
            mSelectionTransformation.getMatrix().reset();
            final float px = child.getLeft() + (child.getWidth()) / 2;
            final float py = child.getTop() + (child.getHeight()) / 2;
            mSelectionTransformation.getMatrix().postScale(mCurrentScale, mCurrentScale, px, py);
            t.compose(mSelectionTransformation);
            hasChanged = true;
        }
        if (hasChanged) {
            child.invalidate();
            this.invalidate();
        }
        return hasChanged;
    }

}
