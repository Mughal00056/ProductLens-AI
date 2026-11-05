package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FavoriteEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.model.ProductInfo
import com.example.data.model.ScanningMode
import com.example.data.model.SortOption
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ProductRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "ProductLensViewModel"

sealed interface ScannerUiState {
    data object Idle : ScannerUiState
    data object Capturing : ScannerUiState
    data class Scanning(val statusMessage: String) : ScannerUiState
    data class Success(val product: ProductInfo) : ScannerUiState
    data class Error(val message: String) : ScannerUiState
}

class ProductLensViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProductRepository(application)
    private val preferencesRepository = UserPreferencesRepository(application)

    private val _scannerUiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val scannerUiState: StateFlow<ScannerUiState> = _scannerUiState.asStateFlow()

    private val _currentProduct = MutableStateFlow<ProductInfo?>(null)
    val currentProduct: StateFlow<ProductInfo?> = _currentProduct.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _currentSortOption = MutableStateFlow(SortOption.BEST_MATCH)
    val currentSortOption: StateFlow<SortOption> = _currentSortOption.asStateFlow()

    private val _scanningMode = MutableStateFlow(ScanningMode.AI_VISION)
    val scanningMode: StateFlow<ScanningMode> = _scanningMode.asStateFlow()

    val searchHistory: StateFlow<List<SearchHistoryEntity>> = repository.searchHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<FavoriteEntity>> = repository.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val themeMode: StateFlow<String> = preferencesRepository.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "dark")

    val preferredCurrency: StateFlow<String> = preferencesRepository.currencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "$")

    fun setScanningMode(mode: ScanningMode) {
        _scanningMode.value = mode
    }

    fun setSortOption(option: SortOption) {
        _currentSortOption.value = option
    }

    fun resetScannerState() {
        _scannerUiState.value = ScannerUiState.Idle
    }

    fun scanImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _scannerUiState.value = ScannerUiState.Scanning("Analyzing product optics with Gemini 3.8 Flash...")
            try {
                val curr = preferredCurrency.value
                val result = repository.scanProductImage(bitmap, curr)
                if (result.isSuccess) {
                    val product = result.getOrThrow()
                    _currentProduct.value = product
                    _scannerUiState.value = ScannerUiState.Success(product)
                    checkIsFavorite(product.id)
                } else {
                    val err = result.exceptionOrNull()?.localizedMessage ?: "Product recognition failed."
                    _scannerUiState.value = ScannerUiState.Error(err)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "scanImage failed", e)
                _scannerUiState.value = ScannerUiState.Error(e.localizedMessage ?: "Scan processing failed.")
            }
        }
    }

    fun scanBarcode(barcode: String) {
        // Prevent duplicate processing if already scanning
        if (_scannerUiState.value is ScannerUiState.Scanning) return

        viewModelScope.launch {
            _scannerUiState.value = ScannerUiState.Scanning("Barcode $barcode detected! Searching databases...")
            try {
                val curr = preferredCurrency.value
                val result = repository.scanBarcode(barcode, curr)
                if (result.isSuccess) {
                    val product = result.getOrThrow()
                    _currentProduct.value = product
                    _scannerUiState.value = ScannerUiState.Success(product)
                    checkIsFavorite(product.id)
                } else {
                    val err = result.exceptionOrNull()?.localizedMessage ?: "Barcode not found in catalog."
                    _scannerUiState.value = ScannerUiState.Error(err)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "scanBarcode failed", e)
                _scannerUiState.value = ScannerUiState.Error(e.localizedMessage ?: "Barcode lookup failed.")
            }
        }
    }

    fun searchProductByText(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _scannerUiState.value = ScannerUiState.Scanning("Searching catalog & price comparisons for '$query'...")
            try {
                val curr = preferredCurrency.value
                val result = repository.searchProductText(query.trim(), curr)
                if (result.isSuccess) {
                    val product = result.getOrThrow()
                    _currentProduct.value = product
                    _scannerUiState.value = ScannerUiState.Success(product)
                    checkIsFavorite(product.id)
                } else {
                    val err = result.exceptionOrNull()?.localizedMessage ?: "Search failed."
                    _scannerUiState.value = ScannerUiState.Error(err)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "searchProductByText failed", e)
                _scannerUiState.value = ScannerUiState.Error(e.localizedMessage ?: "Search failed.")
            }
        }
    }

    fun loadProductById(id: String) {
        viewModelScope.launch {
            val product = repository.getProductById(id)
            if (product != null) {
                _currentProduct.value = product
                checkIsFavorite(product.id)
            }
        }
    }

    fun toggleFavorite(product: ProductInfo) {
        viewModelScope.launch {
            val newFav = !_isFavorite.value
            _isFavorite.value = newFav
            repository.toggleFavorite(product, newFav)
        }
    }

    private fun checkIsFavorite(productId: String) {
        viewModelScope.launch {
            repository.isFavoriteFlow(productId).collect { fav ->
                _isFavorite.value = fav
            }
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(theme)
        }
    }

    fun setCurrency(currency: String) {
        viewModelScope.launch {
            preferencesRepository.setCurrency(currency)
        }
    }
}
