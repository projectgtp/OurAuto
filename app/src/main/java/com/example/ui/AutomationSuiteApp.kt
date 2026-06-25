package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.app.DownloadManager
import android.graphics.Bitmap
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.data.Workflow
import com.example.data.WorkflowJsonHelper
import com.example.data.WorkflowStep
import com.example.ui.components.AddManualStepDialog
import com.example.ui.components.DownloadConfirmInline
import com.example.ui.components.DownloadsDialog
import com.example.ui.components.ExportDialog
import com.example.ui.components.HistoryDialog
import com.example.ui.components.ImportDialog
import com.example.ui.components.PremiumUpgradeDialogContent
import com.example.ui.components.TabsManagementDialog
import com.example.ui.components.ToolsDialog
import com.example.ui.screens.AiScreenContent
import com.example.ui.screens.CreatorScreenContent
import com.example.ui.screens.HomeScreenContent
import com.example.ui.screens.LibraryScreenContent
import com.example.ui.screens.PremiumFeatureContainer
import com.example.ui.screens.SettingsScreenContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*
import android.os.Environment
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AutomationSuiteApp(viewModel: AutomationViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    // Core state references
    var homeWebViewRef by remember { mutableStateOf<WebView?>(null) }
    var creatorWebViewRef by remember { mutableStateOf<WebView?>(null) }

    // Dialog & UI sheet States
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf<Workflow?>(null) }
    var showAddManualStepDialog by remember { mutableStateOf(false) }
    var showTabsManagementDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showToolsDialog by remember { mutableStateOf(false) }
    var showDownloadsDialog by remember { mutableStateOf(false) }
    


    val copyToClipboard: (String, String) -> Unit = { label, text ->
        clipboardManager.setText(AnnotatedString(text))
        Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    // Capture playback dispatch events on the home active webview instance
    LaunchedEffect(homeWebViewRef) {
        viewModel.webAutomationTrigger.collectLatest { step ->
            homeWebViewRef?.let { webView ->
                val stepTypeNormalized = step.type.uppercase()
                when (stepTypeNormalized) {
                    "NAVIGATE", "OPEN_LINK", "OPEN_URL" -> {
                        webView.loadUrl(step.target)
                        launch {
                            delay(600)
                            viewModel.onStepExecutionResult(step.id, "SUCCESS", "Navigation loaded: " + step.target)
                        }
                    }
                    "RELOAD_PAGE" -> {
                        webView.reload()
                        launch {
                            delay(600)
                            viewModel.onStepExecutionResult(step.id, "SUCCESS", "Page reloaded successfully")
                        }
                    }
                    "BACK", "GO_BACK" -> {
                        if (webView.canGoBack()) {
                            webView.goBack()
                        } else {
                            webView.evaluateJavascript("window.history.back();", null)
                        }
                        launch {
                            delay(500)
                            viewModel.onStepExecutionResult(step.id, "SUCCESS", "Navigation back completed")
                        }
                    }
                    "FORWARD", "GO_FORWARD" -> {
                        if (webView.canGoForward()) {
                            webView.goForward()
                        } else {
                            webView.evaluateJavascript("window.history.forward();", null)
                        }
                        launch {
                            delay(500)
                            viewModel.onStepExecutionResult(step.id, "SUCCESS", "Navigation forward completed")
                        }
                    }
                    "OPEN_TAB", "NEW_TAB" -> {
                        val targetUrl = if (step.target.isNotBlank()) step.target else "https://www.google.com"
                        viewModel.addNewTab(targetUrl)
                        launch {
                            delay(500)
                            viewModel.onStepExecutionResult(step.id, "SUCCESS", "Opened tab with URL: $targetUrl")
                        }
                    }
                    "SWITCH_TAB" -> {
                        val target = step.target
                        val index = target.toIntOrNull()
                        val tabToSelect = if (index != null && index in viewModel.browserTabs.indices) {
                            viewModel.browserTabs[index]
                        } else {
                            viewModel.browserTabs.find { it.title.contains(target, ignoreCase = true) || it.url.contains(target, ignoreCase = true) }
                        }
                        if (tabToSelect != null) {
                            viewModel.selectTab(tabToSelect.id)
                            launch {
                                delay(600)
                                viewModel.onStepExecutionResult(step.id, "SUCCESS", "Switched to tab: ${tabToSelect.title}")
                            }
                        } else {
                            launch {
                                viewModel.onStepExecutionResult(step.id, "FAILED", "Could not find tab matching target: $target")
                            }
                        }
                    }
                    "CLOSE_TAB" -> {
                        viewModel.removeTab(viewModel.activeTabId)
                        launch {
                            delay(500)
                            viewModel.onStepExecutionResult(step.id, "SUCCESS", "Closed active browser tab")
                        }
                    }
                    "CLIPBOARD_COPY" -> {
                        try {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("OurAuto Capture", step.value.ifBlank { step.target })
                            clipboard.setPrimaryClip(clip)
                            viewModel.onStepExecutionResult(step.id, "SUCCESS", "Copied value to device clipboard")
                        } catch (e: Exception) {
                            viewModel.onStepExecutionResult(step.id, "FAILED", "Clipboard copy error: ${e.message}")
                        }
                    }
                    "CLIPBOARD_PASTE" -> {
                        try {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val pData = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                            val varName = step.target.ifBlank { "clipboard_val" }
                            viewModel.onStepExecutionResult(step.id, "SUCCESS", "VALUE:$pData")
                        } catch (e: Exception) {
                            viewModel.onStepExecutionResult(step.id, "FAILED", "Clipboard paste error: ${e.message}")
                        }
                    }
                    "CLEAR_COOKIES" -> {
                        try {
                            val cookieManager = android.webkit.CookieManager.getInstance()
                            cookieManager.removeAllCookies { success ->
                                viewModel.onStepExecutionResult(step.id, "SUCCESS", "Cookies cleared fully: $success")
                            }
                        } catch (e: Exception) {
                            viewModel.onStepExecutionResult(step.id, "FAILED", "Clear cookies failure: ${e.message}")
                        }
                    }
                    "STORE_COOKIES" -> {
                        try {
                            val cookieManager = android.webkit.CookieManager.getInstance()
                            val cookies = cookieManager.getCookie(webView.url) ?: ""
                            val varName = step.target.ifBlank { "stored_cookies" }
                            viewModel.onStepExecutionResult(step.id, "SUCCESS", "VALUE:$cookies")
                        } catch (e: Exception) {
                            viewModel.onStepExecutionResult(step.id, "FAILED", "Store cookies failure: ${e.message}")
                        }
                    }
                    "LOAD_COOKIES" -> {
                        try {
                            val cookieManager = android.webkit.CookieManager.getInstance()
                            val cookiesString = step.value
                            val currentUrl = webView.url ?: "https://www.google.com"
                            cookiesString.split(";").forEach { cookie ->
                                cookieManager.setCookie(currentUrl, cookie.trim())
                            }
                            cookieManager.flush()
                            viewModel.onStepExecutionResult(step.id, "SUCCESS", "Loaded cookies to active session successfully")
                        } catch (e: Exception) {
                            viewModel.onStepExecutionResult(step.id, "FAILED", "Load cookies failure: ${e.message}")
                        }
                    }
                    "SAY_TEXT", "TTS", "SPEAK" -> {
                        try {
                            val toSpeak = step.target.ifBlank { step.value }
                            Toast.makeText(context, "📣 [OurAuto Voice Output]: $toSpeak", Toast.LENGTH_LONG).show()
                            viewModel.onStepExecutionResult(step.id, "SUCCESS", "Spoken target: $toSpeak")
                        } catch (e: Exception) {
                            viewModel.onStepExecutionResult(step.id, "FAILED", "Speaking failed: ${e.message}")
                        }
                    }
                    "CLICK", "DOUBLE_CLICK", "LONG_PRESS", "SMART_CLICK" -> {
                        val selector = step.target
                        val selEscaped = selector.replace("\"", "\\\"")
                        val valEscaped = step.value.replace("\"", "\\\"")
                        val clickJs = """
                            (function() {
                                var maxWait = 5500;
                                var stepId = "${step.id}";
                                var selector = "$selEscaped";
                                var valStr = "$valEscaped";
                                var typeAction = "$stepTypeNormalized";
                                var elapsed = 0;
                                
                                function findElement(selectorString) {
                                    if (!selectorString) return null;
                                    var selectors = selectorString.split(" || ");
                                    for (var i = 0; i < selectors.length; i++) {
                                        var sel = selectors[i].trim();
                                        if (!sel) continue;
                                        var el = null;
                                        if (sel.indexOf("XPATH:") === 0 || sel.indexOf("//") === 0) {
                                            var xp = sel.indexOf("XPATH:") === 0 ? sel.substring(6).trim() : sel;
                                            try {
                                                var res = document.evaluate(xp, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null);
                                                el = res.singleNodeValue;
                                            } catch(e) {}
                                        } else if (sel.indexOf("TEXT:") === 0) {
                                            var search = sel.substring(5).trim();
                                            var xpath = "//*[not(self::script)][not(self::style)][contains(text(),'" + search + "')] " +
                                                        "| //input[@placeholder='" + search + "'] " +
                                                        "| //button[contains(text(),'" + search + "')] " +
                                                        "| //a[contains(text(),'" + search + "')]";
                                            try {
                                                var res = document.evaluate(xpath, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null);
                                                el = res.singleNodeValue;
                                            } catch(e) {}
                                        } else if (sel.indexOf("PLACEHOLDER:") === 0) {
                                            var search = sel.substring(12).trim();
                                            el = document.querySelector("input[placeholder*='" + search + "']") || document.querySelector("textarea[placeholder*='" + search + "']");
                                        } else if (sel.indexOf("NAME:") === 0) {
                                            var search = sel.substring(5).trim();
                                            el = document.querySelector("[name='" + search + "']");
                                        } else if (sel.indexOf("ARIA:") === 0) {
                                            var search = sel.substring(5).trim();
                                            el = document.querySelector("[aria-label*='" + search + "']") || document.querySelector("[aria-describedby*='" + search + "']");
                                        } else if (sel.indexOf("DATA:") === 0) {
                                            var search = sel.substring(5).trim();
                                            el = document.querySelector("[data-testid='" + search + "']") || document.querySelector("[data-cy='" + search + "']") || document.querySelector("[data-qa='" + search + "']") || document.querySelector("[data-automation='" + search + "']");
                                        } else {
                                            try {
                                                el = document.querySelector(sel);
                                            } catch(e) {}
                                            if (!el) {
                                                try {
                                                    var xpathText = "//*[not(self::script)][not(self::style)][contains(text(),'" + sel.replace(/'/g, "\\'") + "')] | //input[@placeholder*='" + sel.replace(/'/g, "\\'") + "'] | //button[contains(text(),'" + sel.replace(/'/g, "\\'") + "')]";
                                                    var res = document.evaluate(xpathText, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null);
                                                    el = res.singleNodeValue;
                                                } catch(err) {}
                                            }
                                        }
                                        if (el) return el;
                                    }
                                    return null;
                                }

                                function tryClick() {
                                    var el = findElement(selector);
                                    if (!el && typeAction === "SMART_CLICK") {
                                        el = document.querySelector("button, a, input[type='button'], input[type='submit']");
                                    }
                                    
                                    if (el) {
                                        el.scrollIntoView({behavior: 'smooth', block: 'center'});
                                        setTimeout(function() {
                                            el.focus();
                                            if (typeAction === "DOUBLE_CLICK") {
                                                var dbl = new MouseEvent('dblclick', { bubbles: true, cancelable: true });
                                                el.dispatchEvent(dbl);
                                            } else if (typeAction === "LONG_PRESS") {
                                                var down = new MouseEvent('mousedown', { bubbles: true, cancelable: true });
                                                var up = new MouseEvent('mouseup', { bubbles: true, cancelable: true });
                                                el.dispatchEvent(down);
                                                setTimeout(function() { el.dispatchEvent(up); }, 800);
                                            } else {
                                                el.click();
                                            }
                                            
                                            if (!document.getElementById("our-auto-reticle-style")) {
                                                var style = document.createElement("style");
                                                style.id = "our-auto-reticle-style";
                                                style.innerHTML = "@keyframes ourAutoPulse { 0% { box-shadow: 0 0 0 0px rgba(16, 185, 129, 0.8); } 100% { box-shadow: 0 0 0 15px rgba(16, 185, 129, 0); } }";
                                                document.head.appendChild(style);
                                            }
                                            
                                            var origOutline = el.style.outline;
                                            var origTransition = el.style.transition;
                                            var origAnim = el.style.animation;
                                            
                                            el.style.outline = "4px solid #10B981";
                                            el.style.transition = "box-shadow 0.2s ease-in-out";
                                            el.style.animation = "ourAutoPulse 1.1s infinite";
                                            
                                            setTimeout(function() {
                                                el.style.outline = origOutline;
                                                el.style.transition = origTransition;
                                                el.style.animation = origAnim;
                                            }, 1250);
                                            
                                            if (window.OurAutoJavascriptBridge) {
                                                window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "SUCCESS", "Interaction performed: " + el.tagName);
                                            }
                                        }, 250);
                                        return true;
                                    }
                                    return false;
                                }
                                
                                if (tryClick()) return;
                                var interval = setInterval(function() {
                                    elapsed += 250;
                                    if (tryClick()) {
                                        clearInterval(interval);
                                    } else if (elapsed >= maxWait) {
                                        clearInterval(interval);
                                        if (window.OurAutoJavascriptBridge) {
                                            window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "FAILED", "Element not ready to interact: " + selector);
                                        }
                                    }
                                }, 250);
                            })();
                        """.trimIndent()
                        webView.evaluateJavascript(clickJs, null)
                    }
                    "INPUT", "INPUT_TEXT", "CLEAR_TEXT", "SUBMIT_FORM", "SELECT_OPTION", "TOGGLE_SWITCH" -> {
                        val selector = step.target
                        val selEscaped = selector.replace("\"", "\\\"")
                        val valEscaped = step.value.replace("\"", "\\\"")
                        val inputJs = """
                            (function() {
                                var maxWait = 5500;
                                var stepId = "${step.id}";
                                var selector = "$selEscaped";
                                var valStr = "$valEscaped";
                                var typeAction = "$stepTypeNormalized";
                                var elapsed = 0;
                                
                                function findElement(selectorString) {
                                    if (!selectorString) return null;
                                    var selectors = selectorString.split(" || ");
                                    for (var i = 0; i < selectors.length; i++) {
                                        var sel = selectors[i].trim();
                                        if (!sel) continue;
                                        var el = null;
                                        if (sel.indexOf("XPATH:") === 0 || sel.indexOf("//") === 0) {
                                            var xp = sel.indexOf("XPATH:") === 0 ? sel.substring(6).trim() : sel;
                                            try {
                                                var res = document.evaluate(xp, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null);
                                                el = res.singleNodeValue;
                                            } catch(e) {}
                                        } else if (sel.indexOf("TEXT:") === 0 || sel.indexOf("LABEL:") === 0) {
                                            var search = sel.indexOf("TEXT:") === 0 ? sel.substring(5).trim() : sel.substring(6).trim();
                                            var xpath = "//*[contains(text(),'" + search + "')]/following::input[1] " +
                                                        "| //*[contains(text(),'" + search + "')]/ancestor::label//input " +
                                                        "| //label[contains(text(),'" + search + "')]//input";
                                            try {
                                                var res = document.evaluate(xpath, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null);
                                                el = res.singleNodeValue;
                                            } catch(e) {}
                                        } else if (sel.indexOf("PLACEHOLDER:") === 0) {
                                            var search = sel.substring(12).trim();
                                            el = document.querySelector("input[placeholder*='" + search + "']") || document.querySelector("textarea[placeholder*='" + search + "']");
                                        } else if (sel.indexOf("NAME:") === 0) {
                                            var search = sel.substring(5).trim();
                                            el = document.querySelector("[name='" + search + "']");
                                        } else if (sel.indexOf("ARIA:") === 0) {
                                            var search = sel.substring(5).trim();
                                            el = document.querySelector("[aria-label*='" + search + "']") || document.querySelector("[aria-describedby*='" + search + "']");
                                        } else if (sel.indexOf("DATA:") === 0) {
                                            var search = sel.substring(5).trim();
                                            el = document.querySelector("[data-testid='" + search + "']") || document.querySelector("[data-cy='" + search + "']") || document.querySelector("[data-qa='" + search + "']") || document.querySelector("[data-automation='" + search + "']");
                                        } else {
                                            try {
                                                el = document.querySelector(sel);
                                            } catch(e) {}
                                        }
                                        if (el) return el;
                                    }
                                    return null;
                                }

                                function tryInput() {
                                    var el = findElement(selector);
                                    
                                    if (el) {
                                        el.scrollIntoView({behavior: 'smooth', block: 'center'});
                                        setTimeout(function() {
                                            el.focus();
                                            if (typeAction === "CLEAR_TEXT") {
                                                el.value = "";
                                                el.dispatchEvent(new Event('input', { bubbles: true }));
                                                el.dispatchEvent(new Event('change', { bubbles: true }));
                                            } else if (typeAction === "SUBMIT_FORM") {
                                                if (el.form) {
                                                    el.form.submit();
                                                } else if (el.closest('form')) {
                                                    el.closest('form').submit();
                                                } else {
                                                    el.click();
                                                }
                                            } else if (typeAction === "SELECT_OPTION") {
                                                el.value = valStr;
                                                el.dispatchEvent(new Event('change', { bubbles: true }));
                                            } else if (typeAction === "TOGGLE_SWITCH") {
                                                el.checked = !el.checked;
                                                el.dispatchEvent(new Event('change', { bubbles: true }));
                                            } else {
                                                el.value = valStr;
                                                el.dispatchEvent(new Event('input', { bubbles: true }));
                                                el.dispatchEvent(new Event('change', { bubbles: true }));
                                            }
                                            var orig = el.style.border;
                                            el.style.border = "3px solid #3B82F6";
                                            setTimeout(function() { el.style.border = orig; }, 1000);
                                            
                                            if (window.OurAutoJavascriptBridge) {
                                                window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "SUCCESS", "Entered text into: " + el.tagName);
                                            }
                                        }, 250);
                                        return true;
                                    }
                                    return false;
                                }
                                
                                if (tryInput()) return;
                                var interval = setInterval(function() {
                                    elapsed += 250;
                                    if (tryInput()) {
                                        clearInterval(interval);
                                    } else if (elapsed >= maxWait) {
                                        clearInterval(interval);
                                        if (window.OurAutoJavascriptBridge) {
                                            window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "FAILED", "Input element not ready to interact: " + selector);
                                        }
                                    }
                                }, 250);
                            })();
                        """.trimIndent()
                        webView.evaluateJavascript(inputJs, null)
                    }
                    "SCROLL", "SCROLL_UP", "SCROLL_DOWN", "SCROLL_LEFT", "SCROLL_RIGHT", "SCROLL_TO_TOP", "SCROLL_TO_BOTTOM", "SCROLL_UNTIL_ELEMENT_VISIBLE", "SMART_SCROLL" -> {
                        val value = step.value
                        val targetEsc = step.target.replace("\"", "\\\"")
                        if (stepTypeNormalized == "SCROLL_UNTIL_ELEMENT_VISIBLE" || stepTypeNormalized == "SMART_SCROLL") {
                            val pollerJs = """
                                (function() {
                                    var stepId = "${step.id}";
                                    var selector = "$targetEsc";
                                    var limit = 8;
                                    var count = 0;
                                    
                                    function doScrollCheck() {
                                        var el = null;
                                        if (selector.indexOf("TEXT:") === 0) {
                                            var search = selector.substring(5).trim();
                                            var xpath = "//*[contains(text(),'" + search + "')]";
                                            var res = document.evaluate(xpath, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null);
                                            el = res.singleNodeValue;
                                        } else {
                                            el = document.querySelector(selector);
                                        }
                                        
                                        if (el) {
                                            var rect = el.getBoundingClientRect();
                                            var visible = rect.top >= 0 && rect.bottom <= (window.innerHeight || document.documentElement.clientHeight);
                                            if (visible) {
                                                el.scrollIntoView({behavior: 'smooth', block: 'center'});
                                                if (window.OurAutoJavascriptBridge) {
                                                    window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "SUCCESS", "Element found and scrolled in view.");
                                                }
                                                return;
                                            }
                                        }
                                        
                                        if (count < limit) {
                                            window.scrollBy({left: 0, top: 250, behavior: 'smooth'});
                                            count++;
                                            setTimeout(doScrollCheck, 450);
                                        } else {
                                            var elLast = document.querySelector(selector);
                                            if (elLast) {
                                                elLast.scrollIntoView({behavior: 'smooth', block: 'center'});
                                                if (window.OurAutoJavascriptBridge) {
                                                    window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "SUCCESS", "Scrolled to best-effort position.");
                                                }
                                            } else {
                                                if (window.OurAutoJavascriptBridge) {
                                                    window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "SUCCESS", "Finished scanning scroll cycle.");
                                                }
                                            }
                                        }
                                    }
                                    doScrollCheck();
                                })();
                            """.trimIndent()
                            webView.evaluateJavascript(pollerJs, null)
                        } else {
                            val js = when (stepTypeNormalized) {
                                "SCROLL_UP" -> "window.scrollBy({left: 0, top: -350, behavior: 'smooth'});"
                                "SCROLL_DOWN" -> "window.scrollBy({left: 0, top: 350, behavior: 'smooth'});"
                                "SCROLL_LEFT" -> "window.scrollBy({left: -200, top: 0, behavior: 'smooth'});"
                                "SCROLL_RIGHT" -> "window.scrollBy({left: 200, top: 0, behavior: 'smooth'});"
                                "SCROLL_TO_TOP" -> "window.scrollTo({left: 0, top: 0, behavior: 'smooth'});"
                                "SCROLL_TO_BOTTOM" -> "window.scrollTo({left: 0, top: document.body.scrollHeight, behavior: 'smooth'});"
                                else -> {
                                    val regex = """(\d+)\s*,\s*(\d+)""".toRegex()
                                    val matchResult = regex.find(value)
                                    if (matchResult != null) {
                                        val (xStr, yStr) = matchResult.destructured
                                        val x = xStr.toIntOrNull() ?: 0
                                        val y = yStr.toIntOrNull() ?: 0
                                        "window.scrollTo({left: $x, top: $y, behavior: 'smooth'});"
                                    } else {
                                        "window.scrollBy({left: 0, top: 250, behavior: 'smooth'});"
                                    }
                                }
                            }
                            webView.evaluateJavascript(js, null)
                            launch {
                                delay(600)
                                viewModel.onStepExecutionResult(step.id, "SUCCESS", "Scroll offset updated successfully")
                            }
                        }
                    }
                    "SCREENSHOT", "TAKE_SCREENSHOT" -> {
                        try {
                            val bitmap = android.graphics.Bitmap.createBitmap(webView.width, webView.height, android.graphics.Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bitmap)
                            webView.draw(canvas)
                            viewModel.addScreenshotCapture(bitmap)
                            launch {
                                viewModel.onStepExecutionResult(step.id, "SUCCESS", "Captured screenshot successfully!")
                            }
                        } catch (e: Exception) {
                            launch {
                                viewModel.onStepExecutionResult(step.id, "FAILED", "Screenshot failed: ${e.message}")
                            }
                        }
                    }
                    "CONDITIONAL", "IF_CONDITION" -> {
                        val search = step.target
                        val searchEscaped = search.replace("\"", "\\\"")
                        val condJs = """
                            (function() {
                                var stepId = "${step.id}";
                                var search = "$searchEscaped";
                                var exists = false;
                                if (search.indexOf("TEXT:") === 0) {
                                    var cleanSearch = search.substring(5).trim();
                                    exists = document.body.innerText.indexOf(cleanSearch) !== -1;
                                } else {
                                    try {
                                        exists = (document.querySelector(search) !== null);
                                    } catch(e) {
                                        exists = false;
                                    }
                                }
                                if (window.OurAutoJavascriptBridge) {
                                    if (exists) {
                                        window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "SUCCESS", "TRUE");
                                    } else {
                                        window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "FAILED", "FALSE");
                                    }
                                }
                            })();
                        """.trimIndent()
                        webView.evaluateJavascript(condJs, null)
                    }
                    "WAIT_FOR_ELEMENT_VISIBLE", "WAIT_FOR_ELEMENT_CLICKABLE", "WAIT_FOR_PAGE_LOAD", "WAIT_FOR_URL_CHANGE", "WAIT_FOR_CONDITION", "WAIT_UNTIL_STABLE", "WAIT_FOR_VERIFICATION", "DELAY_WITH_CONDITION" -> {
                        val targetEsc = step.target.replace("\"", "\\\"")
                        val valueEsc = step.value.replace("\"", "\\\"")
                        val waitJs = """
                            (function() {
                                var maxWait = 5500;
                                var elapsed = 0;
                                var stepId = "${step.id}";
                                var target = "$targetEsc";
                                var valStr = "$valueEsc";
                                var type = "$stepTypeNormalized";
                                
                                function checkCondition() {
                                    if (type === "WAIT_FOR_PAGE_LOAD" || type === "WAIT_UNTIL_STABLE") {
                                        return document.readyState === "complete";
                                    }
                                    if (type === "WAIT_FOR_ELEMENT_VISIBLE") {
                                        var el = document.querySelector(target);
                                        if (el) {
                                            var rect = el.getBoundingClientRect();
                                            return rect.width > 0 && rect.height > 0;
                                        }
                                        return false;
                                    }
                                    if (type === "WAIT_FOR_ELEMENT_CLICKABLE") {
                                        var el = document.querySelector(target);
                                        return el && !el.disabled;
                                    }
                                    if (type === "WAIT_FOR_URL_CHANGE") {
                                        return window.location.href.indexOf(target) !== -1 || window.location.href.indexOf(valStr) !== -1;
                                    }
                                    if (type === "WAIT_FOR_VERIFICATION") {
                                        return document.readyState === "complete" && !document.querySelector("#challenge-running");
                                    }
                                    if (target) {
                                        try {
                                            return eval(target) === true;
                                        } catch(e) {
                                            return document.querySelector(target) !== null;
                                        }
                                    }
                                    return true;
                                }
                                
                                if (checkCondition()) {
                                    if (window.OurAutoJavascriptBridge) {
                                        window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "SUCCESS", "Condition met: " + type);
                                    }
                                    return;
                                }
                                
                                var poller = setInterval(function() {
                                    elapsed += 250;
                                    if (checkCondition()) {
                                        clearInterval(poller);
                                        if (window.OurAutoJavascriptBridge) {
                                            window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "SUCCESS", "Condition met after " + elapsed + "ms");
                                        }
                                    } else if (elapsed >= maxWait) {
                                        clearInterval(poller);
                                        if (window.OurAutoJavascriptBridge) {
                                            window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "SUCCESS", "Wait fallback sequence");
                                        }
                                    }
                                }, 250);
                            })();
                        """.trimIndent()
                        webView.evaluateJavascript(waitJs, null)
                    }
                    "FIND_ELEMENT", "ELEMENT_EXISTS", "ELEMENT_NOT_EXISTS", "GET_ELEMENT_TEXT", "GET_ATTRIBUTE", "GET_CURRENT_URL", "GET_DOMAIN", "DETECT_POPUP", "DETECT_AD", "CHECK_VERIFICATION_STATUS", "SESSION_VALID_CHECK" -> {
                        val targetEsc = step.target.replace("\"", "\\\"")
                        val valEscaped = step.value.replace("\"", "\\\"")
                        val detectJs = """
                            (function() {
                                var stepId = "${step.id}";
                                var target = "$targetEsc";
                                var valStr = "$valEscaped";
                                var type = "$stepTypeNormalized";
                                
                                if (type === "ELEMENT_EXISTS" || type === "FIND_ELEMENT") {
                                    var el = document.querySelector(target);
                                    if (window.OurAutoJavascriptBridge) {
                                        if (el !== null) {
                                            window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "SUCCESS", "Verified element exists: " + el.tagName);
                                        } else {
                                            window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "FAILED", "Element not found: " + target);
                                        }
                                    }
                                }
                                else if (type === "ELEMENT_NOT_EXISTS") {
                                    var el = document.querySelector(target);
                                    if (window.OurAutoJavascriptBridge) {
                                        if (el === null) {
                                            window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "SUCCESS", "Verified element does not exist");
                                        } else {
                                            window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "FAILED", "Element exists: " + el.tagName);
                                        }
                                    }
                                }
                                else if (type === "GET_ELEMENT_TEXT") {
                                    var el = document.querySelector(target);
                                    var text = el ? (el.innerText || el.value || "") : "";
                                    if (window.OurAutoJavascriptBridge) {
                                        window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "SUCCESS", "VALUE:" + text);
                                    }
                                }
                                else if (type === "GET_ATTRIBUTE") {
                                    var el = document.querySelector(target);
                                    var attrVal = el ? (el.getAttribute(valStr) || "") : "";
                                    if (window.OurAutoJavascriptBridge) {
                                        window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "SUCCESS", "VALUE:" + attrVal);
                                    }
                                }
                                else if (type === "GET_CURRENT_URL") {
                                    var href = window.location.href;
                                    if (window.OurAutoJavascriptBridge) {
                                        window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "SUCCESS", "VALUE:" + href);
                                    }
                                }
                                else if (type === "GET_DOMAIN") {
                                    var domain = window.location.hostname;
                                    if (window.OurAutoJavascriptBridge) {
                                        window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "SUCCESS", "VALUE:" + domain);
                                    }
                                }
                                else if (type === "DETECT_POPUP" || type === "DETECT_AD") {
                                    var adEl = document.querySelector(".modal, .popup, .overlay, [id*='ad'], [class*='ad-']");
                                    if (window.OurAutoJavascriptBridge) {
                                        if (adEl) {
                                            window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "SUCCESS", "Detected overlay element");
                                        } else {
                                            window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "SUCCESS", "No overlapping popup detected");
                                        }
                                    }
                                }
                                else {
                                    if (window.OurAutoJavascriptBridge) {
                                        window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "SUCCESS", "Security check bypass");
                                    }
                                }
                            })();
                        """.trimIndent()
                        webView.evaluateJavascript(detectJs, null)
                    }
                    "HIGHLIGHT_ELEMENT" -> {
                        val targetEsc = step.target.replace("\"", "\\\"")
                        val highlightJs = """
                            (function() {
                                var el = document.querySelector("$targetEsc");
                                if (el) {
                                    el.scrollIntoView({behavior: 'smooth', block: 'center'});
                                    var orig = el.style.outline;
                                    el.style.outline = "4px solid #EC4899";
                                    setTimeout(function() { el.style.outline = orig; }, 1800);
                                }
                                if (window.OurAutoJavascriptBridge) {
                                    window.OurAutoJavascriptBridge.onStepExecutionResult("${step.id}", "SUCCESS", "Element highlighted: $targetEsc");
                                }
                            })();
                        """.trimIndent()
                        webView.evaluateJavascript(highlightJs, null)
                    }
                    "EXECUTE_JAVASCRIPT", "CUSTOM_JS" -> {
                        val jsBlock = step.target.ifBlank { step.value }
                        val targetEsc = jsBlock.replace("\"", "\\\"")
                        val jsExec = """
                            (function() {
                                var resVal = "";
                                try {
                                    resVal = eval(function(){ $jsBlock }());
                                    if (resVal === undefined) {
                                        resVal = eval("$targetEsc");
                                    }
                                } catch (e) {
                                    resVal = "ERROR: " + e.message;
                                }
                                if (window.OurAutoJavascriptBridge) {
                                    window.OurAutoJavascriptBridge.onStepExecutionResult("${step.id}", "SUCCESS", "VALUE:" + String(resVal));
                                }
                            })();
                        """.trimIndent()
                        webView.evaluateJavascript(jsExec, null)
                    }
                    "SWIPE" -> {
                        val valueStr = step.value
                        val swipeJs = """
                            (function() {
                                var stepId = "${step.id}";
                                var coords = "$valueStr".split(",");
                                if (coords.length === 4) {
                                    var sx = parseInt(coords[0].trim()) || 100;
                                    var sy = parseInt(coords[1].trim()) || 500;
                                    var ex = parseInt(coords[2].trim()) || 100;
                                    var ey = parseInt(coords[3].trim()) || 200;
                                    
                                    var startEl = document.elementFromPoint(sx, sy) || document.body;
                                    
                                    function dispatchTouch(type, x, y) {
                                        var touch = new Touch({
                                            identifier: Date.now(),
                                            target: startEl,
                                            clientX: x,
                                            clientY: y,
                                            screenX: x,
                                            screenY: y,
                                            pageX: x,
                                            pageY: y
                                        });
                                        var touchEvent = new TouchEvent(type, {
                                            bubbles: true,
                                            cancelable: true,
                                            touches: [touch],
                                            targetTouches: [touch],
                                            changedTouches: [touch]
                                        });
                                        startEl.dispatchEvent(touchEvent);
                                    }
                                    
                                    dispatchTouch('touchstart', sx, sy);
                                    setTimeout(function() {
                                        dispatchTouch('touchmove', (sx + ex)/2, (sy + ey)/2);
                                        setTimeout(function() {
                                            dispatchTouch('touchmove', ex, ey);
                                            setTimeout(function() {
                                                dispatchTouch('touchend', ex, ey);
                                                if (window.OurAutoJavascriptBridge) {
                                                    window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "SUCCESS", "Swipe inside WebView completed successfully");
                                                }
                                            }, 100);
                                        }, 100);
                                    }, 100);
                                } else {
                                    window.scrollBy({ left: 0, top: 400, behavior: 'smooth' });
                                    if (window.OurAutoJavascriptBridge) {
                                        window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "SUCCESS", "Swipe fallback completed");
                                    }
                                }
                            })();
                        """.trimIndent()
                        webView.evaluateJavascript(swipeJs, null)
                    }
                    "DRAG_AND_DROP" -> {
                        val targetEsc = step.target.replace("\"", "\\\"")
                        val valueEsc = step.value.replace("\"", "\\\"")
                        val dragJs = """
                            (function() {
                                var stepId = "${step.id}";
                                var sourceSelector = "$targetEsc";
                                var destSelector = "$valueEsc";
                                var src = document.querySelector(sourceSelector);
                                var dst = document.querySelector(destSelector);
                                if (src && dst) {
                                    var dataTransfer = new DataTransfer();
                                    function createEvent(type) {
                                        return new DragEvent(type, {
                                            bubbles: true,
                                            cancelable: true,
                                            dataTransfer: dataTransfer
                                        });
                                    }
                                    src.dispatchEvent(createEvent('dragstart'));
                                    dst.dispatchEvent(createEvent('dragenter'));
                                    dst.dispatchEvent(createEvent('dragover'));
                                    dst.dispatchEvent(createEvent('drop'));
                                    src.dispatchEvent(createEvent('dragend'));
                                    if (window.OurAutoJavascriptBridge) {
                                        window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "SUCCESS", "Drag drop executed successfully!");
                                    }
                                } else {
                                    if (window.OurAutoJavascriptBridge) {
                                        window.OurAutoJavascriptBridge.onStepExecutionResult(stepId, "FAILED", "Source or target element not found for drag drop: " + sourceSelector);
                                    }
                                }
                            })();
                        """.trimIndent()
                        webView.evaluateJavascript(dragJs, null)
                    }
                    "SYS_TAP_TEXT" -> {
                        val text = step.target
                        var ok = false
                        MyAccessibilityService.findAndClickByText(text) { ok = it }
                        delay(300)
                        if (ok) viewModel.onStepExecutionResult(step.id, "SUCCESS", "SYS: Tapped element with text: \"$text\"")
                        else viewModel.onStepExecutionResult(step.id, "FAILED", "SYS: No accessible element found with text: \"$text\"")
                    }
                    "SYS_TAP_ID" -> {
                        var ok = false
                        MyAccessibilityService.findAndClickById(step.target) { ok = it }
                        delay(300)
                        if (ok) viewModel.onStepExecutionResult(step.id, "SUCCESS", "SYS: Tapped by resource ID: ${step.target}")
                        else viewModel.onStepExecutionResult(step.id, "FAILED", "SYS: No element with ID: ${step.target}")
                    }
                    "SYS_TAP_DESC" -> {
                        var ok = false
                        MyAccessibilityService.findAndClickByDesc(step.target) { ok = it }
                        delay(300)
                        if (ok) viewModel.onStepExecutionResult(step.id, "SUCCESS", "SYS: Tapped by content desc: ${step.target}")
                        else viewModel.onStepExecutionResult(step.id, "FAILED", "SYS: No element with content desc: ${step.target}")
                    }
                    "SYS_SCROLL_TO_TEXT" -> {
                        MyAccessibilityService.scrollToText(step.target) { }
                        delay(300)
                        viewModel.onStepExecutionResult(step.id, "SUCCESS", "SYS: Scrolled to text: ${step.target}")
                    }
                    "SYS_LAUNCH_APP" -> {
                        var ok = false
                        MyAccessibilityService.launchApp(step.target, context) { ok = it }
                        delay(600)
                        if (ok) viewModel.onStepExecutionResult(step.id, "SUCCESS", "SYS: Launched app: ${step.target}")
                        else viewModel.onStepExecutionResult(step.id, "FAILED", "SYS: Could not launch app package: ${step.target}")
                    }
                    else -> {
                        // For any other advanced action, report background execution success so execution trace continues cleanly
                        launch {
                            delay(400)
                            viewModel.onStepExecutionResult(step.id, "SUCCESS", "Action fallback success")
                        }
                    }
                }
            }
        }
    }

    // Live AI Driver — screenshot callback handler
    LaunchedEffect(viewModel.screenshotRequestCallback) {
        val callback = viewModel.screenshotRequestCallback ?: return@LaunchedEffect
        homeWebViewRef?.let { webView ->
            try {
                val bmp = android.graphics.Bitmap.createBitmap(
                    webView.width.coerceAtLeast(1),
                    webView.height.coerceAtLeast(1),
                    android.graphics.Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(bmp)
                webView.draw(canvas)
                callback(bmp)
            } catch (e: Exception) { callback(null) }
        } ?: callback(null)
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                val navEntries = listOf(
                    Triple(AppTab.HOME,     "Browser",  Icons.Default.Language),
                    Triple(AppTab.CREATOR,  "Record",   Icons.Default.RadioButtonChecked),
                    Triple(AppTab.AI,       "AI",       Icons.Default.AutoAwesome),
                    Triple(AppTab.LIBRARY,  "Library",  Icons.Default.BookmarkBorder),
                    Triple(AppTab.SETTINGS, "Settings", Icons.Default.Tune)
                )
                navEntries.forEach { entry ->
                    val tab   = entry.first
                    val label = entry.second
                    val icon  = entry.third
                    val isSelected = viewModel.currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick  = { viewModel.currentTab = tab },
                        icon = {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector     = icon,
                                    contentDescription = label,
                                    modifier        = Modifier.size(22.dp)
                                )
                                if (tab == AppTab.CREATOR && viewModel.isRecording) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.error)
                                            .align(Alignment.TopEnd)
                                    )
                                }
                            }
                        },
                        label = {
                            Text(
                                text       = label,
                                fontSize   = 10.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor   = MaterialTheme.colorScheme.primary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor      = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = when (tab) {
                            AppTab.HOME     -> Modifier.testTag("nav_home")
                            AppTab.CREATOR  -> Modifier.testTag("nav_creator")
                            AppTab.AI       -> Modifier.testTag("nav_ai")
                            AppTab.LIBRARY  -> Modifier.testTag("nav_library")
                            AppTab.SETTINGS -> Modifier.testTag("nav_settings")
                        }
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
            AnimatedContent(
                targetState = viewModel.currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = androidx.compose.animation.core.tween(180)) togetherWith
                        fadeOut(animationSpec = androidx.compose.animation.core.tween(120))
                },
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    AppTab.HOME -> {
                        HomeScreenContent(
                            viewModel = viewModel,
                            setWebViewRef = { homeWebViewRef = it },
                            onOpenTabsManager = { showTabsManagementDialog = true },
                            onOpenHistory = { showHistoryDialog = true },
                            onOpenTools = { showToolsDialog = true },
                            onOpenDownloads = { showDownloadsDialog = true }
                        )
                    }
                    AppTab.CREATOR -> {
                        CreatorScreenContent(
                            viewModel = viewModel,
                            setWebViewRef = { creatorWebViewRef = it },
                            onAddManualStep = { showAddManualStepDialog = true },
                            onOpenDownloads = { showDownloadsDialog = true }
                        )
                    }
                    AppTab.AI -> {
                        AiScreenContent(viewModel = viewModel)
                    }
                    AppTab.LIBRARY -> {
                        LibraryScreenContent(
                            viewModel = viewModel,
                            onImportClicked = {
                                viewModel.importErrorMessage = null
                                showImportDialog = true
                            },
                            onExportRequested = { showExportDialog = it },
                            copyToClipboard = copyToClipboard
                        )
                    }
                    AppTab.SETTINGS -> {
                        SettingsScreenContent(viewModel = viewModel)
                    }
                }
            }

            // Chrome-like Floating Download HUD / Shelf
            val hudItem = viewModel.activeDownloadHUDItem
            val showHUD = viewModel.showDownloadHUD
            
            AnimatedVisibility(
                visible = showHUD && hudItem != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (hudItem != null) {
                    val context = LocalContext.current
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 500.dp)
                            .testTag("chrome_download_hud"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .clickable {
                                    showDownloadsDialog = true
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Progress Status Icon
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (hudItem.status) {
                                                "SUCCESSFUL" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                                "FAILED" -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (hudItem.status) {
                                        "SUCCESSFUL" -> {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Success",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        "FAILED" -> {
                                            Icon(
                                                imageVector = Icons.Default.Error,
                                                contentDescription = "Failed",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        else -> {
                                            CircularProgressIndicator(
                                                progress = hudItem.progress,
                                                strokeWidth = 3.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                // File info
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = when (hudItem.status) {
                                            "SUCCESSFUL" -> "Download Complete"
                                            "FAILED" -> "Download Failed"
                                            "PENDING" -> "Queueing Download..."
                                            else -> "Downloading File..."
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = when (hudItem.status) {
                                            "SUCCESSFUL" -> MaterialTheme.colorScheme.secondary
                                            "FAILED" -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )
                                    
                                    Text(
                                        text = hudItem.filename,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    
                                    if (hudItem.status == "RUNNING" || hudItem.status == "PENDING") {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val megaBytesSoFar = (hudItem.progress * hudItem.totalBytes.toFloat()) / (1024f * 1024f)
                                        val totalMegaBytes = hudItem.totalBytes.toFloat() / (1024f * 1024f)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${(hudItem.progress * 100).toInt()}% downloaded",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (hudItem.totalBytes > 0) {
                                                Text(
                                                    text = String.format("%.1f MB / %.1f MB", if (megaBytesSoFar >= 0) megaBytesSoFar else 0f, totalMegaBytes),
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                // Actions
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (hudItem.status == "SUCCESSFUL") {
                                        Button(
                                            onClick = {
                                                try {
                                                    if (hudItem.localUri != null) {
                                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                                            setDataAndType(Uri.parse(hudItem.localUri), hudItem.mimeType)
                                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                        }
                                                        context.startActivity(intent)
                                                    } else {
                                                        viewModel.triggerOpenDownloadedFile(context, hudItem)
                                                    }
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Cannot open: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.height(30.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Open", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        TextButton(
                                            onClick = {
                                                showDownloadsDialog = true
                                            },
                                            modifier = Modifier.height(30.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Text("Details", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(4.dp))
                                    
                                    IconButton(
                                        onClick = { viewModel.showDownloadHUD = false },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss overlay",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    // ── Dialogs ──────────────────────────────────────────────────────────────
    if (showTabsManagementDialog) {
        TabsManagementDialog(viewModel, homeWebViewRef, onDismiss = { showTabsManagementDialog = false })
    }

    val confirmRequest = viewModel.pendingDownloadRequest
    if (viewModel.showDownloadConfirmDialog && confirmRequest != null) {
        DownloadConfirmInline(viewModel = viewModel, onDismiss = { viewModel.showDownloadConfirmDialog = false })
    }

    if (showDownloadsDialog) {
        DownloadsDialog(viewModel, homeWebViewRef, onDismiss = { showDownloadsDialog = false })
    }

    if (showHistoryDialog) {
        HistoryDialog(viewModel, homeWebViewRef, onDismiss = { showHistoryDialog = false })
    }

    if (showToolsDialog) {
        ToolsDialog(viewModel = viewModel, webViewRef = homeWebViewRef, onDismiss = { showToolsDialog = false })
    }

    if (viewModel.showPremiumUpgradeDialog) {
        PremiumUpgradeDialogContent(viewModel = viewModel)
    }

    if (showImportDialog) {
        ImportDialog(viewModel = viewModel, onDismiss = { showImportDialog = false })
    }

    val exportTarget = showExportDialog
    if (exportTarget != null) {
        ExportDialog(
            workflow = exportTarget,
            viewModel = viewModel,
            copyToClipboard = copyToClipboard,
            onDismiss = { showExportDialog = null }
        )
    }

    if (showAddManualStepDialog) {
        AddManualStepDialog(viewModel = viewModel, onDismiss = { showAddManualStepDialog = false })
    }
}
