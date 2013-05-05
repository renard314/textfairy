package com.renard.ocr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.text.Html;
import android.text.Spanned;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.ReadFile;
import com.renard.documentview.DocumentActivity;
import com.renard.ocr.DocumentContentProvider.Columns;
import com.renard.ocr.cropimage.CropImage;
import com.renard.ocr.cropimage.MonitoredActivity;
import com.renard.pdf.Hocr2Pdf;
import com.renard.pdf.Hocr2Pdf.PDFProgressListener;
import com.renard.util.Util;

/**
 * activities which extend this activity can create a new document. this class
 * also containes the code for functionality which is shared by
 * {@link DocumentGridActivity} and {@link DocumentActivity}
 * 
 * @author renard
 * 
 */
public abstract class BaseDocumentActivitiy extends MonitoredActivity {

	public final static String EXTRA_NATIVE_PIX = "pix_pointer";
	public final static String EXTRA_IMAGE_URI = "image_uri";
	public final static String EXTRA_ROTATION = "rotation";

	private static final int PDF_PROGRESS_DIALOG_ID = 0;
	private static final int DELETE_PROGRESS_DIALOG_ID = 1;
	protected static final int HINT_DIALOG_ID = 2;
	private static final int EDIT_TITLE_DIALOG_ID = 3;

	private static final String DIALOG_ARG_MAX = "max";
	private static final String DIALOG_ARG_MESSAGE = "message";
	private static final String DIALOG_ARG_PROGRESS = "progress";
	private static final String DIALOG_ARG_SECONDARY_PROGRESS = "secondary_progress";
	private static final String DIALOG_ARG_TITLE = "title";
	private static final String DIALOG_ARG_DOCUMENT_URI = "document_uri";

	private final static int REQUEST_CODE_MAKE_PHOTO = 0;
	private final static int REQUEST_CODE_PICK_PHOTO = 1;
	private final static int REQUEST_CODE_CROP_PHOTO = 2;
	protected final static int REQUEST_CODE_OCR = 3;

	// reference to image data taken by camera. this way I can avoid passing
	// image bytes via setResult().
	// public static byte[] imageData = null;
	public static File sImageFile = null;

	protected abstract int getParentId();

	ProgressDialog pdfProgressDialog;
	ProgressDialog deleteProgressdialog;

	protected void startCamera() {
		Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
		File photo = new File(Environment.getExternalStorageDirectory(), "TmpPic.jpg");
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
		sImageFile = photo;
		startActivityForResult(intent, REQUEST_CODE_MAKE_PHOTO);
	}

