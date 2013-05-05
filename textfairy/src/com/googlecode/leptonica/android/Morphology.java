
package com.googlecode.leptonica.android;

public class Morphology {

    static {
		System.loadLibrary("gnustl_shared");		
        System.loadLibrary("lept");
    }

    public static Pix tophat(Pix pixs) {
        if (pixs == null)
            throw new IllegalArgumentException("Source pix must be non-null");

        int nativePix = nativeTophat(pixs.mNativePix);

        if (nativePix == 0)
            throw new IllegalStateException("Failed to perform tophat on image");

        return new Pix(nativePix);
    }

    // ***************
    // * NATIVE CODE *
    // ***************

    private static native int nativeTophat(int nativePix);
}
