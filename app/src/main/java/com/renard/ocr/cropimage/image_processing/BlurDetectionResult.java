/*
 * Copyright (C) 2015 Renard Wellnitz.
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
package com.renard.ocr.cropimage.image_processing;

import com.googlecode.leptonica.android.Box;
import com.googlecode.leptonica.android.Pix;

/**
 * Created by renard.
 */
public class BlurDetectionResult {


    public enum Blurriness {
        NOT_BLURRED, MEDIUM_BLUR, STRONG_BLUR;
    }

    private boolean mDestroyed;
    private final Pix mPixBlur;
    private final double mBlurValue;
    private final Box mMostBlurredRegion;


    public BlurDetectionResult(long blurPixPointer, double mBlurValue, long blurRegionPointer) {
        this.mPixBlur = new Pix(blurPixPointer);
        this.mBlurValue = mBlurValue;
        this.mMostBlurredRegion = new Box(blurRegionPointer);
    }

    /**
     * Pix with overlay showing the extend of blurriness.
     */
    public Pix getPixBlur() {
        checkDestroyed();
        return mPixBlur;
    }

    private void checkDestroyed() {
        if (mDestroyed) {
            throw new IllegalStateException("BlurDetectionResult has been destroyed");
        }
    }

    /**
     * Value indicating overall pix blurriness. 0->very blurred, anything greater 0.35 is
     * sharp.
     */
    public double getBlurValue() {
        return mBlurValue;
    }

    public Blurriness getBlurriness() {
        checkDestroyed();
        if (mBlurValue < 0.5) {
            return Blurriness.NOT_BLURRED;
        } else if (mBlurValue < 0.67) {
            return Blurriness.MEDIUM_BLUR;
        } else {
            return Blurriness.STRONG_BLUR;
        }
    }

    public void destroy() {
        checkDestroyed();
        mDestroyed = true;
        mPixBlur.recycle();
        mMostBlurredRegion.recycle();
    }

    /**
     * Bounding box of the most blurry region.
     */
    public Box getMostBlurredRegion() {
        checkDestroyed();
        return mMostBlurredRegion;
    }
}
