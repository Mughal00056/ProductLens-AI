package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val brand: String,
    val model: String,
    val category: String,
    val description: String,
    val imageUrl: String?,
    val barcode: String?,
    val confidence: Int,
    val color: String,
    val productType: String,
    val specsJson: String,
    val storesJson: String,
    val createdAt: Long
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: String,
    val productName: String,
    val brand: String,
    val imageUrl: String?,
    val category: String,
    val scannedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val productId: String,
    val productName: String,
    val brand: String,
    val imageUrl: String?,
    val category: String,
    val priceRange: String,
    val createdAt: Long = System.currentTimeMillis()
)
