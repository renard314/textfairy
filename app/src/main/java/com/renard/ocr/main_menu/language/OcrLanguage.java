package com.renard.ocr.main_menu.language;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author renard
 */
public class OcrLanguage implements Parcelable {


    private boolean mDownloading;
    private final String mValue;
    private final String mDisplayText;
    private InstallStatus mInstallStatus;

    public OcrLanguage(final String value, final String displayText, boolean installed, long size) {
        mInstallStatus = new InstallStatus(installed, size);
        mValue = value;
        mDisplayText = displayText;

    }


    boolean isInstalled() {
        return mInstallStatus.isInstalled;
    }

    void setUninstalled() {
        mInstallStatus = new InstallStatus(false, 0);
    }

    void setInstallStatus(InstallStatus installStatus) {
        mInstallStatus = installStatus;
    }

    public long getSize() {
        return mInstallStatus.installedSize;
    }

    public String getValue() {
        return mValue;
    }

    public String getDisplayText() {
        return mDisplayText;
    }

    boolean isDownloading() {
        return mDownloading;
    }

    void setDownloading(boolean downloading) {
        mDownloading = downloading;
    }


    @Override
    public String toString() {
        return getDisplayText();
    }

    List<Uri> getDownloadUris() {
        List<Uri> result = new ArrayList<>();
        final String part2 = ".traineddata";
        String networkDir = "https://github.com/tesseract-ocr/tessdata/raw/3.04.00/";
        result.add(Uri.parse(networkDir + getValue() + part2));
        return result;
    }


    protected OcrLanguage(Parcel in) {
        mDownloading = in.readByte() != 0x00;
        mValue = in.readString();
        mDisplayText = in.readString();
        mInstallStatus = (InstallStatus) in.readValue(InstallStatus.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (mDownloading ? 0x01 : 0x00));
        dest.writeString(mValue);
        dest.writeString(mDisplayText);
        dest.writeValue(mInstallStatus);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<OcrLanguage> CREATOR = new Parcelable.Creator<OcrLanguage>() {
        @Override
        public OcrLanguage createFromParcel(Parcel in) {
            return new OcrLanguage(in);
        }

        @Override
        public OcrLanguage[] newArray(int size) {
            return new OcrLanguage[size];
        }
    };
}