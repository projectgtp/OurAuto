package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AutomationViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AiScreenContent(viewModel: AutomationViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val isVip = viewModel.isPremiumUnlocked
    
    // Sandbox states
    var rawHtmlInput by remember { mutableStateOf("") }
    var sandboxActionType by remember { mutableStateOf("CLICK") }
    var sandboxPromptValue by remember { mutableStateOf("") }
    var sandboxResultModel by remember { mutableStateOf("") }
    var isSandboxAnalyzing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Simple Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "AI Settings",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose your AI provider and enter your API key",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant)

        // Providers Config Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "AI Provider",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Provider", fontWeight = FontWeight.Medium, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))

                val providers = listOf("Gemini", "OpenAI", "Anthropic", "DeepSeek", "Custom")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    providers.take(3).forEach { prov ->
                        val isSel = viewModel.aiProvider == prov
                        AssistChip(
                            onClick = {
                                if (!isVip && prov != "Gemini") {
                                    Toast.makeText(context, "Provider integration requires license verification.", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.aiProvider = prov
                                    viewModel.aiModelName = when (prov) {
                                        "Gemini" -> "gemini-1.5-flash"
                                        "OpenAI" -> "gpt-4o-mini"
                                        "Anthropic" -> "claude-3-5-sonnet-20241022"
                                        "DeepSeek" -> "deepseek-chat"
                                        else -> "llama3"
                                    }
                                }
                            },
                            label = { Text(prov, fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                labelColor = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    providers.drop(3).forEach { prov ->
                        val isSel = viewModel.aiProvider == prov
                        AssistChip(
                            onClick = {
                                if (!isVip && prov != "Gemini") {
                                    Toast.makeText(context, "Provider integration requires license verification.", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.aiProvider = prov
                                    viewModel.aiModelName = when (prov) {
                                        "DeepSeek" -> "deepseek-chat"
                                        else -> "llama3"
                                    }
                                }
                            },
                            label = { Text(if (prov == "Custom") "Custom / Ollama" else prov, fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                labelColor = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = viewModel.aiModelName,
                    onValueChange = { viewModel.aiModelName = it },
                    label = { Text("Model Name", fontSize = 11.sp) },
                    placeholder = { Text("e.g. gemini-1.5-flash") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    textStyle = TextStyle(fontSize = 12.sp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (viewModel.aiProvider != "Custom") {
                    OutlinedTextField(
                        value = viewModel.aiApiKey,
                        onValueChange = { viewModel.aiApiKey = it },
                        label = {
                            val suffix = if (viewModel.aiProvider == "Gemini") " (optional - falls back to settings secret)" else ""
                            Text("API Key$suffix", fontSize = 11.sp)
                        },
                        placeholder = { Text("Enter api key credential") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        textStyle = TextStyle(fontSize = 12.sp),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                }

                if (viewModel.aiProvider == "Custom") {
                    OutlinedTextField(
                        value = viewModel.aiCustomEndpoint,
                        onValueChange = { viewModel.aiCustomEndpoint = it },
                        label = { Text("Endpoint Base URL", fontSize = 11.sp) },
                        placeholder = { Text("e.g. http://10.0.2.2:11434/v1/chat/completions") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        textStyle = TextStyle(fontSize = 12.sp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "For Ollama, use your local server URL.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Selector Sandbox Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Element Finder",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Paste page HTML and let AI find the right CSS selectors for you.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )

                OutlinedTextField(
                    value = rawHtmlInput,
                    onValueChange = { rawHtmlInput = it },
                    label = { Text("HTML Source Fragment", fontSize = 11.sp) },
                    placeholder = { Text("<button class=\"btn\">Submit</button>") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(6.dp),
                    textStyle = TextStyle(fontSize = 12.sp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sandboxActionType,
                        onValueChange = { sandboxActionType = it },
                        label = { Text("Action Type", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        textStyle = TextStyle(fontSize = 12.sp)
                    )
                    OutlinedTextField(
                        value = sandboxPromptValue,
                        onValueChange = { sandboxPromptValue = it },
                        label = { Text("Target Hint", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        textStyle = TextStyle(fontSize = 12.sp)
                    )
                }

                Button(
                    onClick = {
                        if (rawHtmlInput.isBlank()) {
                            Toast.makeText(context, "Input code is blank", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSandboxAnalyzing = true
                        sandboxResultModel = ""

                        val systemInstruction = "You are an expert browser automation engineer. You repair broken target elements by generating robust selectors."
                        val prompt = """
                            Given the context HTML chunk:
                            $rawHtmlInput
                            
                            Find or construct the single most robust CSS selector to $sandboxActionType on this node.
                            If there is target value target hint, consider it: $sandboxPromptValue
                            
                            Provide only JSON in the exact structure:
                            {"status": "HEALED", "reason": "why chosen name", "healedSelector": "new_css_selector_or_xpath"}
                        """.trimIndent()

                        val scope = kotlinx.coroutines.MainScope()
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            try {
                                val reply = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    com.example.network.RetrofitClient.generateContent(
                                        prompt = prompt,
                                        systemInstructionText = systemInstruction,
                                        provider = viewModel.aiProvider,
                                        apiKeyArg = viewModel.aiApiKey,
                                        modelNameArg = viewModel.aiModelName,
                                        customEndpointArg = viewModel.aiCustomEndpoint
                                    )
                                }
                                sandboxResultModel = reply
                            } catch (e: Exception) {
                                sandboxResultModel = "Execution error: ${e.message}"
                            } finally {
                                isSandboxAnalyzing = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    enabled = !isSandboxAnalyzing
                ) {
                    if (isSandboxAnalyzing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 1.5.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyzing...", fontSize = 12.sp)
                    } else {
                        Text("Compile Selector", fontSize = 12.sp)
                    }
                }

                if (sandboxResultModel.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Suggested Selector Outcome:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = sandboxResultModel,
                            modifier = Modifier.padding(10.dp),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Live Webpage Diagnostics Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Webpage Diagnostics",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Utilizes current active DOM buffer state captured from browser tab.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )

                val htmlSnapshot = viewModel.pageSourceHtml
                if (htmlSnapshot.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No DOM buffer captured. Navigate load some webpage in home tab.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DOM context captured (${htmlSnapshot.length} chars)",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Button(
                        onClick = {
                            viewModel.runSmartPageAnalysis()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Extract Semantic Form Selectors", fontSize = 11.sp)
                    }
                }

                if (!viewModel.aiAnalysisResult.isNullOrBlank()) {
                    Text(
                        text = "Semantic Output:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = viewModel.aiAnalysisResult ?: "",
                            modifier = Modifier.padding(10.dp),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                // --- BREAKTHROUGH: AGENTIC AUTO-GENERATOR LAB ---
                Card(
                     modifier = Modifier.fillMaxWidth().testTag("ai_generator_lab_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "AI Builder",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI Generator",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "Compile custom browser scripts automatically from natural language prompts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Quick Templates Row
                        Text(
                            text = "Quick Task Presets:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val presets = listOf(
                                "Wiki search" to "Go to wikipedia, click search input, type 'Kotlin programming language', click search button, speak first paragraph.",
                                "Google stock" to "Navigate to google.com, style search input, type 'Google stock price today', press launch search, wait 3 seconds.",
                                "HackerNews" to "Navigate to HN news page, click login option, input fake login username, wait 2000 milliseconds."
                            )
                            presets.forEach { (label, promptText) ->
                                val isSelected = viewModel.aiGenPromptText == promptText
                                AssistChip(
                                    onClick = { viewModel.aiGenPromptText = promptText },
                                    label = { Text(label, fontSize = 10.sp) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                )
                            }
                        }

                        // Prompt Input Box
                        OutlinedTextField(
                            value = viewModel.aiGenPromptText,
                            onValueChange = { viewModel.aiGenPromptText = it },
                            label = { Text("Task Description", fontSize = 11.sp) },
                            placeholder = { Text("e.g. Navigate to wikipedia, search for Kotlin language, click first link...") },
                            modifier = Modifier.fillMaxWidth().height(100.dp).testTag("ai_gen_prompt_input"),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = TextStyle(fontSize = 12.sp),
                            trailingIcon = {
                                if (viewModel.aiGenPromptText.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.aiGenPromptText = "" }) {
                                        Icon(Icons.Default.Info, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        )

                        Button(
                            onClick = {
                                if (viewModel.aiGenPromptText.isBlank()) {
                                    Toast.makeText(context, "Command sequence prompt is blank.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.generateAiWorkflow(viewModel.aiGenPromptText)
                            },
                            modifier = Modifier.fillMaxWidth().testTag("ai_compile_action_btn"),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !viewModel.isAiGeneratingWorkflow
                        ) {
                            if (viewModel.isAiGeneratingWorkflow) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generating...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Live Script", fontSize = 12.sp)
                            }
                        }

                        // AI Thought Bubbles/Process State
                        if (viewModel.isAiGeneratingWorkflow) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "🧠 AI Thought: Compiling selectors and routes...",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Error State Display
                        viewModel.aiGenErrorMessage?.let { err ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Generated Preview Sequence Node Stepper Visualizer
                        if (viewModel.aiGenWorkflowSteps.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "BLUEPRINT PREVIEW",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Card header info showing metadata
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Name: ${viewModel.aiGenWorkflowName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Start: ${viewModel.aiGenWorkflowInitialUrl}",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            // The interactive visual Node Pipeline (BETA Canvas step trail)
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                viewModel.aiGenWorkflowSteps.forEachIndexed { idx, step ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Step Left Index Badge
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${idx + 1}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        // Step action summary block
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Action Type Custom Badge Pill
                                                val (pillBg, pillFg) = when (step.type.uppercase()) {
                                                    "NAVIGATE", "OPEN_LINK", "OPEN_URL" -> Color(0xFF7C6FFF).copy(alpha = 0.15f) to Color(0xFF5A50CC)
                                                    "CLICK", "SMART_CLICK" -> Color(0xFF00C9A7).copy(alpha = 0.15f) to Color(0xFF00A08A)
                                                    "INPUT", "INPUT_TEXT" -> Color(0xFF9D8FFF).copy(alpha = 0.15f) to Color(0xFF7C6FFF)
                                                    "WAIT" -> Color(0xFFFFD166).copy(alpha = 0.15f) to Color(0xFFCC9E2E)
                                                    "SPEAK", "SAY_TEXT", "TTS" -> Color(0xFFFF6B6B).copy(alpha = 0.15f) to Color(0xFFCC4444)
                                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) to MaterialTheme.colorScheme.primary
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(pillBg)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = step.type,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = pillFg
                                                    )
                                                }

                                                Column {
                                                    Text(
                                                        text = if (step.target.isNotBlank()) step.target else "No explicit target context",
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (step.value.isNotBlank()) {
                                                        Text(
                                                            text = "Value: ${step.value}",
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Final Decision Controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.saveAiWorkflowToLibrary() },
                                    modifier = Modifier.weight(1f).testTag("save_ai_workflow_btn"),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { viewModel.runAiWorkflowDirectly() },
                                    modifier = Modifier.weight(1.05f).testTag("run_ai_workflow_btn"),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Run", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant)

        // Live AI Browser Driver Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Live AI Browser Driver", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.weight(1f))
                    if (viewModel.isLiveAiRunning) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(4.dp)) {
                            Text("${viewModel.liveAiStepCount}/${viewModel.liveAiMaxSteps} steps", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = androidx.compose.ui.Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "AI captures screenshots and drives the browser autonomously toward your goal using Gemini Vision. Max ${viewModel.liveAiMaxSteps} steps.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                var liveAiTaskText by remember { mutableStateOf("") }

                OutlinedTextField(
                    value = liveAiTaskText,
                    onValueChange = { liveAiTaskText = it },
                    label = { Text("Task for AI Driver", fontSize = 11.sp) },
                    placeholder = { Text("e.g. Search for 'Kotlin coroutines' on Google", fontSize = 10.sp) },
                    enabled = !viewModel.isLiveAiRunning,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.startLiveAiSession(liveAiTaskText) },
                        enabled = !viewModel.isLiveAiRunning && liveAiTaskText.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        if (viewModel.isLiveAiRunning) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (viewModel.isLiveAiRunning) "Running…" else "Start Driver", fontSize = 12.sp)
                    }
                    if (viewModel.isLiveAiRunning) {
                        OutlinedButton(
                            onClick = { viewModel.stopLiveAiSession() },
                            modifier = Modifier.weight(0.7f),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop", fontSize = 11.sp)
                        }
                    }
                }

                if (viewModel.liveAiLog.value.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Activity Feed", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        viewModel.liveAiLog.value.reversed().forEach { logLine ->
                            Text(
                                text = logLine,
                                fontSize = 9.5.sp,
                                color = when {
                                    logLine.startsWith("✅") -> Color(0xFF00C9A7)
                                    logLine.startsWith("❌") -> MaterialTheme.colorScheme.error
                                    logLine.startsWith("🎯") -> MaterialTheme.colorScheme.primary
                                    logLine.startsWith("⛔") || logLine.startsWith("⚠️") -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
