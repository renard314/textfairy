package com.renard.ocr.help;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import com.renard.ocr.R;

public class HintDialog{

	
	public static AlertDialog createDialog(final Context context,final int speechBubbleText, final String pathToHTML){
		AlertDialog.Builder builder;

		View layout = View.inflate(context, R.layout.dialog_fairy_helping, null);
		TextView speech = (TextView) layout.findViewById(R.id.fairy_text);
		speech.setText(speechBubbleText);
		WebView webView = (WebView) layout.findViewById(R.id.webView_help);
		webView.loadUrl(pathToHTML);
		builder = new AlertDialog.Builder(context);
		builder.setView(layout);
		builder.setNegativeButton(android.R.string.ok, null);
		return builder.create();
		
	}

}
