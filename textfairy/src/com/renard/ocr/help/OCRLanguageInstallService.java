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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import android.app.DownloadManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.renard.util.Util;

public class OCRLanguageInstallService extends IntentService {

	protected static final String ACTION_INSTALL_COMPLETED = "com.renard.ocr.ACTION_OCR_LANGUAGE_INSTALLED";
	protected static final String ACTION_INSTALL_FAILED = "com.renard.ocr.ACTION_INSTALL_FAILED";
	protected static final String EXTRA_OCR_LANGUAGE = "ocr_language";
	protected static final String EXTRA_OCR_LANGUAGE_DISPLAY = "ocr_language_display";
	public static final String EXTRA_STATUS = "status";

	
	public OCRLanguageInstallService(){
		this(OCRLanguageInstallService.class.getSimpleName());
	}

	public OCRLanguageInstallService(String name) {
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (!intent.hasExtra(DownloadManager.EXTRA_DOWNLOAD_ID)) {
			return;
		}
		final long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
		if (downloadId != 0) {

			DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
			ParcelFileDescriptor file;
			try {
				file = dm.openDownloadedFile(downloadId);
				FileInputStream fin = new FileInputStream(file.getFileDescriptor());
				BufferedInputStream in = new BufferedInputStream(fin);
				FileOutputStream out = openFileOutput("tess-lang.tmp", Context.MODE_PRIVATE);
				GzipCompressorInputStream gzIn = new GzipCompressorInputStream(in);
				final byte[] buffer = new byte[2048];
				int n = 0;
				while (-1 != (n = gzIn.read(buffer))) {
					out.write(buffer, 0, n);
				}
				out.close();
				gzIn.close();
				FileInputStream fileIn = openFileInput("tess-lang.tmp");
				TarArchiveInputStream tarIn = new TarArchiveInputStream(fileIn);

				TarArchiveEntry entry = tarIn.getNextTarEntry();
				
				while (entry!=null && !(entry.getName().endsWith(".traineddata") && !entry.getName().endsWith("_old.traineddata"))){
					entry = tarIn.getNextTarEntry();
				}
				if (entry != null) {
					byte[] content = new byte[(int) entry.getSize()];
					int bytesRead = 0;
					while (bytesRead < entry.getSize()) {
						bytesRead += tarIn.read(content, bytesRead, content.length - bytesRead);
					}
					File tessDir = Util.getTrainingDataDir(this);
					final String langName = entry.getName().substring("tesseract-ocr/tessdata/".length());
					File trainedData = new File(tessDir, langName);
					FileOutputStream fout = new FileOutputStream(trainedData);
					fout.write(content);
					fout.close();
					String lang = langName.substring(0, langName.length() - ".traineddata".length());
					notifyReceivers(lang);
				}
				tarIn.close();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				String tessDir = Util.getTessDir(this);
				File targetFile = new File(tessDir, OCRLanguageActivity.DOWNLOADED_TRAINING_DATA);
				if (targetFile.exists()){
					targetFile.delete();
				}
			}

		}
	}

	private void notifyReceivers(String lang) {
		//notify language activity
		Intent resultIntent = new Intent(ACTION_INSTALL_COMPLETED);
		resultIntent.putExtra(EXTRA_OCR_LANGUAGE, lang);
		resultIntent.putExtra(EXTRA_STATUS, DownloadManager.STATUS_SUCCESSFUL);
		sendBroadcast(resultIntent);
	}
}
