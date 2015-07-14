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

package com.renard.documentview;

import com.renard.ocr.BaseDocumentActivitiy;
import com.renard.ocr.DocumentContentProvider;
import com.renard.ocr.DocumentContentProvider.Columns;
import com.renard.ocr.R;
import com.renard.ocr.help.HintDialog;
import com.renard.ocr.help.OCRLanguageAdapter;
import com.renard.util.PreferencesUtils;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

public class DocumentActivity extends BaseDocumentActivitiy implements LoaderManager.LoaderCallbacks<Cursor>, GetOpinionDialog.FeedbackDialogClickListener {

    private final static String LOG_TAG = DocumentActivity.class.getSimpleName();
    private static final String STATE_DOCUMENT_URI = "documet_uri";

    public interface DocumentContainerFragment {
        String getLangOfCurrentlyShownDocument();

        void setCursor(final Cursor cursor);

        String getTextOfAllDocuments();

        void setShowText(boolean text);

        boolean getShowText();
    }

    static final int REQUEST_CODE_TTS_CHECK = 6;
    private static final int REQUEST_CODE_OPTIONS = 4;
    private static final int REQUEST_CODE_TABLE_OF_CONTENTS = 5;
    public static final String EXTRA_ACCURACY = "ask_for_title";

    private int mParentId;
    private Cursor mCursor;
    private TtsActionCallback mActionCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_ALARM);

        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_document);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (!init(savedInstanceState)) {
            finish();
            return;
        }

        if (savedInstanceState == null) {
            showResultDialog();
        }
        setDocumentFragmentType();
        initAppIcon(HINT_DIALOG_ID);
        mActionCallback = new TtsActionCallback(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getExtras() != null) {
            setIntent(intent);
            showResultDialog();
        }
    }

    private void showResultDialog() {
        int accuracy = getIntent().getIntExtra(EXTRA_ACCURACY, -1);
        int numberOfSuccessfulScans = PreferencesUtils.getNumberOfSuccessfulScans(getApplicationContext());
        if (accuracy >= OCRResultDialog.MEDIUM_ACCURACY) {
            PreferencesUtils.setNumberOfSuccessfulScans(getApplicationContext(), ++numberOfSuccessfulScans);
        }
        if (numberOfSuccessfulScans == 2) {
            GetOpinionDialog.newInstance().show(getSupportFragmentManager(), GetOpinionDialog.TAG);
            PreferencesUtils.setNumberOfSuccessfulScans(getApplicationContext(), ++numberOfSuccessfulScans);
        } else if (accuracy > -1) {
            OCRResultDialog.newInstance(accuracy).show(getSupportFragmentManager(), OCRResultDialog.TAG);
        }

    }

    @Override
    public void onContinueClicked() {
        int accuracy = getIntent().getIntExtra(EXTRA_ACCURACY, 0);
        OCRResultDialog.newInstance(accuracy).show(getSupportFragmentManager(), OCRResultDialog.TAG);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.document_activity_options, menu);
        return true;
    }

    void exportAsPdf() {
        Set<Integer> idForPdf = new HashSet<>();
        idForPdf.add(getParentId());
        new CreatePDFTask(idForPdf).execute();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }

        if (itemId == R.id.item_view_mode) {
            DocumentContainerFragment fragment = (DocumentContainerFragment) getSupportFragmentManager().findFragmentById(R.id.document_fragment_container);
            fragment.setShowText(!fragment.getShowText());
            return true;
        } else if (itemId == R.id.item_text_options) {
            Intent i = new Intent(this, TextOptionsActivity.class);
            startActivityForResult(i, REQUEST_CODE_OPTIONS);
            return true;
        } else if (itemId == R.id.item_content) {
            Intent tocIndent = new Intent(this, TableOfContentsActivity.class);
            Uri uri = Uri.parse(DocumentContentProvider.CONTENT_URI + "/" + getParentId());
            tocIndent.setData(uri);
            startActivityForResult(tocIndent, REQUEST_CODE_TABLE_OF_CONTENTS);
            return true;
        } else if (itemId == R.id.item_delete) {
            Set<Integer> idToDelete = new HashSet<>();
            idToDelete.add(getParentId());
            new DeleteDocumentTask(idToDelete, true).execute();
            return true;
        } else if (itemId == R.id.item_export_as_pdf) {
            exportAsPdf();
            return true;
        } else if (itemId == R.id.item_copy_to_clipboard) {
            copyTextToClipboard();
            return true;
        } else if (itemId == R.id.item_text_to_speech) {
            startTextToSpeech();
            return true;
        } else if (itemId == R.id.item_share_text) {
            shareText();
            return true;

        }
        return super.onOptionsItemSelected(item);
    }


    void startTextToSpeech() {
        startSupportActionMode(mActionCallback);
    }


    void copyTextToClipboard() {
        final String text = getPlainDocumentText();
        //some apps don't like html text
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//			copyHtmlTextToClipboard(htmlText, text);
//		} else 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            copyTextToClipboardNewApi(text);
        } else {
            copyTextToClipboard(text);
        }

        Toast.makeText(this, getString(R.string.text_was_copied_to_clipboard), Toast.LENGTH_LONG).show();
    }

    String getLanguageOfDocument() {
        return getDocumentContainer().getLangOfCurrentlyShownDocument();
    }

    String getPlainDocumentText() {
        final String htmlText = getDocumentContainer().getTextOfAllDocuments();
        if (htmlText != null) {
            return Html.fromHtml(htmlText).toString();
        } else {
            return null;
        }
    }

    void shareText() {
        String shareBody = getPlainDocumentText();
        if (shareBody == null) {
            Toast.makeText(DocumentActivity.this, R.string.empty_document, Toast.LENGTH_LONG).show();
            return;
        }

        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, R.string.share_subject);
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_chooser_title)));
    }


    @SuppressLint("NewApi")
    private void copyTextToClipboardNewApi(final String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(getString(R.string.app_name), text);
        clipboard.setPrimaryClip(clip);
    }

    @SuppressWarnings("deprecation")
    private void copyTextToClipboard(String text) {
        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setText(text);
    }

    public void onTtsLanguageChosen(OCRLanguageAdapter.OCRLanguage lang) {
        mActionCallback.onTtsLanguageChosen(lang);
    }

    public void onTtsCancelled() {
        mActionCallback.onTtsCancelled();
    }

    public boolean isTtsLanguageAvailable(OCRLanguageAdapter.OCRLanguage lang) {
        return mActionCallback.isLanguageAvailable(lang);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_TTS_CHECK) {
            mActionCallback.onTtsCheck(resultCode);
        } else if (requestCode == REQUEST_CODE_OPTIONS) {
            Fragment frag = getSupportFragmentManager().findFragmentById(R.id.document_fragment_container);
            if (frag instanceof DocumentPagerFragment) {
                DocumentPagerFragment pagerFragment = (DocumentPagerFragment) frag;
                pagerFragment.applyTextPreferences();
            }
        } else if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_TABLE_OF_CONTENTS:
                    int documentPos = data.getIntExtra(TableOfContentsActivity.EXTRA_DOCUMENT_POS, -1);
                    DocumentContainerFragment fragment = getDocumentContainer();
                    if (fragment != null) {
                        ((DocumentPagerFragment) fragment).setDisplayedPage(documentPos);
                    }
                    break;
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case HINT_DIALOG_ID:
                return HintDialog.createDialog(this, R.string.document_help_title, "file:///android_res/raw/document_help.html");
        }
        return super.onCreateDialog(id, args);
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putParcelable(STATE_DOCUMENT_URI, getIntent().getData());
    }


    private boolean init(Bundle savedInstanceState) {
        Uri data = getIntent().getData();
        if (data == null && savedInstanceState != null) {
            data = savedInstanceState.getParcelable(STATE_DOCUMENT_URI);
        }
        if (data == null) {
            return false;
        }
        String id = data.getLastPathSegment();
        int parentId = getParentId(data);
        // Base class needs that value
        if (parentId == -1) {
            mParentId = Integer.parseInt(id);
        } else {
            mParentId = parentId;
        }

        getSupportLoaderManager().initLoader(0, null, this);
        return true;
    }

    private int getParentId(Uri documentUri) {
        int parentId = -1;
        Cursor c = getContentResolver().query(documentUri, new String[]{Columns.PARENT_ID}, null, null, null);
        if (!c.moveToFirst()) {
            return parentId;
        }
        int index = c.getColumnIndex(Columns.PARENT_ID);
        if (index > -1) {
            parentId = c.getInt(index);
        }
        c.close();
        return parentId;
    }

    @Override
    protected int getParentId() {
        return mParentId;
    }

    public DocumentContainerFragment getDocumentContainer() {
        return (DocumentContainerFragment) getSupportFragmentManager().findFragmentById(R.id.document_fragment_container);
    }

    private void setDocumentFragmentType() {
        // Check what fragment is shown, replace if needed.
        DocumentContainerFragment fragment = getDocumentContainer();
        DocumentContainerFragment newFragment = null;
        if (fragment == null) {
            newFragment = new DocumentPagerFragment();
        }
        if (newFragment != null) {
            if (mCursor != null) {
                newFragment.setCursor(mCursor);
            }
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.document_fragment_container, (Fragment) newFragment);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
        }

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.i(LOG_TAG, "onLoadFinished");
        mCursor = cursor;
        DocumentContainerFragment frag = getDocumentContainer();
        frag.setCursor(cursor);
        if (getIntent().getData() != null) {
            String id = getIntent().getData().getLastPathSegment();
            DocumentPagerFragment documentContainer = (DocumentPagerFragment) getDocumentContainer();
            documentContainer.setDisplayedPageByDocumentId(Integer.parseInt(id));
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursor = null;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        Log.i(LOG_TAG, "onCreateLoader");

        return new CursorLoader(this, DocumentContentProvider.CONTENT_URI, null, Columns.PARENT_ID + "=? OR " + Columns.ID + "=?", new String[]{String.valueOf(mParentId),
                String.valueOf(mParentId)}, "created ASC");
    }

}
