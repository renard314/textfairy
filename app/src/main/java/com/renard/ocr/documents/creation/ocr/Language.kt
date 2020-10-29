package com.renard.ocr.documents.creation.ocr

import android.content.Context
import com.renard.ocr.main_menu.language.OcrLanguageDataStore.getInstallStatusFor
import com.renard.ocr.main_menu.language.OcrLanguageDataStore.getInstalledOCRLanguages
import com.renard.ocr.main_menu.language.OcrLanguageDataStore.getUserLocaleOcrLanguage
import com.renard.ocr.util.PreferencesUtils

object Language {

    fun getOcrLanguage(context: Context): String? {
        var language = getOrcLanguageFromPreferences(context)
        if (language == null) {
            language = getOrcLanguageFromUserLocale(context)
        }
        if (language == null) {
            language = getOcrLanguageFromSdCard(context)
        }
        return language
    }

    private fun getOcrLanguageFromSdCard(context: Context): String? {
        val installedOCRLanguages = getInstalledOCRLanguages(context)
        return if (!installedOCRLanguages.isEmpty()) {
            installedOCRLanguages[0].value
        } else {
            null
        }
    }

    private fun getOrcLanguageFromPreferences(context: Context): String? {
        val language = PreferencesUtils.getOCRLanguage(context)
        if (language.first == null) {
            return null
        }
        return if (getInstallStatusFor(language.first!!, context).isInstalled) {
            language.first
        } else {
            null
        }
    }

    private fun getOrcLanguageFromUserLocale(context: Context): String? {
        val userLocaleOcrLanguage = getUserLocaleOcrLanguage(context)
        return if (userLocaleOcrLanguage != null && userLocaleOcrLanguage.isInstalled) {
            userLocaleOcrLanguage.value
        } else null
    }
}