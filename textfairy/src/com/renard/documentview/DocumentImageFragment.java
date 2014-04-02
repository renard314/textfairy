package com.renard.documentview;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

import com.renard.ocr.R;
import com.renard.util.PreferencesUtils;
import com.renard.util.Util;

/**
 * Created by renard on 02/04/14.
 */
public class DocumentImageFragment extends Fragment {

    private ImageView mImageView;
    private ViewSwitcher mViewSwitcher;

    public static DocumentImageFragment newInstance(final String imagePath) {
        DocumentImageFragment f = new DocumentImageFragment();
        // Supply text input as an argument.
        Bundle args = new Bundle();
        args.putString("image_path", imagePath);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final String imagePath= getArguments().getString("image_path");
        View view = inflater.inflate(R.layout.fragment_document_image, container, false);
        mImageView = (ImageView) view.findViewById(R.id.imageView);
        //todo use async loading and https://github.com/chrisbanes/PhotoView
        final Bitmap bitmap = Util.decodeFile(imagePath, 1024, 1024);
        mImageView.setImageBitmap(bitmap);
        mViewSwitcher = (ViewSwitcher) view.findViewById(R.id.viewSwitcher);
        mViewSwitcher.setDisplayedChild(1);
        return view;
    }
}
