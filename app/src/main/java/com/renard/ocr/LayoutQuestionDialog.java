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
package com.renard.ocr;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.renard.ocr.help.OCRLanguageActivity;
import com.renard.ocr.help.OCRLanguageAdapter.OCRLanguage;
import com.renard.util.PreferencesUtils;

public class LayoutQuestionDialog {

    public enum LayoutKind {
        SIMPLE, COMPLEX, DO_NOTHING;
    }

    private static LayoutKind mLayout = LayoutKind.SIMPLE;
    private static String mLanguage;

    public interface LayoutChoseListener {
        void onLayoutChosen(final LayoutKind layoutKind, final String language);
    }

    public static AlertDialog createDialog(final Context context, final LayoutChoseListener listener, boolean accessibility) {

        mLayout = LayoutKind.SIMPLE;
        Pair<String, String> language = PreferencesUtils.getOCRLanguage(context);

        if (!OCRLanguageActivity.isLanguageInstalled(language.first, context)) {
            final String defaultLanguage = context.getString(R.string.default_ocr_language);
            final String defaultLanguageDisplay = context.getString(R.string.default_ocr_display_language);
            language = Pair.create(defaultLanguage, defaultLanguageDisplay);
        }
        mLanguage = language.first;

        AlertDialog.Builder builder;

        // builder = new AlertDialog.Builder(new ContextThemeWrapper(context,
        // R.style.Theme_Sherlock_Light_Dialog));
        builder = new AlertDialog.Builder(context);
        builder.setCancelable(false);
        View layout = View.inflate(context, R.layout.dialog_layout_question, null);
        builder.setView(layout);

        if(accessibility){
            layout.findViewById(R.id.radio_complex).setVisibility(View.GONE);
        }

        final RadioGroup layoutRadioGroup = (RadioGroup) layout.findViewById(R.id.radioGroup_layout_buttons);
        layoutRadioGroup.check(R.id.radio_simple);

        final Button langButton = (Button) layout.findViewById(R.id.button_language);
        langButton.setText(language.second);
        langButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ChooseLanguageDialog.createDialog(context,
                        new ChooseLanguageDialog.OnLanguageChosenListener() {

                            @Override
                            public void onLanguageChosen(OCRLanguage lang) {
                                mLanguage = lang.getValue();
                                langButton.setText(lang.getDisplayText());
                                PreferencesUtils.saveOCRLanguage(context, lang);
                            }
                        }).show();

            }
        });

        builder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        int checked = layoutRadioGroup
                                .getCheckedRadioButtonId();
                        if (checked == R.id.radio_complex) {
                            mLayout = LayoutKind.COMPLEX;
                        } else if (checked == R.id.radio_no_ocr) {
                            mLayout = LayoutKind.DO_NOTHING;
                        } else if (checked == R.id.radio_simple) {
                            mLayout = LayoutKind.SIMPLE;
                        }
                        listener.onLayoutChosen(mLayout, mLanguage);

                    }
                });

        builder.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog dialog = builder.create();

        final Button downloadButton = (Button) layout.findViewById(R.id.button_load_language);
        downloadButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(context, OCRLanguageActivity.class);
                context.startActivity(i);
                dialog.cancel();
            }
        });

        return dialog;

    }

}
