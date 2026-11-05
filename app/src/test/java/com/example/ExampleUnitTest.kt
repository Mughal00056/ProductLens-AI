package com.example

import com.example.data.model.StorePrice
import com.example.data.remote.ShoppingLinkBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testShoppingLinkBuilder() {
        val amazonUrl = ShoppingLinkBuilder.buildStoreUrl("Amazon", "Sony WH-1000XM5")
        assertTrue(amazonUrl.contains("amazon.com/s?k="))
        assertTrue(amazonUrl.contains("Sony"))

        val walmartUrl = ShoppingLinkBuilder.buildStoreUrl("Walmart", "Apple iPad Pro")
        assertTrue(walmartUrl.contains("walmart.com/search?q="))
    }

    @Test
    fun testStorePriceSorting() {
        val stores = listOf(
            StorePrice("Store A", "Product", 150.0, "$", "", isBestMatch = false),
            StorePrice("Store B", "Product", 99.0, "$", "", isBestMatch = true),
            StorePrice("Store C", "Product", 200.0, "$", "", isBestMatch = false)
        )

        val lowest = stores.sortedBy { it.price }
        assertEquals("Store B", lowest.first().storeName)
        assertEquals(99.0, lowest.first().price, 0.01)

        val highest = stores.sortedByDescending { it.price }
        assertEquals("Store C", highest.first().storeName)
    }
}
