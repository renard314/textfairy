package com.renard.ocr.documents.creation.crop;

import com.googlecode.leptonica.android.Pix;
import com.renard.ocr.BuildConfig;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import android.util.Log;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)

public class CropImageScalerTest {

    static {
        System.loadLibrary("lept");
    }


    private static final String LOG_TAG = CropImageScalerTest.class.getSimpleName();

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
    }

    @Test
    public void testScale() throws Exception {

        CropImageScaler scaler = new CropImageScaler();
        //2448 × 3264
        Pix pix = new Pix(2448,3264,32);
        //w = 768
        //h = 1038
        final CropImageScaler.ScaleResult scale = scaler.scale(pix, 768, 1038);
        Log.d(LOG_TAG,"scale factor = " + scale.getScaleFactor() + "; w/h = ("+scale.getPix().getWidth()+","+scale.getPix().getHeight()+")");
        Assert.assertEquals(scale.getPix().getHeight(), 100);
        Assert.assertEquals(scale.getPix().getWidth(), 100);
        pix.recycle();
        scale.getPix().recycle();

    }
}