package com.renard.ocr.help;

import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import com.actionbarsherlock.view.MenuItem;
import com.renard.ocr.R;
import com.renard.ocr.cropimage.MonitoredActivity;

public class LicenseActivity extends MonitoredActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_license);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		TextView leptonica  = (TextView) findViewById(R.id.textView_leptonica);
		TextView tesseract  = (TextView) findViewById(R.id.textView_tesseract);
		leptonica.setMovementMethod(new LinkMovementMethod());
		tesseract.setMovementMethod(new LinkMovementMethod());
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
