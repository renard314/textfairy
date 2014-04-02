/*
 * Copyright (C) 2012,2013 Renard Wellnitz.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.renard.ocr;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import android.view.Menu;
import android.view.MenuItem;
import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.ReadFile;
import com.renard.documentview.DocumentActivity;
import com.renard.ocr.DocumentContentProvider.Columns;
import com.renard.ocr.cropimage.CropImage;
import com.renard.ocr.cropimage.MonitoredActivity;
import com.renard.pdf.Hocr2Pdf;
import com.renard.pdf.Hocr2Pdf.PDFProgressListener;
import com.renard.util.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * activities which extend this activity can create a new document. this class
 * also containes the code for functionality which is shared by
 * {@link DocumentGridActivity} and {@link DocumentActivity}
 *
 * @author renard
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

    private static final String DATE_CAMERA_INTENT_STARTED_STATE = "com.renard.ocr.android.photo.TakePhotoActivity.dateCameraIntentStarted";
    private static Date dateCameraIntentStarted = null;
    private static final String CAMERA_PIC_URI_STATE = "com.renard.ocr.android.photo.TakePhotoActivity.CAMERA_PIC_URI_STATE";
    private static Uri cameraPicUri = null;
    private static final String ROTATE_X_DEGREES_STATE = "com.renard.ocr.android.photo.TakePhotoActivity.ROTATE_X_DEGREES_STATE";
    private static int rotateXDegrees = 0;

    protected enum PixLoadStatus {
       IMAGE_FORMAT_UNSUPPORTED, IMAGE_NOT_32_BIT,IMAGE_COULD_NOT_BE_READ, MEDIA_STORE_RETURNED_NULL, IMAGE_DOES_NOT_EXIST, SUCCESS, IO_ERROR, CAMERA_APP_NOT_FOUND, CAMERA_APP_ERROR, CAMERA_NO_IMAGE_RETURNED

    }

    private static class CameraResult {
        public CameraResult(int requestCode, int resultCode, Intent data) {
            mRequestCode = requestCode;
            mResultCode = resultCode;
            mData = data;
        }

        private int mRequestCode;
        private int mResultCode;
        private Intent mData;
    }

    protected abstract int getParentId();

    ProgressDialog pdfProgressDialog;
    ProgressDialog deleteProgressdialog;
    private AsyncTask<Void, Void, Pair<Pix, PixLoadStatus>> mBitmapLoadTask;
    private CameraResult mCameraResult;

    protected void startGallery() {
        cameraPicUri = null;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT, null);
        i.setType("image/*");
        // File photo = getTmpPhotoFile();
        // i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
        startActivityForResult(i, REQUEST_CODE_PICK_PHOTO);
    }

    protected void startCamera() {
        try {
            cameraPicUri = null;
            dateCameraIntentStarted = new Date();
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Create an image file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";

            File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File image = null;
            try {
                if (!storageDir.exists()){
                    storageDir.mkdirs();
                }
                image = new File(storageDir,imageFileName + ".jpg");
                if (image.exists()){
                    image.createNewFile();
                }
                cameraPicUri = Uri.fromFile(image);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPicUri);
                startActivityForResult(intent, REQUEST_CODE_MAKE_PHOTO);
            } catch (IOException e) {
                showFileError(PixLoadStatus.IO_ERROR);
            }

        } catch (ActivityNotFoundException e) {
            showFileError(PixLoadStatus.CAMERA_APP_NOT_FOUND);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if (dateCameraIntentStarted != null) {
            savedInstanceState.putLong(DATE_CAMERA_INTENT_STARTED_STATE, dateCameraIntentStarted.getTime());
        }
        if (cameraPicUri != null) {
            savedInstanceState.putString(CAMERA_PIC_URI_STATE, cameraPicUri.toString());
        }
        savedInstanceState.putInt(ROTATE_X_DEGREES_STATE, rotateXDegrees);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.containsKey(DATE_CAMERA_INTENT_STARTED_STATE)) {
            dateCameraIntentStarted = new Date(savedInstanceState.getLong(DATE_CAMERA_INTENT_STARTED_STATE));
        }
        if (savedInstanceState.containsKey(CAMERA_PIC_URI_STATE)) {
            cameraPicUri = Uri.parse(savedInstanceState.getString(CAMERA_PIC_URI_STATE));
        }
        rotateXDegrees = savedInstanceState.getInt(ROTATE_X_DEGREES_STATE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.item_camera) {
            startCamera();
            return true;
        } else if (itemId == R.id.item_gallery) {
            startGallery();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.base_document_activity_options, menu);
        return true;
    }

    private void onTakePhotoActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_MAKE_PHOTO) {
                Cursor myCursor = null;
                Date dateOfPicture = null;
                //check if there is a file at the uri we specified
                if (cameraPicUri!=null){
                    File f = new File(cameraPicUri.getPath());
                    if (f.isFile() && f.exists() && f.canRead()){
                        //all is well
                        loadBitmapFromContentUri(cameraPicUri);
                        return;
                    }

                }
                //try to look up the image by querying the media content provider
                try {
                    // Create a Cursor to obtain the file Path for the large
                    // image
                    String[] largeFileProjection = {MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.DATA, MediaStore.Images.ImageColumns.ORIENTATION,
                            MediaStore.Images.ImageColumns.DATE_TAKEN};
                    String largeFileSort = MediaStore.Images.ImageColumns._ID + " DESC";
                    myCursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, largeFileProjection, null, null, largeFileSort);
                    myCursor.moveToFirst();
                    // This will actually give you the file path location of the
                    // image.
                    String largeImagePath = myCursor.getString(myCursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATA));
                    Uri tempCameraPicUri = Uri.fromFile(new File(largeImagePath));
                    if (tempCameraPicUri != null) {
                        dateOfPicture = new Date(myCursor.getLong(myCursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN)));

                        if (dateOfPicture.getTime()==0 || (dateOfPicture != null && dateOfPicture.after(dateCameraIntentStarted))) {
                            cameraPicUri = tempCameraPicUri;
                            rotateXDegrees = myCursor.getInt(myCursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.ORIENTATION));
                        }
                    }
                } catch (Exception e) {
                    // Log.w("TAG",
                    // "Exception - optaining the picture's uri failed: " +
                    // e.toString());
                } finally {
                    if (myCursor != null) {
                        myCursor.close();
                    }
                }
            }

            if (cameraPicUri == null) {
                try {
                    cameraPicUri = intent.getData();
                } catch (Exception e) {
                    showFileError(PixLoadStatus.CAMERA_APP_ERROR);
                }
            }

            if (cameraPicUri != null) {
                loadBitmapFromContentUri(cameraPicUri);
                return;
            } else {
                showFileError(PixLoadStatus.CAMERA_NO_IMAGE_RETURNED);
            }
        }
    }

    protected void loadBitmapFromContentUri(final Uri cameraPicUri) {
        if (mBitmapLoadTask != null) {
            mBitmapLoadTask.cancel(true);
        }
        mBitmapLoadTask = new AsyncTask<Void, Void, Pair<Pix, PixLoadStatus>>() {
            ProgressDialogFragment progressDialog;

            protected void onPreExecute() {
                progressDialog = ProgressDialogFragment.newInstance(R.string.please_wait, R.string.loading_image);
                // getSupportFragmentManager().beginTransaction().show(progressDialog).commit();
                progressDialog.show(getSupportFragmentManager(), "load_image_progress");
            }

            ;

            protected void onPostExecute(Pair<Pix, PixLoadStatus> p) {
                if (progressDialog != null) {
                    try {
                        progressDialog.dismiss();
                        getSupportFragmentManager().beginTransaction().remove(progressDialog).commitAllowingStateLoss();
                    } catch (NullPointerException e) {
                        // workaround strange playstore crash

                    } catch (IllegalStateException e) {
                        // workaround strange playstore crash

                    }
                }
                if (p.second == PixLoadStatus.SUCCESS) {
                    Intent actionIntent = new Intent(BaseDocumentActivitiy.this, CropImage.class);
                    actionIntent.putExtra(EXTRA_NATIVE_PIX, p.first.getNativePix());
                    actionIntent.putExtra(EXTRA_ROTATION, rotateXDegrees);
                    startActivityForResult(actionIntent, REQUEST_CODE_CROP_PHOTO);
                } else {
                    showFileError(p.second);
                }
            }

            ;

            @Override
            protected Pair<Pix, PixLoadStatus> doInBackground(Void... params) {
                try {
                    Pix p = null;
                    String pathForUri = Util.getPathForUri(BaseDocumentActivitiy.this, cameraPicUri);
                    // MediaStore loves to crash with an oom exception. So we
                    // try to load bitmap nativly if it is on internal storage
                    if (pathForUri != null && pathForUri.startsWith("http")) {
                        Bitmap b = MediaStore.Images.Media.getBitmap(getContentResolver(), cameraPicUri);
                        if (b != null) {
                            if (b.getConfig()!= Bitmap.Config.ARGB_8888){
                                return Pair.create(null, PixLoadStatus.IMAGE_NOT_32_BIT);
                            }
                            p = ReadFile.readBitmap(b);
                            b.recycle();
                        } else {
                            return Pair.create(null, PixLoadStatus.MEDIA_STORE_RETURNED_NULL);
                        }
                    } else if (pathForUri != null) {
                        File imageFile = new File(pathForUri);
                        if (imageFile.exists()) {
                            p = ReadFile.readFile(imageFile);
                            if (p==null){
                                return Pair.create(null, PixLoadStatus.IMAGE_FORMAT_UNSUPPORTED);
                            }
                        } else {
                            return Pair.create(null, PixLoadStatus.IMAGE_DOES_NOT_EXIST);
                        }
                    } else if (cameraPicUri.toString().startsWith("content")) {
                        InputStream stream = getContentResolver().openInputStream(cameraPicUri);
                        p = ReadFile.readMem(Util.toByteArray(stream));
                        if (p==null){
                            return Pair.create(null, PixLoadStatus.IMAGE_FORMAT_UNSUPPORTED);
                        }
                    } else {
                        return Pair.create(null, PixLoadStatus.IO_ERROR);
                    }

                    return Pair.create(p, PixLoadStatus.SUCCESS);
                } catch (FileNotFoundException e) {
                    return Pair.create(null, PixLoadStatus.IMAGE_DOES_NOT_EXIST);
                } catch (IOException e) {
                    return Pair.create(null, PixLoadStatus.IO_ERROR);
                }
            }
        }.execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (RESULT_OK == resultCode) {
            switch (requestCode) {
                case REQUEST_CODE_CROP_PHOTO: {
                    int nativePix = data.getIntExtra(EXTRA_NATIVE_PIX, 0);
                    Intent intent = new Intent(this, OCRActivity.class);
                    intent.putExtra(EXTRA_NATIVE_PIX, nativePix);
                    intent.putExtra(OCRActivity.EXTRA_PARENT_DOCUMENT_ID, getParentId());
                    startActivityForResult(intent, REQUEST_CODE_OCR);
                    break;
                }
                case REQUEST_CODE_MAKE_PHOTO:
                case REQUEST_CODE_PICK_PHOTO:
                    mCameraResult = new CameraResult(requestCode, resultCode, data);
                    break;
            }
        }
    }

    protected void onPostResume() {
        super.onPostResume();
        if (mCameraResult != null) {
            onTakePhotoActivityResult(mCameraResult.mRequestCode, mCameraResult.mResultCode, mCameraResult.mData);
            mCameraResult = null;
        }
    }

    ;

    private void showFileError(PixLoadStatus status) {
        showFileError(status, null);
    }

    protected void showFileError(PixLoadStatus second, OnClickListener positiveListener) {
        int textId;
        switch (second) {
            case IMAGE_NOT_32_BIT:
                textId = R.string.image_not_32_bit;
                break;
            case IMAGE_FORMAT_UNSUPPORTED:
                textId = R.string.image_format_unsupported;
                break;
            case IMAGE_COULD_NOT_BE_READ:
                textId = R.string.image_could_not_be_read;
                break;
            case IMAGE_DOES_NOT_EXIST:
                textId = R.string.image_does_not_exist;
                break;
            case IO_ERROR:
                textId = R.string.gallery_io_error;
                break;
            case CAMERA_APP_NOT_FOUND:
                textId = R.string.camera_app_not_found;
                break;
            case MEDIA_STORE_RETURNED_NULL:
                textId = R.string.media_store_returned_null;
                break;
            case CAMERA_APP_ERROR:
                textId = R.string.camera_app_error;
                break;
            case CAMERA_NO_IMAGE_RETURNED:
                textId = R.string.camera_no_image_returned;
                break;
            default:
                textId = R.string.error_could_not_take_photo;
        }

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.error_title);
        final TextView textview = new TextView(this);
        textview.setText(textId);
        alert.setView(textview);
        alert.setPositiveButton(android.R.string.ok, positiveListener);
        alert.show();
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
            SaveDocumentTask saveTask = new SaveDocumentTask(this, documentUri, newTitle);
            saveTask.execute();
        }

    }

    /**
     * *******************************************
     * <p/>
     * ASYNC TASKS
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

        private Pair<File, File> createPDF(File dir, long documentId) {

            Cursor cursor = getContentResolver().query(DocumentContentProvider.CONTENT_URI, null, Columns.PARENT_ID + "=? OR " + Columns.ID + "=?",
                    new String[]{String.valueOf(documentId), String.valueOf(documentId)}, "created ASC");
            cursor.moveToFirst();

            int index = cursor.getColumnIndex(Columns.TITLE);
            final String fileName = documentId + ".pdf";
            File outPdf = new File(dir, fileName);
            File outText = new File(dir, documentId + ".txt");

            mCurrentDocumentName = fileName;
            mCurrentPageCount = cursor.getCount();
            String[] images = new String[cursor.getCount()];
            String[] hocr = new String[cursor.getCount()];
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                int hocrIndex = cursor.getColumnIndex(Columns.HOCR_TEXT);
                index = cursor.getColumnIndex(Columns.PHOTO_PATH);
                Uri imageUri = Uri.parse(cursor.getString(index));
                images[cursor.getPosition()] = Util.getPathForUri(BaseDocumentActivitiy.this, imageUri);
                index = cursor.getColumnIndex(Columns.OCR_TEXT);
                final String text = cursor.getString(index);
                if (text != null && text.length() > 0) {
                    hocr[cursor.getPosition()] = cursor.getString(hocrIndex);
                    FileWriter writer;
                    try {
                        writer = new FileWriter(outText);
                        final String s = Html.fromHtml(text).toString();
                        writer.write(s);
                        writer.close();
                    } catch (IOException ioException) {
                        if (outText.exists()) {
                            outText.delete();
                        }
                        outText = null;
                    }

                    mOCRText.append(text);
                } else {
                    hocr[cursor.getPosition()] = "";
                }
            }
            cursor.close();
            Hocr2Pdf pdf = new Hocr2Pdf(this);
            pdf.hocr2pdf(images, hocr, outPdf.getPath(), true, true);
            return new Pair<File, File>(outPdf, outText);
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
                final Pair<File, File> pair = createPDF(dir, id);
                final File pdf = pair.first;
                final File text = pair.second;
                if (pdf != null) {
                    files.add(pdf);
                }
                if (text != null) {
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
                    Cursor c = client.query(DocumentContentProvider.CONTENT_URI, null, Columns.PARENT_ID + "=? OR " + Columns.ID + "=?",
                            new String[]{String.valueOf(id), String.valueOf(id)}, Columns.PARENT_ID + " ASC");

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

    public static class SaveDocumentTask extends AsyncTask<Void, Integer, Integer> {

        private final Context mContext;
        private ContentValues values = new ContentValues();
        private ArrayList<Uri> mDocumentUri = new ArrayList<Uri>();
        private String mTitle;
        private ArrayList<Spanned> mOcrText = new ArrayList<Spanned>();
        private Toast mSaveToast;

        public SaveDocumentTask(Context context, List<Uri> documentUri, List<Spanned> ocrText) {
            mContext = context;
            this.mDocumentUri.addAll(documentUri);
            this.mTitle = null;
            this.mOcrText.addAll(ocrText);
        }

        public SaveDocumentTask(Context context, Uri documentUri, String title) {
            mContext = context;
            this.mDocumentUri.add(documentUri);
            this.mTitle = title;
        }

        @Override
        protected void onPreExecute() {
            mSaveToast = Toast.makeText(mContext, mContext.getText(R.string.saving_document), Toast.LENGTH_LONG);
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result != null && result > 0) {
                mSaveToast.setText(R.string.save_success);
            } else {
                mSaveToast.setText(R.string.save_fail);
            }
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
                result += mContext.getContentResolver().update(uri, values, null, null);
            }
            return result;
        }
    }

}
