package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.scale
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
import com.example.data.WorkflowStep
import com.example.ui.AppTab
import com.example.ui.AutomationViewModel
import com.example.ui.cleanUrlForComparison

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CreatorScreenContent(
    viewModel: AutomationViewModel,
    setWebViewRef: (WebView) -> Unit,
    onAddManualStep: () -> Unit,
    onOpenDownloads: () -> Unit
) {
    val context = LocalContext.current
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var localUrlInput by remember { mutableStateOf(viewModel.activeBrowserUrl) }
    var showInfoExplanationDialog by remember { mutableStateOf(false) }
    var showThreeDotMenu by remember { mutableStateOf(false) }
    var localWebViewRef by remember { mutableStateOf<WebView?>(null) }
    var isDashboardExpanded by remember { mutableStateOf(false) }
    var dashboardTab by remember { mutableStateOf("Steps") } // "Steps", "AI"

    // Dynamic polling of actual permission status in the OS
    LaunchedEffect(Unit) {
        while (true) {
            hasOverlayPermission = Settings.canDrawOverlays(context)
            kotlinx.coroutines.delay(2000)
        }
    }

    LaunchedEffect(viewModel.activeBrowserUrl) {
        localUrlInput = viewModel.activeBrowserUrl
    }

    if (showInfoExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showInfoExplanationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = "Active info logo", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("What Gets Recorded", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "This records your actions — not a video.",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Every tap, text input, and page visit is saved as a step (CLICK, INPUT, NAVIGATE). When you play it back, the app replays those exact steps automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showInfoExplanationDialog = false }) {
                    Text("Understand")
                }
            }
        )
    }

    val openOverlaySettings = {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
            Toast.makeText(context, "Please allow overlay permission", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            context.startActivity(intent)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Streamlined browser navigation & active recorder status address bar
        Card(
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Chrome-like Back Button inside Recorder
                    IconButton(
                        onClick = {
                            localWebViewRef?.goBack()
                            if (viewModel.isRecording) {
                                viewModel.recordStepEvent("BACK", "browser", "Go Back")
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Browser step back tool",
                            tint = if (localWebViewRef?.canGoBack() == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Chrome-like Forward Button inside Recorder
                    IconButton(
                        onClick = {
                            localWebViewRef?.goForward()
                            if (viewModel.isRecording) {
                                viewModel.recordStepEvent("FORWARD", "browser", "Go Forward")
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Browser step forward tool",
                            tint = if (localWebViewRef?.canGoForward() == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Chrome-like Home Button inside Recorder
                    IconButton(
                        onClick = {
                            val homeUrl = "https://www.google.com"
                            viewModel.activeBrowserUrl = homeUrl
                            if (viewModel.isRecording) {
                                viewModel.recordStepEvent("NAVIGATE", homeUrl, "Go Home")
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Navigate to homepage",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Compact modern address text field inside creator
                    OutlinedTextField(
                        value = localUrlInput,
                        onValueChange = { localUrlInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        textStyle = TextStyle(fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                if (localUrlInput.isNotEmpty()) {
                                    val targetUrl = if (!localUrlInput.startsWith("http://") && !localUrlInput.startsWith("https://")) {
                                        "https://$localUrlInput"
                                    } else {
                                        localUrlInput
                                    }
                                    viewModel.activeBrowserUrl = targetUrl
                                    if (viewModel.isRecording) {
                                        viewModel.recordStepEvent("NAVIGATE", targetUrl, "Manual Enter")
                                    }
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(20.dp),
                        placeholder = { Text("Search or type URL", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingIcon = {
                            val isSecure = localUrlInput.startsWith("https://")
                            Icon(
                                imageVector = if (isSecure) Icons.Default.Lock else Icons.Default.Info,
                                contentDescription = "Security Status",
                                tint = if (isSecure) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        trailingIcon = {
                            if (localUrlInput.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        if (localUrlInput != viewModel.activeBrowserUrl) {
                                            val targetUrl = if (!localUrlInput.startsWith("http://") && !localUrlInput.startsWith("https://")) {
                                                "https://$localUrlInput"
                                            } else {
                                                localUrlInput
                                            }
                                            viewModel.activeBrowserUrl = targetUrl
                                            if (viewModel.isRecording) {
                                                viewModel.recordStepEvent("NAVIGATE", targetUrl, "Go Trigger")
                                            }
                                        } else {
                                            localUrlInput = ""
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (localUrlInput != viewModel.activeBrowserUrl) Icons.Default.Send else Icons.Default.Clear,
                                        contentDescription = "Search action navigate",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    )

                    Box {
                        IconButton(onClick = { showThreeDotMenu = true }, modifier = Modifier.size(32.dp)) {
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
                                text = { Text("Downloads", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = "Downloads list", modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    showThreeDotMenu = false
                                    onOpenDownloads()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Page Inspector", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary) },
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
                                        Text("Desktop Site", fontSize = 13.sp)
                                        Switch(
                                            checked = viewModel.isDesktopSiteEnabled,
                                            onCheckedChange = { viewModel.isDesktopSiteEnabled = it },
                                            thumbContent = null,
                                            modifier = Modifier.scale(0.7f)
                                        )
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Monitor, contentDescription = "Desktop site toggle", modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    viewModel.isDesktopSiteEnabled = !viewModel.isDesktopSiteEnabled
                                    Toast.makeText(context, if (viewModel.isDesktopSiteEnabled) "Desktop site enabled" else "Desktop site disabled", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Automation Help", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Help, contentDescription = "Automation Help", modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    showThreeDotMenu = false
                                    showInfoExplanationDialog = true
                                }
                            )
                        }
                    }
                }

                // Status bar indicating active live recording session
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (viewModel.isRecording) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    if (viewModel.isRecording) {
                                        viewModel.isRecording = false
                                        Toast.makeText(context, "Recorder paused.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.isRecording = true
                                        Toast.makeText(context, "Recorder resumed! Tap elements to log.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (viewModel.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (viewModel.isRecording) "REC ON" else "REC OFF",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (viewModel.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = viewModel.isRecording,
                                onCheckedChange = { checked ->
                                    viewModel.isRecording = checked
                                    if (checked) {
                                        Toast.makeText(context, "Recording started", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Recording paused", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.scale(0.6f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                onClick = {
                                    viewModel.isTemplateMode = !viewModel.isTemplateMode
                                    Toast.makeText(context, if (viewModel.isTemplateMode) "Template Mode — variables enabled" else "Exact Mode — replays precisely", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (viewModel.isTemplateMode)
                                    MaterialTheme.colorScheme.secondaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.testTag("mode_toggle_header")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (viewModel.isTemplateMode) Icons.Default.Code else Icons.Default.FiberManualRecord,
                                        contentDescription = "Mode indicator",
                                        tint = if (viewModel.isTemplateMode)
                                            MaterialTheme.colorScheme.secondary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (viewModel.isTemplateMode) "Template" else "Exact",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (viewModel.isTemplateMode)
                                            MaterialTheme.colorScheme.secondary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Display Overlay Permission Status row
                        if (!hasOverlayPermission) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { openOverlaySettings() }
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = "Overlay setting", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Overlay Disabled (Click shortcut to enable)", fontSize = 8.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Overlay OK", tint = Color(0xFF2E7D32), modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Overlay Enabled", fontSize = 8.sp, color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Active Automated WebView Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize().testTag("browser_webview_creator"),
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
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                url?.let {
                                    viewModel.onBrowserUrlChanged(it, view?.title)
                                    // Smart inject recording interceptors
                                    if (viewModel.isRecording) {
                                        injectLiveInterceptorsJS(view, viewModel.isTemplateMode)
                                    }
                                    
                                    // Injected Browser Extensions
                                    if (viewModel.isDarkThemeExtensionEnabled) {
                                        val darkJs = "javascript:(function() { var style = document.createElement('style'); style.id = 'our-auto-dark'; style.innerHTML = 'html { filter: invert(0.85) hue-rotate(180deg) !important; } img, video, svg, iframe { filter: invert(1) hue-rotate(180deg) !important; }'; document.documentElement.appendChild(style); })();"
                                        view?.evaluateJavascript(darkJs, null)
                                    }
                                    if (viewModel.isFpsHudExtensionEnabled) {
                                        val fpsJs = "javascript:(function() { if (document.getElementById('our-auto-fps')) return; var container = document.createElement('div'); container.id = 'our-auto-fps'; container.style.position = 'fixed'; container.style.bottom = '8px'; container.style.left = '8px'; container.style.padding = '4px 8px'; container.style.background = 'rgba(15, 23, 42, 0.95)'; container.style.color = '#10B981'; container.style.fontFamily = 'monospace'; container.style.fontSize = '9px'; container.style.borderRadius = '4px'; container.style.border = '1px solid #10B981'; container.style.zIndex = '999999'; container.style.pointerEvents = 'none'; container.innerText = 'FPS: --'; document.body.appendChild(container); var lastTime = performance.now(); var frames = 0; function tick() { frames++; var now = performance.now(); if (now >= lastTime + 1000) { var fps = Math.round((frames * 1000) / (now - lastTime)); container.innerText = 'PERFORMANCE FPS: ' + fps; frames = 0; lastTime = now; } requestAnimationFrame(tick); } requestAnimationFrame(tick); })();"
                                        view?.evaluateJavascript(fpsJs, null)
                                    }
                                    if (viewModel.isAutoRefreshExtensionEnabled) {
                                        val refreshJs = "javascript:(function() { if (window.ourAutoRefreshTimer) clearTimeout(window.ourAutoRefreshTimer); window.ourAutoRefreshTimer = setTimeout(function() { window.location.reload(); }, 30000); })();"
                                        view?.evaluateJavascript(refreshJs, null)
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

        // Collapsible high-performance macro control dashboard drawer
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isDashboardExpanded) 340.dp else 56.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 12.dp
        ) {
            if (isDashboardExpanded) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Toolbar row containing: (1) Minimize icon, (2) Capture Actions title/badge, (3) Clear button
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { isDashboardExpanded = false }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize dashboard", modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "${viewModel.recordedSteps.value.size} Captured Actions",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(onClick = onAddManualStep, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Add, tint = MaterialTheme.colorScheme.primary, contentDescription = "Add Manual action", modifier = Modifier.size(16.dp))
                            }
                            if (viewModel.recordedSteps.value.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        viewModel.recordedSteps.value = emptyList()
                                        Toast.makeText(context, "Cleared action draft!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, tint = MaterialTheme.colorScheme.error, contentDescription = "Clear all actions", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { dashboardTab = "Steps" },
                            label = { Text("Actions Queue (${viewModel.recordedSteps.value.size})", fontSize = 10.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (dashboardTab == "Steps") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        )
                        AssistChip(
                            onClick = { dashboardTab = "AI" },
                            label = { Text("AI Smart Detector", fontSize = 10.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (dashboardTab == "AI") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        )
                    }

                    if (dashboardTab == "Steps") {
                        // Mode Switcher — card-based, shows both modes side by side
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(false, true).forEach { isTemplate ->
                                val isSelected = viewModel.isTemplateMode == isTemplate
                                Surface(
                                    onClick = { viewModel.isTemplateMode = isTemplate },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag(if (isTemplate) "template_mode_card" else "exact_mode_card"),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 0.6.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    tonalElevation = if (isSelected) 4.dp else 0.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isTemplate) Icons.Default.Code else Icons.Default.FiberManualRecord,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Column {
                                            Text(
                                                text = if (isTemplate) "Template" else "Exact",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isTemplate) "{{VAR}} placeholders" else "Precise replay",
                                                fontSize = 8.sp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Template variables live extractor — visible only in Template Mode
                    if (viewModel.isTemplateMode) {
                        val variableRegex = remember { Regex("\\{\\{([A-Z0-9_]+)\\}\\}") }
                        val detectedVars = remember(viewModel.recordedSteps.value) {
                            viewModel.recordedSteps.value
                                .flatMap { step ->
                                    variableRegex.findAll(step.target).map { it.groupValues[1] } +
                                    variableRegex.findAll(step.value).map { it.groupValues[1] }
                                }
                                .distinct()
                        }
                        if (detectedVars.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                                ),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.secondary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Template Variables (${detectedVars.size})", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    detectedVars.take(6).forEach { varName ->
                                        Text(
                                            text = "  {{$varName}}",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 1.dp)
                                        )
                                    }
                                    if (detectedVars.size > 6) {
                                        Text("  +${detectedVars.size - 6} more…", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        } else if (viewModel.recordedSteps.value.isNotEmpty()) {
                            Text(
                                text = "No {{VAR}} tokens detected yet — typed text is auto-wrapped into variables.",
                                fontSize = 8.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!viewModel.isRecording) {
                            val emptySteps = viewModel.recordedSteps.value.isEmpty()
                            Button(
                                onClick = {
                                    if (emptySteps) {
                                        viewModel.startNewRecording()
                                        Toast.makeText(context, "Recording started", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.isRecording = true
                                        Toast.makeText(context, "Recording resumed", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.height(30.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Launch recording icon", modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (emptySteps) "Start Recording" else "Resume", fontSize = 10.sp)
                            }
                        } else {
                            var showNamingDialog by remember { mutableStateOf(false) }
                            if (showNamingDialog) {
                                Dialog(onDismissRequest = { showNamingDialog = false }) {
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text("Save Workflow", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = viewModel.routineName,
                                                onValueChange = { viewModel.routineName = it },
                                                label = { Text("Workflow Name") },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                TextButton(onClick = { showNamingDialog = false }) { Text("Cancel") }
                                                Button(onClick = {
                                                    viewModel.stopAndSaveRecording()
                                                    showNamingDialog = false
                                                    Toast.makeText(context, "Workflow saved to Library!", Toast.LENGTH_SHORT).show()
                                                }) {
                                                    Text("Save")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Button(
                                onClick = { showNamingDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.height(30.dp).testTag("save_flow"),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Tick confirm save", modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop & Save", fontSize = 10.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Edit step dialog state
                    var editingStep by remember { mutableStateOf<WorkflowStep?>(null) }
                    editingStep?.let { step ->
                        var editedType by remember(step.id) { mutableStateOf(step.type) }
                        var editedTarget by remember(step.id) { mutableStateOf(step.target) }
                        var editedValue by remember(step.id) { mutableStateOf(step.value) }
                        AlertDialog(
                            onDismissRequest = { editingStep = null },
                            title = { Text("Edit Step", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = editedType,
                                        onValueChange = { editedType = it.uppercase() },
                                        label = { Text("Type", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = editedTarget,
                                        onValueChange = { editedTarget = it },
                                        label = { Text("Target / Selector", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = editedValue,
                                        onValueChange = { editedValue = it },
                                        label = { Text("Value", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    viewModel.updateRecordedStep(step.copy(type = editedType.ifBlank { step.type }, target = editedTarget, value = editedValue))
                                    editingStep = null
                                }) { Text("Save") }
                            },
                            dismissButton = {
                                TextButton(onClick = { editingStep = null }) { Text("Cancel") }
                            }
                        )
                    }

                    // Captured Steps dynamic list display
                    if (viewModel.recordedSteps.value.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No actions captured yet. Tap components or navigate to log events.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(viewModel.recordedSteps.value.reversed()) { recorded ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                                        .padding(6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Badge(
                                            containerColor = when (recorded.type) {
                                                "CLICK" -> Color(0xFF00C9A7)
                                                "INPUT" -> Color(0xFF7C6FFF)
                                                "NAVIGATE" -> Color(0xFF9D8FFF)
                                                "SCROLL" -> Color(0xFFFF6B6B)
                                                "BACK" -> Color(0xFFFF8C6B)
                                                "FORWARD" -> Color(0xFFFFD166)
                                                else -> Color(0xFF6B7280)
                                            }
                                        ) {
                                            Text(recorded.type, color = Color.White, fontSize = 8.sp)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = recorded.target,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (recorded.value.isNotBlank()) {
                                                Text(
                                                    text = "Value: ${recorded.value}",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 8.5.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.testSingleStep(recorded) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Test step", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f), modifier = Modifier.size(12.dp))
                                    }
                                    IconButton(
                                        onClick = { editingStep = recorded },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit step", tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f), modifier = Modifier.size(12.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeRecordedStep(recorded) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Delete step icon", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                    
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            if (viewModel.isAiAnalyzing) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(viewModel.aiStatusMessage, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            } else if (viewModel.aiAnalysisResult == null) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                                ) {
                                    Icon(Icons.Default.Build, contentDescription = "AI helper", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Analyze CSS webelement selectors dynamically using Gemini AI on raw HTML source.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { 
                                            localWebViewRef?.let { wv ->
                                                wv.evaluateJavascript("(function() { return document.documentElement.outerHTML; })();") { htmlStr ->
                                                    var cleanedStr = htmlStr ?: ""
                                                    if (cleanedStr.startsWith("\"") && cleanedStr.endsWith("\"")) {
                                                        cleanedStr = cleanedStr.substring(1, cleanedStr.length - 1)
                                                    }
                                                    cleanedStr = cleanedStr
                                                        .replace("\\u003C", "<")
                                                        .replace("\\u003E", ">")
                                                        .replace("\\u0026", "&")
                                                        .replace("\\\"", "\"")
                                                        .replace("\\'", "'")
                                                        .replace("\\n", "\n")
                                                        .replace("\\r", "\r")
                                                        .replace("\\t", "\t")
                                                        .replace("\\\\", "\\")
                                                    
                                                    viewModel.pageSourceHtml = cleanedStr
                                                    viewModel.runSmartPageAnalysis()
                                                }
                                            } ?: viewModel.runSmartPageAnalysis()
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)
                                    ) {
                                        Text("Run Smart AI Detector", fontSize = 10.sp)
                                    }
                                }
                            } else {
                                SelectionContainer {
                                    Text(
                                        text = viewModel.aiAnalysisResult ?: "",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        localWebViewRef?.let { wv ->
                                            wv.evaluateJavascript("(function() { return document.documentElement.outerHTML; })();") { htmlStr ->
                                                var cleanedStr = htmlStr ?: ""
                                                if (cleanedStr.startsWith("\"") && cleanedStr.endsWith("\"")) {
                                                    cleanedStr = cleanedStr.substring(1, cleanedStr.length - 1)
                                                }
                                                cleanedStr = cleanedStr
                                                    .replace("\\u003C", "<")
                                                    .replace("\\u003E", ">")
                                                    .replace("\\u0026", "&")
                                                    .replace("\\\"", "\"")
                                                    .replace("\\'", "'")
                                                    .replace("\\n", "\n")
                                                    .replace("\\r", "\r")
                                                    .replace("\\t", "\t")
                                                    .replace("\\\\", "\\")
                                                    
                                                viewModel.pageSourceHtml = cleanedStr
                                                viewModel.runSmartPageAnalysis()
                                            }
                                        } ?: viewModel.runSmartPageAnalysis()
                                    },
                                    modifier = Modifier.align(Alignment.CenterHorizontally).height(28.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    Text("Re-Analyze Page", fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.clickable { isDashboardExpanded = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (viewModel.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        )
                        Text(
                            text = if (viewModel.isRecording) "RECORDING" else "IDLE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = if (viewModel.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "(${viewModel.recordedSteps.value.size} steps queued)",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (viewModel.isRecording) {
                                    viewModel.isRecording = false
                                    Toast.makeText(context, "Recorder paused.", Toast.LENGTH_SHORT).show()
                                } else {
                                    if (viewModel.recordedSteps.value.isEmpty()) {
                                        viewModel.startNewRecording()
                                    } else {
                                        viewModel.isRecording = true
                                    }
                                    Toast.makeText(context, "Recorder resumed!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (viewModel.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (viewModel.isRecording) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = "Quick record toggle", 
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (viewModel.isRecording) "Pause" else "Record", fontSize = 10.sp)
                        }
                        
                        OutlinedButton(
                            onClick = { isDashboardExpanded = true },
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Expand ▲", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Javascript bridge for Creator step records
private fun injectLiveInterceptorsJS(view: WebView?, isTemplateMode: Boolean) {
    val js = """
        (function() {
            if (window.hasBrowserRecInjected) return;
            window.hasBrowserRecInjected = true;

            console.log("Developer dynamic interceptors active. Template Mode: $isTemplateMode");

            // Construct extremely precise, resilient CSS selectors tree
            function getElementSelector(el) {
                if (!el) return "";
                if (el.id) {
                    return "#" + el.id;
                }
                var parts = [];
                while (el && el.nodeType === Node.ELEMENT_NODE) {
                    var tag = el.tagName.toLowerCase();
                    var nameAttr = el.getAttribute('name');
                    var typeAttr = el.getAttribute('type');
                    if (el.id) {
                        parts.unshift(tag + '#' + el.id);
                        break;
                    } else if (nameAttr) {
                        parts.unshift(tag + '[name="' + nameAttr + '"]');
                        break;
                    } else {
                        var siblingIndex = 1;
                        var sib = el.previousElementSibling;
                        while (sib) {
                            if (sib.tagName === el.tagName) {
                                siblingIndex++;
                            }
                            sib = sib.previousElementSibling;
                        }
                        var classes = "";
                        if (el.className && typeof el.className === 'string') {
                            var classList = el.className.trim().split(/\s+/).filter(Boolean);
                            if (classList.length > 0) {
                                classes = "." + classList.slice(0, 2).join('.');
                            }
                        }
                        if (siblingIndex > 1) {
                            parts.unshift(tag + classes + ':nth-of-type(' + siblingIndex + ')');
                        } else {
                            parts.unshift(tag + classes);
                        }
                    }
                    el = el.parentElement;
                }
                return parts.join(' > ');
            }

            function getBehavioralSelector(el) {
                if (!el) return "";
                var tag = el.tagName.toLowerCase();
                
                // 1. Check for text inside anchors or buttons
                var text = el.textContent ? el.textContent.trim().replace(/[\r\n\t]+/g, ' ') : "";
                if (text && text.length > 0 && text.length < 40) {
                    if (tag === 'button' || el.closest('button') || tag === 'a' || el.closest('a')) {
                        return "TEXT:" + text.substring(0, 30);
                    }
                }
                
                // 2. Check for placeholder attributes in input fields
                var placeholder = el.getAttribute('placeholder');
                if (placeholder) {
                    return "PLACEHOLDER:" + placeholder.trim();
                }
                
                // 3. Check for specific name attributes
                var name = el.getAttribute('name');
                if (name) {
                    return "NAME:" + name.trim();
                }
                
                return getElementSelector(el);
            }

            function getSmartSelectorChain(el) {
                if (!el) return "";
                var candidates = [];
                
                // 1. Check ID (if not generic/dynamic looking long numeric string)
                if (el.id && el.id.length < 50 && !/\d{5,}/.test(el.id)) {
                    candidates.push("#" + el.id);
                }
                
                // 2. Data Attributes commonly used in enterprise development
                var dataAttrs = ['data-testid', 'data-cy', 'data-automation', 'data-qa', 'data-id'];
                for (var i = 0; i < dataAttrs.length; i++) {
                    var attr = el.getAttribute(dataAttrs[i]);
                    if (attr) {
                        candidates.push("DATA:" + attr);
                        candidates.push("[" + dataAttrs[i] + "='" + attr + "']");
                    }
                }
                
                // 3. Name Attribute
                var name = el.getAttribute('name');
                if (name && name.length < 50) {
                    candidates.push("NAME:" + name);
                    candidates.push("[name='" + name + "']");
                }
                
                // 4. ARIA labels for accessibility and testing support
                var ariaLabel = el.getAttribute('aria-label');
                if (ariaLabel) {
                    candidates.push("ARIA:" + ariaLabel);
                }
                
                // 5. Placeholder matching
                var placeholder = el.getAttribute('placeholder');
                if (placeholder && placeholder.length < 60) {
                    candidates.push("PLACEHOLDER:" + placeholder);
                }
                
                // 6. Natural Language Text (button or anchor elements preferred)
                var tag = el.tagName.toLowerCase();
                var text = el.textContent ? el.textContent.trim().replace(/[\r\n\t]+/g, ' ') : "";
                if (text && text.length > 0 && text.length < 40) {
                    if (tag === 'button' || el.closest('button') || tag === 'a' || el.closest('a') || tag === 'span' || tag === 'div') {
                        candidates.push("TEXT:" + text.substring(0, 30));
                    }
                }
                
                // 7. Behavioral heuristics (fallback)
                var behav = getBehavioralSelector(el);
                if (behav && candidates.indexOf(behav) === -1) {
                    candidates.push(behav);
                }
                
                // 8. Base element selector trail (root CSS selector fallback)
                var baseSel = getElementSelector(el);
                if (baseSel && candidates.indexOf(baseSel) === -1) {
                    candidates.push(baseSel);
                }
                
                // Deduplicate items and assemble chain
                var uniqueCandidates = [];
                for (var i = 0; i < candidates.length; i++) {
                    if (uniqueCandidates.indexOf(candidates[i]) === -1) {
                        uniqueCandidates.push(candidates[i]);
                    }
                }
                return uniqueCandidates.join(" || ");
            }

            // Real Click & Tap with coordinates tracking with double-click deduplication
            var clickTimer = null;
            document.addEventListener('click', function(e) {
                var el = e.target;
                if (!el) return;

                var selector = getSmartSelectorChain(el);
                var text = el.textContent ? el.textContent.trim().substring(0, 30) : "";
                
                // Track finger touch / pointer coordinates
                var x = e.clientX || 0;
                var y = e.clientY || 0;
                var val = text + " coord:" + x + "," + y;

                clearTimeout(clickTimer);
                clickTimer = setTimeout(function() {
                    if (window.OurAutoJavascriptBridge) {
                        window.OurAutoJavascriptBridge.onElementClickTriggered(selector, val);
                    }
                }, 280);
            }, true);

            // High-grade Double Click detector (also cancels pending single click)
            document.addEventListener('dblclick', function(e) {
                clearTimeout(clickTimer);
                var el = e.target;
                if (!el) return;
                var selector = getSmartSelectorChain(el);
                var text = el.textContent ? el.textContent.trim().substring(0, 30) : "";
                var val = text + " coord:" + e.clientX + "," + e.clientY;
                if (window.OurAutoJavascriptBridge && window.OurAutoJavascriptBridge.onElementActionTriggered) {
                    window.OurAutoJavascriptBridge.onElementActionTriggered("DOUBLE_CLICK", selector, val);
                }
            }, true);

            var pressTimer;
            var pressX = 0, pressY = 0;

            function onPressStart(e) {
                var touch = e.touches ? e.touches[0] : e;
                pressX = touch.clientX;
                pressY = touch.clientY;
                var el = e.target;
                if (!el) return;

                clearTimeout(pressTimer);
                pressTimer = setTimeout(function() {
                    var selector = getSmartSelectorChain(el);
                    var text = el.textContent ? el.textContent.trim().substring(0, 30) : "";
                    var val = text + " coord:" + pressX + "," + pressY;
                    if (window.OurAutoJavascriptBridge && window.OurAutoJavascriptBridge.onElementActionTriggered) {
                        window.OurAutoJavascriptBridge.onElementActionTriggered("LONG_PRESS", selector, val);
                    }
                }, 750);
            }

            function onPressMove(e) {
                var touch = e.touches ? e.touches[0] : e;
                var moveX = touch.clientX;
                var moveY = touch.clientY;
                // Cancel long press if drag/scroll movement exceeds 10 pixels
                if (Math.abs(moveX - pressX) > 10 || Math.abs(moveY - pressY) > 10) {
                    clearTimeout(pressTimer);
                }
            }

            function onPressEnd() {
                clearTimeout(pressTimer);
            }

            document.addEventListener('mousedown', onPressStart, true);
            document.addEventListener('mousemove', onPressMove, true);
            document.addEventListener('mouseup', onPressEnd, true);
            document.addEventListener('touchstart', onPressStart, { passive: true, capture: true });
            document.addEventListener('touchmove', onPressMove, { passive: true, capture: true });
            document.addEventListener('touchend', onPressEnd, { passive: true, capture: true });
            document.addEventListener('touchcancel', onPressEnd, { passive: true, capture: true });

            // Real-time Keystrokes & Input Change Track with high efficiency and password masking
            var typingTimeout;
            document.addEventListener('input', function(e) {
                var el = e.target;
                if (el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.isContentEditable)) {
                    var selector = getSmartSelectorChain(el);
                    var val = el.value || el.innerText || "";
                    if (el.type === 'password') {
                        val = "•••••";
                    }
                    clearTimeout(typingTimeout);
                    typingTimeout = setTimeout(function() {
                        if (window.OurAutoJavascriptBridge) {
                            window.OurAutoJavascriptBridge.onElementInputTriggered(selector, val);
                        }
                    }, 500);
                }
            }, true);

            // Listen blurred inputs to ensure catching final value
            document.addEventListener('blur', function(e) {
                var el = e.target;
                if (el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA')) {
                    var selector = getSmartSelectorChain(el);
                    var val = el.value || "";
                    if (el.type === 'password') {
                        val = "•••••";
                    }
                    if (window.OurAutoJavascriptBridge && val) {
                        window.OurAutoJavascriptBridge.onElementInputTriggered(selector, val);
                    }
                }
            }, true);

            // Track standard Select dropdown options natively
            document.addEventListener('change', function(e) {
                var el = e.target;
                if (el && el.tagName === 'SELECT') {
                    var selector = getSmartSelectorChain(el);
                    var val = el.value || "";
                    if (window.OurAutoJavascriptBridge && val) {
                        window.OurAutoJavascriptBridge.onElementActionTriggered("SELECT_OPTION", selector, val);
                    }
                }
            }, true);

            // High Precision Web Scrolling Motion Tracker
            var liveScrollTimeout;
            window.addEventListener('scroll', function() {
                clearTimeout(liveScrollTimeout);
                liveScrollTimeout = setTimeout(function() {
                    var x = window.scrollX || window.pageXOffset || document.documentElement.scrollLeft;
                    var y = window.scrollY || window.pageYOffset || document.documentElement.scrollTop;
                    if (window.OurAutoJavascriptBridge) {
                        window.OurAutoJavascriptBridge.onScrollEvent(Math.round(x), Math.round(y));
                    }
                }, 350);
            }, true);
        })();
    """.trimIndent()
    view?.evaluateJavascript(js, null)
}

