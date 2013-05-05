package com.renard.model;

import java.util.ArrayList;

import android.database.Cursor;

import com.renard.ocr.DocumentContentProvider;

public class Document {

	public int id;
	public int parentId;
	public long created;
	public String photoPath;
	public String title;
	public String ocrText;
	public String hocrText;
	public String pdfPath;
	public boolean changed = false;

	public static ArrayList<Document> fromCursor(final Cursor c) {
		final int count = c.getCount();
		c.moveToPosition(-1);
		final ArrayList<Document> result = new ArrayList<Document>();
		for (int i = 0; i < count; i++) {
			c.moveToNext();
			final Document d = new Document();
			int index = c.getColumnIndex(DocumentContentProvider.Columns.ID);
			if (index > -1) {
				d.id = c.getInt(index);
			}
			index = c.getColumnIndex(DocumentContentProvider.Columns.PARENT_ID);
			if (index > -1) {
				d.parentId = c.getInt(index);
			}
			index = c.getColumnIndex(DocumentContentProvider.Columns.CREATED);
			if (index > -1) {
				d.created = c.getLong(index);
			}
			index = c.getColumnIndex(DocumentContentProvider.Columns.PHOTO_PATH);
			if (index > -1) {
				d.photoPath = c.getString(index);
			}
			index = c.getColumnIndex(DocumentContentProvider.Columns.TITLE);
			if (index > -1) {
				d.title = c.getString(index);
			}
			index = c.getColumnIndex(DocumentContentProvider.Columns.OCR_TEXT);
			if (index > -1) {
				d.ocrText = c.getString(index);
			}
			index = c.getColumnIndex(DocumentContentProvider.Columns.HOCR_TEXT);
			if (index > -1) {
				d.hocrText = c.getString(index);
			}
			index = c.getColumnIndex(DocumentContentProvider.Columns.PDF_URI);
			if (index > -1) {
				d.pdfPath = c.getString(index);
			}
			result.add(d);
		}
		return result;
	}

}
