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
package com.renard.ocr.help;

import android.support.v4.app.NavUtils;
import android.webkit.WebView;

import com.actionbarsherlock.view.MenuItem;
import com.renard.ocr.R;
import com.renard.ocr.cropimage.MonitoredActivity;

public class HelpActivity extends MonitoredActivity {

	public static final String EXTRA_HELP_TEXT_ID = "help_text_id";

	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help_activity);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		// getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// ViewServer.get(this).addWindow(this);

		// TextView title = (TextView) findViewById(R.id.abs__action_bar_title);
		// title.setTextColor(Color.BLACK);
		// LayoutParams params = (LayoutParams) title.getLayoutParams();
		// LinearLayout p = (LinearLayout)title.getParent();
		// p.setBackgroundResource(R.drawable.speech_bubble);

		initAppIcon(this, -1);
		WebView webView = (WebView) findViewById(R.id.webView_help);
		webView.loadUrl("file:///android_res/raw/tips.html");

	};

	@Override
	protected void onDestroy() {
		// ViewServer.get(this).removeWindow(this);
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		// ViewServer.get(this).setFocusedWindow(this);
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
