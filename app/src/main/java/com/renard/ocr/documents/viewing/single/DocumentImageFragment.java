package com.renard.ocr.documents.viewing.single;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.renard.ocr.R;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;

/**
 * @author renard
 */
public class DocumentImageFragment extends Fragment {

    public static final String EXTRA_IMAGE_PATH = "image_path";
    private ImageView mImageView;
    private ProgressBar mProgressBar;

    public static DocumentImageFragment newInstance(final String imagePath) {
        DocumentImageFragment f = new DocumentImageFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_IMAGE_PATH, imagePath);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final String imagePath = getArguments().getString(EXTRA_IMAGE_PATH);
        View view = inflater.inflate(R.layout.fragment_document_image, container, false);
        if (imagePath == null) {
            showError();
            return view;
        }

        mImageView = view.findViewById(R.id.imageView);
        mProgressBar = view.findViewById(R.id.progress);
        Glide.with(mImageView)
                .load(new File(imagePath))
                .apply(RequestOptions.centerInsideTransform())
                .addListener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        mProgressBar.setVisibility(View.GONE);
                        showError();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        mProgressBar.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(mImageView);
        return view;

    }

    private void showError() {
        Toast.makeText(getActivity(), R.string.image_does_not_exist, Toast.LENGTH_LONG).show();
    }


}
