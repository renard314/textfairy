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
class OcrLanguage(
        val value: String,
        installStatus: InstallStatus = InstallStatus(false, 0)
) {

    var isDownloading = false;
    val isInstalled: Boolean
        get() = mInstallStatus.isInstalled
    val size: Long
        get() = mInstallStatus.installedSize

    val displayText: String = LOCALIZED_DISPLAY_LANGUAGES[value]
            ?: OCR_LANGUAGES[value]
            ?: value

    private var mInstallStatus = installStatus

    fun installLanguage(context: Context) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = getDownloadUri(value)
        val request = DownloadManager.Request(uri)
        setTrainedDataDestinationForDownload(context, request, uri.lastPathSegment!!)
        request.setTitle(displayText)
        dm.enqueue(request)
    }


    fun setUninstalled() {
        mInstallStatus = InstallStatus(false, 0)
    }

    fun setInstallStatus(installStatus: InstallStatus) {
        mInstallStatus = installStatus
    }

    override fun toString(): String {
        return displayText
    }

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
