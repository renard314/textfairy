package com.renard.ocr.help;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

public class DownloadBroadCastReceiver extends BroadcastReceiver {
	private static final String LOG_TAG = DownloadBroadCastReceiver.class.getSimpleName();

	@Override
	public void onReceive(final Context context, Intent intent) {
		String action = intent.getAction();
		if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
			Log.i(LOG_TAG,"received ACTION_DOWNLOAD_COMPLETE");
			long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
			DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
			Query query = new Query();
			query.setFilterById(downloadId);
			Cursor c = dm.query(query);
			if (c.moveToFirst()) {
				int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
				int status = c.getInt(columnIndex);

				if (DownloadManager.STATUS_SUCCESSFUL == status) {
					Log.i(LOG_TAG,"Download successful");
					//start service to extract language file
					Intent serviceIntent = new Intent(context, OCRLanguageInstallService.class);
					serviceIntent.putExtra(DownloadManager.EXTRA_DOWNLOAD_ID, downloadId);
					context.startService(serviceIntent);

				} else if (DownloadManager.STATUS_FAILED==status){
					Log.i(LOG_TAG,"Download failed");
					Intent resultIntent = new Intent(OCRLanguageInstallService.ACTION_INSTALL_FAILED);
					columnIndex = c.getColumnIndex(DownloadManager.COLUMN_TITLE);
					String title = c.getString(columnIndex);
					resultIntent.putExtra(OCRLanguageInstallService.EXTRA_STATUS,status );
					resultIntent.putExtra(OCRLanguageInstallService.EXTRA_OCR_LANGUAGE_DISPLAY, title);
					context.sendBroadcast(resultIntent);
				}
			}
			c.close();

		}
	}

}
