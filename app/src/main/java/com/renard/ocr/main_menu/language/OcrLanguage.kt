package com.renard.ocr.main_menu.language

import android.app.DownloadManager
import android.content.Context
import com.renard.ocr.main_menu.language.OcrLanguageDataStore.getDownloadUri
import com.renard.ocr.util.AppStorage.setTrainedDataDestinationForDownload
import java.util.*
import java.util.Locale.getAvailableLocales


/**
 * @author renard
 */
data class OcrLanguage(
        val value: String,
        val installStatus: InstallStatus = InstallStatus(false),
        val isDownloading: Boolean = false
) {

    data class InstallStatus(val isInstalled: Boolean, val installedSize: Long = 0)

    val isInstalled: Boolean
        get() = installStatus.isInstalled
    val size: Long
        get() = installStatus.installedSize

    val displayText: String = LOCALIZED_DISPLAY_LANGUAGES[value]
            ?: OCR_LANGUAGES[value]
            ?: value


    fun installLanguage(context: Context) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = getDownloadUri(value)
        val request = DownloadManager.Request(uri)
        setTrainedDataDestinationForDownload(context, request, uri.lastPathSegment!!)
        request.setTitle(displayText)
        dm.enqueue(request)
    }

    override fun toString() = displayText
}

private val LOCALIZED_DISPLAY_LANGUAGES: Map<String, String> by lazy {
    getAvailableLocales()
            .associateBy({ it.getIso3Language() }, Locale::getDisplayLanguage)
            .filterKeys { it.isNotEmpty() }
            .filterValues { it.isNotEmpty() }
}

private fun Locale.getIso3Language() = try {
    isO3Language
} catch (e: MissingResourceException) {
    ""
}
