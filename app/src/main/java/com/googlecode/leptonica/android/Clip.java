package com.googlecode.leptonica.android;

public class Clip {
    static {
        System.loadLibrary("pngo");
        System.loadLibrary("lept");
    }

    /**
     * box is not clipped if it is outside of the pix dimensions
     * @param source
     * @param box
     * @return
     */
    public static Pix clipRectangle2(Pix source, Box box){
        int result = nativeClipRectangle2(source.getNativePix(), box.getNativeBox());
        if (result!=0) {
            return new Pix(result);
        }
        return null;
    }


    public static Pix clipRectangle(Pix source, Box box){
    	int result = nativeClipRectangle(source.getNativePix(), box.getNativeBox());
    	if (result!=0) {
    		return new Pix(result);
    	}
		return null;
    }
    
    private static native int nativeClipRectangle(long nativePix, long nativeBox);
    private static native int nativeClipRectangle2(long nativePix, long nativeBox);

}
