package com.screenshare.renard314.testapp;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.leptonica.android.WriteFile;

import java.io.File;
import java.io.IOException;


public class MainActivity extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ImageView imageView = (ImageView) findViewById(R.id.imageView);
		Bitmap b = Bitmap.createBitmap(300,300,Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(b);
		Paint p = new Paint(){{
			setColor(Color.BLACK);
			setStrokeWidth(5);
			setStyle(Style.STROKE);
		}
		};
		c.drawLine(0,0, 299,299,p);
		imageView.setImageBitmap(b);
		try {
			final Pix pix = ReadFile.readBitmap(b);
			final File path = Environment.getExternalStorageDirectory();
			File image = new File(path, "test.png");
			image.createNewFile();
			boolean result = WriteFile.writeImpliedFormat(pix, image, 85, true);
			Toast.makeText(this,result+"",Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
}
