package com.renard.ocr.main_menu.language;

import android.os.Parcel;
import android.os.Parcelable;

public class InstallStatus implements Parcelable {
    final boolean isInstalled;
    final long installedSize;

    InstallStatus(boolean isInstalled, long installedSize) {
        this.isInstalled = isInstalled;
        this.installedSize = installedSize;
    }

    public boolean isInstalled() {
        return isInstalled;
    }

    long getInstalledSize() {
        return installedSize;
    }

    private InstallStatus(Parcel in) {
        isInstalled = in.readByte() != 0x00;
        installedSize = in.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (isInstalled ? 0x01 : 0x00));
        dest.writeLong(installedSize);
    }

    @SuppressWarnings("unused")
    public static final Creator<InstallStatus> CREATOR = new Creator<InstallStatus>() {
        @Override
        public InstallStatus createFromParcel(Parcel in) {
            return new InstallStatus(in);
        }

        @Override
        public InstallStatus[] newArray(int size) {
            return new InstallStatus[size];
        }
    };
}
