package com.renard.ocr.documents.viewing.single;

import com.renard.ocr.R;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
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
    public void onDestroyView() {
        super.onDestroyView();
        Picasso.get().cancelRequest(mImageView);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final String imagePath = getArguments().getString(EXTRA_IMAGE_PATH);
        View view = inflater.inflate(R.layout.fragment_document_image, container, false);
        if (imagePath == null) {
            showError();
            return view;
        }

        mImageView = (ImageView) view.findViewById(R.id.imageView);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progress);
        final File file = new File(imagePath);
        Picasso.get().load(file).fit().centerInside().into(mImageView, new Callback() {
            @Override
            public void onSuccess() {
                mProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onError(Exception e) {
                mProgressBar.setVisibility(View.GONE);
                showError();

            }
        });
        return view;

    }

    private void showError() {
        Toast.makeText(getActivity(), R.string.image_does_not_exist, Toast.LENGTH_LONG).show();
    }


}
