package com.example.ui.navigation

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScanningMode
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.LaserCrimson
import com.example.ui.theme.NeonRed
import com.example.ui.theme.RubyRed
import com.example.ui.viewmodel.ProductLensViewModel

enum class ScreenTab {
    HOME,
    HISTORY,
    FAVORITES,
    SETTINGS,
    SCANNER,
    PRODUCT_DETAIL
}

@Composable
fun MainAppContainer(
    viewModel: ProductLensViewModel
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(ScreenTab.HOME) }
    var previousTab by remember { mutableStateOf(ScreenTab.HOME) }
    var scannerInitialMode by remember { mutableStateOf(ScanningMode.AI_VISION) }

    // Pick from gallery from home or elsewhere
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        viewModel.scanImage(bitmap)
                        previousTab = currentTab
                        currentTab = ScreenTab.PRODUCT_DETAIL
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error opening image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Handle Android system back button
    BackHandler(enabled = currentTab != ScreenTab.HOME) {
        if (currentTab == ScreenTab.SCANNER || currentTab == ScreenTab.PRODUCT_DETAIL) {
            currentTab = previousTab
        } else {
            currentTab = ScreenTab.HOME
        }
    }

    Scaffold(
        bottomBar = {
            if (currentTab != ScreenTab.SCANNER && currentTab != ScreenTab.PRODUCT_DETAIL) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.border(0.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    NavigationBarItem(
                        selected = currentTab == ScreenTab.HOME,
                        onClick = { currentTab = ScreenTab.HOME },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonRed,
                            selectedTextColor = NeonRed,
                            indicatorColor = Color(0x22FF1744)
                        ),
                        modifier = Modifier.testTag("nav_tab_home")
                    )

                    NavigationBarItem(
                        selected = currentTab == ScreenTab.HISTORY,
                        onClick = { currentTab = ScreenTab.HISTORY },
                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                        label = { Text("History", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonRed,
                            selectedTextColor = NeonRed,
                            indicatorColor = Color(0x22FF1744)
                        ),
                        modifier = Modifier.testTag("nav_tab_history")
                    )

                    NavigationBarItem(
                        selected = currentTab == ScreenTab.FAVORITES,
                        onClick = { currentTab = ScreenTab.FAVORITES },
                        icon = { Icon(Icons.Default.Bookmark, contentDescription = "Favorites") },
                        label = { Text("Favorites", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonRed,
                            selectedTextColor = NeonRed,
                            indicatorColor = Color(0x22FF1744)
                        ),
                        modifier = Modifier.testTag("nav_tab_favorites")
                    )

                    NavigationBarItem(
                        selected = currentTab == ScreenTab.SETTINGS,
                        onClick = { currentTab = ScreenTab.SETTINGS },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonRed,
                            selectedTextColor = NeonRed,
                            indicatorColor = Color(0x22FF1744)
                        ),
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTab != ScreenTab.SCANNER && currentTab != ScreenTab.PRODUCT_DETAIL) {
                FloatingActionButton(
                    onClick = {
                        scannerInitialMode = ScanningMode.AI_VISION
                        previousTab = currentTab
                        currentTab = ScreenTab.SCANNER
                    },
                    containerColor = NeonRed,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .size(60.dp)
                        .border(2.dp, Color(0x66FFFFFF), CircleShape)
                        .shadow(12.dp, CircleShape)
                        .testTag("main_scanner_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Scan Product",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                ScreenTab.HOME -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToScanner = { mode ->
                            scannerInitialMode = mode
                            previousTab = ScreenTab.HOME
                            currentTab = ScreenTab.SCANNER
                        },
                        onNavigateToProduct = { _ ->
                            previousTab = ScreenTab.HOME
                            currentTab = ScreenTab.PRODUCT_DETAIL
                        },
                        onPickGalleryImage = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }

                ScreenTab.HISTORY -> {
                    HistoryScreen(
                        viewModel = viewModel,
                        onNavigateToProduct = { _ ->
                            previousTab = ScreenTab.HISTORY
                            currentTab = ScreenTab.PRODUCT_DETAIL
                        }
                    )
                }

                ScreenTab.FAVORITES -> {
                    FavoritesScreen(
                        viewModel = viewModel,
                        onNavigateToProduct = { _ ->
                            previousTab = ScreenTab.FAVORITES
                            currentTab = ScreenTab.PRODUCT_DETAIL
                        }
                    )
                }

                ScreenTab.SETTINGS -> {
                    SettingsScreen(viewModel = viewModel)
                }

                ScreenTab.SCANNER -> {
                    ScannerScreen(
                        viewModel = viewModel,
                        initialMode = scannerInitialMode,
                        onNavigateBack = {
                            currentTab = previousTab
                        },
                        onProductIdentified = { _ ->
                            currentTab = ScreenTab.PRODUCT_DETAIL
                        }
                    )
                }

                ScreenTab.PRODUCT_DETAIL -> {
                    ProductDetailScreen(
                        viewModel = viewModel,
                        onNavigateBack = {
                            currentTab = previousTab
                        },
                        onScanAgain = {
                            scannerInitialMode = ScanningMode.AI_VISION
                            currentTab = ScreenTab.SCANNER
                        }
                    )
                }
            }
        }
    }
}
