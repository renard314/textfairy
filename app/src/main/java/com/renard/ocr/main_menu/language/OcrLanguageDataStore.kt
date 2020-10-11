package com.renard.ocr.main_menu.language

import android.content.Context
import android.net.Uri
import androidx.core.os.ConfigurationCompat
import com.renard.ocr.R
import com.renard.ocr.util.AppStorage.getOldTrainingDataDir
import com.renard.ocr.util.AppStorage.getTrainingDataDir
import java.io.File
import java.util.*

/**
 * @author renard
 */
object OcrLanguageDataStore {
    const val LATIN_SCRIPT = "Latin"
    private val EMPTY_FILE_ARRAY = arrayOf<File>()

    @JvmStatic
    fun getOldInstalledOCRLanguages(appContext: Context): List<OcrLanguage> {
        val knownLanguages = appContext.resources.getStringArray(R.array.ocr_languages)
                .map { it.split(' ', limit = 2).run { first() to last() } }
                .toMap()

        return (getOldTrainingDataDir().listFiles()
                ?: emptyArray())
                .filter { it.name.endsWith("traineddata") }
                .filter { knownLanguages.contains(it.nameWithoutExtension) }
                .map {
                    OcrLanguage(
                            it.nameWithoutExtension,
                            knownLanguages[it.nameWithoutExtension],
                            true,
                            sumFileSizes(it))
                }
    }

    @JvmStatic
    fun getInstalledOCRLanguages(appContext: Context) =
            getAvailableOcrLanguages(appContext).filter { it.isInstalled }

    @JvmStatic
    fun getAvailableOcrLanguages(context: Context): List<OcrLanguage> {
        val languages: MutableList<OcrLanguage> = ArrayList()
        // actual values uses by tesseract
        val languageValues = context.resources.getStringArray(R.array.ocr_languages)
        // values shown to the user
        val languageDisplayValues = arrayOfNulls<String>(languageValues.size)
        for (i in languageValues.indices) {
            val `val` = languageValues[i]
            val firstSpace = `val`.indexOf(' ')
            languageDisplayValues[i] = languageValues[i].substring(firstSpace + 1)
            languageValues[i] = languageValues[i].substring(0, firstSpace)
        }
        for (i in languageValues.indices) {
            val installStatus = getInstallStatusFor(languageValues[i], context)
            val language = OcrLanguage(languageValues[i], languageDisplayValues[i], installStatus.isInstalled(), installStatus.getInstalledSize())
            languages.add(language)
        }
        return languages
    }

    @JvmStatic
    fun getInstallStatusFor(ocrLang: String, context: Context): InstallStatus {
        val languageFiles = getAllFilesFor(ocrLang, context)
        return if (languageFiles.isEmpty()) {
            InstallStatus(false, 0)
        } else InstallStatus(true, sumFileSizes(*languageFiles))
    }

    private fun getAllFilesFor(ocrLang: String, context: Context): Array<File> {
        val tessDir = getTrainingDataDir(context)
        if (!tessDir.exists()) {
            return EMPTY_FILE_ARRAY
        }
        val files = tessDir.listFiles { pathname: File -> isLanguageFileFor(pathname, ocrLang) }
        return files ?: EMPTY_FILE_ARRAY
    }

    private fun sumFileSizes(vararg languageFiles: File) =
            languageFiles.fold(0L, { acc, file -> acc + file.length() })

    private fun isLanguageFileFor(pathname: File, ocrLang: String) =
            pathname.name.startsWith("$ocrLang.") && pathname.isFile

    fun deleteLanguage(language: String, context: Context): Boolean {
        val languageFiles = getAllFilesFor(language, context)
        if (languageFiles.isEmpty()) {
            return false
        }
        return languageFiles.map { it.delete() }.all { it }
    }

    @JvmStatic
    fun deleteLanguage(language: OcrLanguage, context: Context): Boolean {
        val languageFiles = getAllFilesFor(language.value, context)
        if (languageFiles.isEmpty()) {
            language.setUninstalled()
            return false
        }
        val deletedList = languageFiles.map { it.delete() }
        if (deletedList.any { it }) {
            language.setUninstalled()
        }
        return deletedList.all { it }
    }

    @JvmStatic
    fun getDownloadUri(language: String): Uri {
        return Uri.parse("https://github.com/tesseract-ocr/tessdata_fast/raw/4.0.0/$language.traineddata")
    }

    @JvmStatic
    fun getUserLocaleOcrLanguage(context: Context): OcrLanguage? {
        return try {
            val iso3Language = ConfigurationCompat.getLocales(context.resources.configuration)[0]
                    .isO3Language
            getAvailableOcrLanguages(context).firstOrNull { it.value == iso3Language }
        } catch (e: MissingResourceException) {
            null
        }
    }
}