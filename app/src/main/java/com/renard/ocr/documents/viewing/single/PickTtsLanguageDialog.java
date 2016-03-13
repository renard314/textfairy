package com.renard.ocr.documents.viewing.single;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import com.renard.ocr.R;
import com.renard.ocr.main_menu.language.OcrLanguage;
import com.renard.ocr.main_menu.language.OCRLanguageAdapter;

import java.util.ArrayList;

/**
 * Created by renard on 12/11/13.
 */
public class PickTtsLanguageDialog extends DialogFragment {

    public static final String TAG = PickTtsLanguageDialog.class.getSimpleName();
    private final static String ARG_DOCUMENT_LANGUAGE = "language";
    private final static String ARG_LANGUAGES = "languages";
    private final static String ARG_LANGUAGE_SUPPORTED = "language_supported";


    public static PickTtsLanguageDialog newInstance(String documentLanguage, boolean languageSupported, Context c) {
        Bundle arguments = new Bundle();
        arguments.putString(ARG_DOCUMENT_LANGUAGE, documentLanguage);
        arguments.putBoolean(ARG_LANGUAGE_SUPPORTED, languageSupported);
        final ArrayList<OcrLanguage> languages = getLanguages(c);
        arguments.putParcelableArrayList(ARG_LANGUAGES, languages);
        final PickTtsLanguageDialog dialog = new PickTtsLanguageDialog();
        dialog.setArguments(arguments);
        return dialog;
    }

    public static PickTtsLanguageDialog newInstance(Context c) {
        Bundle arguments = new Bundle();
        final ArrayList<OcrLanguage> languages = getLanguages(c);
        arguments.putParcelableArrayList(ARG_LANGUAGES, languages);
        final PickTtsLanguageDialog dialog = new PickTtsLanguageDialog();
        dialog.setArguments(arguments);
        return dialog;
    }



    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
        builder.setCancelable(true);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final DocumentActivity activity = (DocumentActivity) getActivity();
                activity.onTtsCancelled();

            }
        });
        final String language = getArguments().getString(ARG_DOCUMENT_LANGUAGE);
        ArrayList<OcrLanguage> languages = getArguments().getParcelableArrayList(ARG_LANGUAGES);
        String displayLanguage = null;
        for (OcrLanguage lang : languages) {
            if (lang.getValue().equalsIgnoreCase(language)) {
                displayLanguage = lang.getDisplayText();
                break;
            }
        }
        if (displayLanguage != null) {
            if (getArguments().getBoolean(ARG_LANGUAGE_SUPPORTED)) {
                builder.setTitle(getString(R.string.choose_language));
            } else {
                builder.setTitle(getString(R.string.cannot_speak_language, displayLanguage));
            }
        } else {
            builder.setTitle(getString(R.string.choose_language));
        }
        final OCRLanguageAdapter adapter = new OCRLanguageAdapter(getActivity(), true);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final DocumentActivity activity = (DocumentActivity) getActivity();
                OcrLanguage lang = (OcrLanguage) adapter.getItem(which);
                activity.onTtsLanguageChosen(lang);
            }
        });

        final AlertDialog alertDialog = builder.create();
        final LayoutAnimationController layoutAnimationController = AnimationUtils.loadLayoutAnimation(getActivity(), R.anim.layout_animation);
        alertDialog.getListView().setLayoutAnimation(layoutAnimationController);
        fillAdapterWithAllowedTtsLanguages(adapter, languages);
        alertDialog.getWindow().getAttributes().height = WindowManager.LayoutParams.MATCH_PARENT;
        return alertDialog;
    }

    private static ArrayList<OcrLanguage> getLanguages(Context c) {
        final String[] languageValues = c.getResources().getStringArray(R.array.ocr_languages);
        final ArrayList<OcrLanguage> languages = new ArrayList<OcrLanguage>();

        for (String val : languageValues) {
            final int firstSpace = val.indexOf(' ');
            final String value = val.substring(0, firstSpace);
            final String displayText = val.substring(firstSpace + 1, val.length());
            languages.add(new OcrLanguage(value, displayText, false, 0));
        }
        return languages;
    }

    private void fillAdapterWithAllowedTtsLanguages(final OCRLanguageAdapter adapter, ArrayList<OcrLanguage> allLanguages) {
        new AsyncTask<ArrayList<OcrLanguage>, Void, ArrayList<OcrLanguage>>() {

            @Override
            protected ArrayList<OcrLanguage> doInBackground(ArrayList<OcrLanguage>... params) {
                ArrayList<OcrLanguage> resultList = new ArrayList<>();
                for (final OcrLanguage lang : params[0]) {
                    DocumentActivity activity = (DocumentActivity) getActivity();
                    if (activity != null) {
                        boolean available = activity.isTtsLanguageAvailable(lang);
                        if (available) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.add(lang);
                                }
                            });
                            resultList.add(lang);
                        }
                    }
                }
                return resultList;
            }
        }.execute(allLanguages);
    }


}