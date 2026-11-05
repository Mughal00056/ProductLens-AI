package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.data.ai.ProductAiService
import com.example.data.local.AppDatabase
import com.example.data.local.FavoriteEntity
import com.example.data.local.ProductEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.model.ProductInfo
import com.example.data.model.StorePrice
import com.example.data.remote.BarcodeLookupService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ProductRepository(
    private val context: Context,
    private val aiService: ProductAiService = ProductAiService(),
    private val barcodeService: BarcodeLookupService = BarcodeLookupService(),
    private val database: AppDatabase = AppDatabase.getDatabase(context)
) {
    private val productDao = database.productDao()
    private val searchHistoryDao = database.searchHistoryDao()
    private val favoriteDao = database.favoriteDao()

    suspend fun scanProductImage(
        bitmap: Bitmap,
        currencySymbol: String = "$"
    ): Result<ProductInfo> = withContext(Dispatchers.IO) {
        // Compress bitmap slightly if large
        val scaledBitmap = scaleBitmapIfNeeded(bitmap, 1280)
        val imagePath = saveImageLocally(scaledBitmap)

        val result = aiService.identifyProductFromImage(scaledBitmap, currencySymbol)
        if (result.isSuccess) {
            val product = result.getOrThrow().copy(imageUrl = imagePath)
            saveProductLocally(product)
            logSearchHistory(product)
            Result.success(product)
        } else {
            result
        }
    }

    suspend fun searchProductText(
        query: String,
        currencySymbol: String = "$"
    ): Result<ProductInfo> = withContext(Dispatchers.IO) {
        val result = aiService.searchProductByText(query, currencySymbol)
        if (result.isSuccess) {
            val product = result.getOrThrow()
            saveProductLocally(product)
            logSearchHistory(product)
            Result.success(product)
        } else {
            result
        }
    }

    suspend fun scanBarcode(
        barcode: String,
        currencySymbol: String = "$"
    ): Result<ProductInfo> = withContext(Dispatchers.IO) {
        // Check local database first
        val cached = productDao.getProductByBarcode(barcode)
        if (cached != null) {
            val product = entityToProduct(cached)
            logSearchHistory(product)
            return@withContext Result.success(product)
        }

        // Try Open Food Facts first for speed & accuracy
        val offResult = barcodeService.lookupBarcode(barcode)

        // Enrich with Gemini AI to get store prices, ratings, and specifications
        val aiResult = aiService.enrichBarcodeProduct(
            barcode = barcode,
            knownName = offResult?.name,
            knownBrand = offResult?.brand,
            currencySymbol = currencySymbol
        )

        if (aiResult.isSuccess) {
            var product = aiResult.getOrThrow()
            if (offResult?.imageUrl != null && product.imageUrl == null) {
                product = product.copy(imageUrl = offResult.imageUrl)
            }
            saveProductLocally(product)
            logSearchHistory(product)
            Result.success(product)
        } else if (offResult != null) {
            // Fallback to basic details if AI times out
            val fallback = ProductInfo(
                id = UUID.randomUUID().toString(),
                name = offResult.name,
                brand = offResult.brand,
                category = offResult.category,
                description = "Barcode: $barcode. Quantity: ${offResult.quantity}",
                confidence = 92,
                barcode = barcode,
                imageUrl = offResult.imageUrl,
                stores = emptyList()
            )
            saveProductLocally(fallback)
            logSearchHistory(fallback)
            Result.success(fallback)
        } else {
            aiResult
        }
    }

    suspend fun getProductById(id: String): ProductInfo? = withContext(Dispatchers.IO) {
        val entity = productDao.getProductById(id) ?: return@withContext null
        entityToProduct(entity)
    }

    val searchHistoryFlow: Flow<List<SearchHistoryEntity>> = searchHistoryDao.getAllHistory()

    val favoritesFlow: Flow<List<FavoriteEntity>> = favoriteDao.getAllFavorites()

    fun isFavoriteFlow(productId: String): Flow<Boolean> = favoriteDao.isFavorite(productId)

    suspend fun toggleFavorite(product: ProductInfo, isFav: Boolean) = withContext(Dispatchers.IO) {
        if (isFav) {
            val minP = product.stores.minOfOrNull { it.price } ?: 0.0
            val maxP = product.stores.maxOfOrNull { it.price } ?: 0.0
            val curr = product.stores.firstOrNull()?.currency ?: "$"
            val range = if (minP > 0) {
                if (minP == maxP) "$curr%.2f".format(minP) else "$curr%.2f - $curr%.2f".format(minP, maxP)
            } else "Check Stores"

            favoriteDao.insertFavorite(
                FavoriteEntity(
                    productId = product.id,
                    productName = product.name,
                    brand = product.brand,
                    imageUrl = product.imageUrl,
                    category = product.category,
                    priceRange = range
                )
            )
        } else {
            favoriteDao.removeFavorite(product.id)
        }
    }

    suspend fun deleteHistoryItem(id: Long) = withContext(Dispatchers.IO) {
        searchHistoryDao.deleteHistoryById(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        searchHistoryDao.clearAllHistory()
    }

    private suspend fun saveProductLocally(product: ProductInfo) {
        val specsJson = JSONObject(product.specifications as Map<*, *>).toString()
        val storesArray = JSONArray()
        for (s in product.stores) {
            val o = JSONObject()
            o.put("storeName", s.storeName)
            o.put("productTitle", s.productTitle)
            o.put("price", s.price)
            o.put("currency", s.currency)
            o.put("productUrl", s.productUrl)
            o.put("availability", s.availability)
            o.put("matchConfidence", s.matchConfidence)
            o.put("lastUpdated", s.lastUpdated)
            o.put("rating", s.rating)
            o.put("isBestMatch", s.isBestMatch)
            storesArray.put(o)
        }

        val entity = ProductEntity(
            id = product.id,
            name = product.name,
            brand = product.brand,
            model = product.model,
            category = product.category,
            description = product.description,
            imageUrl = product.imageUrl,
            barcode = product.barcode,
            confidence = product.confidence,
            color = product.color,
            productType = product.productType,
            specsJson = specsJson,
            storesJson = storesArray.toString(),
            createdAt = product.scannedAt
        )
        productDao.insertProduct(entity)
    }

    private suspend fun logSearchHistory(product: ProductInfo) {
        searchHistoryDao.insertHistory(
            SearchHistoryEntity(
                productId = product.id,
                productName = product.name,
                brand = product.brand,
                imageUrl = product.imageUrl,
                category = product.category,
                scannedAt = System.currentTimeMillis()
            )
        )
    }

    private fun entityToProduct(e: ProductEntity): ProductInfo {
        val specsMap = mutableMapOf<String, String>()
        try {
            val o = JSONObject(e.specsJson)
            val keys = o.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                specsMap[k] = o.optString(k, "")
            }
        } catch (_: Exception) {}

        val storesList = mutableListOf<StorePrice>()
        try {
            val a = JSONArray(e.storesJson)
            for (i in 0 until a.length()) {
                val o = a.optJSONObject(i) ?: continue
                storesList.add(
                    StorePrice(
                        storeName = o.optString("storeName", "Store"),
                        productTitle = o.optString("productTitle", e.name),
                        price = o.optDouble("price", 0.0),
                        currency = o.optString("currency", "$"),
                        productUrl = o.optString("productUrl", ""),
                        availability = o.optString("availability", "In Stock"),
                        matchConfidence = o.optInt("matchConfidence", 95),
                        lastUpdated = o.optString("lastUpdated", "Recently"),
                        rating = o.optDouble("rating", 4.5),
                        isBestMatch = o.optBoolean("isBestMatch", false)
                    )
                )
            }
        } catch (_: Exception) {}

        return ProductInfo(
            id = e.id,
            name = e.name,
            brand = e.brand,
            model = e.model,
            category = e.category,
            description = e.description,
            color = e.color,
            productType = e.productType,
            specifications = specsMap,
            confidence = e.confidence,
            barcode = e.barcode,
            imageUrl = e.imageUrl,
            stores = storesList,
            scannedAt = e.createdAt
        )
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap

        val ratio = w.toFloat() / h.toFloat()
        val newW: Int
        val newH: Int
        if (ratio > 1) {
            newW = maxDim
            newH = (maxDim / ratio).toInt()
        } else {
            newH = maxDim
            newW = (maxDim * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun saveImageLocally(bitmap: Bitmap): String {
        return try {
            val dir = File(context.filesDir, "scans")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "scan_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.w("ProductRepository", "Could not cache scan image", e)
            ""
        }
    }
}
