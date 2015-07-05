package com.renard.documentview;

import com.renard.ocr.R;
import com.renard.util.Util;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

/**
 * Created by renard on 02/04/14.
 */
public class DocumentImageFragment extends Fragment {

    private ImageView mImageView;
    private ViewSwitcher mViewSwitcher;
    private LoadImageAsyncTask mImageTask;

    public static DocumentImageFragment newInstance(final String imagePath) {
        DocumentImageFragment f = new DocumentImageFragment();
        // Supply text input as an argument.
        Bundle args = new Bundle();
        args.putString("image_path", imagePath);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mImageTask != null) {
            mImageTask.cancel(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final String imagePath = getArguments().getString("image_path");
        View view = inflater.inflate(R.layout.fragment_document_image, container, false);
        mImageView = (ImageView) view.findViewById(R.id.imageView);
        mViewSwitcher = (ViewSwitcher) view.findViewById(R.id.viewSwitcher);
        //use async loading

        mImageView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {

                    @Override
                    @TargetApi(16)
                    public void onGlobalLayout() {
                        if (mImageTask != null) {
                            mImageTask.cancel(true);
                        }
                        mImageTask = new LoadImageAsyncTask(mImageView, mViewSwitcher);
                        mImageTask.execute(imagePath);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            mImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        } else {
                            mImageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        }
                    }

                });

        return view;
    }

    private static class LoadImageAsyncTask extends AsyncTask<String, Void, Bitmap> {


        private final ImageView mImageView;
        private final ViewSwitcher mViewSwitcher;
        private final int mWidth, mHeight;

        private LoadImageAsyncTask(final ImageView imageView, ViewSwitcher viewSwitcher) {
            mImageView = imageView;
            mViewSwitcher = viewSwitcher;
            mHeight = mViewSwitcher.getHeight();
            mWidth = mViewSwitcher.getWidth();
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            final String imagePath = params[0];
            return Util.decodeFile(imagePath, mWidth, mHeight);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mViewSwitcher.setDisplayedChild(0);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            mImageView.setImageBitmap(bitmap);
            mViewSwitcher.setDisplayedChild(1);
        }
    }

}
