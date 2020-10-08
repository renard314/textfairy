package com.renard.ocr.main_menu.language;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;

import com.renard.ocr.R;
import com.renard.ocr.util.AppStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author renard
 */
public class OcrLanguageDataStore {


    private static final File[] EMPTY_FILE_ARRAY = new File[0];

    public static List<OcrLanguage> getOldInstalledOCRLanguages(Context appContext) {
        List<OcrLanguage> ocrLanguages = new ArrayList<>();
        final String[] languageValues = appContext.getResources().getStringArray(R.array.ocr_languages);
        final File oldTrainingDataDir = AppStorage.getOldTrainingDataDir();
        for (String languageValue : languageValues) {
            final File[] files = oldTrainingDataDir.listFiles(pathname -> isLanguageFileFor(pathname, languageValue));
            if (files != null && files.length > 0) {
                InstallStatus installStatus = new InstallStatus(true, sumFileSizes(files));
                if (installStatus.isInstalled) {
                    ocrLanguages.add(
                            new OcrLanguage(
                                    languageValue,
                                    languageValue,
                                    installStatus.isInstalled(),
                                    installStatus.getInstalledSize()
                            )
                    );
                }
            }
        }
        return ocrLanguages;
    }

    public static List<OcrLanguage> getInstalledOCRLanguages(Context appContext) {
        final List<OcrLanguage> ocrLanguages = getAvailableOcrLanguages(appContext);
        final List<OcrLanguage> result = new ArrayList<>();
        for (OcrLanguage lang : ocrLanguages) {
            if (lang.isInstalled()) {
                result.add(lang);
            }
        }
        return result;
    }

    public static List<OcrLanguage> getAvailableOcrLanguages(Context context) {
        List<OcrLanguage> languages = new ArrayList<>();
        // actual values uses by tesseract
        final String[] languageValues = context.getResources().getStringArray(R.array.ocr_languages);
        // values shown to the user
        final String[] languageDisplayValues = new String[languageValues.length];
        for (int i = 0; i < languageValues.length; i++) {
            final String val = languageValues[i];
            final int firstSpace = val.indexOf(' ');
            languageDisplayValues[i] = languageValues[i].substring(firstSpace + 1);
            languageValues[i] = languageValues[i].substring(0, firstSpace);
        }
        for (int i = 0; i < languageValues.length; i++) {
            final InstallStatus installStatus = isLanguageInstalled(languageValues[i], context);
            OcrLanguage language = new OcrLanguage(languageValues[i], languageDisplayValues[i], installStatus.isInstalled(), installStatus.getInstalledSize());
            languages.add(language);
        }
        return languages;
    }

    public static InstallStatus isLanguageInstalled(final String ocrLang, Context context) {
        final File[] languageFiles = getAllFilesFor(ocrLang, context);
        if (languageFiles.length == 0) {
            return new InstallStatus(false, 0);
        }
        return new InstallStatus(true, sumFileSizes(languageFiles));
    }

    private static File[] getAllFilesFor(final String ocrLang, Context context) {
        final File tessDir = AppStorage.getTrainingDataDir(context);
        if (!tessDir.exists()) {
            return EMPTY_FILE_ARRAY;
        }

        final File[] files = tessDir.listFiles(pathname -> isLanguageFileFor(pathname, ocrLang));
        if (files == null) {
            return EMPTY_FILE_ARRAY;
        } else {
            return files;
        }
    }

    private static long sumFileSizes(File[] languageFiles) {
        if (languageFiles == null) {
            return 0;
        }
        long sum = 0;
        for (File f : languageFiles) {
            sum += f.length();
        }
        return sum;
    }

    private static boolean isLanguageFileFor(File pathname, String ocrLang) {
        return pathname.getName().startsWith(ocrLang + ".") && pathname.isFile();
    }

    public static boolean deleteLanguage(String language, Context context) {
        final File[] languageFiles = getAllFilesFor(language, context);
        if (languageFiles.length == 0) {
            return false;
        }

        boolean success = true;

        for (File file : languageFiles) {
            success &= file.delete();
        }
        return success;
    }

    public static boolean deleteLanguage(OcrLanguage language, Context context) {
        final File[] languageFiles = getAllFilesFor(language.getValue(), context);
        if (languageFiles.length == 0) {
            language.setUninstalled();
            return false;
        }

        boolean success = true;
        boolean atLeastOneDeleted = false;

        for (File file : languageFiles) {
            final boolean deleted = file.delete();
            success &= deleted;
            atLeastOneDeleted |= deleted;
        }
        if (atLeastOneDeleted) {
            language.setUninstalled();
        }
        return success;
    }


    static Uri getDownloadUri(String language) {
        final String part2 = ".traineddata";
        String networkDir = "https://github.com/tesseract-ocr/tessdata_fast/raw/4.0.0/";
        return Uri.parse(networkDir + language + part2);
    }

}
