package com.renard.ocr.analytics

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.Event.SCREEN_VIEW
import com.google.firebase.analytics.FirebaseAnalytics.Param.SCREEN_NAME
import com.google.firebase.analytics.ktx.logEvent
import com.renard.ocr.cropimage.image_processing.BlurDetectionResult
import com.renard.ocr.documents.creation.ocr.LayoutQuestionDialog.LayoutKind
import com.renard.ocr.main_menu.language.OcrLanguage

/**
 * @author renard
 */
internal class AnalyticsWithGoogle(private val mApplicationContext: Context) : Analytics {
    private val firebaseAnalytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(mApplicationContext)

    private fun listenForGoogleAnalyticsOptOut() {
        val userPrefs = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
        userPrefs.registerOnSharedPreferenceChangeListener { sharedPreferences: SharedPreferences, key: String ->
            if (key == TRACKING_PREF_KEY) {
                val optOut = sharedPreferences.getBoolean(key, false)
                Log.i(LOG_TAG, "tracking preference was changed. setting app opt out to: $optOut")
                firebaseAnalytics.setAnalyticsCollectionEnabled(optOut)
            }
        }
    }

    override fun getAppOptOut(): Boolean {
        val userPrefs = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
        return userPrefs.getBoolean(TRACKING_PREF_KEY, false)
    }

    override fun toggleTracking(optOut: Boolean) {
        Log.i(LOG_TAG, "toggleTracking($optOut)")
        val userPrefs = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
        userPrefs.edit().putBoolean(TRACKING_PREF_KEY, optOut).apply()
    }

    override fun sendScreenView(screenName: String) {
        firebaseAnalytics.logEvent(SCREEN_VIEW) { param(SCREEN_NAME, screenName) }
    }

    override fun sendStartDownload(language: OcrLanguage) {
        firebaseAnalytics.logEvent("download_language") {
            param(PARAM_LANGUAGE, language.value)
        }
    }

    override fun sendDeleteLanguage(language: OcrLanguage) {
        firebaseAnalytics.logEvent("delete_language") {
            param(PARAM_LANGUAGE, language.value)
        }
    }

    override fun sendOcrResult(language: String, accuracy: Int) {
        val bracket = accuracy - accuracy % 10
        firebaseAnalytics.logEvent("scan_accuracy") {
            param(PARAM_LANGUAGE, language)
            param(PARAM_ACCURACY_BRACKET, bracket.toString())
        }
        firebaseAnalytics.logEvent("scan_complete") {
            param(PARAM_LANGUAGE, language)
            param(PARAM_ACCURACY, accuracy.toLong())
        }
    }

    override fun sendOcrLanguageChanged(language: OcrLanguage) {
        firebaseAnalytics.logEvent("change_ocr_language") {
            param(PARAM_LANGUAGE, language.value)
        }
    }

    override fun sendLayoutDialogCancelled() {
        firebaseAnalytics.logEvent("scan_cancelled") {
            param(PARAM_OCR_STEP, "layout_dialog")
        }
    }

    override fun sendOcrCancelled() {
        firebaseAnalytics.logEvent("scan_cancelled") {
            param(PARAM_OCR_STEP, "activity_finished")
        }
    }

    override fun sendOcrStarted(language: String, layout: LayoutKind) {
        firebaseAnalytics.logEvent("scan_started") {
            param("layout", layout.name)
            param(PARAM_LANGUAGE, language)
        }
    }

    override fun optionTranslateText() {
        firebaseAnalytics.logEvent("translate_text")
    }

    override fun noLanguageInstalled() {
        firebaseAnalytics.logEvent("no_Language_installed")
    }

    override fun optionDocumentViewMode(showingText: Boolean) {
        firebaseAnalytics.logEvent("switch_document_view_mode") {
            param("mode", if (showingText) "text" else "image")
        }
    }


    override fun optionTextSettings() {
        firebaseAnalytics.logEvent("select_text_settings")
    }

    override fun optionTableOfContents() {
        firebaseAnalytics.logEvent("select_table_of_contents")
    }

