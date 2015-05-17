/*
 * Copyright (C) 2010 Google Inc.
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

package com.renard.image_processing;

import com.googlecode.leptonica.android.Pix;

/**
 * @author renard
 */
public class Blur {
    static {
        System.loadLibrary("lept");
        System.loadLibrary("image_processing_jni");
    }


    public static Pix blurDetext(Pix pixs) {
        if (pixs == null) {
            throw new IllegalArgumentException("Source pix must be non-null");
        }
        final long result = nativeBlurDetect(pixs.getNativePix());
        if(result!=0){
            return new Pix(result);
        } else {
            return null;
        }
    }


    // ***************
    // * NATIVE CODE *
    // ***************
    private static native long nativeBlurDetect(long pix );
}
