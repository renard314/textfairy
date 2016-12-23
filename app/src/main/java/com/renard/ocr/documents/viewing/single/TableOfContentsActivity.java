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

import com.renard.ocr.documents.viewing.DocumentContentProvider;
import com.renard.ocr.documents.viewing.DocumentContentProvider.Columns;
import com.renard.ocr.HintDialog;
import com.renard.ocr.MonitoredActivity;
import com.renard.ocr.R;
import com.renard.ocr.documents.viewing.single.SimpleDocumentAdapter.DocumentViewHolder;
import com.renard.ocr.documents.viewing.single.SimpleDocumentAdapter.ViewBinder;

import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class TableOfContentsActivity extends MonitoredActivity implements LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener {
    private final static String[] PROJECTION = {Columns.ID, Columns.TITLE, Columns.OCR_TEXT, Columns.CREATED};


    public final static String EXTRA_DOCUMENT_ID = "document_id";
    public final static String EXTRA_DOCUMENT_POS = "document_pos";
    private ListView mList;
    private static final int HINT_DIALOG_ID = 2;


    @Override
    public String getScreenName() {
        return "Table of contents";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_of_contents);
        initToolbar();
        setToolbarMessage(R.string.toc_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (getIntent() == null || getIntent().getData() == null) {
            finish();
            return;
        }
        mList = (ListView) findViewById(R.id.list);
        mList.setOnItemClickListener(this);
        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected int getHintDialogId() {
        return -1;
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case HINT_DIALOG_ID:
                return HintDialog.createDialog(this, R.string.toc_help_title, R.raw.toc_help);
        }
        return super.onCreateDialog(id, args);
    }


    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        final Uri documentUri = getIntent().getData();
        final String selection = DocumentContentProvider.Columns.PARENT_ID + "=? OR " + Columns.ID + "=?";
        final String[] args = new String[]{documentUri.getLastPathSegment(), documentUri.getLastPathSegment()};
        return new CursorLoader(this, DocumentContentProvider.CONTENT_URI, PROJECTION, selection, args, "created ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {

        final SimpleDocumentAdapter adapter = new SimpleDocumentAdapter(this, R.layout.item_list_table_of_contents_element, cursor, new ViewBinder() {

            @Override
            public void bind(View v, DocumentViewHolder holder, String title, CharSequence formattedDate, String text, int position, final int id) {
                holder.date.setText(formattedDate);
                holder.text.setText(title);
                final String pageNo = String.valueOf(position + 1);
                holder.mPageNumber.setText(pageNo);
            }

        });
        mList.setAdapter(adapter);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent data = new Intent();
        data.putExtra(EXTRA_DOCUMENT_ID, (int) id);
        data.putExtra(EXTRA_DOCUMENT_POS, position);
        setResult(RESULT_OK, data);
        finish();
    }
}
