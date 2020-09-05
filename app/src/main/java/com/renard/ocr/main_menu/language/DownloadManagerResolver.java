package com.renard.ocr.main_menu.language;

import com.renard.ocr.R;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import androidx.appcompat.app.AlertDialog;

final class DownloadManagerResolver {

    private static final String DOWNLOAD_MANAGER_PACKAGE_NAME = "com.android.providers.downloads";

    /**
     * Resolve whether the DownloadManager is enable in current devices.
     *
     * @return true if DownloadManager is enable,false otherwise.
     */
    public boolean resolve(Context context) {
        boolean enable = resolveEnable(context);
        if (!enable) {
            final AlertDialog dialog = createDialog(context);
            dialog.show();
        }
        return enable;
    }

    /**
     * Resolve whether the DownloadManager is enable in current devices.
     *
     * @return true if DownloadManager is enable,false otherwise.
     */
    private static boolean resolveEnable(Context context) {
        int state;
        try {
            state = context.getPackageManager()
                    .getApplicationEnabledSetting(DOWNLOAD_MANAGER_PACKAGE_NAME);
        } catch (java.lang.IllegalArgumentException e) {
            return false;
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return !(state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ||
                    state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                    || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED);
        } else {
            return !(state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ||
                    state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER);
        }
    }

    private AlertDialog createDialog(final Context context) {
        return new AlertDialog.Builder(context)
                .setMessage(R.string.download_manager_disabled)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        enableDownloadManager(context);
                    }
                })
                .setCancelable(true)
                .create();
    }

    /**
     * Start activity to enable DownloadManager in Settings.
     */
    private void enableDownloadManager(Context context) {
        try {
            //Open the specific App Info page:
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + DOWNLOAD_MANAGER_PACKAGE_NAME));
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            //Open the generic Apps page:
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
            context.startActivity(intent);
        }
    }
}