    override fun optionsDeleteDocument() {
        firebaseAnalytics.logEvent("delete_document")
    }

    override fun optionsCreatePdf() {
        firebaseAnalytics.logEvent(EVENT_CREATE_PDF) {
            param(PARAM_CONTEXT, "options")
        }
    }

    override fun optionsCopyToClipboard() {
        firebaseAnalytics.logEvent(EVENT_COPY_TEXT_TO_CLIPBOARD) {
            param(PARAM_CONTEXT, "options")
        }
    }

    override fun optionsShareText() {
        firebaseAnalytics.logEvent(EVENT_SHARE_TEXT) {
            param(PARAM_CONTEXT, "options")
        }
    }

    override fun ttsStart(language: String) {
        firebaseAnalytics.logEvent(EVENT_START_TTS) {
            param(PARAM_LANGUAGE, language)
        }
    }

    override fun startGallery() {
        firebaseAnalytics.logEvent("start_gallery")
    }

    override fun startCamera() {
        firebaseAnalytics.logEvent("start_camera")
    }

    override fun sendCropError() {
        firebaseAnalytics.logEvent("crop_error")
    }

    override fun sendBlurResult(blurriness: BlurDetectionResult) {
        firebaseAnalytics.logEvent("blur_result") {
            param("blurriness", (blurriness.blurValue * 100).toLong())
            param("blurriness_bracket", blurriness.blurriness.name)
        }
    }

    override fun newImageBecauseOfBlurWarning(blurriness: Float) {
        firebaseAnalytics.logEvent("discard_blurred_image") {
            param("blurriness", (blurriness * 100).toLong())
        }
    }

    override fun continueDespiteOfBlurWarning(blurriness: Float) {
        firebaseAnalytics.logEvent("continue_with_blurred_image") {
            param("blurriness", (blurriness * 100).toLong())
        }
    }

    override fun ocrResultSendFeedback() {
        firebaseAnalytics.logEvent("send_feedback_after_scan")
    }

    override fun ocrResultCopyToClipboard() {
        firebaseAnalytics.logEvent(EVENT_COPY_TEXT_TO_CLIPBOARD) {
            param(PARAM_CONTEXT, "ocr_result")
        }
    }

    override fun ocrResultShowTips() {
        firebaseAnalytics.logEvent("show_tips")
    }

    override fun ocrResultCreatePdf() {
        firebaseAnalytics.logEvent(EVENT_CREATE_PDF) {
            param(PARAM_CONTEXT, "ocr_result")
        }
    }

    override fun ocrResultShareText() {
        firebaseAnalytics.logEvent(EVENT_SHARE_TEXT) {
            param(PARAM_CONTEXT, "ocr_result")
        }
    }

    override fun sendClickYoutube() {
        firebaseAnalytics.logEvent("youtube_clicked")
    }

    override fun sendIgnoreMemoryWarning(availableMegs: Long) {
        firebaseAnalytics.logEvent("ignore_low_ram") {
            param("free_ram", availableMegs)
        }
    }

    override fun sendHeedMemoryWarning(availableMegs: Long) {
        firebaseAnalytics.logEvent("abort_scan_low_ram") {
            param("free_ram", availableMegs)
        }
    }

    companion object {
        const val EVENT_SHARE_TEXT = "share_text"
        const val EVENT_CREATE_PDF = "create_pdf"
        const val EVENT_COPY_TEXT_TO_CLIPBOARD = "copy_text_to_clipboard"
        const val EVENT_START_TTS = "start_tts"
        const val PARAM_CONTEXT = "context"
        const val PARAM_ACCURACY_BRACKET = "accuracy_bracket"
        const val PARAM_OCR_STEP = "ocr_step"
        const val PARAM_ACCURACY = "accuracy"
        const val PARAM_LANGUAGE = "language"
        const val TRACKING_PREF_KEY = "tracking_key"
        private val LOG_TAG = AnalyticsWithGoogle::class.java.simpleName
    }

    init {
        listenForGoogleAnalyticsOptOut()
    }
}

private fun FirebaseAnalytics.logEvent(event: String) {
    logEvent(event, null)
}
