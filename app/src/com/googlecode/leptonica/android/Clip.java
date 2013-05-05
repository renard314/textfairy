package com.googlecode.leptonica.android;

public class Clip {
    static {
		System.loadLibrary("gnustl_shared");		
        System.loadLibrary("lept");
    }

    public static Pix clipRectangle(Pix source, Box box){
    	int result = nativeClipRectangle(source.getNativePix(), box.mNativeBox);
    	if (result!=0) {
    		return new Pix(result);
    	}
		return null;
    }
    
    private static native int nativeClipRectangle(int nativePix, int nativeBox);

}
