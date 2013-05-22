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

import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.renard.ocr.help.OCRLanguageActivity;
import com.renard.ocr.help.OCRLanguageAdapter;
import com.renard.ocr.help.OCRLanguageAdapter.OCRLanguage;

public class ChooseLanguageDialog {
	
	interface OnLanguageChosenListener {
		void onLanguageChosen(final OCRLanguage lang);
	}

	public static AlertDialog createDialog(Context context, final OnLanguageChosenListener onLanguageChosenListener) {
		List<Pair<String,Long>> installedLanguages = OCRLanguageActivity.getInstalledLanguages(context);
		// actual values uses by tesseract
		final String[] languageValues = context.getResources().getStringArray(R.array.ocr_languages);
		OCRLanguageAdapter adapter = new OCRLanguageAdapter(context,true);
		for(String val : languageValues){
			final int firstSpace = val.indexOf(' ');
			final String displayText= val.substring(firstSpace + 1, val.length());
			final String value = val.substring(0, firstSpace);
			for (Pair<String,Long> installedLang: installedLanguages){
				if (installedLang.first.equalsIgnoreCase(value)) {
					OCRLanguage language = new OCRLanguage(value,displayText,true,installedLang.second);
					adapter.add(language);					
				}
			}
		}
		LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout =  li.inflate(R.layout.dialog_language_list, null,false);
		final ListView list = (ListView) layout.findViewById(R.id.listView_languages);
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setCancelable(true);
		builder.setView(layout);
		final AlertDialog result = builder.create();
		list.setAdapter(adapter);
		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				OCRLanguage lang = (OCRLanguage) list.getItemAtPosition(position);
				onLanguageChosenListener.onLanguageChosen(lang);
				result.cancel();
				
			}
		});
		return result;
	}

}
