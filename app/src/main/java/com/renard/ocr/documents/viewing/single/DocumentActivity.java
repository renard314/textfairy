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

package com.renard.ocr.documents.viewing.single;

import com.renard.ocr.HintDialog;
import com.renard.ocr.R;
import com.renard.ocr.documents.creation.NewDocumentActivity;
import com.renard.ocr.documents.viewing.DocumentContentProvider;
import com.renard.ocr.documents.viewing.DocumentContentProvider.Columns;
import com.renard.ocr.util.PreferencesUtils;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import java.util.HashSet;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DocumentActivity extends NewDocumentActivity implements LoaderManager.LoaderCallbacks<Cursor>, GetOpinionDialog.FeedbackDialogClickListener {

    public static final String OCR_RESULT_DIALOG = "Ocr Result Dialog";
    private final static String LOG_TAG = DocumentActivity.class.getSimpleName();
    private static final String STATE_DOCUMENT_URI = "documet_uri";
    public static final int DOCUMENT_CURSOR_LOADER_ID = 45678998;
    private boolean mIsCursorLoaded = false;
    private boolean mMoveToPageFromIntent;
    private final Translator mTranslator = new Translator();


    @BindView(R.id.expand_collapse_icon)
    protected View mCollapseExpandIcon;
    @BindView(R.id.bottom_sheet)
    protected LinearLayout mBottomSheet;
    @BindView(R.id.view_mode_switcher)
    protected ViewSwitcher mViewModeSwitcher;

    public interface DocumentContainerFragment {
        
        String getTextOfCurrentlyShownDocument();

        int getDocumentCount();

        void setCursor(final Cursor cursor);

        String getTextOfAllDocuments();

        void setShowText(boolean text);

        boolean getShowText();
    }

    private static final int REQUEST_CODE_OPTIONS = 4;
    private static final int REQUEST_CODE_TABLE_OF_CONTENTS = 5;
    public static final String EXTRA_ACCURACY = "extra_accuracy";
    public static final String EXTRA_LANGUAGE = "extra_language";

    private int mParentId;
    private Cursor mCursor;

    @Override
    public String getScreenName() {
        return "";
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(R.layout.activity_document);
        ButterKnife.bind(this);
        if (!init(savedInstanceState)) {
            finish();
            return;
        }

        if (savedInstanceState == null && isStartedAfterAScan(getIntent())) {
            showResultDialog();
        } else {
            mAnalytics.sendScreenView("Document");
        }
        setDocumentFragmentType();
        initToolbar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        initBottomSheet();
    }

    private void initBottomSheet() {
        BottomSheetBehavior behavior = BottomSheetBehavior.from(mBottomSheet);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                mCollapseExpandIcon.setRotation(slideOffset * 180);
            }
        });
    }

    @Override
    protected int getHintDialogId() {
        return HINT_DIALOG_ID;
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (isStartedAfterAScan(intent)) {
            mMoveToPageFromIntent = true;
            setIntent(intent);
            showResultDialog();
        } else {
            mAnalytics.sendScreenView("Document");
        }

    }


    @OnClick(R.id.switch_view_to_image)
    public void onSwitchToImage() {
        toggleViewMode();
    }

    @OnClick(R.id.switch_view_to_text)
    public void onSwitchToText() {
        toggleViewMode();
    }

    private void toggleViewMode() {
        DocumentContainerFragment fragment = (DocumentContainerFragment) getSupportFragmentManager().findFragmentById(R.id.document_fragment_container);
        final boolean showText = fragment.getShowText();
        fragment.setShowText(!showText);
        mAnalytics.optionDocumentViewMode(!showText);
        if (showText) {
            mViewModeSwitcher.setDisplayedChild(1);
        } else {
            mViewModeSwitcher.setDisplayedChild(0);
        }

    }


    @OnClick(R.id.share_text)
    public void onShareText() {
        mAnalytics.optionsShareText();
        shareText();
    }

    @OnClick(R.id.expand_collapse_button)
    public void onExpandCollapseButtonClicked() {
        BottomSheetBehavior behavior = BottomSheetBehavior.from(mBottomSheet);
        if (behavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else if (behavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

    }

    @OnClick(R.id.pdf)
    public void onCreatePdf() {
        mAnalytics.optionsCreatePdf();
        exportAsPdf();
    }

    @OnClick(R.id.copy_to_clipboard)
    public void onCopyToClipboard() {
        mAnalytics.optionsCopyToClipboard();
        copyTextToClipboard();
    }

    @OnClick(R.id.translate)
    public void onTranslate() {
        mAnalytics.optionTranslateText();
        translateText();
    }

    private boolean isStartedAfterAScan(Intent intent) {
        return intent.getExtras() != null && getIntent().hasExtra(EXTRA_ACCURACY);
    }

    private void showResultDialog() {
        int accuracy = getIntent().getIntExtra(EXTRA_ACCURACY, -1);
        String language = getIntent().getStringExtra(EXTRA_LANGUAGE);
        mAnalytics.sendOcrResult(language, accuracy);

        int numberOfSuccessfulScans = PreferencesUtils.getNumberOfSuccessfulScans(getApplicationContext());
        if (accuracy >= OCRResultDialog.MEDIUM_ACCURACY) {
            PreferencesUtils.setNumberOfSuccessfulScans(getApplicationContext(), ++numberOfSuccessfulScans);
        }
        if (numberOfSuccessfulScans == 2) {
            GetOpinionDialog.newInstance(language).show(getSupportFragmentManager(), GetOpinionDialog.TAG);
            PreferencesUtils.setNumberOfSuccessfulScans(getApplicationContext(), ++numberOfSuccessfulScans);
        } else if (accuracy > -1) {
            OCRResultDialog.newInstance(accuracy, language).show(getSupportFragmentManager(), OCRResultDialog.TAG);
            mAnalytics.sendScreenView(OCR_RESULT_DIALOG);
        }
        expandBottomSheet();
    }

    private void expandBottomSheet() {
        BottomSheetBehavior behavior = BottomSheetBehavior.from(mBottomSheet);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        mCollapseExpandIcon.setRotation(180);
    }

    @Override
    public void onContinueClicked() {
        int accuracy = getIntent().getIntExtra(EXTRA_ACCURACY, 0);
        final String languageOfDocument = getIntent().getStringExtra(EXTRA_LANGUAGE);
        OCRResultDialog.newInstance(accuracy, languageOfDocument).show(getSupportFragmentManager(), OCRResultDialog.TAG);
        mAnalytics.sendScreenView(OCR_RESULT_DIALOG);
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

        if (itemId == R.id.item_text_options) {
            Intent i = new Intent(this, TextSettingsActivity.class);
            startActivityForResult(i, REQUEST_CODE_OPTIONS);
            mAnalytics.optionTextSettings();
            return true;
        } else if (itemId == R.id.item_content) {
            Intent tocIndent = new Intent(this, TableOfContentsActivity.class);
            Uri uri = Uri.parse(DocumentContentProvider.CONTENT_URI + "/" + getParentId());
            tocIndent.setData(uri);
            startActivityForResult(tocIndent, REQUEST_CODE_TABLE_OF_CONTENTS);
            mAnalytics.optionTableOfContents();
            return true;
        } else if (itemId == R.id.item_delete) {
            deleteDocument();
            mAnalytics.optionsDeleteDocument();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void translateText() {
        String documentText = getPlainDocumentText();
        if (documentText == null) {
            Toast.makeText(DocumentActivity.this, R.string.empty_document, Toast.LENGTH_LONG).show();
            return;
        }
        mTranslator.startTranslation(this, documentText);

    }

    @Override
    protected synchronized void onDestroy() {
        super.onDestroy();
        BottomSheetBehavior behavior = BottomSheetBehavior.from(mBottomSheet);
        behavior.setBottomSheetCallback(null);
    }

    private void deleteDocument() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.delete_whole_document);
        builder.setMessage(getString(R.string.delete_document_message));
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Set<Integer> idToDelete = new HashSet<>();
                idToDelete.add(getParentId());
                new DeleteDocumentTask(idToDelete, true).execute();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }


    void copyTextToClipboard() {
        final String text = getPlainDocumentText();
        if (text == null) {
            Toast.makeText(this, getString(R.string.empty_document), Toast.LENGTH_LONG).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            copyTextToClipboardNewApi(text);
        } else {
            copyTextToClipboard(text);
        }

        Toast.makeText(this, getString(R.string.text_was_copied_to_clipboard), Toast.LENGTH_LONG).show();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPTIONS) {
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
                return HintDialog.createDialog(this, R.string.document_help_title, R.raw.document_help);
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

        mIsCursorLoaded = false;
        getSupportLoaderManager().initLoader(DOCUMENT_CURSOR_LOADER_ID, null, this);
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
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.commit();
        }

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.i(LOG_TAG, "onLoadFinished");
        mCursor = cursor;
        DocumentContainerFragment frag = getDocumentContainer();
        frag.setCursor(cursor);
        if (getIntent().getData() != null && !mIsCursorLoaded || mMoveToPageFromIntent) {
            mMoveToPageFromIntent = false;
            mIsCursorLoaded = true;
            String id = getIntent().getData().getLastPathSegment();
            DocumentPagerFragment documentContainer = (DocumentPagerFragment) getDocumentContainer();
            documentContainer.setDisplayedPageByDocumentId(Integer.parseInt(id));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.i(LOG_TAG, "onLoaderReset");
        mCursor = null;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        Log.i(LOG_TAG, "onCreateLoader");

        return new CursorLoader(this, DocumentContentProvider.CONTENT_URI, null, Columns.PARENT_ID + "=? OR " + Columns.ID + "=?", new String[]{String.valueOf(mParentId),
                String.valueOf(mParentId)}, "created ASC");
    }

}
