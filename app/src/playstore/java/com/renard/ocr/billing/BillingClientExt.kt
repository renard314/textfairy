package com.renard.ocr.billing

import com.android.billingclient.api.*
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Calls [block] when the BillingClient is connected. Uses automatic retries.
 */
internal fun BillingClient.ensureConnected(block: () -> Unit) {
    if (isReady) {
        block()
    } else {
        CoroutineScope(Dispatchers.IO).launch {
            val result: BillingResult = retry(billingResult = {it}) {
                startConnection()
            }
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                withContext(Dispatchers.Main) {
                    block()
                }
            }
        }
    }
}

/**
 * Gets SkuDetails with automatic retries
 */
internal suspend fun BillingClient.querySkuDetails(): List<SkuDetails> {
    return retry(billingResult = SkuDetailsResult::billingResult) {
        val params = SkuDetailsParams.newBuilder()
                .setSkusList(listOf("scan_multiple"))
                .setType(BillingClient.SkuType.INAPP)
                .build()
        querySkuDetails(params)
    }.skuDetailsList ?: emptyList()
}

/**
 * Gets Purchases with automatic retries
 */
internal suspend fun BillingClient.queryPurchases(): List<Purchase> {
    val purchases = retry(billingResult = Purchase.PurchasesResult::getBillingResult) {
        queryPurchases(BillingClient.SkuType.INAPP)
    }
    return purchases.purchasesList ?: emptyList()
}


/**
 * Acknowledges a Purchase with automatic retries
 */
internal suspend fun BillingClient.acknowledgePurchase(token: String) {
    retry(billingResult = { billingResult: BillingResult -> billingResult}) {
        suspendCoroutine { cont ->
            val params = AcknowledgePurchaseParams
                    .newBuilder()
                    .setPurchaseToken(token)
                    .build()

            acknowledgePurchase(params) { billingResult ->
                cont.resume(billingResult)
            }
        }
    }

}

private suspend fun BillingClient.startConnection():BillingResult = suspendCoroutine { cont ->
    startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    cont.resume(billingResult)
                }

                override fun onBillingServiceDisconnected() {
                    cont.resume(BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED).build())
                }
            }
    )
}

private fun BillingResult.shouldRetry() =
        when (responseCode) {
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE, BillingClient.BillingResponseCode.SERVICE_TIMEOUT, BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> true
            else -> false
        }


private suspend fun <T> retry(
        times: Int = 5,
        billingResult: (T) -> BillingResult,
        block: suspend () -> T): T {
    var currentDelay:Long = 100
    repeat(times-1) {
        val result = block()
        if (billingResult(result).shouldRetry()) {
            delay(currentDelay)
            currentDelay = (currentDelay * 2).coerceAtMost(2000)
        } else {
            return result
        }
    }
    return block()
}