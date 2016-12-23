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
package com.renard.ocr.main_menu.language;

import com.renard.ocr.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class OCRLanguageAdapter extends BaseAdapter implements ListAdapter {

    private static Comparator<OcrLanguage> mLanguageComparator = new Comparator<OcrLanguage>() {

        @Override
        public int compare(OcrLanguage lhs, OcrLanguage rhs) {
            return lhs.getDisplayText().compareTo(rhs.getDisplayText());
        }
    };

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }


    private static class ViewHolder {
        ViewFlipper mFlipper;
        TextView mTextViewLanguage;
    }

    private final List<OcrLanguage> mLanguages = new ArrayList<OcrLanguage>();
    private final LayoutInflater mInflater;
    private boolean mShowOnlyLanguageNames;

    public OCRLanguageAdapter(final Context context, boolean showOnlyLanguageNames) {
        this.mShowOnlyLanguageNames = showOnlyLanguageNames;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void addAll(List<OcrLanguage> languages) {
        mLanguages.addAll(languages);
        Collections.sort(mLanguages, mLanguageComparator);
    }


    public void add(OcrLanguage language) {
        mLanguages.add(language);
        Collections.sort(mLanguages, mLanguageComparator);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mLanguages.size();
    }

    @Override
    public Object getItem(int position) {
        return mLanguages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_list_ocr_language, null);
            holder = new ViewHolder();
            holder.mFlipper = (ViewFlipper) convertView.findViewById(R.id.viewFlipper);
            holder.mTextViewLanguage = (TextView) convertView.findViewById(R.id.textView_language);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        OcrLanguage language = mLanguages.get(position);
        if (mShowOnlyLanguageNames) {
            holder.mFlipper.setVisibility(View.INVISIBLE);
        } else {
            if (language.isInstalled()) {
                holder.mFlipper.setDisplayedChild(2);
            } else if (language.isDownloading()) {
                holder.mFlipper.setDisplayedChild(1);
            } else {
                holder.mFlipper.setDisplayedChild(0);
            }

        }

        holder.mTextViewLanguage.setText(language.getDisplayText());
        return convertView;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return mLanguages.isEmpty();
    }

    void setDownloading(String languageDisplayValue, boolean downloading) {
        for (OcrLanguage lang : this.mLanguages) {
            if (lang.getDisplayText().equalsIgnoreCase(languageDisplayValue)) {
                lang.setDownloading(downloading);
                break;
            }
        }
    }
}
