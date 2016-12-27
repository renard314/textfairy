package com.renard.ocr.documents.viewing.single.tts;

import com.renard.ocr.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import de.greenrobot.event.EventBus;

/**
 * Allows user to pick a language for text to speech
 */
public class PickTtsLanguageDialog extends DialogFragment {

    public static final String TAG = PickTtsLanguageDialog.class.getSimpleName();
    private final static String ARG_LANGUAGES = "languages";
    private final static String ARG_COUNTRIES = "countries";


    public static PickTtsLanguageDialog newInstance(Collection<Locale> locales) {
        Bundle arguments = new Bundle();
        saveLocalesToArguments(locales, arguments);
        final PickTtsLanguageDialog dialog = new PickTtsLanguageDialog();
        dialog.setArguments(arguments);
        return dialog;
    }

    private static void saveLocalesToArguments(Collection<Locale> locales, Bundle arguments) {
        ArrayList<String> languageList = new ArrayList<>(locales.size());
        ArrayList<String> countryList = new ArrayList<>(locales.size());
        for (Locale locale : locales) {
            languageList.add(locale.getLanguage());
            countryList.add(locale.getCountry());
        }

        arguments.putStringArrayList(ARG_LANGUAGES, languageList);
        arguments.putStringArrayList(ARG_COUNTRIES, countryList);
    }

    private List<DisplayLocale> readLocaleListFromArguments() {
        Set<DisplayLocale> localeSet = new HashSet<>();
        final List<String> languages = getArguments().getStringArrayList(ARG_LANGUAGES);
        final List<String> countries = getArguments().getStringArrayList(ARG_COUNTRIES);
        for (int i = 0; i < languages.size(); i++) {
            final Locale locale = new Locale(languages.get(i), countries.get(i));
            localeSet.add(new DisplayLocale(locale));
        }
        List<DisplayLocale> result = new ArrayList<>();
        result.addAll(localeSet);
        return result;
    }


    private static class DisplayLocale {

        final Locale mLocale;

        private DisplayLocale(Locale locale) {
            mLocale = locale;
        }

        @Override
        public String toString() {
            return mLocale.getDisplayLanguage();
        }

        @Override
        public int hashCode() {
            return mLocale.getDisplayLanguage().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof DisplayLocale && toString().equals(obj.toString());
        }

        Locale getLocale() {
            return mLocale;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
        builder.setCancelable(true);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        builder.setTitle(getString(R.string.choose_language));

        final List<DisplayLocale> locales = readLocaleListFromArguments();

        showListOfLanguages(builder, locales);

        return builder.create();
    }

    private void showListOfLanguages(AlertDialog.Builder builder, List<DisplayLocale> locales) {
        final ArrayAdapter<DisplayLocale> arrayAdapter = new ArrayAdapter<>(getContext(), R.layout.item_tts_language);

        Collections.sort(locales, new Comparator<DisplayLocale>() {
            @Override
            public int compare(DisplayLocale o1, DisplayLocale o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });
        arrayAdapter.addAll(locales);


        builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                DisplayLocale locale = arrayAdapter.getItem(which);
                EventBus.getDefault().post(new TtsLanguageChoosen(locale.getLocale()));
            }
        });
    }


}