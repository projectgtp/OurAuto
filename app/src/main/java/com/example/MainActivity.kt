package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AutomationSuiteApp
import com.example.ui.AutomationViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Pre-create WebView Code Cache directories to satisfy Chromium simple_file_enumerator and prevent opendir warnings
    try {
      val bases = listOf(cacheDir, codeCacheDir, filesDir)
      bases.forEach { base ->
        if (base != null) {
          listOf(
            "WebView/Default/HTTP Cache/Code Cache/js",
            "WebView/Default/HTTP Cache/Code Cache/wasm"
          ).forEach { subPath ->
            val dir = java.io.File(base, subPath)
            if (!dir.exists()) {
              dir.mkdirs()
            }
            // Create a dummy placeholder file to ensure directory entry registers reliably
            try {
              val placeholder = java.io.File(dir, ".placeholder")
              if (!placeholder.exists()) {
                placeholder.createNewFile()
              }
            } catch (ignored: Exception) {}
          }
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    setContent {
      val app = applicationContext as Application
      val viewModel: AutomationViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
      )
      MyApplicationTheme(
        themeMode = viewModel.selectedThemeMode,
        themeAccent = viewModel.selectedThemeAccent
      ) {
        AutomationSuiteApp(viewModel = viewModel)
      }
    }
  }
}
