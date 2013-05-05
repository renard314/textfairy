package com.renard.pdf;



public class Hocr2Pdf {
    static {
		System.loadLibrary("gnustl_shared");		
        System.loadLibrary("hocr2pdf");
		System.loadLibrary("hocr2pdfjni");
    }
    
    private PDFProgressListener mListener;
    
    public static interface PDFProgressListener{
    	void onNewPage(int pageNumber);
    }
    
    public Hocr2Pdf(PDFProgressListener listener) {
    	mListener = listener;
    }

    public void hocr2pdf(String[] images, String[] hocr, String pdfFileName, boolean sloppy, boolean overlayImage) {
        nativeHocr2pdf(images, hocr, pdfFileName, sloppy, overlayImage);
    }
    
    private void onProgress(int pageNumber) {
    	if (mListener!=null){
    		mListener.onNewPage(pageNumber);
    	}
    }
    
    private native void nativeHocr2pdf( String[] images, String[] hocr, String pdfFileName, boolean sloppy, boolean overlayImage);
}
