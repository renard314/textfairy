/*
 * Copyright (C) 2012,2013 Renard Wellnitz.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.renard.ocr.documents.creation.visualisation;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.ViewFlipper;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.os.ConfigurationCompat;
import androidx.fragment.app.DialogFragment;

import com.renard.ocr.MonitoredActivity;
import com.renard.ocr.R;
import com.renard.ocr.analytics.Analytics;
import com.renard.ocr.analytics.CrashLogger;
import com.renard.ocr.main_menu.language.InstallStatus;
import com.renard.ocr.main_menu.language.OcrLanguage;
import com.renard.ocr.main_menu.language.OcrLanguageDataStore;
import com.renard.ocr.util.PreferencesUtils;

import java.util.List;
import java.util.MissingResourceException;

import static com.renard.ocr.main_menu.language.OcrLanguageDataStore.getInstallStatusFor;
import static com.renard.ocr.main_menu.language.OcrLanguageDataStore.getInstalledOCRLanguages;

public class LayoutQuestionDialog extends DialogFragment {

    public static final String TAG = LayoutQuestionDialog.class.getSimpleName();
    private static final String SCREEN_NAME = "Layout Question Dialog";

    private Analytics mAnalytics;
    private CrashLogger mCrashLogger;

    public static LayoutQuestionDialog newInstance() {
        return new LayoutQuestionDialog();
    }

    public enum LayoutKind {
        SIMPLE,
        COMPLEX,
        DO_NOTHING;
    }

    private static LayoutKind mLayout = LayoutKind.SIMPLE;
    private static String mLanguage;

    public interface LayoutChoseListener {
        void onLayoutChosen(final LayoutKind layoutKind, final String language);
    }

    public Analytics getAnalytics() {
        if (mAnalytics == null && getActivity() != null) {
            MonitoredActivity activity = (MonitoredActivity) getActivity();
            return activity.getAnaLytics();
        }
        return mAnalytics;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        MonitoredActivity monitoredActivity = (MonitoredActivity) getActivity();
        mAnalytics = monitoredActivity.getAnaLytics();
        mCrashLogger = monitoredActivity.getCrashLogger();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        getAnalytics().sendScreenView(SCREEN_NAME);
        final Context context = getContext();
        mLayout = null;
        mLanguage = getOcrLanguage(context);
        if(mLanguage == null){
            throw new IllegalStateException("No OCR Language Available.");
        }

        AlertDialog.Builder builder;

        builder = new AlertDialog.Builder(context);
        builder.setCancelable(false);
        View layout = View.inflate(context, R.layout.dialog_layout_question, null);
        builder.setView(layout);

        final ViewFlipper titleViewFlipper = (ViewFlipper) layout.findViewById(R.id.layout_title);
        final ImageView columnLayout = (ImageView) layout.findViewById(R.id.column_layout);
        final ImageView pageLayout = (ImageView) layout.findViewById(R.id.page_layout);
        final ImageSwitcher fairy = (ImageSwitcher) layout.findViewById(R.id.fairy_layout);
        fairy.setFactory(
                new ViewSwitcher.ViewFactory() {
                    public View makeView() {
                        return new ImageView(context);
                    }
                });
        fairy.setImageResource(R.drawable.fairy_looks_center);
        fairy.setInAnimation(context, android.R.anim.fade_in);
        fairy.setOutAnimation(context, android.R.anim.fade_out);

        final int color = context.getResources().getColor(R.color.progress_color);

        final PorterDuffColorFilter colorFilter =
                new PorterDuffColorFilter(color, PorterDuff.Mode.LIGHTEN);

        columnLayout.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mLayout != LayoutKind.COMPLEX) {
                            fairy.setImageResource(R.drawable.fairy_looks_left);
                            titleViewFlipper.setDisplayedChild(2);
                            columnLayout.setColorFilter(colorFilter);
                            pageLayout.clearColorFilter();
                            mLayout = LayoutKind.COMPLEX;
                        }
                    }
                });
        pageLayout.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mLayout != LayoutKind.SIMPLE) {
                            mLayout = LayoutKind.SIMPLE;
                            titleViewFlipper.setDisplayedChild(1);
                            fairy.setImageResource(R.drawable.fairy_looks_right);
                            pageLayout.setColorFilter(colorFilter);
                            columnLayout.clearColorFilter();
                        }
                    }
                });

        final Spinner langButton = (Spinner) layout.findViewById(R.id.button_language);
        List<OcrLanguage> installedLanguages =
                OcrLanguageDataStore.getInstalledOCRLanguages(context);

        // actual values uses by tesseract
        final ArrayAdapter<OcrLanguage> adapter =
                new ArrayAdapter<>(context, R.layout.item_spinner_language, installedLanguages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        langButton.setAdapter(adapter);
        for (int i = 0; i < installedLanguages.size(); i++) {
            OcrLanguage lang = installedLanguages.get(i);
            if (lang.getValue().equals(mLanguage)) {
                langButton.setSelection(i, false);
                break;
            }
        }
        langButton.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        final OcrLanguage item = adapter.getItem(position);
                        mLanguage = item.getValue();
                        PreferencesUtils.saveOCRLanguage(context, item);
                        getAnalytics().sendOcrLanguageChanged(item);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });

        builder.setPositiveButton(
                R.string.start_scan,
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        if (mLayout == null) {
                            mLayout = LayoutKind.SIMPLE;
                        }
                        LayoutChoseListener listener = (LayoutChoseListener) getActivity();
                        listener.onLayoutChosen(mLayout, mLanguage);
                        getAnalytics().sendOcrStarted(mLanguage, mLayout);
                        dialog.dismiss();
                    }
                });

        builder.setNegativeButton(
                R.string.cancel,
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        getActivity().finish();
                        dialog.dismiss();
                        getAnalytics().sendLayoutDialogCancelled();
                    }
                });

        final AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        return alertDialog;
    }

    @Nullable
    private String getOcrLanguage(Context context) {
        String language = getOrcLanguageFromPreferences(context);
        if (language == null) {
            language = getOrcLanguageFromUserLocale(context);
        }
        if (language == null) {
            language = getOcrLanguageFromSdCard(context);
        }
        return language;
    }

    @Nullable
    private String getOcrLanguageFromSdCard(Context context) {
        if (getInstallStatusFor(OcrLanguageDataStore.LATIN_SCRIPT, context).isInstalled()){
            return OcrLanguageDataStore.LATIN_SCRIPT;
        } else {
            final List<OcrLanguage> installedOCRLanguages = getInstalledOCRLanguages(context);
            if(!installedOCRLanguages.isEmpty()){
                return installedOCRLanguages.get(0).getValue();
            } else {
                return null;
            }
        }
    }

    @Nullable
    private String getOrcLanguageFromPreferences(Context context) {
        Pair<String, String> language = PreferencesUtils.getOCRLanguage(context);
        if (language.first == null) {
            return null;
        }
        if (getInstallStatusFor(language.first, context).isInstalled()) {
            return language.first;
        } else {
            return null;
        }
    }

    @Nullable
    private String getOrcLanguageFromUserLocale(Context context) {
        try {
            final String iso3Language =
                    ConfigurationCompat.getLocales(getResources().getConfiguration())
                            .get(0)
                            .getISO3Language();
            if (getInstallStatusFor(iso3Language, context).isInstalled()) {
                return iso3Language;
            }
        } catch (MissingResourceException e) {
            return null;
        }
        return null;
    }
}
