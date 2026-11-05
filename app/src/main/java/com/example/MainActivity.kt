package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.navigation.MainAppContainer
import com.example.ui.theme.ProductLensTheme
import com.example.ui.viewmodel.ProductLensViewModel
import com.example.util.configureAppCheck

class MainActivity : ComponentActivity() {

    private val viewModel: ProductLensViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            val themePreference by viewModel.themeMode.collectAsState()
            val isSystemDark = isSystemInDarkTheme()
            val isDark = when (themePreference) {
                "dark" -> true
                "light" -> false
                else -> isSystemDark
            }

            ProductLensTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppContainer(viewModel = viewModel)
                }
            }
        }
    }
}
