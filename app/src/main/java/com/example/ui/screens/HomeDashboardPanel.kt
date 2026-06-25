package com.example.ui.screens

import android.webkit.WebView
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppTab
import com.example.ui.AutomationViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardPanel(viewModel: AutomationViewModel, webViewRef: WebView?) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showOnboarding by remember { mutableStateOf(true) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    
    // Schedule state variables
    var selectedWorkflowId by remember { mutableStateOf<Long?>(null) }
    var inSecondsText by remember { mutableStateOf("10") }
    
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper dynamic brand logo card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Automation Dashboard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Play scripts, manage variables, track schedules, and automate processes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Dashboard logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Onboarding Tour banner
        if (showOnboarding) {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("onboarding_banner"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Tour",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Platform Guide", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                        IconButton(
                            onClick = { showOnboarding = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Close", modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val stepsList = listOf(
                        "1. Variables" to "Substituted dynamically in scripts using bracket placeholders like {{NAME}}.",
                        "2. AI Generator" to "Describe scripts in natural language and generate structured steps automatically.",
                        "3. Task Scheduler" to "Run automated scripts automatically at specified delayed intervals.",
                        "4. Service Helper" to "Enable global simulation gestures in Android settings."
                    )
                    
                    stepsList.forEach { (title, desc) ->
                        Row(modifier = Modifier.padding(vertical = 3.dp)) {
                            Text(
                                text = "• ", 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 11.sp, 
                                color = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(title, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                                Text(desc, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // --- NEW REDESIGN: QUICK FEATURE NAVIGATOR GRID ---
        Text(
            text = "Quick Actions",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Creator Screen
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.currentTab = AppTab.CREATOR }
                    .testTag("nav_quick_creator"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Creator", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Macro Creator", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("Record live interactions", fontSize = 8.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }

            // Card 2: Library Screen
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.currentTab = AppTab.LIBRARY }
                    .testTag("nav_quick_library"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = "Library", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Saved Library", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("Playbooks & databases", fontSize = 8.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }

            // Card 3: AI Diagnostics Lab
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.currentTab = AppTab.AI }
                    .testTag("nav_quick_ai"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Sandbox", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("AI Diagnostics", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("Model sandbox console", fontSize = 8.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }

        // System Telemetry Metrics row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Radial Gauge representing reliability
            Card(
                modifier = Modifier.weight(0.48f).height(125.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Macro Run Health", fontWeight = FontWeight.Medium, fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    val total = viewModel.totalExecutedStepsSuccess + viewModel.totalExecutedStepsError
                    val percentage = if (total > 0) (viewModel.totalExecutedStepsSuccess.toFloat() / total * 100).toInt() else 100
                    
                    Box(modifier = Modifier.size(52.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 5.dp.toPx()
                            val outlineCol = Color(0xFF3A3A5C).copy(alpha = 0.5f)
                            val arcCol = androidx.compose.ui.graphics.Color(0xFF00C9A7) // Mint teal accent

                            drawCircle(
                                color = outlineCol,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
                            )
                            drawArc(
                                color = arcCol,
                                startAngle = -90f,
                                sweepAngle = (percentage / 100f) * 360f,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
                            )
                        }
                        Text("$percentage%", fontWeight = FontWeight.Black, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Total: $total runs", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Simple micro statistcs
            Column(
                modifier = Modifier.weight(0.52f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp).fillMaxSize(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text("✓", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Actions Succeeded", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            Text("${viewModel.totalExecutedStepsSuccess}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp).fillMaxSize(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color(0xFFEC4899).copy(alpha = 0.12f), RoundedCornerShape(6.dp)),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text("⏱", fontWeight = FontWeight.Bold, color = Color(0xFFEC4899), fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Human Time Saved", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            val mins = String.format("%.1f m", viewModel.totalTimeSavedMinutes)
                            Text(mins, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // --- NEW HIGH-INTEGRITY EMBEDDED CONSOLE: AGENTIC AUTO-GENERATOR WIDGET ---
        Card(
            modifier = Modifier.fillMaxWidth().testTag("home_ai_generator_lab_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "AI Compiler",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Generator",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Describe an automation task below and compile live steps instantly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )

                // Quick presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val presets = listOf(
                        "Wiki Search" to "Go to wikipedia, click search input, type 'Kotlin programming language', click search button, speak first paragraph.",
                        "Google stock" to "Navigate to google.com, style search input, type 'Google stock price today', press launch search, wait 3 seconds."
                    )
                    presets.forEach { (label, promptText) ->
                        val isSelected = viewModel.aiGenPromptText == promptText
                        AssistChip(
                            onClick = { viewModel.aiGenPromptText = promptText },
                            label = { Text(label, fontSize = 9.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = viewModel.aiGenPromptText,
                    onValueChange = { viewModel.aiGenPromptText = it },
                    label = { Text("Task Description", fontSize = 10.sp) },
                    placeholder = { Text("e.g. Navigate to wikipedia and double-click search...") },
                    modifier = Modifier.fillMaxWidth().height(80.dp).testTag("ai_gen_prompt_input_home"),
                    shape = RoundedCornerShape(8.dp),
                    textStyle = TextStyle(fontSize = 11.5.sp),
                    trailingIcon = {
                        if (viewModel.aiGenPromptText.isNotEmpty()) {
                            IconButton(onClick = { viewModel.aiGenPromptText = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                )

                Button(
                    onClick = {
                        if (viewModel.aiGenPromptText.isBlank()) {
                            Toast.makeText(context, "Please enter some descriptive task instructions first.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.generateAiWorkflow(viewModel.aiGenPromptText)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("ai_compile_action_btn_home"),
                    shape = RoundedCornerShape(6.dp),
                    enabled = !viewModel.isAiGeneratingWorkflow
                ) {
                    if (viewModel.isAiGeneratingWorkflow) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 1.5.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generating...", fontSize = 11.sp)
                    } else {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Live Script", fontSize = 11.sp)
                    }
                }

                // Compile result stepping nodes inline display
                if (viewModel.aiGenWorkflowSteps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "BLUEPRINT PREVIEW",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Name: ${viewModel.aiGenWorkflowName}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Start: ${viewModel.aiGenWorkflowInitialUrl}", fontSize = 10.sp, maxLines = 1, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        viewModel.aiGenWorkflowSteps.forEachIndexed { idx, step ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${idx + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(step.type, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Text(if (step.target.isNotBlank()) step.target else "Global scope", fontWeight = FontWeight.Medium, fontSize = 10.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { viewModel.saveAiWorkflowToLibrary() },
                            modifier = Modifier.weight(1f).testTag("save_ai_workflow_btn_home"),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Save to Library", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.runAiWorkflowDirectly() },
                            modifier = Modifier.weight(1f).testTag("run_ai_workflow_btn_home"),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Run Instantly", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // --- NEW COMPONENT: WORKSPACE ENVIRONMENT VARIABLE INLINE BADGE HUD ---
        Card(
            modifier = Modifier.fillMaxWidth().testTag("dynamic_workspace_variables_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Variables", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                
                Text(
                    text = "Declare placeholders like {{NAME}} to substitute key values dynamically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )

                // Inline form to add parameter instantly
                var inlineKey by remember { mutableStateOf("") }
                var inlineValue by remember { mutableStateOf("") }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inlineKey,
                        onValueChange = { inlineKey = it },
                        placeholder = { Text("Key name", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f).height(46.dp),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 11.sp),
                        shape = RoundedCornerShape(6.dp)
                    )
                    OutlinedTextField(
                        value = inlineValue,
                        onValueChange = { inlineValue = it },
                        placeholder = { Text("Value payload", fontSize = 10.sp) },
                        modifier = Modifier.weight(1.2f).height(46.dp),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 11.sp),
                        shape = RoundedCornerShape(6.dp)
                    )
                    IconButton(
                        onClick = {
                            if (inlineKey.isNotBlank()) {
                                val currentMap = viewModel.globalVariables.toMutableMap()
                                currentMap[inlineKey.uppercase().trim()] = inlineValue
                                viewModel.saveGlobalVariables(currentMap)
                                Toast.makeText(context, "Added '${inlineKey.uppercase()}'!", Toast.LENGTH_SHORT).show()
                                inlineKey = ""
                                inlineValue = ""
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add key", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                    }
                }

                // Active List
                val variables = viewModel.globalVariables
                if (variables.isEmpty()) {
                    Text("No variables added yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.5.sp)
                } else {
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        variables.forEach { (key, value) ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.clickable {
                                    val currentMap = viewModel.globalVariables.toMutableMap()
                                    currentMap.remove(key)
                                    viewModel.saveGlobalVariables(currentMap)
                                    Toast.makeText(context, "Removed parameter '$key'", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("$key: ", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.primary)
                                    Text(value, fontSize = 9.5.sp, maxLines = 1)
                                    Icon(Icons.Default.Clear, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(10.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Scheduled Workflows Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Scheduler clock",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Scheduler",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                    Button(
                        onClick = { showScheduleDialog = true },
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("+ Plan", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                val currentPlans = viewModel.scheduledWorkflows
                if (currentPlans.isEmpty()) {
                    Text(
                        text = "No active scheduled triggers planned.",
                        fontSize = 9.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    currentPlans.forEach { plan ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column {
                                Text(plan.workflowName, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                val remainingSecs = (plan.triggerTimeMs - System.currentTimeMillis()) / 1000
                                val timeText = if (remainingSecs <= 0) {
                                    if (plan.isCompleted) "Completed" else "Executing..."
                                } else {
                                    "Launches in ${remainingSecs}s"
                                }
                                Text("Status: $timeText", fontSize = 8.5.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = {
                                    val filtered = currentPlans.filter { it.id != plan.id }
                                    viewModel.saveScheduledWorkflows(filtered)
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Delete run", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }

        // Quick Web Script Injector
        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Script catalog",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Web Scripts",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Run custom JS snippets directly in the active browser view.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.scriptSnippets.forEach { snippet ->
                        Button(
                            onClick = {
                                viewModel.activeBrowserUrl = "javascript:" + snippet.snippet
                                Toast.makeText(context, "Executing '${snippet.name}' script...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(snippet.name, fontSize = 8.5.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showScheduleDialog) {
        val workflowsList = viewModel.workflows.collectAsState().value
        Dialog(onDismissRequest = { showScheduleDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(10.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("⏰ Plan Delayed Workflow Trigger", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (workflowsList.isEmpty()) {
                        Text("No saved macro scripts in database to schedule.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showScheduleDialog = false }) { Text("Dismiss") }
                        }
                    } else {
                        Text("Select Script Recipe", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        workflowsList.forEach { wf ->
                            val isSel = selectedWorkflowId == wf.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedWorkflowId = wf.id }
                                    .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                RadioButton(selected = isSel, onClick = { selectedWorkflowId = wf.id })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(wf.name, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = inSecondsText,
                            onValueChange = { inSecondsText = it },
                            label = { Text("Launch delay in seconds") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 12.sp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showScheduleDialog = false }) { Text("Cancel") }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val delayS = inSecondsText.toLongOrNull() ?: 10L
                                    val wfId = selectedWorkflowId ?: workflowsList.firstOrNull()?.id
                                    if (wfId != null) {
                                        val targetWf = workflowsList.find { it.id == wfId }
                                        if (targetWf != null) {
                                            val list = viewModel.scheduledWorkflows.toMutableList()
                                            list.add(
                                                AutomationViewModel.ScheduledWorkflow(
                                                    workflowId = targetWf.id,
                                                    workflowName = targetWf.name,
                                                    triggerTimeMs = System.currentTimeMillis() + (delayS * 1000L)
                                                )
                                            )
                                            viewModel.saveScheduledWorkflows(list)
                                            showScheduleDialog = false
                                            Toast.makeText(context, "Delayed scheduler trigger plotted successfully!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) {
                                Text("Schedule Trigger")
                            }
                        }
                    }
                }
            }
        }
    }
}

