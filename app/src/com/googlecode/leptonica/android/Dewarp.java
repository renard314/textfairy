package com.googlecode.leptonica.android;


public class Dewarp {
    static {
		System.loadLibrary("gnustl_shared");		
        System.loadLibrary("lept");
    }

    public static Pix dewarp(Pix pixs) {
        if (pixs == null)
            throw new IllegalArgumentException("Source pix must be non-null");

        int nativePix = nativeDewarp(pixs.mNativePix);

        if (nativePix == 0) {
            return null;
        }

        return new Pix(nativePix);
    }

    // ***************
    // * NATIVE CODE *
    // ***************

    private static native int nativeDewarp(int nativePix);
}


