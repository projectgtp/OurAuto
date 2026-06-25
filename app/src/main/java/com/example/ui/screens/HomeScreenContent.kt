package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.AppTab
import com.example.ui.AutomationViewModel
import com.example.ui.cleanUrlForComparison
import com.example.ui.screens.HomeDashboardPanel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HomeScreenContent(
    viewModel: AutomationViewModel,
    setWebViewRef: (WebView) -> Unit,
    onOpenTabsManager: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenDownloads: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var urlTextBarInput by remember(viewModel.activeBrowserUrl) { mutableStateOf(viewModel.activeBrowserUrl) }
    var showThreeDotMenu by remember { mutableStateOf(false) }
    var localWebViewRef by remember { mutableStateOf<WebView?>(null) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Premium Google Chrome style navigation and URL bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Chrome Back Navigation
            IconButton(
                onClick = { localWebViewRef?.goBack() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                    contentDescription = "Navigate back",
                    tint = if (localWebViewRef?.canGoBack() == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Chrome Forward Navigation
            IconButton(
                onClick = { localWebViewRef?.goForward() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward, 
                    contentDescription = "Navigate forward",
                    tint = if (localWebViewRef?.canGoForward() == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Chrome Home Button
            IconButton(
                onClick = {
                    viewModel.activeBrowserUrl = "https://www.google.com"
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home, 
                    contentDescription = "Go Home",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            // Rounded Chrome-style Omnibox
            OutlinedTextField(
                value = urlTextBarInput,
                onValueChange = { urlTextBarInput = it },
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("browser_address_bar"),
                placeholder = { Text("Search or type URL", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedBorderColor = Color.Transparent
                ),
                leadingIcon = {
                    val isSecure = urlTextBarInput.startsWith("https://")
                    Icon(
                        imageVector = if (isSecure) Icons.Default.Lock else Icons.Default.Info,
                        contentDescription = "Security Status",
                        tint = if (isSecure) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    val formatted = viewModel.formatSearchQuery(urlTextBarInput)
                    viewModel.activeBrowserUrl = formatted
                    keyboardController?.hide()
                }),
                trailingIcon = {
                    if (viewModel.isBrowserLoading) {
                        IconButton(
                            onClick = { localWebViewRef?.stopLoading() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Stop Loading",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                if (urlTextBarInput != viewModel.activeBrowserUrl) {
                                    val formatted = viewModel.formatSearchQuery(urlTextBarInput)
                                    viewModel.activeBrowserUrl = formatted
                                } else {
                                    localWebViewRef?.reload()
                                }
                                keyboardController?.hide()
                            },
                            modifier = Modifier.testTag("browser_go_button").size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (urlTextBarInput != viewModel.activeBrowserUrl) Icons.Default.Send else Icons.Default.Refresh,
                                contentDescription = "Refresh or Go",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            )

            // Modern Chrome-like Tabs Counter Button
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(6.dp))
                    .clickable { onOpenTabsManager() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = viewModel.browserTabs.size.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Beautiful standard 3-dot overflow menu trigger
            Box {
                IconButton(onClick = { showThreeDotMenu = true }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options menu")
                }

                DropdownMenu(
                    expanded = showThreeDotMenu,
                    onDismissRequest = { showThreeDotMenu = false },
                    modifier = Modifier
                        .width(190.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("AI Tools", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = "AI Tools", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            showThreeDotMenu = false
                            viewModel.currentTab = AppTab.AI
                            Toast.makeText(context, "Opened AI Tools", Toast.LENGTH_SHORT).show()
                        }
                    )
                    
                    DropdownMenuItem(
                        text = { 
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("New Tab", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                if (!viewModel.isPremiumUnlocked && viewModel.browserTabs.size >= 3) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked Feature",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = "New Tab Option", modifier = Modifier.size(16.dp)) },
                        onClick = {
                            showThreeDotMenu = false
                            if (!viewModel.isPremiumUnlocked && viewModel.browserTabs.size >= 3) {
                                viewModel.showPremiumUpgradeDialog = true
                                Toast.makeText(context, "Free tier is limited to 3 active tabs.", Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.addNewTab("https://www.google.com")
                                Toast.makeText(context, "Opened Google in a new tab!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    
                    DropdownMenuItem(
                        text = { Text("Open Tabs (${viewModel.browserTabs.size})", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Tabs list dropdown", modifier = Modifier.size(16.dp)) },
                        onClick = {
                            showThreeDotMenu = false
                            onOpenTabsManager()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("History Logs", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = "History list", modifier = Modifier.size(16.dp)) },
                        onClick = {
                            showThreeDotMenu = false
                            onOpenHistory()
                        }
                    )
                    
                    DropdownMenuItem(
                        text = { Text("Downloads", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = "Downloads list", modifier = Modifier.size(16.dp)) },
                        onClick = {
                            showThreeDotMenu = false
                            onOpenDownloads()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("In-Page Inspector", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary) },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = "Real element inspector", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            showThreeDotMenu = false
                            val js = """
                                (function() {
                                    if (window.eruda) {
                                        eruda.show();
                                        return;
                                    }
                                    var script = document.createElement('script');
                                    script.src = 'https://cdn.jsdelivr.net/npm/eruda';
                                    document.body.appendChild(script);
                                    script.onload = function() {
                                        eruda.init();
                                        eruda.show();
                                    };
                                })();
                            """.trimIndent()
                            localWebViewRef?.evaluateJavascript(js, null)
                            Toast.makeText(context, "Real element DOM inspector (Eruda) loaded! Tap the floating gear icon.", Toast.LENGTH_LONG).show()
                        }
                    )

                    DropdownMenuItem(
                        text = { 
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Ads Shield", fontSize = 13.sp)
                                if (!viewModel.isPremiumUnlocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked Feature",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else {
                                    Switch(
                                        checked = viewModel.isAdblockEnabled,
                                        onCheckedChange = { viewModel.isAdblockEnabled = it },
                                        thumbContent = null,
                                        modifier = Modifier.scale(0.7f)
                                    )
                                }
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.Warning, contentDescription = "Ad blocker toggle", modifier = Modifier.size(16.dp)) },
                        onClick = {
                            if (!viewModel.isPremiumUnlocked) {
                                showThreeDotMenu = false
                                viewModel.showPremiumUpgradeDialog = true
                            } else {
                                viewModel.isAdblockEnabled = !viewModel.isAdblockEnabled
                                Toast.makeText(context, if (viewModel.isAdblockEnabled) "Ads Shield enabled" else "Ads Shield disabled", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    DropdownMenuItem(
                        text = { 
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Desktop Site", fontSize = 13.sp)
                                if (!viewModel.isPremiumUnlocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked Feature",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else {
                                    Switch(
                                        checked = viewModel.isDesktopSiteEnabled,
                                        onCheckedChange = { viewModel.isDesktopSiteEnabled = it },
                                        thumbContent = null,
                                        modifier = Modifier.scale(0.7f)
                                    )
                                }
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.Monitor, contentDescription = "Desktop site toggle", modifier = Modifier.size(16.dp)) },
                        onClick = {
                            if (!viewModel.isPremiumUnlocked) {
                                showThreeDotMenu = false
                                viewModel.showPremiumUpgradeDialog = true
                            } else {
                                viewModel.isDesktopSiteEnabled = !viewModel.isDesktopSiteEnabled
                                Toast.makeText(context, if (viewModel.isDesktopSiteEnabled) "Desktop site enabled" else "Desktop site disabled", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Automator Tools", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Build, contentDescription = "Launch utilities panel", modifier = Modifier.size(16.dp)) },
                        onClick = {
                            showThreeDotMenu = false
                            onOpenTools()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Preferences", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = "Preferences screen", modifier = Modifier.size(16.dp)) },
                        onClick = {
                            showThreeDotMenu = false
                            viewModel.currentTab = AppTab.SETTINGS
                        }
                    )
                }
            }
        }

        // Mini divider
        LinearProgressIndicator(
            progress = if (viewModel.isBrowserLoading) 0.5f else 0f,
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Transparent
        )

        // Web Rendering panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isCustomLaunchCenter = viewModel.activeBrowserUrl == "https://www.google.com" || viewModel.activeBrowserUrl.trim().lowercase().contains("google.com") || viewModel.activeBrowserUrl.isBlank()
            if (isCustomLaunchCenter) {
                HomeDashboardPanel(viewModel, localWebViewRef)
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize().testTag("browser_webview"),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            textZoom = 100
                        }
                        setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                            viewModel.triggerDownload(url, userAgent, contentDisposition, mimetype, contentLength)
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: return false
                                if (handleWebViewScheme(view, ctx, url)) {
                                    return true
                                }
                                if (isDownloadableUrl(url)) {
                                    viewModel.triggerDownload(url, view?.settings?.userAgentString, null, null, 0L)
                                    return true
                                }
                                return false
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                viewModel.isBrowserLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                viewModel.isBrowserLoading = false
                                url?.let {
                                    viewModel.onBrowserUrlChanged(it, view?.title)
                                    // Live inject standard ads script blocking selector routines
                                    if (viewModel.isAdblockEnabled) {
                                        injectAdblockJS(view)
                                    }
                                    
                                    // Real custom Chrome Extensions matches injection
                                    val currentUrl = url ?: ""
                                    viewModel.installedExtensions.forEach { ext ->
                                        if (ext.isEnabled) {
                                            val pattern = ext.urlMatchPattern
                                            val shouldInject = pattern == "*" || pattern.trim().isEmpty() || currentUrl.contains(pattern, ignoreCase = true)
                                            if (shouldInject) {
                                                view?.evaluateJavascript("javascript:(function() { " + ext.jsContent + " })();", null)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                return true
                            }
                        }
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onElementClickTriggered(selector: String, tag: String) {
                                viewModel.recordStepEvent("CLICK", selector, tag)
                            }
                            @JavascriptInterface
                            fun onElementInputTriggered(selector: String, txtVal: String) {
                                viewModel.recordStepEvent("INPUT", selector, txtVal)
                            }
                            @JavascriptInterface
                            fun onElementActionTriggered(actionType: String, selector: String, value: String) {
                                viewModel.recordStepEvent(actionType, selector, value)
                            }
                            @JavascriptInterface
                            fun onScrollEvent(x: Int, y: Int) {
                                viewModel.recordStepEvent("SCROLL", "window", "$x,$y")
                            }
                            @JavascriptInterface
                            fun onStepExecutionResult(stepId: String, status: String, message: String) {
                                viewModel.onStepExecutionResult(stepId, status, message)
                            }
                        }, "OurAutoJavascriptBridge")

                        loadUrl(viewModel.activeBrowserUrl)
                        setWebViewRef(this)
                        localWebViewRef = this
                    }
                },
                update = { view ->
                    val resolvedUa = if (viewModel.isDesktopSiteEnabled) {
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    } else {
                        when (viewModel.currentUserAgentType) {
                            "Desktop Chrome" -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                            "iPhone iOS Safari" -> "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1"
                            "Custom" -> viewModel.customUserAgentString.ifBlank { "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36" }
                            else -> "Default"
                        }
                    }
                    val currentUa = view.settings.userAgentString ?: "Default"
                    if (resolvedUa == "Default") {
                        if (view.settings.userAgentString != null) {
                            view.settings.userAgentString = null
                            view.reload()
                        }
                    } else if (currentUa != resolvedUa) {
                        view.settings.userAgentString = resolvedUa
                        view.reload()
                    }

                    val cleanedView = cleanUrlForComparison(view.url)
                    val cleanedTarget = cleanUrlForComparison(viewModel.activeBrowserUrl)
                    if (cleanedView != cleanedTarget && !viewModel.activeBrowserUrl.startsWith("javascript:") && viewModel.activeBrowserUrl.isNotBlank()) {
                        view.loadUrl(viewModel.activeBrowserUrl)
                    }
                }
            )
            }
        }

        // Execution Log Panel — visible when a routine is playing or just failed
        val showLog = viewModel.isPlaying || viewModel.showStepFailureDialog
        AnimatedVisibility(
            visible = showLog,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            ExecutionLogPanel(viewModel = viewModel)
        }
    }
}

@Composable
fun ExecutionLogPanel(viewModel: AutomationViewModel) {
    val traceSteps = viewModel.runTraceSteps.value
    val failedInfo = viewModel.lastFailedStepInfo

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp, max = 220.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (viewModel.isPlaying) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        text = if (viewModel.isPlaying) "Running: ${viewModel.activeRunningWorkflowName}" else "Step Failed",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (viewModel.isPlaying) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                IconButton(onClick = { viewModel.stopExecution() }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }

            // Step failure card
            if (failedInfo != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Badge(containerColor = MaterialTheme.colorScheme.error) {
                                Text(failedInfo.first, fontSize = 9.sp, color = Color.White)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = failedInfo.second.take(60),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = failedInfo.third,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { viewModel.retryCurrentStep() },
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("Retry", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.skipCurrentStep() },
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) { Text("Skip Step", fontSize = 10.sp) }
                            OutlinedButton(
                                onClick = { viewModel.stopExecution() },
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            ) { Text("Stop", fontSize = 10.sp) }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { viewModel.currentTab = AppTab.CREATOR },
                            modifier = Modifier.height(26.dp).fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Edit Step in Creator", fontSize = 10.sp)
                        }
                    }
                }
            }

            // Trace steps list
            if (traceSteps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(traceSteps) { idx, trace ->
                        val isActive = idx == viewModel.currentExecutingStepIndex
                        val statusColor = when (trace.status) {
                            "SUCCESS" -> Color(0xFF10B981)
                            "FAILED" -> MaterialTheme.colorScheme.error
                            "EXECUTING" -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.5.dp)
                                .background(
                                    if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(7.dp).background(statusColor, CircleShape))
                            Text(
                                text = trace.step.type,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.widthIn(min = 52.dp)
                            )
                            Text(
                                text = trace.step.target.take(55).ifBlank { trace.step.value.take(55) },
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (trace.status == "EXECUTING") {
                                CircularProgressIndicator(modifier = Modifier.size(9.dp), strokeWidth = 1.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Intercepts and shields generic ads class tags
private fun injectAdblockJS(view: WebView?) {
    val js = """
        (function() {
            var selectors = [
                '.ads', '.ad-box', '.ad-banner', '.advertisement', 
                '[id*="google_ads_iframe"]', '[class*="sponsored"]',
                '.sponsor-container', '[class*="advert-"]', '[id*="ad-"]'
            ];
            selectors.forEach(function(sel) {
                document.querySelectorAll(sel).forEach(function(el) {
                    el.style.display = 'none';
                    console.log("Shielded Ad Container of element selector style: " + sel);
                });
            });
        })();
    """.trimIndent()
    view?.evaluateJavascript(js, null)
}

