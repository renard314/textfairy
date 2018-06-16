package com.googlecode.leptonica.android;

public class Clip {

    /**
     * box is not clipped if it is outside of the pix dimensions
     */
    public static Pix clipRectangle2(Pix source, Box box) {
        long result = nativeClipRectangle2(source.getNativePix(), box.getNativeBox());
        if (result != 0) {
            return new Pix(result);
        }
        return null;
    }


    public static Pix clipRectangle(Pix source, Box box) {
        long result = nativeClipRectangle(source.getNativePix(), box.getNativeBox());
        if (result != 0) {
            return new Pix(result);
        }
        return null;
    }

    private static native long nativeClipRectangle(long nativePix, long nativeBox);

    private static native long nativeClipRectangle2(long nativePix, long nativeBox);

}
