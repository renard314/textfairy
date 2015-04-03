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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.database.DataSetObserver;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.renard.ocr.R;

public class OCRLanguageAdapter extends BaseAdapter implements ListAdapter {

	private static Comparator<OCRLanguage> mLanguageComparator = new Comparator<OCRLanguageAdapter.OCRLanguage>() {

		@Override
		public int compare(OCRLanguage lhs, OCRLanguage rhs) {
			return lhs.mDisplayText.compareTo(rhs.mDisplayText);
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


    public static class OCRLanguage implements Parcelable{

		public String getValue() {
			return mValue;
		}

		public String getDisplayText() {
			return mDisplayText;
		}

		boolean mDownloaded;
		boolean mDownloading;
        boolean needsCubeData;
		String mValue;
		String mDisplayText;
		long mSize;

        public OCRLanguage(Parcel in){
            mValue = in.readString();
            mDisplayText = in.readString();
        }

        public OCRLanguage(final String value, final String displayText, boolean downloaded, long size) {
			mDownloaded = downloaded;
			mValue = value;
			mDisplayText = displayText;
			this.mSize = size;
            if ("ara".equalsIgnoreCase(value) || "hin".equalsIgnoreCase(value)){
                needsCubeData = true;
            }
		}

        public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
            public OCRLanguage createFromParcel(Parcel in) {
                return new OCRLanguage(in);
            }

            public OCRLanguage[] newArray(int size) {
                return new OCRLanguage[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mValue);
            dest.writeString(mDisplayText);
        }

        @Override
        public String toString() {
            return mDisplayText;
        }
    }

	private static class ViewHolder {
		ViewFlipper mFlipper;
		TextView mTextViewLanguage;
	}

	private final List<OCRLanguage> mLanguages = new ArrayList<OCRLanguage>();
	private final LayoutInflater mInflater;
    private boolean mShowOnlyLanguageNames;

	public OCRLanguageAdapter(final Context context, boolean showOnlyLanguageNames) {
        this.mShowOnlyLanguageNames = showOnlyLanguageNames;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

    public void addAll(List<OCRLanguage> languages) {
        mLanguages.addAll(languages);
        Collections.sort(mLanguages, mLanguageComparator);
    }


    public void add(OCRLanguage language) {
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
			convertView = mInflater.inflate(R.layout.ocr_language_list_item, null);
			holder = new ViewHolder();
			holder.mFlipper = (ViewFlipper) convertView.findViewById(R.id.viewFlipper);
			holder.mTextViewLanguage = (TextView) convertView.findViewById(R.id.textView_language);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		OCRLanguage language = mLanguages.get(position);
		if (mShowOnlyLanguageNames) {
			holder.mFlipper.setVisibility(View.INVISIBLE);
		} else {
			if (language.mDownloaded == true) {
				holder.mFlipper.setDisplayedChild(2);
			} else if (language.mDownloading == true) {
				holder.mFlipper.setDisplayedChild(1);
			} else {
				holder.mFlipper.setDisplayedChild(0);
			}

		}
		
		holder.mTextViewLanguage.setText(language.mDisplayText);
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

    public void setDownloading(String languageDisplayValue, boolean downloading) {
		for (OCRLanguage lang : this.mLanguages) {
			if (lang.mDisplayText.equalsIgnoreCase(languageDisplayValue)) {
				lang.mDownloading = downloading;
				break;
			}
		}
	}
}
