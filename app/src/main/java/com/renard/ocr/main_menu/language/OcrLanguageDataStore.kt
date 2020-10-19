package com.renard.ocr.main_menu.language

import android.content.Context
import android.net.Uri
import androidx.core.os.ConfigurationCompat
import com.renard.ocr.util.AppStorage.getTrainingDataDir
import java.io.File
import java.util.*

/**
 * @author renard
 */
object OcrLanguageDataStore {
    private val EMPTY_FILE_ARRAY = arrayOf<File>()

    @JvmStatic
    fun getInstalledOCRLanguages(appContext: Context) =
            getAllOcrLanguages(appContext).filter { it.isInstalled }

    @JvmStatic
    fun getAllOcrLanguages(context: Context) =
            OCR_LANGUAGES.keys.map { OcrLanguage(it, getInstallStatusFor(it, context)) }

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
            getAllOcrLanguages(context).firstOrNull { it.value == iso3Language }
        } catch (e: MissingResourceException) {
            null
        }
    }
}

val OCR_LANGUAGES = mapOf(
        "afr" to "Afrikaans",
        "amh" to "Amharic",
        "ara" to "Arabic",
        "asm" to "Assamese",
        "aze" to "Azerbaijani",
        "aze_cyrl" to "Azerbaijani, - Cyrillic",
        "bel" to "Belarusian",
        "ben" to "Bengali",
        "bod" to "Tibetan",
        "bos" to "Bosnian",
        "bul" to "Bulgarian",
        "cat" to "Catalan; Valencian",
        "ceb" to "Cebuano",
        "ces" to "Czech",
        "chi_sim" to "Chinese - Simplified",
        "chi_tra" to "Chinese - Traditional",
        "chi_sim_vert" to "Chinese - Simplified - Vertical",
        "chi_tra_vert" to "Chinese - Traditional - Vertical",
        "chr" to "Cherokee",
        "cos" to "Corsican",
        "cym" to "Welsh",
        "dan" to "Danish",
        "deu" to "German",
        "div" to "Divehi; Dhivehi; Maldivian",
        "dzo" to "Dzongkha",
        "ell" to "Greek, Modern (1453-)",
        "eng" to "English",
        "enm" to "English, Middle (1100-1500)",
        "epo" to "Esperanto",
        "est" to "Estonian",
        "eus" to "Basque",
        "equ" to "Math/Equation",
        "fas" to "Persian",
        "fin" to "Finnish",
        "fra" to "French",
        "frk" to "German Fraktur",
        "frm" to "French, Middle (ca. 1400-1600)",
        "gle" to "Irish",
        "glg" to "Galician",
        "grc" to "Greek, Ancient (-1453)",
        "guj" to "Gujarati",
        "hat" to "Haitian; Haitian Creole",
        "heb" to "Hebrew",
        "hin" to "Hindi",
        "hrv" to "Croatian",
        "hun" to "Hungarian",
        "iku" to "Inuktitut",
        "ind" to "Indonesian",
        "isl" to "Icelandic",
        "ita" to "Italian",
        "ita_old" to "Italian - Old",
        "jav" to "Javanese",
        "jpn" to "Japanese",
        "jpn_vert" to "Japanese - vertical",
        "kan" to "Kannada",
        "kat" to "Georgian",
        "kat_old" to "Georgian - Old",
        "kaz" to "Kazakh",
        "khm" to "Central Khmer",
        "kir" to "Kirghiz; Kyrgyz",
        "kor" to "Korean",
        "kor_vert" to "Korean - vertical",
        "kur" to "Kurdish",
        "kmr" to "Kurdish - Northern",
        "lao" to "Lao",
        "lat" to "Latin",
        "lav" to "Latvian",
        "lit" to "Lithuanian",
        "mal" to "Malayalam",
        "mar" to "Marathi",
        "mkd" to "Macedonian",
        "mlt" to "Maltese",
        "msa" to "Malay",
        "mya" to "Burmese",
        "nep" to "Nepali",
        "nld" to "Dutch; Flemish",
        "nor" to "Norwegian",
        "oci" to "Occitan (post 1500)",
        "ori" to "Oriya",
        "pan" to "Panjabi; Punjabi",
        "pol" to "Polish",
        "por" to "Portuguese",
        "pus" to "Pushto; Pashto",
        "ron" to "Romanian; Moldavian; Moldovan",
        "rus" to "Russian",
        "san" to "Sanskrit",
        "sin" to "Sinhala; Sinhalese",
        "slk" to "Slovak",
        "slv" to "Slovenian",
        "spa" to "Spanish; Castilian",
        "spa_old" to "Spanish; Castilian - Old",
        "sqi" to "Albanian",
        "srp" to "Serbian",
        "srp_latn" to "Serbian - Latin",
        "sun" to "Sundanese",
        "swa" to "Swahili",
        "swe" to "Swedish",
        "syr" to "Syriac",
        "tam" to "Tamil",
        "tel" to "Telugu",
        "tgk" to "Tajik",
        "tgl" to "Tagalog",
        "tha" to "Thai",
        "tir" to "Tigrinya",
        "tur" to "Turkish",
        "uig" to "Uighur; Uyghur",
        "ukr" to "Ukrainian",
        "urd" to "Urdu",
        "uzb" to "Uzbek",
        "uzb_cyrl" to "Uzbek - Cyrillic",
        "vie" to "Vietnamese",
        "yid" to "Yiddish",
)