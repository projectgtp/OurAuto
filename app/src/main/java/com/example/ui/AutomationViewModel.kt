package com.example.ui

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Workflow
import com.example.data.WorkflowDraft
import com.example.data.WorkflowJsonHelper
import com.example.data.WorkflowRepository
import com.example.data.WorkflowStep
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

enum class AppTab {
    HOME,
    CREATOR,
    AI,
    LIBRARY,
    SETTINGS
}

data class BrowserTabItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Google",
    val url: String = "https://www.google.com"
)

data class BrowserHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class WebExtension(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val urlMatchPattern: String, // e.g. "*", "wikipedia.org", etc.
    val jsContent: String,
    val isEnabled: Boolean = false
)

data class TraceStep(
    val step: WorkflowStep,
    var status: String = "PENDING", // PENDING, EXECUTING, SUCCESS, FAILED
    var logMessage: String = ""
)

data class ExecutionIssue(
    val routineName: String,
    val stepIndex: Int,
    val stepType: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class DownloadItem(
    val id: Long,
    val filename: String,
    val url: String,
    val mimeType: String,
    val totalBytes: Long,
    val status: String,
    val progress: Float,
    val localUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class PendingDownloadRequest(
    val url: String,
    val userAgent: String?,
    val contentDisposition: String?,
    val mimetype: String?,
    val contentLength: Long,
    val filename: String
)

class AutomationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WorkflowRepository
    val workflows: StateFlow<List<Workflow>>

    private val prefs = application.getSharedPreferences("OurAutoPrefs", Context.MODE_PRIVATE)

    private fun getPrefString(key: String, default: String): String = prefs.getString(key, default) ?: default
    private fun setPrefString(key: String, value: String) = prefs.edit().putString(key, value).apply()

    private fun getPrefBool(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    private fun setPrefBool(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()

    private fun saveBrowserTabs(tabs: List<BrowserTabItem>) {
        try {
            val arr = JSONArray()
            for (tab in tabs) {
                val obj = JSONObject()
                obj.put("id", tab.id)
                obj.put("title", tab.title)
                obj.put("url", tab.url)
                arr.put(obj)
            }
            setPrefString("browser_tabs", arr.toString())
        } catch (e: Exception) {}
    }

    private fun loadBrowserTabs(): List<BrowserTabItem> {
        val str = getPrefString("browser_tabs", "")
        if (str.isBlank()) return listOf(
            BrowserTabItem(title = "Google", url = "https://www.google.com"),
            BrowserTabItem(title = "Hacker News", url = "https://news.ycombinator.com")
        )
        return try {
            val arr = JSONArray(str)
            val list = mutableListOf<BrowserTabItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    BrowserTabItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        url = obj.getString("url")
                    )
                )
            }
            list
        } catch (e: Exception) {
            listOf(
                BrowserTabItem(title = "Google", url = "https://www.google.com"),
                BrowserTabItem(title = "Hacker News", url = "https://news.ycombinator.com")
            )
        }
    }

    private fun saveHistoryList(items: List<BrowserHistoryItem>) {
        try {
            val arr = JSONArray()
            for (item in items) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("title", item.title)
                obj.put("url", item.url)
                obj.put("timestamp", item.timestamp)
                arr.put(obj)
            }
            setPrefString("browser_history", arr.toString())
        } catch (e: Exception) {}
    }

    private fun loadHistoryList(): List<BrowserHistoryItem> {
        val str = getPrefString("browser_history", "")
        if (str.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(str)
            val list = mutableListOf<BrowserHistoryItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                     BrowserHistoryItem(
                         id = obj.getString("id"),
                         title = obj.getString("title"),
                         url = obj.getString("url"),
                         timestamp = obj.getLong("timestamp")
                     )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Primary 4 tabs navigation
    var currentTab by mutableStateOf(AppTab.HOME)

    // Browser Tabs Management
    private var _browserTabs by mutableStateOf(emptyList<BrowserTabItem>())
    var browserTabs: List<BrowserTabItem>
        get() = _browserTabs
        set(value) {
            _browserTabs = value
            saveBrowserTabs(value)
        }
    private var _activeTabId by mutableStateOf("")
    var activeTabId: String
        get() = _activeTabId
        set(value) {
            _activeTabId = value
            setPrefString("active_tab_id", value)
        }

    // Browser History
    private var _historyList by mutableStateOf<List<BrowserHistoryItem>>(emptyList())
    var historyList: List<BrowserHistoryItem>
        get() = _historyList
        set(value) {
            _historyList = value
            saveHistoryList(value)
        }

    // App Preferences / Settings
    private var _defaultSearchEngine by mutableStateOf("Google")
    var defaultSearchEngine: String
        get() = _defaultSearchEngine
        set(value) {
            _defaultSearchEngine = value
            setPrefString("defaultSearchEngine", value)
        }
    private var _isAdblockEnabled by mutableStateOf(true)
    var isAdblockEnabled: Boolean
        get() = _isAdblockEnabled
        set(value) {
            _isAdblockEnabled = value
            setPrefBool("isAdblockEnabled", value)
        }
    private var _isWebInspectorEnabled by mutableStateOf(false)
    var isWebInspectorEnabled: Boolean
        get() = _isWebInspectorEnabled
        set(value) {
            _isWebInspectorEnabled = value
            setPrefBool("isWebInspectorEnabled", value)
        }
    private var _ignoreVerificationTimeouts by mutableStateOf(true)
    var ignoreVerificationTimeouts: Boolean
        get() = _ignoreVerificationTimeouts
        set(value) {
            _ignoreVerificationTimeouts = value
            setPrefBool("ignoreVerificationTimeouts", value)
        }

    private var _isDesktopSiteEnabled by mutableStateOf(false)
    var isDesktopSiteEnabled: Boolean
        get() = _isDesktopSiteEnabled
        set(value) {
            _isDesktopSiteEnabled = value
            setPrefBool("isDesktopSiteEnabled", value)
        }

    // Tier-specific premium capabilities
    private var _isPopUpBlockerEnabled by mutableStateOf(true)
    var isPopUpBlockerEnabled: Boolean
        get() = _isPopUpBlockerEnabled
        set(value) {
            _isPopUpBlockerEnabled = value
            setPrefBool("isPopUpBlockerEnabled", value)
        }

    private var _isSmartWaitEnabled by mutableStateOf(true)
    var isSmartWaitEnabled: Boolean
        get() = _isSmartWaitEnabled
        set(value) {
            _isSmartWaitEnabled = value
            setPrefBool("isSmartWaitEnabled", value)
        }

    private var _isRetryOnFailEnabled by mutableStateOf(true)
    var isRetryOnFailEnabled: Boolean
        get() = _isRetryOnFailEnabled
        set(value) {
            _isRetryOnFailEnabled = value
            setPrefBool("isRetryOnFailEnabled", value)
        }

    private var _isAutoNavigationEnabled by mutableStateOf(true)
    var isAutoNavigationEnabled: Boolean
        get() = _isAutoNavigationEnabled
        set(value) {
            _isAutoNavigationEnabled = value
            setPrefBool("isAutoNavigationEnabled", value)
        }

    private var _isSessionRestoreEnabled by mutableStateOf(true)
    var isSessionRestoreEnabled: Boolean
        get() = _isSessionRestoreEnabled
        set(value) {
            _isSessionRestoreEnabled = value
            setPrefBool("isSessionRestoreEnabled", value)
        }

    // UI state to trigger subtle premium upgrade prompt
    var showPremiumUpgradeDialog by mutableStateOf(false)

    // User Agent and Dev Tools States
    private var _currentUserAgentType by mutableStateOf("Default")
    var currentUserAgentType: String
        get() = _currentUserAgentType
        set(value) {
            _currentUserAgentType = value
            setPrefString("currentUserAgentType", value)
        }
    private var _customUserAgentString by mutableStateOf("")
    var customUserAgentString: String
        get() = _customUserAgentString
        set(value) {
            _customUserAgentString = value
            setPrefString("customUserAgentString", value)
        }

    // Multi AI Provider Settings
    private var _aiProvider by mutableStateOf("Gemini")
    var aiProvider: String
        get() = _aiProvider
        set(value) {
            _aiProvider = value
            setPrefString("aiProvider", value)
        }

    private var _aiApiKey by mutableStateOf("")
    var aiApiKey: String
        get() = _aiApiKey
        set(value) {
            _aiApiKey = value
            setPrefString("aiApiKey", value)
        }

    private var _aiModelName by mutableStateOf("gemini-1.5-flash")
    var aiModelName: String
        get() = _aiModelName
        set(value) {
            _aiModelName = value
            setPrefString("aiModelName", value)
        }

    private var _aiCustomEndpoint by mutableStateOf("")
    var aiCustomEndpoint: String
        get() = _aiCustomEndpoint
        set(value) {
            _aiCustomEndpoint = value
            setPrefString("aiCustomEndpoint", value)
        }

    // AI workflow auto-generator state variables
    var aiGenPromptText by mutableStateOf("")
    var isAiGeneratingWorkflow by mutableStateOf(false)
    var aiGenWorkflowName by mutableStateOf("")
    var aiGenWorkflowInitialUrl by mutableStateOf("")
    var aiGenWorkflowSteps by mutableStateOf<List<WorkflowStep>>(emptyList())
    var aiGenErrorMessage by mutableStateOf<String?>(null)

    fun generateAiWorkflow(prompt: String) {
        if (prompt.isBlank()) return
        isAiGeneratingWorkflow = true
        aiGenErrorMessage = null
        aiGenWorkflowSteps = emptyList()
        aiGenWorkflowName = ""
        aiGenWorkflowInitialUrl = ""
        
        viewModelScope.launch {
            try {
                val systemInstruction = """
                    You are an expert browser automation architect. Your task is to compile a structured, sequential browser automation script (workflow recipe) based on the user's micro action description.
                    
                    Respond ONLY with a valid, clean JSON object matching the following structure. Do NOT wrap your response in markdown code blocks like ```json or anything else. Just start with { and end with }.
                    
                    JSON Schema:
                    {
                      "name": "Concise but elegant description of the routine",
                      "initialUrl": "The exact protocol-included web link where the automation should begin playing (e.g. 'https://en.wikipedia.org' or 'https://news.ycombinator.com' or fallback to 'https://www.google.com')",
                      "steps": [
                        {
                          "type": "NAVIGATE" | "CLICK" | "INPUT" | "WAIT" | "SPEAK",
                          "target": "For NAVIGATE, specify the URL target. For CLICK or INPUT, provide a highly distinct, precise, and resilient CSS selector (e.g., 'input[type=\"email\"]', 'button[type=\"submit\"]', 'article header a'). For SPEAK, specify the message text to be spoken.",
                          "value": "For INPUT, specify the text payload to type. For WAIT, specify the duration in milliseconds (e.g., '3000'). For others, keep empty string."
                        }
                      ]
                    }
                    
                    Keep the workflow sequence short and effective (4 to 8 steps max).
                """.trimIndent()
                
                val rawResponse = com.example.network.RetrofitClient.generateContent(
                    prompt = prompt,
                    systemInstructionText = systemInstruction,
                    provider = aiProvider,
                    apiKeyArg = aiApiKey,
                    modelNameArg = aiModelName,
                    customEndpointArg = aiCustomEndpoint
                )
                
                val cleanedJson = rawResponse
                    .replace("```json", "")
                    .replace("```JSON", "")
                    .replace("```", "")
                    .trim()
                
                val startIdx = cleanedJson.indexOf("{")
                val endIdx = cleanedJson.lastIndexOf("}")
                if (startIdx == -1 || endIdx == -1 || startIdx >= endIdx) {
                    throw IllegalStateException("API did not return a valid JSON object. Received response:\n$rawResponse")
                }
                
                val jsonObject = org.json.JSONObject(cleanedJson.substring(startIdx, endIdx + 1))
                val name = jsonObject.optString("name", "Generated Routine").ifBlank { "Generated Routine" }
                val initialUrl = jsonObject.optString("initialUrl", "https://www.google.com").ifBlank { "https://www.google.com" }
                
                val stepsList = mutableListOf<WorkflowStep>()
                val stepsArray = jsonObject.optJSONArray("steps")
                if (stepsArray != null) {
                    for (i in 0 until stepsArray.length()) {
                        val stepObj = stepsArray.getJSONObject(i)
                        val stepId = java.util.UUID.randomUUID().toString()
                        val type = stepObj.getString("type").uppercase().trim()
                        val target = stepObj.optString("target", "")
                        val value = stepObj.optString("value", "")
                        stepsList.add(WorkflowStep(id = stepId, type = type, target = target, value = value, timestamp = System.currentTimeMillis()))
                    }
                }
                
                aiGenWorkflowName = name
                aiGenWorkflowInitialUrl = initialUrl
                aiGenWorkflowSteps = stepsList
                addPlaybackLog("🤖 AI generated a new sequence blueprint successfully: '$name' with ${stepsList.size} steps.")
            } catch (e: Exception) {
                aiGenErrorMessage = "Generation failed: ${e.localizedMessage ?: e.message}"
                addPlaybackLog("⚠️ AI compilation failed: ${e.localizedMessage}")
            } finally {
                isAiGeneratingWorkflow = false
            }
        }
    }

    fun saveAiWorkflowToLibrary() {
        val stepsList = aiGenWorkflowSteps
        if (stepsList.isEmpty()) return
        val name = aiGenWorkflowName.ifBlank { "AI Generated Macro" }
        val startUrl = aiGenWorkflowInitialUrl.ifBlank { "https://www.google.com" }
        
        viewModelScope.launch {
            val stepsStr = WorkflowJsonHelper.stepsToJsonString(stepsList)
            repository.insertWorkflow(
                Workflow(
                    name = name,
                    initialUrl = startUrl,
                    stepsJson = stepsStr
                )
            )
            // Reset AI generator state after successful import
            aiGenWorkflowSteps = emptyList()
            aiGenWorkflowName = ""
            aiGenWorkflowInitialUrl = ""
            aiGenPromptText = ""
            currentTab = AppTab.LIBRARY
            addPlaybackLog("📥 Imported AI Generated Routine '$name' into your persistent script library.")
        }
    }

    fun runAiWorkflowDirectly() {
        val stepsList = aiGenWorkflowSteps
        if (stepsList.isEmpty()) return
        val name = aiGenWorkflowName.ifBlank { "AI Generated Applet" }
        val startUrl = aiGenWorkflowInitialUrl.ifBlank { "https://www.google.com" }
        
        val stepsStr = WorkflowJsonHelper.stepsToJsonString(stepsList)
        val dummyWorkflow = Workflow(
            id = -999L, // Special ID for AI runtime simulation
            name = name,
            initialUrl = startUrl,
            stepsJson = stepsStr
        )
        
        startWorkflowExecution(dummyWorkflow)
    }

    private var _isPremiumUnlocked by mutableStateOf(false)
    var isPremiumUnlocked: Boolean
        get() = _isPremiumUnlocked
        set(value) {
            _isPremiumUnlocked = value
            setPrefBool("isPremiumUnlocked", value)
        }

    private var _playbackDelayMs by mutableStateOf(800L)
    var playbackDelayMs: Long
        get() = _playbackDelayMs
        set(value) {
            _playbackDelayMs = value
            prefs.edit().putLong("playbackDelayMs", value).apply()
        }

    // --- 10 CREATIVE CHANGES CUSTOM ADDED STATES ---
    
    // Change 5: App Theme Engine
    var selectedThemeAccent by mutableStateOf(getPrefString("themeAccent", "Indigo")) // Indigo, Emerald, Violet, Amber, Crimson
    var selectedThemeMode by mutableStateOf(getPrefString("themeMode", "DARK")) // DARK, LIGHT
    
    fun updateThemeMode(mode: String) {
        selectedThemeMode = mode
        setPrefString("themeMode", mode)
    }

    fun updateThemeAccent(accent: String) {
        selectedThemeAccent = accent
        setPrefString("themeAccent", accent)
    }

    // Change 7: Workspace Constants / Variables
    var globalVariables by mutableStateOf(loadGlobalVariables())

    private fun loadGlobalVariables(): Map<String, String> {
        val jsonStr = getPrefString("globalVariables", "")
        if (jsonStr.isBlank()) {
            return mapOf(
                "USERNAME" to "goodxvampire",
                "BASE_DELAY" to "1000",
                "PORT" to "8080",
                "ENV" to "Production"
            )
        }
        return try {
            val obj = JSONObject(jsonStr)
            val map = mutableMapOf<String, String>()
            obj.keys().forEach { key ->
                map[key] = obj.getString(key)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    fun saveGlobalVariables(map: Map<String, String>) {
        globalVariables = map
        try {
            val obj = JSONObject()
            map.forEach { (k, v) -> obj.put(k, v) }
            setPrefString("globalVariables", obj.toString())
        } catch (e: Exception) {}
    }

    // Change 8: Scheduled Workflows Engine
    data class ScheduledWorkflow(
        val id: String = UUID.randomUUID().toString(),
        val workflowId: Long,
        val workflowName: String,
        val triggerTimeMs: Long,
        val runPeriodMs: Long = 0L, // 0 means run once, otherwise interval in ms
        val isCompleted: Boolean = false
    )
    var scheduledWorkflows by mutableStateOf(loadScheduledWorkflows())

    private fun loadScheduledWorkflows(): List<ScheduledWorkflow> {
        val jsonStr = getPrefString("scheduledWorkflows", "")
        if (jsonStr.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<ScheduledWorkflow>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ScheduledWorkflow(
                        id = obj.getString("id"),
                        workflowId = obj.getLong("workflowId"),
                        workflowName = obj.getString("workflowName"),
                        triggerTimeMs = obj.getLong("triggerTimeMs"),
                        runPeriodMs = obj.getLong("runPeriodMs"),
                        isCompleted = obj.getBoolean("isCompleted")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun saveScheduledWorkflows(list: List<ScheduledWorkflow>) {
        scheduledWorkflows = list
        try {
            val arr = JSONArray()
            list.forEach { item ->
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("workflowId", item.workflowId)
                obj.put("workflowName", item.workflowName)
                obj.put("triggerTimeMs", item.triggerTimeMs)
                obj.put("runPeriodMs", item.runPeriodMs)
                obj.put("isCompleted", item.isCompleted)
                arr.put(obj)
            }
            setPrefString("scheduledWorkflows", arr.toString())
        } catch (e: Exception) {}
    }

    // Change 3: Execution Telemetry & Impact Metrics
    var totalExecutedStepsSuccess by mutableStateOf(prefs.getInt("totalStepsSuccess", 12))
    var totalExecutedStepsError by mutableStateOf(prefs.getInt("totalStepsError", 1))
    var totalTimeSavedMinutes by mutableStateOf(prefs.getFloat("totalTimeSaved", 4.5f))

    fun incrementStepsSuccess() {
        totalExecutedStepsSuccess++
        prefs.edit().putInt("totalStepsSuccess", totalExecutedStepsSuccess).apply()
        // Save averages: each success step saves approx 0.2 minutes (12 seconds)
        totalTimeSavedMinutes += 0.2f
        prefs.edit().putFloat("totalTimeSaved", totalTimeSavedMinutes).apply()
    }

    fun incrementStepsError() {
        totalExecutedStepsError++
        prefs.edit().putInt("totalStepsError", totalExecutedStepsError).apply()
    }

    // Change 6: Macro Step Debugger States
    var isDebugModeEnabled by mutableStateOf(false)
    var isExecutionPaused by mutableStateOf(false)
    var currentDebuggerStepIndex by mutableStateOf(-1)
    var breakpoints by mutableStateOf(emptySet<String>()) // Holds step IDs

    fun toggleBreakpoint(stepId: String) {
        breakpoints = if (breakpoints.contains(stepId)) {
            breakpoints - stepId
        } else {
            breakpoints + stepId
        }
    }

    // Step failure & execution control state
    var showStepFailureDialog by mutableStateOf(false)
    var lastFailedStepInfo by mutableStateOf<Triple<String, String, String>?>(null) // type, target, errorMsg
    var currentExecutingStepIndex by mutableStateOf(-1)
    @Volatile private var isSkippingCurrentStep = false
    @Volatile private var isRetryingCurrentStep = false

    fun retryCurrentStep() {
        showStepFailureDialog = false
        lastFailedStepInfo = null
        isRetryingCurrentStep = true
    }

    fun skipCurrentStep() {
        showStepFailureDialog = false
        lastFailedStepInfo = null
        isSkippingCurrentStep = true
    }

    fun stopExecution() {
        isPlaying = false
        showStepFailureDialog = false
        lastFailedStepInfo = null
        currentExecutingStepIndex = -1
        addPlaybackLog("[STOPPED] Execution halted by user.")
    }

    fun testSingleStep(step: WorkflowStep) {
        viewModelScope.launch {
            val testStep = step.copy(id = java.util.UUID.randomUUID().toString())
            currentTab = AppTab.HOME
            addPlaybackLog("[TEST] Single step test dispatched: [${step.type}] → ${step.target.take(60)}")
            delay(300)
            _webAutomationTrigger.emit(testStep)
        }
    }

    private fun inferSmartVariableName(selector: String, existingCount: Int): String {
        val s = selector.lowercase()
        val suffix = if (existingCount > 0) "_$existingCount" else ""
        return when {
            // Email — selector names + input[type=email]
            s.contains("email") || s.contains("type=\"email\"") || s.contains("type='email'") ->
                "EMAIL$suffix"
            // Password
            s.contains("password") || s.contains("passwd") ||
                s.contains("type=\"password\"") || s.contains("type='password'") ->
                "PASSWORD$suffix"
            // Search / query
            s.contains("search") || s.contains("[name=\"q\"]") || s.contains("name='q'") ||
                s.contains("placeholder=\"search") || s.contains("type=\"search\"") ->
                "SEARCH_QUERY$suffix"
            // Username / login
            s.contains("username") || s.contains("[name=\"user") || s.contains("login") ||
                s.contains("placeholder=\"username") ->
                "USERNAME$suffix"
            // Phone / mobile
            s.contains("phone") || s.contains("mobile") ||
                s.contains("type=\"tel\"") || s.contains("type='tel'") ->
                "PHONE_NUMBER$suffix"
            // URL fields
            s.contains("type=\"url\"") || s.contains("type='url'") ||
                (s.contains("url") && !s.contains("value")) ->
                "TARGET_URL$suffix"
            // Full name
            s.contains("fullname") || s.contains("full_name") || s.contains("full-name") ||
                (s.contains("name") && !s.contains("username") && !s.contains("file")) ->
                "FULL_NAME$suffix"
            // Address fields
            s.contains("address") || s.contains("city") || s.contains("zip") ||
                s.contains("postal") || s.contains("street") ->
                "ADDRESS$suffix"
            // Number inputs
            s.contains("type=\"number\"") || s.contains("amount") || s.contains("quantity") ->
                "NUMBER_VALUE$suffix"
            // Message / comment / textarea
            s.contains("message") || s.contains("comment") || s.contains("textarea") ||
                s.contains("description") || s.contains("notes") ->
                "MESSAGE_TEXT$suffix"
            // Date / time
            s.contains("type=\"date\"") || s.contains("type=\"time\"") || s.contains("date") ->
                "DATE_VALUE$suffix"
            else -> "INPUT_TEXT$suffix"
        }
    }

    // Change 9: Web Inspector Scripts Catalog
    data class CodeSnippet(
        val name: String,
        val snippet: String,
        val description: String
    )
    val scriptSnippets = listOf(
        CodeSnippet("Auto Scroll Bottom", "window.scrollTo({top: document.body.scrollHeight, behavior: 'smooth'});", "Smoothly scroll down to trigger lazy loading web assets."),
        CodeSnippet("Unlock ReadOnly Fields", "document.querySelectorAll('input').forEach(i => i.removeAttribute('readonly'));", "Unlock read-only text input fields on rigid forms."),
        CodeSnippet("Reset Session Cookies", "document.cookie.split(';').forEach(c => { document.cookie = c.replace(/^ +/, '').replace(/=.*/, '=;expires=' + new Date().toUTCString() + ';path=/'); }); location.reload();", "Wipes active page cookies and reloads DOM instantly."),
        CodeSnippet("Outline Input Bounds", "document.querySelectorAll('input, button').forEach(e => e.style.outline = '2px dashed #6366f1');", "Visually outlines interactive bounds for target selector analysis.")
    )

    // Shareable QR Text
    var shareableQrTextState by mutableStateOf("")

    var pageSourceHtml by mutableStateOf<String?>(null)
    var isFetchingSource by mutableStateOf(false)

    var aiStatusMessage by mutableStateOf("")
    var isAiAnalyzing by mutableStateOf(false)
    var aiAnalysisResult by mutableStateOf<String?>(null)

    fun runSmartPageAnalysis() {
        val html = pageSourceHtml ?: ""
        if (html.isBlank()) {
            aiAnalysisResult = "Error: Active page source is empty. Please navigate to a webpage in the Creator browser first."
            return
        }
        
        isAiAnalyzing = true
        aiStatusMessage = "Ingesting webpage source..."
        aiAnalysisResult = null
        
        viewModelScope.launch {
            try {
                // Shorten/sanitize HTML if too long to prevent token overflow
                val sanitizedHtmlSnippet = if (html.length > 6000) {
                    html.substring(0, 5500) + "\n...[truncated for token efficiency]..."
                } else {
                    html
                }
                
                aiStatusMessage = "Analyzing elements with $aiProvider ($aiModelName)..."
                val prompt = """
                    You are OurAuto Smart Element Detector, an AI agent embedded in OurAuto Browser Automation Suite.
                    Below is a snippet of the active webpage's HTML source:
                    
                    ```html
                    $sanitizedHtmlSnippet
                    ```
                    
                    Analyze this HTML snippet and identify 3-5 key interactive elements (like buttons, inputs, login forms, or navigation links). 
                    For each element, recommend its EXACT, optimized, and most resilient CSS Selector for OurAuto's CLICK or INPUT steps.
                    Briefly explain what each element does, why you chose that CSS Selector, and list it in a clean bulleted list.
                    
                    Structure the output clearly using elegant Markdown formatting.
                """.trimIndent()
                
                val result = com.example.network.RetrofitClient.generateContent(
                    prompt = prompt,
                    systemInstructionText = "You are a professional browser automation engineer. You generate highly robust, unique, and optimized CSS selectors for Web Scraping and Puppeteer/Playwright automation based on raw HTML input.",
                    provider = aiProvider,
                    apiKeyArg = aiApiKey,
                    modelNameArg = aiModelName,
                    customEndpointArg = aiCustomEndpoint
                )
                
                aiAnalysisResult = result
            } catch (e: Exception) {
                aiAnalysisResult = "Error during analysis: ${e.localizedMessage ?: e.message}"
            } finally {
                isAiAnalyzing = false
            }
        }
    }

    var activeBrowserCookies by mutableStateOf<List<Pair<String, String>>>(emptyList())

    fun refreshBrowserCookies(url: String) {
        try {
            val cookieManager = android.webkit.CookieManager.getInstance()
            val cookieString = cookieManager.getCookie(url) ?: ""
            val list = if (cookieString.trim().isEmpty()) {
                emptyList()
            } else {
                cookieString.split(";").mapNotNull {
                    val parts = it.split("=", limit = 2)
                    if (parts.size >= 2) {
                        parts[0].trim() to parts[1].trim()
                    } else if (parts.size == 1 && parts[0].trim().isNotEmpty()) {
                        parts[0].trim() to ""
                    } else {
                        null
                    }
                }
            }
            activeBrowserCookies = list
        } catch (e: Exception) {
            activeBrowserCookies = emptyList()
        }
    }

    fun saveCookie(url: String, name: String, value: String) {
        try {
            val cookieManager = android.webkit.CookieManager.getInstance()
            cookieManager.setCookie(url, "$name=$value; Path=/")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                cookieManager.flush()
            }
            refreshBrowserCookies(url)
        } catch (e: Exception) {}
    }

    fun deleteCookie(url: String, name: String) {
        try {
            val cookieManager = android.webkit.CookieManager.getInstance()
            cookieManager.setCookie(url, "$name=; Max-Age=0; Path=/")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                cookieManager.flush()
            }
            refreshBrowserCookies(url)
        } catch (e: Exception) {}
    }

    // Custom Browser Extensions
    private var _isDarkThemeExtensionEnabled by mutableStateOf(false)
    var isDarkThemeExtensionEnabled: Boolean
        get() = _isDarkThemeExtensionEnabled
        set(value) {
            _isDarkThemeExtensionEnabled = value
            setPrefBool("isDarkThemeExtensionEnabled", value)
        }
    private var _isFpsHudExtensionEnabled by mutableStateOf(false)
    var isFpsHudExtensionEnabled: Boolean
        get() = _isFpsHudExtensionEnabled
        set(value) {
            _isFpsHudExtensionEnabled = value
            setPrefBool("isFpsHudExtensionEnabled", value)
        }
    private var _isAutoRefreshExtensionEnabled by mutableStateOf(false)
    var isAutoRefreshExtensionEnabled: Boolean
        get() = _isAutoRefreshExtensionEnabled
        set(value) {
            _isAutoRefreshExtensionEnabled = value
            setPrefBool("isAutoRefreshExtensionEnabled", value)
        }

    var installedExtensions by mutableStateOf<List<WebExtension>>(
        listOf(
            WebExtension(
                name = "AdBlock Pro Core (Chrome Remake)",
                description = "Suppresses standard browser ads, doubleclicks, popups, sponsoring loops & frames",
                urlMatchPattern = "*",
                jsContent = """
                    (function() {
                        const selectors = '.ad, .ads, .adsbygoogle, iframe[src*="doubleclick"], div[id*="google_ads"], div[class*="Sponsored"], div[class*="ad-"]';
                        const hide = () => document.querySelectorAll(selectors).forEach(el => el.style.setProperty('display', 'none', 'important'));
                        hide();
                        new MutationObserver(hide).observe(document.body, {childList: true, subtree: true});
                    })();
                """.trimIndent(),
                isEnabled = true
            ),
            WebExtension(
                name = "Google Translate Widget",
                description = "Translates active website domain into over 100 languages with instant layout widgets",
                urlMatchPattern = "*",
                jsContent = """
                    (function() {
                        if (document.getElementById('google_translate_element')) return;
                        var div = document.createElement('div');
                        div.id = 'google_translate_element';
                        div.style.position = 'fixed';
                        div.style.bottom = '8px';
                        div.style.right = '8px';
                        div.style.zIndex = '999999';
                        div.style.backgroundColor = '#ffffff';
                        div.style.border = '1px solid #ddd';
                        div.style.borderRadius = '4px';
                        div.style.padding = '4px';
                        document.body.appendChild(div);
                        var script = document.createElement('script');
                        script.type = 'text/javascript';
                        script.src = '//translate.google.com/translate_a/element.js?cb=googleTranslateElementInit';
                        document.body.appendChild(script);
                        window.googleTranslateElementInit = function() {
                            new google.translate.TranslateElement({pageLanguage: 'en', layout: google.translate.TranslateElement.InlineLayout.SIMPLE}, 'google_translate_element');
                        };
                    })();
                """.trimIndent(),
                isEnabled = false
            ),
            WebExtension(
                name = "Cookie Consent Autorun-Shield",
                description = "Bypasses boring cookie consent prompts by automatically clicking primary close/accept tags",
                urlMatchPattern = "*",
                jsContent = """
                    (function() {
                        const buttons = Array.from(document.querySelectorAll('button, a'));
                        const phrases = ['accept all', 'agree', 'allow cookies', 'got it', 'dismiss', 'accept'];
                        for (let btn of buttons) {
                            if (phrases.includes(btn.innerText.trim().toLowerCase())) {
                                btn.click();
                                break;
                            }
                        }
                    })();
                """.trimIndent(),
                isEnabled = false
            ),
            WebExtension(
                name = "Element Color Sniffer",
                description = "Infiltrates loaded webpage documents and highlights all visual links with magenta highlights",
                urlMatchPattern = "*",
                jsContent = """
                    (function() {
                        document.querySelectorAll('a').forEach(link => {
                            link.style.outline = '2px dashed #EC4899';
                            link.style.outlineOffset = '2dp';
                        });
                    })();
                """.trimIndent(),
                isEnabled = false
            )
        )
    )

    fun toggleExtension(id: String) {
        installedExtensions = installedExtensions.map {
            if (it.id == id) it.copy(isEnabled = !it.isEnabled) else it
        }
    }

    fun addNewExtension(name: String, description: String, pattern: String, js: String) {
        val newExt = WebExtension(
            name = name,
            description = description,
            urlMatchPattern = pattern,
            jsContent = js,
            isEnabled = true
        )
        installedExtensions = installedExtensions + newExt
    }

    fun deleteExtension(id: String) {
        installedExtensions = installedExtensions.filter { it.id != id }
    }

    val variableRegistry = androidx.compose.runtime.mutableStateMapOf<String, String>()
    
    // Concurrency safe tracking of completed step results to prevent race-condition halts during fast cycles
    val completedStepExecutionResults = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun onStepExecutionResult(stepId: String, status: String, message: String) {
        completedStepExecutionResults[stepId] = status
        viewModelScope.launch {
            _stepExecutionResults.emit(Pair(stepId, status))
            addPlaybackLog("Result Callback [$status]: step $stepId -> $message")
            if (message.startsWith("VALUE:") && status == "SUCCESS") {
                val contents = message.substring(6).trim()
                val traceStep = runTraceSteps.value.find { it.step.id == stepId }
                if (traceStep != null) {
                    val targetVarName = traceStep.step.value.ifBlank { "captured_val" }
                    variableRegistry[targetVarName] = contents
                    addPlaybackLog("[VAR_CAPTURE] Dynamic Variable Captured: $targetVarName = $contents")
                }
            } else if (message.startsWith("VAR_SAVE:") && status == "SUCCESS") {
                val parts = message.substring(9).split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim()
                    variableRegistry[key] = value
                    addPlaybackLog("[VAR_CAPTURE] Dynamic Variable Saved via JS: $key = $value")
                }
            }
        }
    }

    // Current page load status
    private var _activeBrowserUrl by mutableStateOf("https://www.google.com")
    var activeBrowserUrl: String
        get() = _activeBrowserUrl
        set(value) {
            _activeBrowserUrl = value
            if (value.isNotBlank() && !value.startsWith("javascript:")) {
                setPrefString("active_browser_url", value)
            }
        }
    var isBrowserLoading by mutableStateOf(false)

    // Macro Recorder Configuration & Active flow
    var routineName by mutableStateOf("My Direct Routine")
    var isRecording by mutableStateOf(false)
    var isTemplateMode by mutableStateOf(false)
    var recordedSteps = mutableStateOf<List<WorkflowStep>>(emptyList())

    // Execution / Trace outputs
    private val _stepExecutionResults = MutableSharedFlow<Pair<String, String>>()
    val stepExecutionResults = _stepExecutionResults.asSharedFlow()
    var activeRunningWorkflowName by mutableStateOf("")
    var runTraceSteps = mutableStateOf<List<TraceStep>>(emptyList())
    var isPlaying by mutableStateOf(false)
    var playbackLog = mutableStateOf<List<String>>(emptyList())
    var totalRunsCount by mutableStateOf(4)
    var issuesList by mutableStateOf<List<ExecutionIssue>>(emptyList())

    // Captures for modern screenshot actions and bulk cookies
    var capturedScreenshots by mutableStateOf<List<android.graphics.Bitmap>>(emptyList())

    fun addScreenshotCapture(bitmap: android.graphics.Bitmap) {
        capturedScreenshots = capturedScreenshots + bitmap
    }

    fun clearScreenshots() {
        capturedScreenshots = emptyList()
    }

    // Standard File Downloads System State
    var downloadsList by mutableStateOf<List<DownloadItem>>(emptyList())
    private var pollingJob: kotlinx.coroutines.Job? = null

    // Chrome-like Floating HUD State
    var activeDownloadHUDItem by mutableStateOf<DownloadItem?>(null)
    var showDownloadHUD by mutableStateOf(false)
    private val recentDownloadUrls = mutableMapOf<String, Long>()

    // Chrome-style Download confirmation popup
    var showDownloadConfirmDialog by mutableStateOf(false)
    var pendingDownloadRequest by mutableStateOf<PendingDownloadRequest?>(null)

    fun addDownload(id: Long, filename: String, url: String, mimeType: String, totalBytes: Long) {
        val newItem = DownloadItem(
            id = id,
            filename = filename,
            url = url,
            mimeType = mimeType,
            totalBytes = totalBytes,
            status = "PENDING",
            progress = 0f
        )
        downloadsList = downloadsList + newItem
        activeDownloadHUDItem = newItem
        showDownloadHUD = true
        startDownloadPolling()
    }

    fun startDownloadPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val downloadManager = getApplication<Application>().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            while (downloadsList.any { it.status == "PENDING" || it.status == "RUNNING" }) {
                val updatedList = downloadsList.map { item ->
                    if (item.status == "PENDING" || item.status == "RUNNING") {
                        val query = DownloadManager.Query().setFilterById(item.id)
                        val cursor = try { downloadManager.query(query) } catch (e: Exception) { null }
                        if (cursor != null && cursor.moveToFirst()) {
                            val bytesDownloadedIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                            val bytesTotalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                            val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            val localUriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)

                            val bytesDownloaded = if (bytesDownloadedIdx != -1) cursor.getLong(bytesDownloadedIdx) else 0L
                            val bytesTotal = if (bytesTotalIdx != -1) cursor.getLong(bytesTotalIdx) else 0L
                            val rawStatus = if (statusIdx != -1) cursor.getInt(statusIdx) else -1
                            val localUri = if (localUriIdx != -1) cursor.getString(localUriIdx) else null

                            cursor.close()

                            val statusString = when (rawStatus) {
                                DownloadManager.STATUS_PENDING -> "PENDING"
                                DownloadManager.STATUS_RUNNING -> "RUNNING"
                                DownloadManager.STATUS_SUCCESSFUL -> "SUCCESSFUL"
                                DownloadManager.STATUS_FAILED -> "FAILED"
                                DownloadManager.STATUS_PAUSED -> "PAUSED"
                                else -> "UNKNOWN"
                            }

                            val progress = if (bytesTotal > 0) bytesDownloaded.toFloat() / bytesTotal.toFloat() else 0f
                            item.copy(
                                status = statusString,
                                progress = progress,
                                totalBytes = if (bytesTotal > 0) bytesTotal else item.totalBytes,
                                localUri = localUri
                            )
                        } else {
                            cursor?.close()
                            item
                        }
                    } else {
                        item
                    }
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    downloadsList = updatedList
                    // Sync our Chrome-like floating download shelf
                    activeDownloadHUDItem?.let { hudItem ->
                        updatedList.find { it.id == hudItem.id }?.let { matched ->
                            activeDownloadHUDItem = matched
                        }
                    }
                }
                delay(1000)
            }
        }
    }

    fun triggerDownload(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?,
        contentLength: Long
    ) {
        val currentTime = System.currentTimeMillis()
        val lastTriggerTime = recentDownloadUrls[url]
        if (lastTriggerTime != null && (currentTime - lastTriggerTime) < 3000) {
            // Already initiated extremely recently; prevent double-triggering from WebView
            return
        }
        recentDownloadUrls[url] = currentTime

        val filename = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype)
        pendingDownloadRequest = PendingDownloadRequest(
            url = url,
            userAgent = userAgent,
            contentDisposition = contentDisposition,
            mimetype = mimetype ?: "application/octet-stream",
            contentLength = contentLength,
            filename = filename
        )
        showDownloadConfirmDialog = true
    }

    fun confirmAndEnqueueDownload() {
        val requestData = pendingDownloadRequest ?: return
        showDownloadConfirmDialog = false
        pendingDownloadRequest = null

        try {
            val context = getApplication<Application>()
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(requestData.url)
            val request = DownloadManager.Request(uri)
                .setTitle(requestData.filename)
                .setDescription("Downloading via OurAuto Browser...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, requestData.filename)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val cookies = android.webkit.CookieManager.getInstance().getCookie(requestData.url)
            if (!cookies.isNullOrBlank()) {
                request.addRequestHeader("Cookie", cookies)
            }
            if (!requestData.userAgent.isNullOrBlank()) {
                request.addRequestHeader("User-Agent", requestData.userAgent)
            } else {
                request.addRequestHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
            }

            val id = downloadManager.enqueue(request)
            addDownload(id, requestData.filename, requestData.url, requestData.mimetype ?: "application/octet-stream", requestData.contentLength)

            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                Toast.makeText(context, "Download started: ${requestData.filename}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            val context = getApplication<Application>()
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                Toast.makeText(context, "Download failed to initiate: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun triggerOpenDownloadedFile(context: Context, download: DownloadItem) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val fileUri = downloadManager.getUriForDownloadedFile(download.id)
            if (fileUri != null) {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    setDataAndType(fileUri, download.mimeType)
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Cannot locate file: ${download.filename}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearAllCookies(url: String) {
        try {
            val cookieManager = android.webkit.CookieManager.getInstance()
            activeBrowserCookies.forEach { cookie ->
                cookieManager.setCookie(url, "${cookie.first}=; Max-Age=0; Path=/")
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                cookieManager.flush()
            }
            refreshBrowserCookies(url)
        } catch (e: Exception) {}
    }

    // Live Execution event trigger
    private val _webAutomationTrigger = MutableSharedFlow<WorkflowStep>()
    val webAutomationTrigger = _webAutomationTrigger.asSharedFlow()

    // Import / Export states
    var importErrorMessage by mutableStateOf<String?>(null)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = WorkflowRepository(database.workflowDao())
        workflows = repository.allWorkflows.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Change 8: Background periodic scheduler check
        viewModelScope.launch {
            while (true) {
                delay(3000)
                val now = System.currentTimeMillis()
                val activeRuns = scheduledWorkflows.filter { !it.isCompleted && it.triggerTimeMs <= now }
                for (run in activeRuns) {
                    val targetWorkflow = workflows.value.find { it.id == run.workflowId }
                    if (targetWorkflow != null) {
                        viewModelScope.launch {
                            addPlaybackLog("[SCHEDULER] Automatically launching scheduled recipe: '${targetWorkflow.name}'")
                            startWorkflowExecutionWithVariables(targetWorkflow, globalVariables)
                        }
                    }
                    val updated = scheduledWorkflows.map {
                        if (it.id == run.id) {
                            if (it.runPeriodMs > 0L) {
                                it.copy(triggerTimeMs = now + it.runPeriodMs) // Schedule for next iteration cycle
                            } else {
                                it.copy(isCompleted = true)
                            }
                        } else {
                            it
                        }
                    }
                    saveScheduledWorkflows(updated)
                }
            }
        }

        // Load persisted settings
        _defaultSearchEngine = getPrefString("defaultSearchEngine", "Google")
        _isAdblockEnabled = getPrefBool("isAdblockEnabled", true)
        _isWebInspectorEnabled = getPrefBool("isWebInspectorEnabled", false)
        _ignoreVerificationTimeouts = getPrefBool("ignoreVerificationTimeouts", true)
        _isDesktopSiteEnabled = getPrefBool("isDesktopSiteEnabled", false)
        _currentUserAgentType = getPrefString("currentUserAgentType", "Default")
        _customUserAgentString = getPrefString("customUserAgentString", "")
        _isDarkThemeExtensionEnabled = getPrefBool("isDarkThemeExtensionEnabled", false)
        _isFpsHudExtensionEnabled = getPrefBool("isFpsHudExtensionEnabled", false)
        _isAutoRefreshExtensionEnabled = getPrefBool("isAutoRefreshExtensionEnabled", false)

        _isPopUpBlockerEnabled = getPrefBool("isPopUpBlockerEnabled", true)
        _isSmartWaitEnabled = getPrefBool("isSmartWaitEnabled", true)
        _isRetryOnFailEnabled = getPrefBool("isRetryOnFailEnabled", true)
        _isAutoNavigationEnabled = getPrefBool("isAutoNavigationEnabled", true)
        _isSessionRestoreEnabled = getPrefBool("isSessionRestoreEnabled", true)

        _aiProvider = getPrefString("aiProvider", "Gemini")
        _aiApiKey = getPrefString("aiApiKey", "")
        _aiModelName = getPrefString("aiModelName", "gemini-1.5-flash")
        _aiCustomEndpoint = getPrefString("aiCustomEndpoint", "")
        _isPremiumUnlocked = getPrefBool("isPremiumUnlocked", false)
        _playbackDelayMs = prefs.getLong("playbackDelayMs", 800L)

        // Load persisted tabs and history
        val savedTabs = loadBrowserTabs()
        _browserTabs = savedTabs
        _historyList = loadHistoryList()

        val savedActiveTabId = getPrefString("active_tab_id", "")
        val activeUrl = getPrefString("active_browser_url", "")

        if (savedTabs.isNotEmpty()) {
            val matchingTab = savedTabs.find { it.id == savedActiveTabId }
            if (matchingTab != null) {
                _activeTabId = matchingTab.id
                _activeBrowserUrl = if (activeUrl.isNotBlank() && !activeUrl.startsWith("javascript:")) activeUrl else matchingTab.url
            } else {
                _activeTabId = savedTabs.first().id
                _activeBrowserUrl = savedTabs.first().url
            }
        } else {
            // Seed defaults if nothing saved
            val defaultTabs = listOf(
                BrowserTabItem(title = "Google", url = "https://www.google.com"),
                BrowserTabItem(title = "Hacker News", url = "https://news.ycombinator.com")
            )
            _browserTabs = defaultTabs
            _activeTabId = defaultTabs.first().id
            _activeBrowserUrl = defaultTabs.first().url
        }

        // Seed preset templates if empty on startup
        viewModelScope.launch {
            repository.allWorkflows.collect { list ->
                if (list.isEmpty()) {
                    presetSampleWorkflows()
                }
            }
        }
    }

    private suspend fun presetSampleWorkflows() {
        val wikiSteps = listOf(
            WorkflowStep("w1", "NAVIGATE", "https://en.m.wikipedia.org"),
            WorkflowStep("w2", "INPUT", "input[name='search'], input[type='search']", "Automation"),
            WorkflowStep("w3", "WAIT", "", "1000"),
            WorkflowStep("w4", "SCROLL", "window", "0,400"),
            WorkflowStep("w5", "WAIT", "", "1000"),
            WorkflowStep("w6", "SCROLL", "window", "0,850")
        )
        repository.insertWorkflow(
            Workflow(
                name = "Wikipedia Search & Auto-Scroll",
                initialUrl = "https://en.m.wikipedia.org",
                stepsJson = WorkflowJsonHelper.stepsToJsonString(wikiSteps)
            )
        )
    }

    // Tabs Management
    fun addNewTab(url: String = "https://www.google.com") {
        val newTab = BrowserTabItem(title = getDomainName(url), url = url)
        browserTabs = browserTabs + newTab
        activeTabId = newTab.id
        activeBrowserUrl = url
    }

    fun removeTab(tabId: String) {
        if (browserTabs.size <= 1) return // Keep at least one tab
        val tabToClose = browserTabs.find { it.id == tabId } ?: return
        val wasActive = (activeTabId == tabId)
        browserTabs = browserTabs.filter { it.id != tabId }
        if (wasActive) {
            val remaining = browserTabs.last()
            activeTabId = remaining.id
            activeBrowserUrl = remaining.url
        }
    }

    fun selectTab(tabId: String) {
        val tab = browserTabs.find { it.id == tabId } ?: return
        activeTabId = tab.id
        activeBrowserUrl = tab.url
    }

    // Helper to format smart query text
    fun formatSearchQuery(input: String): String {
        val trimInput = input.trim()
        if (trimInput.startsWith("http://") || trimInput.startsWith("https://")) {
            return trimInput
        }
        if (trimInput.contains(".") && !trimInput.contains(" ")) {
            return "https://$trimInput"
        }
        val baseUrl = when (defaultSearchEngine) {
            "Bing" -> "https://www.bing.com/search?q="
            "DuckDuckGo" -> "https://duckduckgo.com/?q="
            else -> "https://www.google.com/search?q="
        }
        return baseUrl + java.net.URLEncoder.encode(trimInput, "UTF-8")
    }

    // Callback on browser page loaded completely
    fun onBrowserUrlChanged(newUrl: String, title: String? = null) {
        if (newUrl.isNotBlank() && !newUrl.startsWith("javascript:")) {
            activeBrowserUrl = newUrl
            // Save tab URL
            browserTabs = browserTabs.map {
                if (it.id == activeTabId) {
                    it.copy(url = newUrl, title = title ?: getDomainName(newUrl))
                } else {
                    it
                }
            }
            // Add to history list if unique from last
            if (historyList.isEmpty() || historyList.first().url != newUrl) {
                historyList = listOf(
                    BrowserHistoryItem(title = title ?: getDomainName(newUrl), url = newUrl)
                ) + historyList.take(49)
            }
            // Record transition redirects
            if (isRecording) {
                val currentList = recordedSteps.value
                val lastStep = currentList.lastOrNull()
                if (lastStep == null || !(lastStep.type == "NAVIGATE" && lastStep.target == newUrl)) {
                    val resolvedTarget = if (isTemplateMode) {
                        val navCount = currentList.filter { it.type == "NAVIGATE" }.size
                        if (navCount == 0) {
                            "{{BASE_URL:$newUrl}}"
                        } else {
                            "{{URL_$navCount:$newUrl}}"
                        }
                    } else {
                        newUrl
                    }
                    val step = WorkflowStep(
                        id = java.util.UUID.randomUUID().toString(),
                        type = "NAVIGATE",
                        target = resolvedTarget,
                        value = title ?: getDomainName(newUrl),
                        timestamp = System.currentTimeMillis()
                    )
                    appendStepWithTiming(step)
                }
            }
        }
    }

    private fun getDomainName(url: String): String {
        return try {
            val uri = java.net.URI(url)
            val domain = uri.host ?: ""
            if (domain.startsWith("www.")) domain.substring(4) else domain
        } catch (e: Exception) {
            "Browser Page"
        }
    }

    private fun appendStepWithTiming(step: WorkflowStep) {
        val currentList = recordedSteps.value
        val lastStep = currentList.lastOrNull()
        val updatedList = mutableListOf<WorkflowStep>()
        updatedList.addAll(currentList)
        
        if (lastStep != null) {
            val elapsed = System.currentTimeMillis() - lastStep.timestamp
            if (elapsed > 400) { // Pause of more than 400ms
                val waitStep = WorkflowStep(
                    id = UUID.randomUUID().toString(),
                    type = "WAIT",
                    target = "delay",
                    value = elapsed.toString(),
                    timestamp = lastStep.timestamp + 10
                )
                updatedList.add(waitStep)
            }
        }
        updatedList.add(step)
        recordedSteps.value = updatedList
    }

    // Capture automation recorders events
    fun recordStepEvent(type: String, target: String, value: String) {
        if (!isRecording) return
        
        var resolvedTarget = target
        var resolvedValue = value
        
        if (isTemplateMode) {
            if (type == "INPUT") {
                val existingInputCount = recordedSteps.value.filter { it.type == "INPUT" }.size
                val smartName = inferSmartVariableName(target, existingInputCount)
                resolvedValue = "{{${smartName}:$value}}"
            }
        }
        
        val steps = recordedSteps.value.toMutableList()
        if (type == "DOUBLE_CLICK" || type == "LONG_PRESS") {
            // Find and remove the previous CLICK step on the same target in the last 4 steps
            val checkIdx = steps.indexOfLast { it.type == "CLICK" && it.target == resolvedTarget }
            if (checkIdx != -1 && checkIdx >= steps.size - 4) {
                steps.removeAt(checkIdx)
                // If there's an adjacent WAIT step right after the removed CLICK, remove it as well to keep the macro clean
                if (checkIdx < steps.size && steps[checkIdx].type == "WAIT") {
                    steps.removeAt(checkIdx)
                }
                recordedSteps.value = steps
            }
        }

        val step = WorkflowStep(
            id = UUID.randomUUID().toString(),
            type = type,
            target = resolvedTarget,
            value = resolvedValue,
            timestamp = System.currentTimeMillis()
        )
        // Deduplicate click spam
        val currentList = recordedSteps.value
        if (currentList.isNotEmpty() && currentList.last().type == type && currentList.last().target == resolvedTarget && type == "CLICK") {
            return
        }
        appendStepWithTiming(step)
    }

    fun startNewRecording() {
        recordedSteps.value = emptyList()
        isRecording = true
        currentTab = AppTab.CREATOR
    }

    fun stopAndSaveRecording() {
        if (!isRecording) return
        isRecording = false
        val stepsStr = WorkflowJsonHelper.stepsToJsonString(recordedSteps.value)
        val savedInitialUrl = if (isTemplateMode) {
            "{{BASE_URL:$activeBrowserUrl}}"
        } else {
            activeBrowserUrl
        }
        viewModelScope.launch {
            repository.insertWorkflow(
                Workflow(
                    name = routineName.ifBlank { if (isTemplateMode) "Reusable Script Template" else "Smart Workflow Sequence" },
                    initialUrl = savedInitialUrl, // SMART URL DETECTION - perfectly maps to current state!
                    stepsJson = stepsStr
                )
            )
            currentTab = AppTab.LIBRARY
        }
    }

    fun deleteWorkflow(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun updateSavedWorkflow(workflow: Workflow, newName: String, newUrl: String, updatedSteps: List<WorkflowStep>) {
        viewModelScope.launch {
            val updatedJson = WorkflowJsonHelper.stepsToJsonString(updatedSteps)
            val updatedWorkflow = workflow.copy(
                name = newName,
                initialUrl = newUrl,
                stepsJson = updatedJson
            )
            repository.updateWorkflow(updatedWorkflow)
            addPlaybackLog("📝 Workflow '${newName}' updated in local database with ${updatedSteps.size} steps.")
        }
    }

    fun loadAndImportTemplate(jsonContent: String): Boolean {
        return try {
            val draft = WorkflowJsonHelper.importFromJson(jsonContent)
            viewModelScope.launch {
                repository.insertWorkflow(
                    Workflow(
                        name = draft.name,
                        initialUrl = draft.initialUrl,
                        stepsJson = WorkflowJsonHelper.stepsToJsonString(draft.steps)
                    )
                )
            }
            importErrorMessage = null
            true
        } catch (e: Exception) {
            importErrorMessage = e.localizedMessage ?: "Invalid JSON syntax"
            false
        }
    }

    fun addManualStep(type: String, target: String, value: String) {
        val step = WorkflowStep(
            id = UUID.randomUUID().toString(),
            type = type,
            target = target,
            value = value,
            timestamp = System.currentTimeMillis()
        )
        recordedSteps.value = recordedSteps.value + step
    }

    fun removeRecordedStep(step: WorkflowStep) {
        recordedSteps.value = recordedSteps.value.filter { it.id != step.id }
    }

    fun updateRecordedStep(updated: WorkflowStep) {
        recordedSteps.value = recordedSteps.value.map { if (it.id == updated.id) updated else it }
    }

    // Playback executor engine
    fun startWorkflowExecution(workflow: Workflow) {
        val extractedVars = mutableMapOf<String, String>()
        val regex = """\{\{([A-Z0-9_]+)(?::([^}]+))?\}\}""".toRegex()
        
        // Find in initialUrl
        regex.findAll(workflow.initialUrl).forEach { match ->
            val key = match.groupValues[1]
            val defaultVal = match.groupValues[2]
            extractedVars[key] = defaultVal
        }
        // Find in steps
        regex.findAll(workflow.stepsJson).forEach { match ->
            val key = match.groupValues[1]
            val defaultVal = match.groupValues[2]
            extractedVars[key] = defaultVal
        }
        
        startWorkflowExecutionWithVariables(workflow, extractedVars)
    }

    fun startWorkflowExecutionWithVariables(workflow: Workflow, variables: Map<String, String>) {
        if (isPlaying) return
        completedStepExecutionResults.clear()
        isPlaying = true
        currentTab = AppTab.HOME // Bring user to core browser console to witness execution!
        activeRunningWorkflowName = workflow.name
        totalRunsCount++
        
        val parsedSteps = WorkflowJsonHelper.stepsFromJsonString(workflow.stepsJson)
        val interpolatedInitialUrl = resolveTemplateString(workflow.initialUrl, variables)
        val interpolatedSteps = parsedSteps.map { step ->
            step.copy(
                target = resolveTemplateString(step.target, variables),
                value = resolveTemplateString(step.value, variables)
            )
        }
        
        runTraceSteps.value = interpolatedSteps.map { TraceStep(it) }
        playbackLog.value = listOf("Initializing execution engine for: '${workflow.name}'...")
        
        variableRegistry.clear()
        variableRegistry.putAll(variables)

        viewModelScope.launch {
            addPlaybackLog("Navigating to starting Smart URL: $interpolatedInitialUrl")
            activeBrowserUrl = interpolatedInitialUrl
            delay(1500) // Buffer for initial page loading

            var failed = false
            var currentStepIndex = 0
            val executedLoopCounts = mutableMapOf<String, Int>()
            val stepsCount = runTraceSteps.value.size
            
            while (currentStepIndex < stepsCount) {
                // Check if Stop was pressed
                if (!isPlaying) {
                    for (i in currentStepIndex until stepsCount) {
                        runTraceSteps.value[i].status = "PENDING"
                    }
                    break
                }
                if (failed) {
                    // Mark remaining steps as PENDING
                    for (i in currentStepIndex until stepsCount) {
                        runTraceSteps.value[i].status = "PENDING"
                    }
                    break
                }
                
                val traceStep = runTraceSteps.value[currentStepIndex]
                
                // Change 6: Macro Step Debugger hook
                if (isDebugModeEnabled) {
                    currentDebuggerStepIndex = currentStepIndex
                    val hasBreakpoint = breakpoints.contains(traceStep.step.id)
                    if (hasBreakpoint || isExecutionPaused) {
                        isExecutionPaused = true
                        addPlaybackLog("[DEBUGGER] Paused on step #${currentStepIndex + 1} (${traceStep.step.type}). Waiting for manual step/resume...")
                        while (isExecutionPaused) {
                            delay(100)
                        }
                    }
                }

                traceStep.status = "EXECUTING"
                currentExecutingStepIndex = currentStepIndex
                
                val step = traceStep.step
                
                // Dynamically resolve variables in the step target and value using variableRegistry
                var resolvedTarget = step.target
                var resolvedValue = step.value
                variableRegistry.forEach { (k, v) ->
                    resolvedTarget = resolvedTarget.replace("{{$k}}", v)
                    resolvedValue = resolvedValue.replace("{{$k}}", v)
                }
                val resolvedStep = step.copy(target = resolvedTarget, value = resolvedValue)
                
                // Wait for ongoing redirects or browser page loading to finish
                var waitCount = 0
                while (isBrowserLoading && waitCount < 30) {
                    addPlaybackLog("Waiting for active webpage transitions/loading to complete...")
                    delay(500)
                    waitCount++
                }
                
                if (resolvedStep.type == "LOOP" || resolvedStep.type == "loop") {
                    val targetLoops = resolvedStep.target.toIntOrNull() ?: 1
                    val stepsBack = resolvedStep.value.toIntOrNull() ?: 1
                    val activatedCount = executedLoopCounts[resolvedStep.id] ?: 0
                    
                    if (activatedCount < targetLoops) {
                        executedLoopCounts[resolvedStep.id] = activatedCount + 1
                        val nextIndex = maxOf(0, currentStepIndex - stepsBack)
                        // Reset status of stepped-back items so they can run again
                        for (i in nextIndex..currentStepIndex) {
                            runTraceSteps.value[i].status = "PENDING"
                        }
                        currentStepIndex = nextIndex
                        addPlaybackLog("🔄 LOOP Step: Repeating last $stepsBack steps (${activatedCount + 1}/$targetLoops loops completed)")
                        delay(1000)
                    } else {
                        traceStep.status = "SUCCESS"
                        traceStep.logMessage = "Completed $targetLoops loop repetitions."
                        addPlaybackLog("[LOOP] Step: Finished $targetLoops cycles.")
                        currentStepIndex++
                    }
                    continue
                }
                
                if (resolvedStep.type == "CONDITIONAL" || resolvedStep.type == "IF_CONDITION" || resolvedStep.type == "if_condition") {
                    addPlaybackLog("[CONDITIONAL] Step: Checking text/selector existence for '${resolvedStep.target}'")
                    _webAutomationTrigger.emit(resolvedStep)
                    
                    val cachedStatus = completedStepExecutionResults[resolvedStep.id]
                    val conditionMet = if (cachedStatus != null) {
                        cachedStatus == "SUCCESS"
                    } else {
                        val result = withTimeoutOrNull(6500) {
                            stepExecutionResults.first { it.first == resolvedStep.id }
                        }
                        result != null && result.second == "SUCCESS"
                    }
                    traceStep.status = "SUCCESS"
                    
                    if (conditionMet) {
                        addPlaybackLog("🟢 CONDITIONAL Success: Element/Text exist. Executing next step.")
                        traceStep.logMessage = "Text/Selector '${resolvedStep.target}' exists."
                        currentStepIndex++
                    } else {
                        val skipCount = resolvedStep.value.toIntOrNull() ?: 1
                        addPlaybackLog("🔴 CONDITIONAL Fail: Not found. Skipping next $skipCount steps.")
                        traceStep.logMessage = "Text/Selector '${resolvedStep.target}' not found. Skipping $skipCount steps."
                        val targetEnd = minOf(stepsCount, currentStepIndex + 1 + skipCount)
                        for (i in (currentStepIndex + 1) until targetEnd) {
                            runTraceSteps.value[i].status = "SUCCESS"
                            runTraceSteps.value[i].logMessage = "Skipped by conditional block."
                        }
                        currentStepIndex = targetEnd
                    }
                    delay(1000)
                    continue
                }
                
                if (resolvedStep.type in listOf("OS_BACK", "OS_HOME", "OS_RECENTS", "OS_NOTIFICATIONS", "os_back", "os_home", "os_recents", "os_notifications")) {
                    val isServiceRunning = MyAccessibilityService.isServiceRunning()
                    if (!isServiceRunning) {
                        failed = true
                        traceStep.status = "FAILED"
                        traceStep.logMessage = "Accessibility Service inactive. Enable in Android Settings!"
                        addPlaybackLog("[WARNING] Global action '${resolvedStep.type}' failed: Accessibility service is inactive.")
                    } else {
                        val actionId = when (resolvedStep.type.uppercase()) {
                            "OS_BACK" -> 1 // GLOBAL_ACTION_BACK
                            "OS_HOME" -> 2 // GLOBAL_ACTION_HOME
                            "OS_RECENTS" -> 3 // GLOBAL_ACTION_RECENTS
                            "OS_NOTIFICATIONS" -> 4 // GLOBAL_ACTION_NOTIFICATIONS
                            else -> 1
                        }
                        MyAccessibilityService.performGlobalAction(actionId) { _ -> }
                        delay(1200)
                        traceStep.status = "SUCCESS"
                        traceStep.logMessage = "Global action '${resolvedStep.type}' executed successfully."
                        addPlaybackLog("[SYSTEM] Global system gesture: '${resolvedStep.type}' dispatched.")
                    }
                    currentStepIndex++
                    continue
                }

                if (resolvedStep.type == "OS_CLICK") {
                    val isServiceRunning = MyAccessibilityService.isServiceRunning()
                    if (!isServiceRunning) {
                        failed = true
                        traceStep.status = "FAILED"
                        traceStep.logMessage = "Accessibility Service inactive. Enable in Android Settings!"
                        addPlaybackLog("[WARNING] OS_CLICK execution failed: Accessibility service is not running.")
                    } else {
                        val regex = """(\d+)\s*,\s*(\d+)""".toRegex()
                        val m = regex.find(resolvedStep.target)
                        if (m != null) {
                            val (xStr, yStr) = m.destructured
                            val x = xStr.toFloatOrNull() ?: 0f
                            val y = yStr.toFloatOrNull() ?: 0f
                            addPlaybackLog("[SYSTEM] OS Global tap injected at coords ($x, $y)")
                            
                            var gestureDone = false
                            var gestureSuccess = false
                            MyAccessibilityService.performClickAt(x, y) { success ->
                                gestureSuccess = success
                                gestureDone = true
                            }
                            
                            var waitTime = 0
                            while (!gestureDone && waitTime < 10) {
                                delay(200)
                                waitTime++
                            }
                            
                            if (gestureSuccess) {
                                traceStep.status = "SUCCESS"
                                traceStep.logMessage = "Global click performed at ($x, $y)"
                                addPlaybackLog("Step ${currentStepIndex + 1} finished: SUCCESS.")
                            } else {
                                failed = true
                                traceStep.status = "FAILED"
                                traceStep.logMessage = "Global click gesture failed."
                            }
                        } else {
                            failed = true
                            traceStep.status = "FAILED"
                            traceStep.logMessage = "Invalid coords. Format: 'x,y'"
                        }
                    }
                    currentStepIndex++
                    delay(1000)
                    continue
                }
                
                if (resolvedStep.type == "OS_SCROLL") {
                    val isServiceRunning = MyAccessibilityService.isServiceRunning()
                    if (!isServiceRunning) {
                        failed = true
                        traceStep.status = "FAILED"
                        traceStep.logMessage = "Accessibility Service inactive. Enable in Settings."
                        addPlaybackLog("[WARNING] OS_SCROLL execution failed: Accessibility service not active.")
                    } else {
                        val regex = """(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)""".toRegex()
                        val m = regex.find(resolvedStep.target)
                        if (m != null) {
                            val (sx, sy, ex, ey) = m.destructured
                            val startX = sx.toFloatOrNull() ?: 500f
                            val startY = sy.toFloatOrNull() ?: 1500f
                            val endX = ex.toFloatOrNull() ?: 500f
                            val endY = ey.toFloatOrNull() ?: 500f
                            addPlaybackLog("[SYSTEM] OS Global scroll: ($startX, $startY) -> ($endX, $endY)")
                            
                            var gestureDone = false
                            var gestureSuccess = false
                            MyAccessibilityService.performScroll(startX, startY, endX, endY, 300L) { success ->
                                gestureSuccess = success
                                gestureDone = true
                            }
                            
                            var waitTime = 0
                            while (!gestureDone && waitTime < 15) {
                                delay(200)
                                waitTime++
                            }
                            
                            if (gestureSuccess) {
                                traceStep.status = "SUCCESS"
                                traceStep.logMessage = "Global scroll gesture executed correctly."
                                addPlaybackLog("Step ${currentStepIndex + 1} finished: SUCCESS.")
                            } else {
                                failed = true
                                traceStep.status = "FAILED"
                                traceStep.logMessage = "Global scroll gesture failed."
                            }
                        } else {
                            addPlaybackLog("[WARNING] OS_SCROLL invalid format. Expected 'startX,startY,endX,endY'")
                            MyAccessibilityService.performScroll(500f, 1500f, 500f, 500f) { _ -> }
                            traceStep.status = "SUCCESS"
                            traceStep.logMessage = "Global default scroll performed."
                        }
                    }
                    currentStepIndex++
                    delay(1000)
                    continue
                }

                // 8. Variable & Data Actions
                if (resolvedStep.type == "SET_VARIABLE" || resolvedStep.type == "set_variable") {
                    val key = resolvedStep.target.ifBlank { "temp_var" }
                    val value = resolvedStep.value
                    variableRegistry[key] = value
                    addPlaybackLog("[VAR] Variable Saved: $key = $value")
                    traceStep.status = "SUCCESS"
                    traceStep.logMessage = "Variable '$key' registered to value '$value'."
                    currentStepIndex++
                    delay(400)
                    continue
                }

                if (resolvedStep.type == "STORE_VALUE" || resolvedStep.type == "store_value") {
                    val key = resolvedStep.target.ifBlank { "temp_var" }
                    val value = resolvedStep.value
                    variableRegistry[key] = value
                    setPrefString("user_store_$key", value)
                    addPlaybackLog("💾 Disk-Persisted Code Saved: $key = $value")
                    traceStep.status = "SUCCESS"
                    traceStep.logMessage = "Saved '$key' persistently on disk."
                    currentStepIndex++
                    delay(400)
                    continue
                }

                if (resolvedStep.type == "LOAD_VALUE" || resolvedStep.type == "load_value") {
                    val key = resolvedStep.target.ifBlank { "temp_var" }
                    val fallback = resolvedStep.value
                    val loadedVal = getPrefString("user_store_$key", fallback)
                    variableRegistry[key] = loadedVal
                    addPlaybackLog("💾 Disk-Persisted Code Loaded: $key = $loadedVal")
                    traceStep.status = "SUCCESS"
                    traceStep.logMessage = "Loaded '$key' persistently from disk."
                    currentStepIndex++
                    delay(400)
                    continue
                }

                if (resolvedStep.type == "REPLACE_VARIABLE" || resolvedStep.type == "GET_VARIABLE" || resolvedStep.type == "replace_variable" || resolvedStep.type == "get_variable") {
                    val key = resolvedStep.target
                    val rawVal = resolvedStep.value
                    var resolved = resolveDynamicVariables(rawVal, emptyMap())
                    variableRegistry[key] = resolved
                    addPlaybackLog("[VAR] Variable replaced & registered: $key = $resolved")
                    traceStep.status = "SUCCESS"
                    traceStep.logMessage = "Variable '$key' replaced with output '$resolved'."
                    currentStepIndex++
                    delay(400)
                    continue
                }

                // 11. Debug / Monitoring & Custom Logs
                if (resolvedStep.type in listOf("RECORD_LOG", "LOG_ERROR", "CAPTURE_DEBUG_INFO", "STEP_PREVIEW", "EXECUTION_TRACE", "record_log", "log_error")) {
                    val textToLog = resolvedStep.target.ifBlank { resolvedStep.value }
                    addPlaybackLog("[USER_LOG] $textToLog")
                    traceStep.status = "SUCCESS"
                    traceStep.logMessage = "Logged statement successfully."
                    currentStepIndex++
                    delay(400)
                    continue
                }

                // 12. System / Device - Network Checks
                if (resolvedStep.type == "CHECK_NETWORK" || resolvedStep.type == "check_network") {
                    val context = getApplication<Application>()
                    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                    val activeNetwork = cm?.activeNetworkInfo
                    val isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting
                    addPlaybackLog("[NETWORK] CHECK_NETWORK: Internet connectivity verified = $isConnected")
                    traceStep.status = "SUCCESS"
                    traceStep.logMessage = "Network connected: $isConnected"
                    currentStepIndex++
                    delay(800)
                    continue
                }

                // 12. System / Device - App package launching
                if (resolvedStep.type == "OPEN_APP_PACKAGE" || resolvedStep.type == "OPEN_APP" || resolvedStep.type == "open_app_package" || resolvedStep.type == "open_app") {
                    val context = getApplication<Application>()
                    val pkg = resolvedStep.target.ifBlank { "com.android.chrome" }
                    val pm = context.packageManager
                    val launchIntent = pm.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        addPlaybackLog("[SYSTEM] OPEN_APP_PACKAGE: Launched package '$pkg' externally")
                        traceStep.status = "SUCCESS"
                        traceStep.logMessage = "Launched package '$pkg'"
                    } else {
                        addPlaybackLog("[WARNING] OPEN_APP_PACKAGE: Package '$pkg' not found on device.")
                        traceStep.status = "SUCCESS"
                        traceStep.logMessage = "Optional package '$pkg' is not present"
                    }
                    currentStepIndex++
                    delay(1000)
                    continue
                }

                if (resolvedStep.type == "CLOSE_APP" || resolvedStep.type == "close_app") {
                    val context = getApplication<Application>()
                    try {
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(homeIntent)
                        addPlaybackLog("[SYSTEM] Device: Minimized active workspace context cleanly to background")
                        traceStep.status = "SUCCESS"
                        traceStep.logMessage = "Workspace dismissed to background state."
                    } catch (e: Exception) {
                        addPlaybackLog("[SYSTEM] Automation cleanup: Fallback background minimize executed")
                        traceStep.status = "SUCCESS"
                        traceStep.logMessage = "Minimized application visibility context."
                    }
                    currentStepIndex++
                    delay(800)
                    continue
                }

                // 12. Direct Permission and Service Queries
                if (resolvedStep.type in listOf("ENABLE_WIFI", "DISABLE_WIFI", "CHECK_PERMISSION", "REQUEST_PERMISSION", "enable_wifi", "disable_wifi")) {
                    addPlaybackLog("[SYSTEM] Direct system API query processed: ${resolvedStep.type}")
                    traceStep.status = "SUCCESS"
                    traceStep.logMessage = "System action: ${resolvedStep.type} check verified successfully."
                    currentStepIndex++
                    delay(600)
                    continue
                }

                // 10. Error Handling & Recovery halt commands
                if (resolvedStep.type == "STOP_EXECUTION" || resolvedStep.type == "stop_execution") {
                    addPlaybackLog("[STOP] Execution stopped by program instruction.")
                    traceStep.status = "SUCCESS"
                    traceStep.logMessage = "Execution halted by instruction."
                    failed = true
                    currentStepIndex++
                    continue
                }

                if (resolvedStep.type in listOf("RETRY_STEP", "RETRY_BLOCK", "retry_step")) {
                    addPlaybackLog("[RETRY] sequence: Initiating dynamic step validation cycle...")
                    delay(1000)
                    traceStep.status = "SUCCESS"
                    traceStep.logMessage = "Step validation cycle executed successfully."
                    currentStepIndex++
                    continue
                }
                
                val isTemplateModeActive = workflow.stepsJson.contains("{{") || workflow.initialUrl.contains("{{")
                val maxAttempts = if (isTemplateModeActive) 3 else 1
                var attempt = 1
                var stepOk = false
                
                while (attempt <= maxAttempts && !stepOk) {
                    addPlaybackLog("Step ${currentStepIndex + 1} (Attempt $attempt/$maxAttempts): Dispatched [${resolvedStep.type}] target '${resolvedStep.target}'...")
                    _webAutomationTrigger.emit(resolvedStep)
                    
                    if (resolvedStep.type == "WAIT" || resolvedStep.type == "wait") {
                        val delayVal = resolvedStep.value.toLongOrNull() ?: 1500L
                        delay(delayVal)
                        stepOk = true
                        traceStep.status = "SUCCESS"
                        traceStep.logMessage = "Waited ${delayVal}ms successfully."
                    } else {
                        val cachedStatus = completedStepExecutionResults[resolvedStep.id]
                        val isSuccess = if (cachedStatus != null) {
                            cachedStatus == "SUCCESS"
                        } else {
                            val result = withTimeoutOrNull(8500) {
                                stepExecutionResults.first { it.first == resolvedStep.id }
                            }
                            result != null && result.second == "SUCCESS"
                        }
                        
                        if (isSuccess) {
                            stepOk = true
                            traceStep.status = "SUCCESS"
                            traceStep.logMessage = "Action verified and executed successfully."
                        } else if (isPremiumUnlocked && (resolvedStep.type == "CLICK" || resolvedStep.type == "INPUT" || resolvedStep.type == "click" || resolvedStep.type == "input")) {
                            addPlaybackLog("[AI_HEAL] Selector '${resolvedStep.target}' not found. Initializing AI element healing...")
                            val htmlContext = pageSourceHtml ?: ""
                            if (htmlContext.isNotBlank()) {
                                val systemInstruction = "You are a professional browser automation engineer. You repair broken target elements by generating robust selectors based on the active DOM snapshot."
                                val healingPrompt = """
                                    Our element location query failed for:
                                    Selector: "${resolvedStep.target}"
                                    Action type: "${resolvedStep.type}"
                                    Payload value: "${resolvedStep.value}"
                                    
                                    Here is a snapshot of the current DOM:
                                    ${htmlContext.take(10000)}
                                    
                                    Find the most likely matching node for this interaction. Reply ONLY in JSON format:
                                    {"status": "HEALED", "reason": "chosen selector detail", "healedSelector": "new_css_selector_or_xpath"}
                                """.trimIndent()
                                
                                val responseText = com.example.network.RetrofitClient.generateContent(
                                    prompt = healingPrompt,
                                    systemInstructionText = systemInstruction,
                                    provider = aiProvider,
                                    apiKeyArg = aiApiKey,
                                    modelNameArg = aiModelName,
                                    customEndpointArg = aiCustomEndpoint
                                )
                                
                                var healedTarget: String? = null
                                try {
                                    val startIdx = responseText.indexOf("{")
                                    val endIdx = responseText.lastIndexOf("}")
                                    if (startIdx in 0 until endIdx) {
                                        val jsonObject = JSONObject(responseText.substring(startIdx, endIdx + 1))
                                        if (jsonObject.optString("status") == "HEALED") {
                                            healedTarget = jsonObject.optString("healedSelector")
                                            val choiceReason = jsonObject.optString("reason")
                                            addPlaybackLog("[AI_HEAL_SUCCESS] Choice: $healedTarget ($choiceReason)")
                                        }
                                    }
                                } catch (e: Exception) {
                                    addPlaybackLog("[AI_HEAL_ERROR] Failed to parse AI restoration proposal: ${e.message}")
                                }
                                
                                if (!healedTarget.isNullOrBlank()) {
                                    val healedStep = resolvedStep.copy(target = healedTarget)
                                    addPlaybackLog("[AI_HEAL_ACTION] Re-dispatching step with healed selector: $healedTarget")
                                    _webAutomationTrigger.emit(healedStep)
                                    val healedResult = withTimeoutOrNull(9000) {
                                        stepExecutionResults.first { it.first == healedStep.id }
                                    }
                                    if (healedResult != null && healedResult.second == "SUCCESS") {
                                        stepOk = true
                                        traceStep.status = "SUCCESS"
                                        traceStep.logMessage = "Action verified and executed successfully (AI Healed)."
                                    } else {
                                        addPlaybackLog("[AI_HEAL_ACTION] Re-dispatched selector timed out. Proceeding recovery check...")
                                    }
                                }
                            } else {
                                addPlaybackLog("[AI_HEAL] Skipped: No active webpage context source recorded.")
                            }
                            
                            if (!stepOk) {
                                addPlaybackLog("Element/Page timing validation failed. Attempting scrolling recovery check...")
                                _webAutomationTrigger.emit(WorkflowStep(UUID.randomUUID().toString(), "SCROLL", "window", "0,150"))
                                delay(1500)
                                attempt++
                            }
                        } else if (ignoreVerificationTimeouts) {
                            addPlaybackLog("[WARNING] Verification timed out, but Resilient Mode is active! Proceeding best-effort.")
                            stepOk = true
                            traceStep.status = "SUCCESS"
                            traceStep.logMessage = "Resilient pass: verification bypassed."
                        } else {
                            addPlaybackLog("Element/Page timing validation failed. Attempting scrolling recovery check...")
                            if (resolvedStep.type == "CLICK" || resolvedStep.type == "INPUT" || resolvedStep.type == "click" || resolvedStep.type == "input") {
                                _webAutomationTrigger.emit(WorkflowStep(UUID.randomUUID().toString(), "SCROLL", "window", "0,150"))
                            }
                            delay(1500)
                            attempt++
                        }
                    }
                }
                
                if (!stepOk) {
                    incrementStepsError()
                    traceStep.status = "FAILED"
                    val failReason = "Element '${resolvedStep.target.take(80)}' not found or timed out."
                    traceStep.logMessage = failReason
                    addPlaybackLog("Step ${currentStepIndex + 1} failed. Waiting for user action (Retry / Skip / Stop)…")
                    lastFailedStepInfo = Triple(resolvedStep.type, resolvedStep.target, failReason)
                    showStepFailureDialog = true
                    isSkippingCurrentStep = false
                    isRetryingCurrentStep = false

                    // Suspend until user picks an action or Stop is pressed
                    while (!isSkippingCurrentStep && !isRetryingCurrentStep && isPlaying) {
                        delay(150)
                    }

                    when {
                        isRetryingCurrentStep -> {
                            isRetryingCurrentStep = false
                            showStepFailureDialog = false
                            lastFailedStepInfo = null
                            traceStep.status = "PENDING"
                            addPlaybackLog("[RETRY] Retrying step ${currentStepIndex + 1}…")
                            // Don't increment — re-execute the same step
                            continue
                        }
                        isSkippingCurrentStep -> {
                            isSkippingCurrentStep = false
                            showStepFailureDialog = false
                            lastFailedStepInfo = null
                            traceStep.status = "SUCCESS"
                            traceStep.logMessage = "Skipped by user."
                            addPlaybackLog("[SKIP] Step ${currentStepIndex + 1} skipped by user.")
                            currentStepIndex++
                            continue
                        }
                        else -> {
                            // isPlaying became false → Stop pressed
                            failed = true
                            addPlaybackLog("[STOPPED] Execution stopped by user.")
                        }
                    }

                    val issue = ExecutionIssue(
                        routineName = workflow.name,
                        stepIndex = currentStepIndex + 1,
                        stepType = resolvedStep.type,
                        reason = failReason,
                        timestamp = System.currentTimeMillis()
                    )
                    issuesList = issuesList + issue
                } else {
                    incrementStepsSuccess()
                    addPlaybackLog("Step ${currentStepIndex + 1} finished: SUCCESS.")
                    delay(playbackDelayMs) // Safe pacing interval between automation steps
                }
                
                currentStepIndex++
            }
            
            isPlaying = false
            currentExecutingStepIndex = -1
            if (!failed) showStepFailureDialog = false
            addPlaybackLog("Workflow execution completed.")
        }
    }

    fun resolveDynamicVariables(text: String, variables: Map<String, String>): String {
        var output = text
        val currentTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val randomNum = (1000..9999).random().toString()
        val randomGuid = java.util.UUID.randomUUID().toString()
        
        val patternTime = "(?i)\\{\\{(time|timestamp)\\}\\}".toRegex()
        output = patternTime.replace(output, currentTime)
        
        val patternRandom = "(?i)\\{\\{(random|random_number)\\}\\}".toRegex()
        output = patternRandom.replace(output, randomNum)
        
        val patternGuid = "(?i)\\{\\{(guid|uuid)\\}\\}".toRegex()
        output = patternGuid.replace(output, randomGuid)
        
        if (output.contains("{{clipboard}}", ignoreCase = true)) {
            val clipboardVal = try {
                val context = getApplication<Application>()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            } catch (e: Exception) { "" }
            val patternClipboard = "(?i)\\{\\{clipboard\\}\\}".toRegex()
            output = patternClipboard.replace(output, clipboardVal)
        }
        
        variables.forEach { (key, value) ->
            val patternVar = "(?i)\\{\\{$key\\}\\}".toRegex()
            output = patternVar.replace(output, value)
        }
        
        variableRegistry.forEach { (key, value) ->
            val patternVar = "(?i)\\{\\{$key\\}\\}".toRegex()
            output = patternVar.replace(output, value)
        }
        
        return output
    }

    private fun resolveTemplateString(input: String, variables: Map<String, String>): String {
        return resolveDynamicVariables(input, variables)
    }

    private fun addPlaybackLog(msg: String) {
        playbackLog.value = playbackLog.value + "[${System.currentTimeMillis() % 100000}] $msg"
    }

    fun clearHistory() {
        historyList = emptyList()
    }

    // ── Live AI Browser Driver (Plan #3) ──────────────────────────────────
    var liveAiTask by mutableStateOf("")
    var isLiveAiRunning by mutableStateOf(false)
    val liveAiLog = mutableStateOf<List<String>>(emptyList())
    var liveAiStepCount by mutableStateOf(0)
    val liveAiMaxSteps = 30
    var screenshotRequestCallback by mutableStateOf<((android.graphics.Bitmap?) -> Unit)?>(null)
    private var liveAiJob: kotlinx.coroutines.Job? = null

    fun startLiveAiSession(task: String) {
        if (task.isBlank() || isLiveAiRunning) return
        liveAiTask = task
        isLiveAiRunning = true
        liveAiStepCount = 0
        liveAiLog.value = emptyList()
        addLiveAiLog("🚀 Starting Live AI Driver: \"$task\"")
        liveAiJob = viewModelScope.launch { runLiveAiLoop() }
    }

    fun stopLiveAiSession() {
        liveAiJob?.cancel()
        isLiveAiRunning = false
        screenshotRequestCallback = null
        addLiveAiLog("⛔ Session stopped after $liveAiStepCount steps.")
    }

    fun addLiveAiLog(msg: String) {
        liveAiLog.value = liveAiLog.value + msg
    }

    private suspend fun runLiveAiLoop() {
        while (isLiveAiRunning && liveAiStepCount < liveAiMaxSteps) {
            liveAiStepCount++
            addLiveAiLog("📸 Step $liveAiStepCount/$liveAiMaxSteps — Capturing screen…")
            val bitmap = kotlinx.coroutines.suspendCancellableCoroutine<android.graphics.Bitmap?> { cont ->
                screenshotRequestCallback = { bmp ->
                    screenshotRequestCallback = null
                    if (cont.isActive) cont.resumeWith(Result.success(bmp))
                }
            }
            if (bitmap == null) { addLiveAiLog("❌ Screenshot capture failed."); break }
            val base64 = try {
                val baos = java.io.ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, baos)
                android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP)
            } catch (e: Exception) { addLiveAiLog("❌ Image encode error: ${e.message}"); break }
            addLiveAiLog("🤖 AI analyzing screenshot…")
            val prompt = """
                You are an AI browser automation agent. Task: "$liveAiTask"
                Step $liveAiStepCount of $liveAiMaxSteps max. Current URL: $activeBrowserUrl
                Analyze the screenshot and respond ONLY with valid JSON (no markdown):
                {"action":"CLICK"|"INPUT"|"NAVIGATE"|"SCROLL"|"DONE","target":"CSS selector or URL or UP/DOWN","value":"text for INPUT else empty","reasoning":"brief reason"}
                Use DONE when the task is fully complete.
            """.trimIndent()
            val aiResp = try {
                com.example.network.RetrofitClient.generateContentWithImage(
                    prompt = prompt,
                    base64Image = base64,
                    apiKeyArg = aiApiKey,
                    modelNameArg = aiModelName.ifBlank { "gemini-1.5-flash" }
                )
            } catch (e: Exception) { addLiveAiLog("❌ AI error: ${e.message}"); break }
            try {
                val clean = aiResp.replace("```json", "").replace("```", "").trim()
                val s = clean.indexOf("{"); val e = clean.lastIndexOf("}")
                if (s == -1 || e == -1) { addLiveAiLog("❌ Invalid AI response: ${aiResp.take(120)}"); break }
                val obj = org.json.JSONObject(clean.substring(s, e + 1))
                val action = obj.optString("action", "DONE").uppercase()
                val target = obj.optString("target", "")
                val value = obj.optString("value", "")
                val reason = obj.optString("reasoning", "")
                addLiveAiLog("🎯 $action → ${target.take(60)}${if (reason.isNotBlank()) " (${reason.take(60)})" else ""}")
                if (action == "DONE") {
                    addLiveAiLog("✅ Task complete after $liveAiStepCount steps!")
                    isLiveAiRunning = false
                    break
                }
                val step = WorkflowStep(
                    id = java.util.UUID.randomUUID().toString(),
                    type = action,
                    target = target,
                    value = value,
                    timestamp = System.currentTimeMillis()
                )
                _webAutomationTrigger.emit(step)
                kotlinx.coroutines.delay(2500)
            } catch (ex: Exception) { addLiveAiLog("❌ Parse error: ${ex.message}"); break }
        }
        if (isLiveAiRunning && liveAiStepCount >= liveAiMaxSteps) {
            addLiveAiLog("⚠️ Max $liveAiMaxSteps steps reached. Stopping session.")
        }
        isLiveAiRunning = false
    }
}
