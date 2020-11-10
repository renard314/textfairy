package com.renard.ocr.billing.cache

import androidx.lifecycle.LiveData
import androidx.room.*
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.SkuDetails

@Dao
interface AugmentedSkuDetailsDao {

   @Query("SELECT * FROM AugmentedSkuDetails WHERE type = '${BillingClient.SkuType.INAPP}'")
   fun getInappSkuDetails(): LiveData<List<AugmentedSkuDetails>>

   @Transaction
   fun insertOrUpdate(skuDetails: SkuDetails) = skuDetails.apply {
       val result = getById(sku)
       val canPurchase = result?.canPurchase ?: true
       val originalJson = toString().substring("SkuDetails: ".length)
       insert(AugmentedSkuDetails(canPurchase, sku, type, price, title, description, originalJson))
   }

   @Query("SELECT * FROM AugmentedSkuDetails WHERE sku = :sku")
   fun getById(sku: String): AugmentedSkuDetails?

   @Insert(onConflict = OnConflictStrategy.REPLACE)
   fun insert(augmentedSkuDetails: AugmentedSkuDetails)

   @Query("UPDATE AugmentedSkuDetails SET canPurchase = :canPurchase WHERE sku = :sku")
   fun update(sku: String, canPurchase: Boolean)
}