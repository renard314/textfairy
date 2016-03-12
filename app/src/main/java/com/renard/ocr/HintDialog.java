/*
 * Copyright (C) 2012,2013 Renard Wellnitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.renard.ocr;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import com.renard.ocr.R;

public class HintDialog{

	private HintDialog(){
		
	}
	
	public static AlertDialog createDialog(final Context context,final int speechBubbleText, final String pathToHTML){
		AlertDialog.Builder builder;

		View layout = View.inflate(context, R.layout.dialog_fairy_helping, null);
		TextView speech = (TextView) layout.findViewById(R.id.help_header);
		speech.setText(speechBubbleText);
		WebView webView = (WebView) layout.findViewById(R.id.webView_help);
		webView.loadUrl(pathToHTML);
		builder = new AlertDialog.Builder(context);
		builder.setView(layout);
		builder.setNegativeButton(android.R.string.ok, null);

		return builder.create();
		
	}

}