	protected void startGallery() {
		Intent i = new Intent(Intent.ACTION_GET_CONTENT, null);
		i.setType("image/*");
		File photo = new File(Environment.getExternalStorageDirectory(), "TmpPic.jpg");
		i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
		sImageFile = photo;
		startActivityForResult(i, REQUEST_CODE_PICK_PHOTO);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_camera:
			startCamera();
			return true;
		case R.id.item_gallery:
			startGallery();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.base_document_activity_options, menu);
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (RESULT_OK == resultCode) {
			Pix p = null;
			switch (requestCode) {
			case REQUEST_CODE_CROP_PHOTO: {
				int nativePix = data.getIntExtra(EXTRA_NATIVE_PIX, 0);
				Intent intent = new Intent(this, OCRActivity.class);
				intent.putExtra(EXTRA_NATIVE_PIX, nativePix);
				intent.putExtra(OCRActivity.EXTRA_PARENT_DOCUMENT_ID, getParentId());
				startActivityForResult(intent, REQUEST_CODE_OCR);

				break;
			}
			case REQUEST_CODE_MAKE_PHOTO: {
				int[] degree = new int[1];
				/* also gets rotation info from exif data */
				p = readImageFromFile(sImageFile, degree);
				File photo = new File(Environment.getExternalStorageDirectory(), "TmpPic.jpg");
				photo.delete();
				if (p != null) {
					Intent intent = new Intent(this, CropImage.class);
					intent.putExtra(EXTRA_NATIVE_PIX, p.getNativePix());
					intent.putExtra(EXTRA_ROTATION, degree[0]);
					startActivityForResult(intent, REQUEST_CODE_CROP_PHOTO);
				} else {
					showFileError();
				}
				break;
			}
			case REQUEST_CODE_PICK_PHOTO: {
				int[] degree = new int[1];
				/* also gets rotation info from exif data */
				if (data != null) {
					Uri fileUri = data.getData();
					if (fileUri == null) {
						fileUri = Uri.parse(data.getAction());
					}
					p = readImageFromUri(fileUri, degree);
					if (p != null) {
						Intent intent = new Intent(this, CropImage.class);
						intent.putExtra(EXTRA_NATIVE_PIX, p.getNativePix());
						intent.putExtra(EXTRA_ROTATION, degree[0]);
						startActivityForResult(intent, REQUEST_CODE_CROP_PHOTO);
					} else {
						showFileError();
					}
				}
				break;

			}
			}
		}
	}

	private void showFileError() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(R.string.error_title);
		final TextView textview = new TextView(this);
		textview.setText(R.string.error_load_file);
		alert.setView(textview);

		alert.setPositiveButton(android.R.string.ok, null);
		alert.show();
	}

	private Pix readImageFromFile(final File imageFile, int[] degree) {
		degree[0] = Util.getExifOrientation(imageFile.getAbsolutePath());
		try {
			return ReadFile.readFile(imageFile);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private Pix readImageFromUri(Uri imageUri, int[] degree) {
		InputStream is = null;
		String path = Util.getPathForUri(this, imageUri);
		if (path == null) {
			try {
				BitmapFactory.Options options = new Options();
				options.inPreferredConfig = Bitmap.Config.ARGB_8888;
				options.inScaled = false;
				is = getContentResolver().openInputStream(imageUri);
				final Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
				if (bitmap != null) {
					final Pix p = ReadFile.readBitmap(bitmap);
					bitmap.recycle();
					return p;
				}
				return null;
			} catch (FileNotFoundException e1) {
				return null;
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException ignored) {
					}
				}
			}
		}
		File file = new File(path);
		degree[0] = Util.getExifOrientation(path);
		try {
			return ReadFile.readFile(file);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		switch (id) {

		case PDF_PROGRESS_DIALOG_ID:
			int max = args.getInt(DIALOG_ARG_MAX);
			String message = args.getString(DIALOG_ARG_MESSAGE);
			String title = args.getString(DIALOG_ARG_TITLE);
			pdfProgressDialog = new ProgressDialog(this);
			pdfProgressDialog.setMessage(message);
			pdfProgressDialog.setTitle(title);
			pdfProgressDialog.setIndeterminate(false);
			pdfProgressDialog.setMax(max);
			pdfProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			pdfProgressDialog.setCancelable(false);
			return pdfProgressDialog;
		case DELETE_PROGRESS_DIALOG_ID:
			max = args.getInt(DIALOG_ARG_MAX);
			message = args.getString(DIALOG_ARG_MESSAGE);
			deleteProgressdialog = new ProgressDialog(this);
			deleteProgressdialog.setMessage(message);
			deleteProgressdialog.setIndeterminate(false);
			deleteProgressdialog.setMax(max);
			deleteProgressdialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			deleteProgressdialog.setCancelable(false);
			return deleteProgressdialog;
		case EDIT_TITLE_DIALOG_ID:
			View layout = getLayoutInflater().inflate(R.layout.edit_title_dialog, null);
			final Uri documentUri = Uri.parse(args.getString(DIALOG_ARG_DOCUMENT_URI));
			final String oldTitle = args.getString(DIALOG_ARG_TITLE);
			final EditText edit = (EditText) layout.findViewById(R.id.edit_title);
			edit.setText(oldTitle);

			AlertDialog.Builder builder = new Builder(this);
			builder.setView(layout);
			builder.setTitle(R.string.edit_dialog_title);
			builder.setIcon(R.drawable.fairy_showing);
			builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					final String title = edit.getText().toString();
					saveTitle(title, documentUri);

				}
			});
			builder.setNegativeButton(R.string.cancel, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {

				}
			});
			builder.show();
		}
		return super.onCreateDialog(id, args);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
		switch (id) {
		case EDIT_TITLE_DIALOG_ID:
			final Uri documentUri = Uri.parse(args.getString(DIALOG_ARG_DOCUMENT_URI));
			final String oldTitle = args.getString(DIALOG_ARG_TITLE);
			final EditText edit = (EditText) dialog.findViewById(R.id.edit_title);
			edit.setText(oldTitle);
			AlertDialog alertDialog = (AlertDialog) dialog;
			Button okButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
			okButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					final String title = edit.getText().toString();
					saveTitle(title, documentUri);
				}
			});
			break;
		case HINT_DIALOG_ID:
			break;
		default:
			if (args != null) {
				final int max = args.getInt(DIALOG_ARG_MAX);
				final int progress = args.getInt(DIALOG_ARG_PROGRESS);
				// final int secondaryProgress =
				// args.getInt(DIALOG_ARG_SECONDARY_PROGRESS);
				final String message = args.getString(DIALOG_ARG_MESSAGE);
				final String title = args.getString(DIALOG_ARG_TITLE);
				if (id == PDF_PROGRESS_DIALOG_ID) {
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							pdfProgressDialog.setProgress(progress);
							pdfProgressDialog.setMax(max);
							if (message != null) {
								pdfProgressDialog.setMessage(message);
							}
							if (title != null) {
								pdfProgressDialog.setTitle(title);
							}
						}
					});

				} else if (id == DELETE_PROGRESS_DIALOG_ID) {
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							deleteProgressdialog.setProgress(progress);
							deleteProgressdialog.setMax(max);
							if (message != null) {
								deleteProgressdialog.setMessage(message);
							}
						}
					});
				}
			}
		}
		super.onPrepareDialog(id, dialog, args);
	}

	protected void askUserForNewTitle(final String oldTitle, final Uri documentUri) {
		Bundle bundle = new Bundle(2);
		bundle.putString(DIALOG_ARG_TITLE, oldTitle);
		bundle.putString(DIALOG_ARG_DOCUMENT_URI, documentUri.toString());
		showDialog(EDIT_TITLE_DIALOG_ID, bundle);
	}

	private void saveTitle(final String newTitle, final Uri documentUri) {
		Uri uri = documentUri;
		if (uri == null) {
			uri = getIntent().getData();
		}
		if (uri != null) {
			SaveDocumentTask saveTask = new SaveDocumentTask(documentUri, newTitle);
			saveTask.execute();
		}

	}

	/**********************************************
	 * 
	 * ASYNC TASKS
	 * 
	 */

	protected class CreatePDFTask extends AsyncTask<Void, Integer, ArrayList<Uri>> implements PDFProgressListener {

		private Set<Integer> mIds = new HashSet<Integer>();
		private int mCurrentPageCount;
		private int mCurrentDocumentIndex;
		private String mCurrentDocumentName;
		private StringBuilder mOCRText = new StringBuilder();

		public CreatePDFTask(Set<Integer> ids) {
			mIds.addAll(ids);
		}

		@Override
		public void onNewPage(int pageNumber) {
			Bundle args = new Bundle(5);

			String progressMsg = getResources().getString(R.string.progress_pfd_creation);
			progressMsg = String.format(progressMsg, pageNumber, mCurrentPageCount, mCurrentDocumentName);

			String title = getResources().getString(R.string.pdf_creation_message);

			args.putString(DIALOG_ARG_MESSAGE, title);
			args.putString(DIALOG_ARG_MESSAGE, progressMsg);
			args.putInt(DIALOG_ARG_MAX, mIds.size());
			args.putInt(DIALOG_ARG_PROGRESS, mCurrentDocumentIndex);
			args.putInt(DIALOG_ARG_SECONDARY_PROGRESS, pageNumber);
			showDialog(PDF_PROGRESS_DIALOG_ID, args);
		}

		@Override
		protected void onPreExecute() {
			mOCRText.delete(0, mOCRText.length());
			Bundle args = new Bundle(2);
			args.putInt(DIALOG_ARG_MAX, mIds.size());
			args.putInt(DIALOG_ARG_PROGRESS, 0);
			String message = getText(R.string.pdf_creation_message).toString();
			args.putString(DIALOG_ARG_MESSAGE, message);
			showDialog(PDF_PROGRESS_DIALOG_ID, args);
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(ArrayList<Uri> pdfFiles) {
			dismissDialog(PDF_PROGRESS_DIALOG_ID);
			if (pdfFiles != null) {
				Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
				shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getText(R.string.share_subject));
				CharSequence seq = Html.fromHtml(mOCRText.toString());
				shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, seq);
				// shareIntent.putExtra(android.content.Intent.EXTRA_TEXT,
				// mHOCRText.toString());
				shareIntent.setType("application/pdf");

				shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, pdfFiles);
				startActivity(Intent.createChooser(shareIntent, getText(R.string.share_chooser_title)));
			} else {
				Toast.makeText(getApplicationContext(), getText(R.string.error_create_file), Toast.LENGTH_LONG).show();
			}
		}
		

		private Pair<File,File> createPDF(File dir, long documentId) {
			
			Cursor cursor = getContentResolver().query(DocumentContentProvider.CONTENT_URI, null, Columns.PARENT_ID + "=? OR " + Columns.ID + "=?", new String[] { String.valueOf(documentId), String.valueOf(documentId) }, "created ASC");
			cursor.moveToFirst();

			int index = cursor.getColumnIndex(Columns.TITLE);
			final String fileName = documentId + ".pdf";
			File outPdf = new File(dir, fileName);
			File outText = new File(dir, documentId+".txt");

			mCurrentDocumentName = fileName;
			mCurrentPageCount = cursor.getCount();
			String[] images = new String[cursor.getCount()];
			String[] hocr = new String[cursor.getCount()];
			cursor.moveToPosition(-1);
			while (cursor.moveToNext()) {
				index = cursor.getColumnIndex(Columns.HOCR_TEXT);
				hocr[cursor.getPosition()] = cursor.getString(index);
				index = cursor.getColumnIndex(Columns.PHOTO_PATH);
				Uri imageUri = Uri.parse(cursor.getString(index));
				images[cursor.getPosition()] = Util.getPathForUri(BaseDocumentActivitiy.this, imageUri);
				index = cursor.getColumnIndex(Columns.OCR_TEXT);
				final String text = cursor.getString(index);
				FileWriter writer;
				try {
					writer = new FileWriter(outText);
					writer.write(Html.fromHtml(text).toString());
					writer.close();
				} catch (IOException ioException) {
					if (outText.exists()){
						outText.delete();						
					}
					outText=null;
				}
				
				mOCRText.append(text);
			}
			cursor.close();
			Hocr2Pdf pdf = new Hocr2Pdf(this);
			pdf.hocr2pdf(images, hocr, outPdf.getPath(), false, true);
			return new Pair<File,File>(outPdf,outText);
		}

		@Override
		protected ArrayList<Uri> doInBackground(Void... params) {
			File dir = Util.getPDFDir();
			if (!dir.exists()) {
				if (!dir.mkdir()) {
					return null;
				}
			}

			ArrayList<File> files = new ArrayList<File>();
			mCurrentDocumentIndex = 0;
			for (long id : mIds) {
				final Pair<File,File> pair = createPDF(dir, id);
				final File pdf = pair.first;
				final File text =pair.second;
				if (pdf != null) {
					files.add(pdf);
				}
				if (text!=null){
					files.add(text);
				}
				mCurrentDocumentIndex++;
			}
			ArrayList<Uri> uris = new ArrayList<Uri>();
			for (File file : files) {
				Uri u = Uri.fromFile(file);
				uris.add(u);
			}
			return uris;
		}
	}

	protected class DeleteDocumentTask extends AsyncTask<Void, Void, Integer> {
		Set<Integer> mIds = new HashSet<Integer>();
		private final static int RESULT_REMOTE_EXCEPTION = -1;
		final boolean mFinishActivity;

		public DeleteDocumentTask(Set<Integer> parentDocumentIds, final boolean finishActivityAfterExecution) {
			mIds.addAll(parentDocumentIds);
			mFinishActivity = finishActivityAfterExecution;
		}

		@Override
		protected void onPreExecute() {
			Bundle args = new Bundle(2);
			args.putInt(DIALOG_ARG_MAX, mIds.size());
			String message = getText(R.string.delete_dialog_message).toString();
			args.putString(DIALOG_ARG_MESSAGE, message);
			showDialog(DELETE_PROGRESS_DIALOG_ID, args);
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (result == RESULT_REMOTE_EXCEPTION) {
				Toast.makeText(getApplicationContext(), getText(R.string.delete_error), Toast.LENGTH_LONG).show();
			} else if (result > 0) {
				// String deleteMsg = null;
				// if (result == 1) {
				// deleteMsg = getResources().getString(R.string.deleted_page);
				// } else if (result > 1) {
				// deleteMsg = getResources().getString(R.string.deleted_pages);
				// deleteMsg = String.format(deleteMsg, result);
				// }
				// Toast.makeText(getApplicationContext(), deleteMsg,
				// Toast.LENGTH_LONG).show();
			}
			dismissDialog(DELETE_PROGRESS_DIALOG_ID);
			super.onPostExecute(result);
			if (mFinishActivity) {
				finish();
			}
		}

		private int deleteDocument(Cursor c, ContentProviderClient client) throws RemoteException {
			int index = c.getColumnIndex(Columns.ID);
			int currentId = c.getInt(index);
			Uri currentDocumentUri = Uri.withAppendedPath(DocumentContentProvider.CONTENT_URI, String.valueOf(currentId));
			index = c.getColumnIndex(Columns.PHOTO_PATH);
			String imagePath = c.getString(index);
			if (imagePath != null) {
				new File(imagePath).delete();
			}
			return client.delete(currentDocumentUri, null, null);
		}

		@Override
		protected Integer doInBackground(Void... params) {

			ContentProviderClient client = getContentResolver().acquireContentProviderClient(DocumentContentProvider.CONTENT_URI);

			int count = 0;
			int progress = 0;
			for (Integer id : mIds) {
				try {
					Cursor c = client.query(DocumentContentProvider.CONTENT_URI, null, Columns.PARENT_ID + "=? OR " + Columns.ID + "=?", new String[] { String.valueOf(id), String.valueOf(id) }, Columns.PARENT_ID + " ASC");

					while (c.moveToNext()) {
						count += deleteDocument(c, client);
					}

				} catch (RemoteException exc) {
					return RESULT_REMOTE_EXCEPTION;
				}
				deleteProgressdialog.setProgress(++progress);
			}
			return count;
		}
	}

	protected class SaveDocumentTask extends AsyncTask<Void, Integer, Integer> {

		private ContentValues values = new ContentValues();
		private ArrayList<Uri> mDocumentUri = new ArrayList<Uri>();
		private String mTitle;
		private ArrayList<Spanned> mOcrText = new ArrayList<Spanned>();
		private Toast mSaveToast;

		public SaveDocumentTask(List<Uri> documentUri, List<Spanned> ocrText) {
			this.mDocumentUri.addAll(documentUri);
			this.mTitle = null;
			this.mOcrText.addAll(ocrText);
		}

		public SaveDocumentTask(Uri documentUri, String title) {
			this.mDocumentUri.add(documentUri);
			this.mTitle = title;
		}

		@Override
		protected void onPreExecute() {
			mSaveToast = Toast.makeText(BaseDocumentActivitiy.this, getText(R.string.saving_document), Toast.LENGTH_LONG);
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (result != null && result > 0) {
				mSaveToast.setText(R.string.save_success);
			} else {
				mSaveToast.setText(R.string.save_fail);
			}
			super.onPostExecute(result);
		}

		@Override
		protected Integer doInBackground(Void... params) {
			mSaveToast.show();
			int result = 0;
			for (int i = 0; i < mDocumentUri.size(); i++) {
				values.clear();
				Uri uri = mDocumentUri.get(i);
				if (mOcrText != null && i < mOcrText.size()) {
					final String text = Html.toHtml(mOcrText.get(i));
					values.put(Columns.OCR_TEXT, text);

				}
				if (mTitle != null) {
					values.put(Columns.TITLE, mTitle);
				}

				onProgressUpdate(i);
				result += getContentResolver().update(uri, values, null, null);
			}
			return result;
		}
	}

}
