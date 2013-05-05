package com.renard.ocr.help;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.NavUtils;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.actionbarsherlock.view.MenuItem;
import com.renard.ocr.R;
import com.renard.ocr.cropimage.MonitoredActivity;
import com.renard.ocr.help.OCRLanguageAdapter.OCRLanguage;
import com.renard.util.Util;

public class OCRLanguageActivity extends MonitoredActivity {

	public final static String ACTION_OCR_LANGUAGE_READY = "com.renard.ocr.language_ready";
	public final static String EXTRA_OCR_LANGUAGE = "com.renard.ocr.language";

	private BroadcastReceiver mDownloadReceiver;
	private ListView mList;
	private OCRLanguageAdapter mAdapter;
	private ViewSwitcher mSwitcher;
	private BroadcastReceiver mFailedReceiver;

	private class LoadListAsyncTask extends AsyncTask<Void, Void, OCRLanguageAdapter> {

		@Override
		protected void onPostExecute(OCRLanguageAdapter result) {
			super.onPostExecute(result);
			registerDownloadReceiver();
			mAdapter = result;
			mList.setAdapter(result);
			mSwitcher.setDisplayedChild(1);
			mList.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

					OCRLanguage language = (OCRLanguage) mAdapter.getItem(position);
					if (!language.mDownloaded) {
						final DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
						final String part1 = "https://tesseract-ocr.googlecode.com/files/tesseract-ocr-3.02.";
						final String part2 = ".tar.gz";
						Uri uri = Uri.parse(part1 + language.mValue + part2);
						Request request = new Request(uri);
						request.setTitle(language.mDisplayText);
						File extDir = Environment.getExternalStorageDirectory();
						File tessDir = new File(extDir, Util.OCR_DATA_DIRECTORY);
						request.setDestinationUri(Uri.fromFile(tessDir));
						long downloadId = dm.enqueue(request);
						// PreferencesUtils.pushDownloadId(OCRLanguageActivity.this,
						// downloadId);
						language.mDownloading = true;
						mAdapter.notifyDataSetChanged();

					} else {
						deleteLanguage(position);
					}

				}
			});

		}

		protected void deleteLanguage(int position) {
			final OCRLanguage language = (OCRLanguage) mAdapter.getItem(position);
			AlertDialog.Builder b = new Builder(OCRLanguageActivity.this);
			String msg = getString(R.string.delete_language_message);
			String title = getString(R.string.delete_language_title);
			title = String.format(title, language.mDisplayText);
			msg = String.format(msg, language.mSize / 1024);
			b.setTitle(title);
			b.setMessage(msg);
			b.setCancelable(true);
			b.setNegativeButton(R.string.cancel, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {

				}
			});
			b.setPositiveButton(R.string.ocr_language_delete, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					File tessDir = getTessDir();
					if (!tessDir.exists()) {
						return;
					}
					File lang = new File(tessDir, language.mValue + ".traineddata");
					if (lang.delete()) {
						language.mDownloaded = false;
						mAdapter.notifyDataSetChanged();
					}

				}
			});
			b.show();

		}

		@Override
		protected OCRLanguageAdapter doInBackground(Void... params) {
			return initLanguageList();
		}

	}

	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ocr_language_activity);
		mList = (ListView) findViewById(R.id.list_ocr_languages);
		mSwitcher = (ViewSwitcher) findViewById(R.id.viewSwitcher_language_list);
		initAppIcon(this, -1);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		new LoadListAsyncTask().execute();
	};

	private OCRLanguageAdapter initLanguageList() {
		// actual values uses by tesseract
		final String[] languageValues = getResources().getStringArray(R.array.ocr_languages);
		// values shown to the user
		final String[] languageDisplayValues = new String[languageValues.length];
		for (int i = 0; i < languageValues.length; i++) {
			final String val = languageValues[i];
			final int firstSpace = val.indexOf(' ');
			languageDisplayValues[i] = languageValues[i].substring(firstSpace + 1, languageValues[i].length());
			languageValues[i] = languageValues[i].substring(0, firstSpace);
		}
		final List<Pair<String, Long>> installedLanguages = getInstalledLanguages();
		OCRLanguageAdapter adapter = new OCRLanguageAdapter(getApplicationContext(), false);
		for (int i = 0; i < languageValues.length; i++) {
			boolean downloaded = false;
			long size = 0;
			for (Pair<String, Long> installedLang : installedLanguages) {
				if (installedLang.first.equalsIgnoreCase(languageValues[i])) {
					downloaded = true;
					size = installedLang.second;
					break;
				}
			}
			OCRLanguage language = new OCRLanguage(languageValues[i], languageDisplayValues[i], downloaded, size);
			adapter.add(language);
		}
		updateLanguageListWithDownloadManagerStatus(adapter);
		return adapter;
	}

	private static File getTessDir() {
		File extDir = Environment.getExternalStorageDirectory();
		return new File(extDir, Util.OCR_DATA_DIRECTORY);
	}

	public static final List<Pair<String, Long>> getInstalledLanguages() {
		final List<Pair<String, Long>> result = new ArrayList<Pair<String, Long>>();
		final File tessDir = getTessDir();
		if (!tessDir.exists()) {
			return result;
		}
		final String[] languageFiles = tessDir.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String filename) {
				if (filename.endsWith(".traineddata")) {
					return true;
				}
				return false;
			}
		});

		for (final String val : languageFiles) {
			final int dotIndex = val.indexOf('.');
			if (dotIndex > -1) {
				File f = new File(tessDir, val);
				result.add(Pair.create(val.substring(0, dotIndex), f.length()));
			}
		}
		return result;
	}

	@Override
	protected synchronized void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mDownloadReceiver);
		unregisterReceiver(mFailedReceiver);
	}

	// @Override
	// protected void onResume() {
	// super.onResume();
	// updateLanguageListWithDownloadManagerStatus(mAdapter);
	// registerDownloadReceiver();
	// }

	private void updateLanguageListWithDownloadManagerStatus(OCRLanguageAdapter adapter) {
		if (adapter != null) {
			// find languages that are currently beeing downloaded
			Query query = new Query();
			query.setFilterByStatus(DownloadManager.STATUS_RUNNING | DownloadManager.STATUS_PENDING | DownloadManager.STATUS_PAUSED);
			final DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
			Cursor c = dm.query(query);
			int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_TITLE);
			while (c.moveToNext()) {
				final String title = c.getString(columnIndex);
				adapter.setDownloading(title, true);
			}
			adapter.notifyDataSetChanged();
			c.close();
		}
	}

	private void registerDownloadReceiver() {
		mDownloadReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				String lang = intent.getStringExtra(OCRLanguageInstallService.EXTRA_OCR_LANGUAGE);
				int status = intent.getIntExtra(OCRLanguageInstallService.EXTRA_STATUS, -1);
				updateLanguageList(lang, status);
			}

		};
		mFailedReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				String lang = intent.getStringExtra(OCRLanguageInstallService.EXTRA_OCR_LANGUAGE_DISPLAY);
				int status = intent.getIntExtra(OCRLanguageInstallService.EXTRA_STATUS, -1);
				updateLanguageListByDisplayValue(lang, status);
			}
		};
		registerReceiver(mFailedReceiver, new IntentFilter(OCRLanguageInstallService.ACTION_INSTALL_FAILED));
		registerReceiver(mDownloadReceiver, new IntentFilter(OCRLanguageInstallService.ACTION_INSTALL_COMPLETED));
	}

	protected void updateLanguageListByDisplayValue(String displayValue, int status) {
		for (int i = 0; i < mAdapter.getCount(); i++) {
			final OCRLanguage language = (OCRLanguage) mAdapter.getItem(i);
			if (language.getDisplayText().equalsIgnoreCase(displayValue)) {
				updateLanguage(language, status);
				return;
			}
		}
	}

	protected void updateLanguageList(String lang, int status) {
		for (int i = 0; i < mAdapter.getCount(); i++) {
			final OCRLanguage language = (OCRLanguage) mAdapter.getItem(i);
			if (language.getValue().equalsIgnoreCase(lang)) {
				updateLanguage(language, status);
				return;
			}
		}
	}

	private void updateLanguage(final OCRLanguage language, int status) {
		language.mDownloading = false;
		if (status == DownloadManager.STATUS_SUCCESSFUL) {
			language.mDownloaded = true;
		} else {
			language.mDownloaded = false;
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					String msg = getString(R.string.download_failed);
					msg = String.format(msg, language.mDisplayText);
					Toast.makeText(OCRLanguageActivity.this, msg, Toast.LENGTH_LONG).show();
				}
			});
		}
		mAdapter.notifyDataSetChanged();
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
