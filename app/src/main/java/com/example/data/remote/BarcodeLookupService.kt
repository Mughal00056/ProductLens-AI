package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object ShoppingLinkBuilder {
    fun buildStoreUrl(storeName: String, query: String): String {
        val encoded = try {
            URLEncoder.encode(query, "UTF-8")
        } catch (_: Exception) {
            query.replace(" ", "+")
        }
        return when (storeName.lowercase()) {
            "amazon" -> "https://www.amazon.com/s?k=$encoded"
            "walmart" -> "https://www.walmart.com/search?q=$encoded"
            "target" -> "https://www.target.com/s?searchTerm=$encoded"
            "best buy" -> "https://www.bestbuy.com/site/searchpage.jsp?st=$encoded"
            "ebay" -> "https://www.ebay.com/sch/i.html?_nkw=$encoded"
            else -> "https://www.google.com/search?tbm=shop&q=$encoded"
        }
    }
}

class BarcodeLookupService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun lookupBarcode(barcode: String): BarcodeResult? = withContext(Dispatchers.IO) {
        try {
            val url = "https://world.openfoodfacts.org/api/v2/product/$barcode.json"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ProductLensAI-Android/1.0 (contact: info@productlens.ai)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val status = json.optInt("status", 0)
                if (status != 1) return@withContext null

                val productObj = json.optJSONObject("product") ?: return@withContext null
                val productName = productObj.optString("product_name", "")
                val brand = productObj.optString("brands", "")
                val category = productObj.optString("categories", "Groceries & Food")
                val imageUrl = productObj.optString("image_url", null)
                val quantity = productObj.optString("quantity", "")

                if (productName.isNotBlank()) {
                    BarcodeResult(
                        name = productName,
                        brand = if (brand.isNotBlank()) brand else "Unknown Brand",
                        category = category.split(",").firstOrNull()?.trim() ?: "Groceries",
                        imageUrl = imageUrl,
                        quantity = quantity,
                        barcode = barcode
                    )
                } else null
            }
        } catch (e: Exception) {
            Log.w("BarcodeLookupService", "OpenFoodFacts barcode lookup failed: ${e.message}")
            null
        }
    }
}

data class BarcodeResult(
    val name: String,
    val brand: String,
    val category: String,
    val imageUrl: String?,
    val quantity: String,
    val barcode: String
)
