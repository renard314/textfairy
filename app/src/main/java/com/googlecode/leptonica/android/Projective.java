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

package com.googlecode.leptonica.android;

/**
 * @author renard
 */
public class Projective {
    static {
        System.loadLibrary("pngo");
        System.loadLibrary("lept");
    }



    /**
     *  pixProjectivePtaColor()
     *
     *      Input:  pixs (32 bpp)
     *              ptad  (4 pts of final coordinate space)
     *              ptas  (4 pts of initial coordinate space)
     *
     *      Return: pixd, or null on error
     *
     **/
    public static Pix projectiveTransform(Pix pixs, float[] dest, float[] src) {
        if (pixs == null) {
            throw new IllegalArgumentException("Source pix must be non-null");
        }
        final long result = nativeProjectivePtaGray(pixs.getNativePix(), dest, src);
        if(result!=0){
            return new Pix(result);
        } else {
            return null;
        }
    }


    // ***************
    // * NATIVE CODE *
    // ***************
    private static native long nativeProjectivePtaGray(long pix , float[] dest, float[] src);
}
