package com.renard.ocr.billing

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode.*
import com.renard.ocr.billing.cache.AugmentedSkuDetails
import com.renard.ocr.billing.cache.LocalBillingDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BillingRepository private constructor(application: Application) {

    val inAppSkuDetailsListLiveData: LiveData<List<AugmentedSkuDetails>> by lazy {
        localCache.skuDetailsDao().getInappSkuDetails()
    }

    private val playStoreBillingClient: BillingClient =
            BillingClient
                    .newBuilder(application.applicationContext)
                    .setListener(::onPurchaseUpdated)
                    .build()

    private val localCache: LocalBillingDb = LocalBillingDb.getInstance(application)

    fun startDataSourceConnections() {
        playStoreBillingClient.ensureConnected {
            CoroutineScope(Dispatchers.IO).launch {
                fetchSkuDetails()
                fetchPurchases()
            }
        }
    }

    fun endDataSourceConnections() {
        playStoreBillingClient.endConnection()
    }

    private fun onPurchaseUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            OK -> CoroutineScope(Dispatchers.IO).launch {
                processPurchaseResult(purchases ?: emptyList())
            }
            ITEM_ALREADY_OWNED -> CoroutineScope(Dispatchers.IO).launch {
                Log.d(LOG_TAG, "already owned items")
                fetchPurchases()
            }
            DEVELOPER_ERROR -> {
                Log.e(LOG_TAG, "Your app's configuration is incorrect. Review in the Google Play" +
                        "Console. Possible causes of this error include: APK is not signed with " +
                        "release key; SKU productId mismatch.")
            }
        }
    }

    private suspend fun fetchSkuDetails() {
        playStoreBillingClient.querySkuDetails().forEach { localCache.skuDetailsDao().insertOrUpdate(it) }

    }

    private suspend fun fetchPurchases() {
        processPurchaseResult(playStoreBillingClient.queryPurchases())
    }

    private fun processPurchaseResult(purchasesResult: List<Purchase>) {
        val cachedPurchases = localCache.purchaseDao().getPurchases()
        val newBatch = purchasesResult.subtract(cachedPurchases.map { it.data })

        newBatch.forEach { purchase ->
            localCache.skuDetailsDao().update(purchase.sku, false)
        }

        newBatch.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                .filterNot { it.isAcknowledged }
                .map { it.purchaseToken }
                .forEach {
                    CoroutineScope(Dispatchers.IO).launch {
                        playStoreBillingClient.acknowledgePurchase(it)
                    }
                }

        localCache.purchaseDao().insert(*newBatch.toTypedArray())
    }

    fun startBillingFlow(activity: Activity, sku: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val byId = localCache.skuDetailsDao().getById(sku)!!
            val purchaseParams = BillingFlowParams
                    .newBuilder()
                    .setSkuDetails(SkuDetails(byId.originalJson))
                    .build()

            withContext(Dispatchers.Main) {
                playStoreBillingClient.ensureConnected {
                    playStoreBillingClient.launchBillingFlow(activity, purchaseParams)
                }
            }
        }
    }

    companion object {
        private const val LOG_TAG = "BillingRepository"

        @Volatile
        private var INSTANCE: BillingRepository? = null

        fun getInstance(application: Application): BillingRepository =
                INSTANCE ?: synchronized(this) {
                    INSTANCE
                            ?: BillingRepository(application)
                                    .also { INSTANCE = it }
                }
    }

}

