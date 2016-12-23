package com.renard.ocr.documents.viewing.single;

import android.os.AsyncTask;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ViewSwitcher;

class HtmlToSpannedAsyncTask extends AsyncTask<String, Void, Spanned> {

    private final EditText mEditText;
    private final ViewSwitcher mViewSwitcher;
    private final TextWatcher mTextWatcher;

    HtmlToSpannedAsyncTask(final EditText editText, ViewSwitcher viewSwitcher, TextWatcher textWatcher) {
        mEditText = editText;
        mViewSwitcher = viewSwitcher;
        mTextWatcher = textWatcher;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mEditText.removeTextChangedListener(mTextWatcher);
        mViewSwitcher.setDisplayedChild(0);
    }

    @Override
    protected Spanned doInBackground(String... params) {
        if (params != null && params.length > 0 && params[0] != null && params[0].length() > 0) {
            return Html.fromHtml(params[0]);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Spanned spanned) {
        super.onPostExecute(spanned);
        mEditText.addTextChangedListener(mTextWatcher);
        mEditText.setText(spanned);
        mViewSwitcher.setDisplayedChild(1);
    }
}
