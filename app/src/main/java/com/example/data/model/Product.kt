package com.example.data.model

data class ProductInfo(
    val id: String,
    val name: String,
    val brand: String,
    val model: String = "",
    val category: String = "General",
    val description: String = "",
    val color: String = "",
    val productType: String = "",
    val specifications: Map<String, String> = emptyMap(),
    val confidence: Int = 95,
    val barcode: String? = null,
    val imageUrl: String? = null,
    val stores: List<StorePrice> = emptyList(),
    val scannedAt: Long = System.currentTimeMillis()
)

data class StorePrice(
    val storeName: String,
    val productTitle: String,
    val price: Double,
    val currency: String = "$",
    val productUrl: String,
    val availability: String = "In Stock",
    val matchConfidence: Int = 95,
    val lastUpdated: String = "Just now",
    val rating: Double = 4.5,
    val isBestMatch: Boolean = false
)

enum class ScanningMode {
    AI_VISION,
    BARCODE
}

enum class SortOption(val displayName: String) {
    BEST_MATCH("Best Match"),
    LOWEST_PRICE("Lowest Price"),
    HIGHEST_PRICE("Highest Price")
}
