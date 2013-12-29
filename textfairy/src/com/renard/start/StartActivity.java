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
package com.renard.start;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.renard.install.InstallActivity;
import com.renard.ocr.BaseDocumentActivitiy;
import com.renard.ocr.DocumentGridActivity;
import com.renard.ocr.R;
import com.renard.ocr.help.AppOptionsActivity;
import com.renard.ocr.help.ReleaseNoteDialog;
import com.renard.util.Util;

public class StartActivity extends BaseDocumentActivitiy {

	private ViewFlipper mTextFlipper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.start_activity);
		mTextFlipper = (ViewFlipper) findViewById(R.id.viewflipper_text);
		ImageView fairy = (ImageView) findViewById(R.id.imageView_fairy);
		View.OnClickListener clicker = new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mTextFlipper.showNext();
			}
		};

		fairy.setOnClickListener(clicker);
		mTextFlipper.setOnClickListener(clicker);

		getSupportActionBar().setDisplayShowHomeEnabled(false);

		initButtons();
		initTextBubbles();

	}



	private void initTextBubbles() {
		String[] texts = getResources().getStringArray(R.array.start_activity_texts);
		LayoutInflater inflater = getLayoutInflater();
		for (String text : texts) {
			LinearLayout view = (LinearLayout) inflater.inflate(R.layout.speechbubble_textview, null);
			TextView textView = (TextView) view.findViewById(R.id.textView_welcome_text);
			textView.setText(text);
			mTextFlipper.addView(view, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		}
		mTextFlipper.setDisplayedChild(0);
	}

	private void initButtons() {
		final Button newDocumentFromCamera = (Button) findViewById(R.id.button_new_document_camera);
		final Button newDocumentFromGallery = (Button) findViewById(R.id.button_new_document_gallery);
		final Button manageDocuments = (Button) findViewById(R.id.button_show_documents);
		final Button options = (Button) findViewById(R.id.button_preferences);

		options.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(StartActivity.this, AppOptionsActivity.class);
				startActivity(i);
				overridePendingTransition(R.anim.push_up_in, R.anim.push_up_out);
			}
		});

		newDocumentFromCamera.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startCamera();
				overridePendingTransition(R.anim.push_down_in, R.anim.push_down_out);
			}
		});
		newDocumentFromGallery.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startGallery();
				overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);

			}
		});
		manageDocuments.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(StartActivity.this, DocumentGridActivity.class);
				startActivity(intent);
				overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.start_activity_options, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.whats_new) {

			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * documents created in this activity have no parent
	 */
	@Override
	protected int getParentId() {
		return -1;
	}




}
