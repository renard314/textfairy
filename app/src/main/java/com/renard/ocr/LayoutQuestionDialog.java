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
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.widget.ViewSwitcher;

import com.renard.ocr.help.OCRLanguageActivity;
import com.renard.ocr.help.OCRLanguageAdapter;
import com.renard.ocr.help.OCRLanguageAdapter.OCRLanguage;
import com.renard.util.PreferencesUtils;

import java.util.List;

import static android.widget.ArrayAdapter.*;

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

        mLayout = null;
        Pair<String, String> language = PreferencesUtils.getOCRLanguage(context);

        if (!OCRLanguageActivity.isLanguageInstalled(language.first, context)) {
            final String defaultLanguage = context.getString(R.string.default_ocr_language);
            final String defaultLanguageDisplay = context.getString(R.string.default_ocr_display_language);
            language = Pair.create(defaultLanguage, defaultLanguageDisplay);
        }
        mLanguage = language.first;

        AlertDialog.Builder builder;

        builder = new AlertDialog.Builder(context);
        builder.setCancelable(false);
        View layout = View.inflate(context, R.layout.dialog_layout_question, null);
        builder.setView(layout);


        final ViewFlipper titleViewFlipper = (ViewFlipper) layout.findViewById(R.id.layout_title);
        final ImageView columnLayout = (ImageView) layout.findViewById(R.id.column_layout);
        final ImageView pageLayout = (ImageView) layout.findViewById(R.id.page_layout);
        final ImageSwitcher fairy = (ImageSwitcher) layout.findViewById(R.id.fairy_layout);
        fairy.setFactory(new ViewSwitcher.ViewFactory() {
            public View makeView() {
                ImageView myView = new ImageView(context);
                return myView;
            }
        });
        fairy.setImageResource(R.drawable.fairy_looks_center);
        fairy.setInAnimation(context, android.R.anim.fade_in);
        fairy.setOutAnimation(context, android.R.anim.fade_out);

        final int color = context.getResources().getColor(R.color.progress_color);

        final PorterDuffColorFilter colorFilter = new PorterDuffColorFilter(color, PorterDuff.Mode.LIGHTEN);


        columnLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mLayout!=LayoutKind.COMPLEX) {
                    fairy.setImageResource(R.drawable.fairy_looks_left);
                    titleViewFlipper.setDisplayedChild(2);
                    columnLayout.setColorFilter(colorFilter);
                    pageLayout.clearColorFilter();
                    mLayout = LayoutKind.COMPLEX;
                }

            }
        });
        pageLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mLayout!=LayoutKind.SIMPLE) {
                    mLayout = LayoutKind.SIMPLE;
                    titleViewFlipper.setDisplayedChild(1);
                    fairy.setImageResource(R.drawable.fairy_looks_right);
                    pageLayout.setColorFilter(colorFilter);
                    columnLayout.clearColorFilter();
                }
            }
        });


        final Spinner langButton = (Spinner) layout.findViewById(R.id.button_language);
        List<OCRLanguage> installedLanguages = OCRLanguageActivity.getInstalledOCRLanguages(context);

        // actual values uses by tesseract
        final ArrayAdapter<OCRLanguage> adapter = new ArrayAdapter<>(context,android.R.layout.simple_spinner_item, installedLanguages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        langButton.setAdapter(adapter);
        for(int i = 0; i < installedLanguages.size(); i++){
            OCRLanguage lang = installedLanguages.get(i);
            if(lang.getValue().equals(language.first)){
                langButton.setSelection(i);
                break;
            }
        }
        langButton.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final OCRLanguage item = adapter.getItem(position);
                mLanguage = item.getValue();
                PreferencesUtils.saveOCRLanguage(context, item);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        builder.setPositiveButton(R.string.start_scan,
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        if(mLayout==null){
                            mLayout= LayoutKind.SIMPLE;
                        }
                        listener.onLayoutChosen(mLayout,mLanguage);
                    }
                });

        builder.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog dialog = builder.create();



        return dialog;

    }

}
