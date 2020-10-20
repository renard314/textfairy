package com.renard.ocr.main_menu.language

import android.app.Application
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.map
import androidx.lifecycle.Transformations.switchMap
import androidx.lifecycle.viewModelScope
import com.renard.ocr.R
import com.renard.ocr.TextFairyApplication
import com.renard.ocr.main_menu.language.OcrLanguageDataStore.deleteLanguage
import com.renard.ocr.main_menu.language.OcrLanguageDataStore.getInstallStatusFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LanguageListViewModel(application: Application) : AndroidViewModel(application) {

    private val mDownloadReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val lang = intent.getStringExtra(DownloadBroadCastReceiver.EXTRA_OCR_LANGUAGE)!!
            val status = intent.getIntExtra(DownloadBroadCastReceiver.EXTRA_STATUS, -1)
            updateLanguageList(lang, status)
        }
    }
    private var mFailedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val lang = intent.getStringExtra(DownloadBroadCastReceiver.EXTRA_OCR_LANGUAGE)!!
            val status = intent.getIntExtra(DownloadBroadCastReceiver.EXTRA_STATUS, -1)
            updateLanguageList(lang, status)
        }
    }

    enum class LoadingState {
        LOADING, LOADED
    }

    private val _loading = MutableLiveData<LoadingState>()
    val loading: LiveData<LoadingState>
        get() = _loading

    private val _query = MutableLiveData<String>()
    private val _data = MutableLiveData<List<OcrLanguage>>()
    val data: LiveData<List<OcrLanguage>>
        get() = switchMap(_query) { query ->
            if (query.isNullOrBlank()) {
                _data
            } else {
                map(_data) {
                    it.filter { lang ->
                        lang.displayText.contains(query, ignoreCase = true)
                    }
                }
            }
        }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(LoadingState.LOADING)
            val allOcrLanguages = OcrLanguageDataStore.getAllOcrLanguages(getApplication())
            _data.postValue(allOcrLanguages)
            _query.postValue("")
            _loading.postValue(LoadingState.LOADED)
        }
        application.registerReceiver(mFailedReceiver, IntentFilter(DownloadBroadCastReceiver.ACTION_INSTALL_FAILED))
        application.registerReceiver(mDownloadReceiver, IntentFilter(DownloadBroadCastReceiver.ACTION_INSTALL_COMPLETED))

    }

    override fun onCleared() {
        getApplication<Application>().run {
            unregisterReceiver(mDownloadReceiver)
            unregisterReceiver(mFailedReceiver)
        }
    }

    fun deleteLanguage(language: OcrLanguage) {
        getApplication<TextFairyApplication>().analytics.sendDeleteLanguage(language)
        updateLanguage(language) { deleteLanguage(it, getApplication()) }
    }


    fun startDownload(language: OcrLanguage) {
        val application = getApplication<TextFairyApplication>()
        application.analytics.sendStartDownload(language)
        language.installLanguage(application)
        updateLanguage(language) { it.copy(isDownloading = true) }
    }

    private fun updateLanguageList(lang: String, status: Int) {
        updateLanguage(lang) {
            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    val installStatus = getInstallStatusFor(it.value, getApplication())
                    it.copy(isDownloading = false, installStatus = installStatus)
                }
                DownloadManager.STATUS_FAILED -> {
                    Toast.makeText(
                            getApplication(),
                            String.format(
                                    getApplication<Application>().getString(R.string.download_failed),
                                    it.displayText
                            ),
                            Toast.LENGTH_LONG
                    ).show()
                    it.copy(isDownloading = false)
                }
                else -> it
            }
        }
    }

    private fun updateLanguage(language: OcrLanguage, update: (OcrLanguage) -> OcrLanguage) {
        updateLanguage(language.value, update)
    }

    private fun updateLanguage(language: String, update: (OcrLanguage) -> OcrLanguage) {
        val find = _data.value?.indexOfFirst { it.value == language } ?: -1
        if (find != -1) {
            val mutableList = _data.value as MutableList
            mutableList[find] = update(mutableList[find])
            _data.value = _data.value
            _query.value = _query.value
        }
    }

    fun filter(query: String) {
        _query.value = query
    }
}