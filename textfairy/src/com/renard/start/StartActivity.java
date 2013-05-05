package com.renard.start;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.actionbarsherlock.view.Menu;
import com.renard.install.InstallActivity;
import com.renard.ocr.BaseDocumentActivitiy;
import com.renard.ocr.DocumentGridActivity;
import com.renard.ocr.R;
import com.renard.ocr.help.AppOptionsActivity;
import com.renard.util.Util;

public class StartActivity extends BaseDocumentActivitiy {

	private static final int REQUEST_CODE_INSTALL = 234;
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
		startInstallActivityIfNeeded();

		getSupportActionBar().setDisplayShowHomeEnabled(false);
		
		//View actionbar = findViewById(R.id.abs__action_bar_container);
		//actionbar.setVisibility(View.GONE);

		initButtons();
		initTextBubbles();
		final int columnWidth = Util.determineThumbnailSize(this, null);
		Util.setThumbnailSize(columnWidth, columnWidth, this);

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
				overridePendingTransition(R.anim.push_up_in,R.anim.push_up_out);  
			}
		});

		
		newDocumentFromCamera.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startCamera();
				overridePendingTransition(R.anim.push_down_in,R.anim.push_down_out);  
			}
		});
		newDocumentFromGallery.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startGallery();
				overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right);  

			}
		});
		manageDocuments.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(StartActivity.this, DocumentGridActivity.class);
				startActivity(intent);
				overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);  
			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return false;
	}

	/**
	 * documents created in this activity have no parent
	 */
	@Override
	protected int getParentId() {
		return -1;
	}

	/**
	 * Start the InstallActivity if possible and needed.
	 */
	private void startInstallActivityIfNeeded() {
		final String state = Environment.getExternalStorageState();
		if (state.equals(Environment.MEDIA_MOUNTED)) {
			if (InstallActivity.IsInstalled() == false) {
				// install the languages if needed, create directory structure
				// (one
				// time)
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setClassName(this, com.renard.install.InstallActivity.class.getName());
				startActivityForResult(intent, REQUEST_CODE_INSTALL);
			}
		} else {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			// alert.setTitle(R.string.no_sd_card);
			alert.setMessage(getString(R.string.no_sd_card));
			alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			});
			alert.show();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE_INSTALL) {
			if (RESULT_OK == resultCode) {
				// install successfull, show happy fairy or introduction text

			} else {
				// install failed, quit immediately
				finish();
			}

		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}
}
