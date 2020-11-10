package com.renard.ocr.billing

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.renard.ocr.billing.cache.AugmentedSkuDetails

class BillingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BillingRepository = BillingRepository.getInstance(application)

    val inAppSkuDetailsListLiveData: LiveData<List<AugmentedSkuDetails>> = repository.inAppSkuDetailsListLiveData

    val multiScanEnabled: LiveData<Boolean> = Transformations.map(inAppSkuDetailsListLiveData) { list ->
        list.any { it.canPurchase && it.sku == "multi_scan" }
    }

    init {
        repository.startDataSourceConnections()
    }

    fun startBillingFlow(activity: Activity, sku: String) {
        repository.startBillingFlow(activity, sku)

    }

    override fun onCleared() {
        repository.endDataSourceConnections()
    }
}