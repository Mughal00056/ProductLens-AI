package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.ProductInfo
import com.example.data.model.StorePrice
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.remote.GeminiDirectApiClient
import com.example.data.remote.ShoppingLinkBuilder
import com.example.util.ensureFirebaseInitialized
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

private const val TAG = "ProductAiService"

class ProductAiService(
    private val context: Context,
    private val directClient: GeminiDirectApiClient = GeminiDirectApiClient(),
    private val preferencesRepository: UserPreferencesRepository = UserPreferencesRepository(context)
) {

    private val productSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "name" to Schema.string(description = "Exact or best identified product name"),
                "brand" to Schema.string(description = "Manufacturer or brand name"),
                "model" to Schema.string(description = "Model name or number if visible/known"),
                "category" to Schema.string(description = "Product category, e.g. Electronics, Fashion, Groceries, Footwear, Books, Home, Health"),
                "description" to Schema.string(description = "Concise description of the product and its key features"),
                "color" to Schema.string(description = "Primary colors"),
                "productType" to Schema.string(description = "Specific product type or subtype"),
                "confidence" to Schema.integer(description = "Identification confidence from 0 to 100"),
                "specifications" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "key" to Schema.string(description = "Specification name, e.g. Material, Dimensions, Storage, Ingredients"),
                            "value" to Schema.string(description = "Specification value")
                        )
                    ),
                    description = "Key visible or known technical specifications"
                ),
                "estimatedPrices" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "storeName" to Schema.string(description = "Store name like Amazon, Walmart, Target, Best Buy, eBay"),
                            "productTitle" to Schema.string(description = "Product listing title for this store"),
                            "price" to Schema.double(description = "Realistic retail price in preferred currency"),
                            "currency" to Schema.string(description = "Currency symbol e.g. $, ₹, €, £"),
                            "availability" to Schema.string(description = "Stock status: In Stock, Low Stock, or Online Only"),
                            "searchQuery" to Schema.string(description = "Accurate search query for store product link"),
                            "matchConfidence" to Schema.integer(description = "Confidence percentage 0-100"),
                            "rating" to Schema.double(description = "Customer rating from 1.0 to 5.0")
                        )
                    ),
                    description = "Price comparison data across major retail stores"
                )
            ),
            optionalProperties = listOf("model", "color", "productType")
        )
    }

    private val model by lazy {
        ensureFirebaseInitialized(context)
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = productSchema
                temperature = 0.2f
            }
        )
    }

    suspend fun identifyProductFromImage(
        bitmap: Bitmap,
        currencySymbol: String = "$"
    ): Result<ProductInfo> = withContext(Dispatchers.IO) {
        try {
            val useCustom = preferencesRepository.useCustomApiFlow.first()
            val customKey = preferencesRepository.customApiKeyFlow.first()

            val prompt = """
                Analyze this image and identify the physical product accurately.
                Examine packaging, brand logos, model text, typography, visual shape, materials, and colors.
                Return clean JSON matching this format:
                {
                  "name": "...",
                  "brand": "...",
                  "model": "...",
                  "category": "...",
                  "description": "...",
                  "color": "...",
                  "productType": "...",
                  "confidence": 95,
                  "specifications": [ { "key": "...", "value": "..." } ],
                  "estimatedPrices": [
                    {
                      "storeName": "Amazon",
                      "productTitle": "...",
                      "price": 49.99,
                      "currency": "$currencySymbol",
                      "availability": "In Stock",
                      "searchQuery": "...",
                      "matchConfidence": 95,
                      "rating": 4.5
                    }
                  ]
                }
                Provide realistic prices across at least 4 major stores (Amazon, Walmart, Target, Best Buy, or eBay) in currency $currencySymbol.
            """.trimIndent()

            val jsonString: String = if (useCustom && customKey.isNotBlank()) {
                val directRes = directClient.generateContent(customKey.trim(), prompt, bitmap)
                if (directRes.isFailure) {
                    return@withContext Result.failure(directRes.exceptionOrNull() ?: Exception("Custom API error."))
                }
                directRes.getOrThrow()
            } else {
                val response = withTimeout(60_000L) {
                    model.generateContent(
                        content {
                            image(bitmap)
                            text(prompt)
                        }
                    )
                }
                response.text ?: return@withContext Result.failure(IllegalStateException("No response from AI model."))
            }

            val productInfo = parseProductJson(jsonString, currencySymbol, barcode = null)
            Result.success(productInfo)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Product identification timed out", e)
            Result.failure(Exception("AI analysis timed out. Please try with better lighting or closer angle."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI Product identification failed", e)
            Result.failure(e)
        }
    }

    suspend fun searchProductByText(
        query: String,
        currencySymbol: String = "$"
    ): Result<ProductInfo> = withContext(Dispatchers.IO) {
        try {
            val useCustom = preferencesRepository.useCustomApiFlow.first()
            val customKey = preferencesRepository.customApiKeyFlow.first()

            val prompt = """
                Find detailed specifications and price comparison for the product: "$query".
                Return JSON format with:
                {
                  "name": "...",
                  "brand": "...",
                  "model": "...",
                  "category": "...",
                  "description": "...",
                  "color": "...",
                  "productType": "...",
                  "confidence": 95,
                  "specifications": [ { "key": "...", "value": "..." } ],
                  "estimatedPrices": [
                    {
                      "storeName": "Amazon",
                      "productTitle": "...",
                      "price": 99.99,
                      "currency": "$currencySymbol",
                      "availability": "In Stock",
                      "searchQuery": "...",
                      "matchConfidence": 95,
                      "rating": 4.6
                    }
                  ]
                }
                Real current market prices across online stores in currency $currencySymbol.
            """.trimIndent()

            val jsonString: String = if (useCustom && customKey.isNotBlank()) {
                val directRes = directClient.generateContent(customKey.trim(), prompt, null)
                if (directRes.isFailure) {
                    return@withContext Result.failure(directRes.exceptionOrNull() ?: Exception("Custom API error."))
                }
                directRes.getOrThrow()
            } else {
                val response = withTimeout(60_000L) {
                    model.generateContent(
                        content {
                            text(prompt)
                        }
                    )
                }
                response.text ?: return@withContext Result.failure(IllegalStateException("Empty AI response."))
            }

            val productInfo = parseProductJson(jsonString, currencySymbol, barcode = null)
            Result.success(productInfo)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Product search timed out", e)
            Result.failure(Exception("Search timed out. Please check internet connection."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI Product search failed", e)
            Result.failure(e)
        }
    }

    suspend fun enrichBarcodeProduct(
        barcode: String,
        knownName: String?,
        knownBrand: String?,
        currencySymbol: String = "$"
    ): Result<ProductInfo> = withContext(Dispatchers.IO) {
        try {
            val useCustom = preferencesRepository.useCustomApiFlow.first()
            val customKey = preferencesRepository.customApiKeyFlow.first()

            val hint = if (!knownName.isNullOrBlank()) "Known product: $knownName by ${knownBrand ?: "Unknown"}." else ""
            val prompt = """
                Barcode detected: "$barcode". $hint
                Retrieve or identify this commercial product.
                Return JSON format:
                {
                  "name": "...",
                  "brand": "...",
                  "model": "...",
                  "category": "...",
                  "description": "...",
                  "color": "...",
                  "productType": "...",
                  "confidence": 95,
                  "specifications": [ { "key": "...", "value": "..." } ],
                  "estimatedPrices": [
                    {
                      "storeName": "Amazon",
                      "productTitle": "...",
                      "price": 24.99,
                      "currency": "$currencySymbol",
                      "availability": "In Stock",
                      "searchQuery": "...",
                      "matchConfidence": 95,
                      "rating": 4.5
                    }
                  ]
                }
                Provide complete details and realistic retail prices across major stores in currency $currencySymbol.
            """.trimIndent()

            val jsonString: String = if (useCustom && customKey.isNotBlank()) {
                val directRes = directClient.generateContent(customKey.trim(), prompt, null)
                if (directRes.isFailure) {
                    return@withContext Result.failure(directRes.exceptionOrNull() ?: Exception("Custom API error."))
                }
                directRes.getOrThrow()
            } else {
                val response = withTimeout(60_000L) {
                    model.generateContent(
                        content {
                            text(prompt)
                        }
                    )
                }
                response.text ?: return@withContext Result.failure(IllegalStateException("Empty AI response for barcode."))
            }

            val productInfo = parseProductJson(jsonString, currencySymbol, barcode = barcode)
            Result.success(productInfo)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Barcode enrichment timed out", e)
            Result.failure(Exception("Barcode search timed out."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Barcode enrichment failed", e)
            Result.failure(e)
        }
    }

    private fun parseProductJson(
        jsonString: String,
        fallbackCurrency: String,
        barcode: String?
    ): ProductInfo {
        // Strip markdown fences if present
        val cleanJson = jsonString.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val root = JSONObject(cleanJson)
        val name = root.optString("name", "Unknown Product")
        val brand = root.optString("brand", "Generic")
        val model = root.optString("model", "")
        val category = root.optString("category", "General")
        val description = root.optString("description", "")
        val color = root.optString("color", "")
        val productType = root.optString("productType", "")
        val confidence = root.optInt("confidence", 90).coerceIn(0, 100)

        val specsMap = mutableMapOf<String, String>()
        val specsArray = root.optJSONArray("specifications")
        if (specsArray != null) {
            for (i in 0 until specsArray.length()) {
                val specObj = specsArray.optJSONObject(i) ?: continue
                val k = specObj.optString("key", "").trim()
                val v = specObj.optString("value", "").trim()
                if (k.isNotEmpty() && v.isNotEmpty()) {
                    specsMap[k] = v
                }
            }
        }

        val storesList = mutableListOf<StorePrice>()
        val pricesArray = root.optJSONArray("estimatedPrices")
        if (pricesArray != null && pricesArray.length() > 0) {
            var minPrice = Double.MAX_VALUE
            var bestIdx = 0
            for (i in 0 until pricesArray.length()) {
                val pObj = pricesArray.optJSONObject(i) ?: continue
                val storeName = pObj.optString("storeName", "Online Retailer")
                val title = pObj.optString("productTitle", name)
                val price = pObj.optDouble("price", 0.0)
                val currency = pObj.optString("currency", fallbackCurrency)
                val avail = pObj.optString("availability", "In Stock")
                val query = pObj.optString("searchQuery", "$brand $name")
                val matchConf = pObj.optInt("matchConfidence", 95).coerceIn(0, 100)
                val rating = pObj.optDouble("rating", 4.5).coerceIn(1.0, 5.0)

                val storeUrl = ShoppingLinkBuilder.buildStoreUrl(storeName, query)
                if (price in 0.01..<minPrice) {
                    minPrice = price
                    bestIdx = storesList.size
                }

                storesList.add(
                    StorePrice(
                        storeName = storeName,
                        productTitle = title,
                        price = price,
                        currency = currency,
                        productUrl = storeUrl,
                        availability = avail,
                        matchConfidence = matchConf,
                        lastUpdated = "Live Online",
                        rating = rating,
                        isBestMatch = false
                    )
                )
            }

            if (storesList.isNotEmpty()) {
                val best = storesList[bestIdx]
                storesList[bestIdx] = best.copy(isBestMatch = true)
            }
        }

        if (storesList.isEmpty()) {
            val defaultStores = listOf("Amazon", "Walmart", "Target", "Best Buy", "eBay")
            defaultStores.forEachIndexed { idx, sName ->
                storesList.add(
                    StorePrice(
                        storeName = sName,
                        productTitle = "$brand $name",
                        price = 0.0,
                        currency = fallbackCurrency,
                        productUrl = ShoppingLinkBuilder.buildStoreUrl(sName, "$brand $name"),
                        availability = "Available Online",
                        matchConfidence = 90,
                        lastUpdated = "Live",
                        rating = 4.3 + (idx % 3) * 0.2,
                        isBestMatch = idx == 0
                    )
                )
            }
        }

        return ProductInfo(
            id = UUID.randomUUID().toString(),
            name = name,
            brand = brand,
            model = model,
            category = category,
            description = description,
            color = color,
            productType = productType,
            specifications = specsMap,
            confidence = confidence,
            barcode = barcode,
            imageUrl = null,
            stores = storesList,
            scannedAt = System.currentTimeMillis()
        )
    }
}
