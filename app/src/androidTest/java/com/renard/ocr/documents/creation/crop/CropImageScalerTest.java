package com.renard.ocr.documents.creation.crop;

import com.googlecode.leptonica.android.Pix;
import com.renard.ocr.BuildConfig;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

import android.test.AndroidTestCase;
import android.util.Log;

public class CropImageScalerTest extends AndroidTestCase {
    private static final String LOG_TAG = CropImageScalerTest.class.getSimpleName();


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        //Workaround for https://code.google.com/p/dexmaker/issues/detail?id=2
        System.setProperty("dexmaker.dexcache", "/data/data/" + BuildConfig.APPLICATION_ID + ".test/cache");
    }

    static {
        System.loadLibrary("lept");
    }

    @Test
    @Ignore
    public void testScale() throws Exception {

        CropImageScaler scaler = new CropImageScaler();
        //2448 × 3264
        Pix pix = new Pix(2448, 3264, 32);
        //w = 768
        //h = 1038
        final CropImageScaler.ScaleResult scale = scaler.scale(pix, 768, 1038);
        Log.d(LOG_TAG, "scale factor = " + scale.getScaleFactor() + "; w/h = (" + scale.getPix().getWidth() + "," + scale.getPix().getHeight() + ")");
        Assert.assertTrue(scale.getPix().getHeight() <= 1038);
        Assert.assertTrue(scale.getPix().getWidth() <= 768);
        pix.recycle();
        scale.getPix().recycle();

    }


}