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
