package com.renard.ocr.cropimage;

import com.googlecode.leptonica.android.Pix;

import android.os.AsyncTask;

/**
 * Created by renard on 23/05/15.
 */
public class CheckImageAsyncTask extends AsyncTask<Void,Void, Void> {
    private final Pix mPix;

    CheckImageAsyncTask(Pix pix){
        mPix = pix;
    }

    @Override
    protected Void doInBackground(Void... params) {
        return null;
    }
}
