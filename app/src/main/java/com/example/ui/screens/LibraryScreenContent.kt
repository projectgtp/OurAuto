package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Workflow
import com.example.data.WorkflowJsonHelper
import com.example.data.WorkflowStep
import com.example.ui.AutomationViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreenContent(
    viewModel: AutomationViewModel,
    onImportClicked: () -> Unit,
    onExportRequested: (Workflow) -> Unit,
    copyToClipboard: (String, String) -> Unit
) {
    val workflows by viewModel.workflows.collectAsState()
    val context = LocalContext.current
    var activeRunTemplateWorkflow by remember { mutableStateOf<Workflow?>(null) }
    var templateVariablesMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    
    // Premium search & filtering states
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Templates", "Exact"

    var editWorkflowDialogTarget by remember { mutableStateOf<Workflow?>(null) }
    var editWorkflowName by remember { mutableStateOf("") }
    var editWorkflowUrl by remember { mutableStateOf("") }
    var editWorkflowSteps by remember { mutableStateOf<List<WorkflowStep>>(emptyList()) }

    val filteredWorkflows = remember(workflows, searchQuery, selectedFilter) {
        workflows.filter { item ->
            val matchesSearch = item.name.contains(searchQuery, ignoreCase = true) || item.initialUrl.contains(searchQuery, ignoreCase = true)
            val isTemplate = item.initialUrl.contains("{{") || item.stepsJson.contains("{{")
            val matchesFilter = when (selectedFilter) {
                "Templates" -> isTemplate
                "Exact" -> !isTemplate
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Upper Quick actions bar with premium gradient styling hint
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onImportClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .weight(1.0f)
                    .height(44.dp)
                    .testTag("import_template_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Action import",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Import Template",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = {
                    copyToClipboard("Workflow JSON Template", WorkflowJsonHelper.getExampleTemplateJson())
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .weight(1.0f)
                    .height(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Formatting specifications guide",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Copy JSON Format",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Minimal metrics row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Total Runs",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = viewModel.totalRunsCount.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search workflows...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        // Filter chips row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Templates", "Exact").forEach { filterType ->
                val isSelected = selectedFilter == filterType
                Surface(
                    onClick = { selectedFilter = filterType },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.height(32.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filterType,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Text(
            text = "Workflows (${filteredWorkflows.size})",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Workflow cards catalog
        if (workflows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "No workflows saved yet.",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Record one in the Creator tab, or import a JSON file.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (filteredWorkflows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No workflows match search queries.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("workflows_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredWorkflows) { item ->
                    val stepsList = remember(item) { WorkflowJsonHelper.stepsFromJsonString(item.stepsJson) }
                    val isTemplate = item.initialUrl.contains("{{") || item.stepsJson.contains("{{")
                    
                    // Detailed visual flow of steps (visualize chain)
                    val stepsTraceString = remember(stepsList) {
                        if (stepsList.isEmpty()) "Empty macro"
                        else stepsList.take(4).map { it.type.uppercase() }.joinToString(" ➔ ") + (if (stepsList.size > 4) " ➔ ..." else "")
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = item.name,
                                            fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        
                                        // Mode Indicator Stamp
                                        Badge(
                                            containerColor = if (isTemplate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                        ) {
                                            Text(
                                                text = if (isTemplate) "TEMPLATE" else "EXACT",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.initialUrl,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    // Steps preview chain bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Flow: $stepsTraceString",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Play Trigger Button
                                    IconButton(
                                        onClick = {
                                            if (isTemplate) {
                                                // Extract all variables
                                                val extracted = mutableMapOf<String, String>()
                                                val regex = """\{\{([A-Z0-9_]+)(?::([^}]+))?\}\}""".toRegex()
                                                
                                                regex.findAll(item.initialUrl).forEach { match ->
                                                    val key = match.groupValues[1]
                                                    val defaultVal = match.groupValues[2]
                                                    extracted[key] = defaultVal
                                                }
                                                regex.findAll(item.stepsJson).forEach { match ->
                                                    val key = match.groupValues[1]
                                                    val defaultVal = match.groupValues[2]
                                                    extracted[key] = defaultVal
                                                }
                                                
                                                templateVariablesMap = extracted
                                                activeRunTemplateWorkflow = item
                                            } else {
                                                viewModel.startWorkflowExecution(item)
                                            }
                                        }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play workflow execution",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // Edit Steps Button
                                    IconButton(
                                        onClick = {
                                            editWorkflowDialogTarget = item
                                            editWorkflowName = item.name
                                            editWorkflowUrl = item.initialUrl
                                            editWorkflowSteps = com.example.data.WorkflowJsonHelper.stepsFromJsonString(item.stepsJson)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit steps button",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Share Button
                                    IconButton(onClick = { onExportRequested(item) }) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Export recipe button",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Delete Button
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteWorkflow(item.id)
                                            Toast.makeText(context, "Deleted safely!", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete recipe button",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Diagnostic Console Telemetry Logs Section
        Text(
            text = "Error Log",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF12131A) // Monospace styling environment
        ) {
            if (viewModel.issuesList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No errors",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(viewModel.issuesList.reversed()) { issue ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "➜ [FAIL] ",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${issue.routineName}: Step #${issue.stepIndex} [${issue.stepType}]",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = issue.reason,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Comprehensive Dialog for Editing Saved Recipes and Step Actions
    if (editWorkflowDialogTarget != null) {
        val targetItem = editWorkflowDialogTarget!!
        AlertDialog(
            onDismissRequest = { editWorkflowDialogTarget = null },
            shape = RoundedCornerShape(16.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Macro Outline",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edit Recipe & Steps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Fine-tune starting web URL patterns or reorder/edit macro operations in your saved recipe.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = editWorkflowName,
                        onValueChange = { editWorkflowName = it },
                        label = { Text("Recipe/Macro Name", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 12.sp),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = editWorkflowUrl,
                        onValueChange = { editWorkflowUrl = it },
                        label = { Text("Starting URL template", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 12.sp),
                        singleLine = true
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Actions Chain (${editWorkflowSteps.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Button(
                            onClick = {
                                val newStep = WorkflowStep(
                                    id = java.util.UUID.randomUUID().toString(),
                                    type = "CLICK",
                                    target = "button",
                                    value = "",
                                    timestamp = System.currentTimeMillis()
                                )
                                editWorkflowSteps = editWorkflowSteps + newStep
                            },
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Text("+ Add Step", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    if (editWorkflowSteps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No actions in this flow.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            itemsIndexed(editWorkflowSteps) { idx, step ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                                    Text("#${idx + 1}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Action Params", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            
                                            // Action controls
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        if (idx > 0) {
                                                            val list = editWorkflowSteps.toMutableList()
                                                            val temp = list[idx]
                                                            list[idx] = list[idx - 1]
                                                            list[idx - 1] = temp
                                                            editWorkflowSteps = list
                                                        }
                                                    },
                                                    enabled = idx > 0,
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", modifier = Modifier.size(14.dp))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        if (idx < editWorkflowSteps.size - 1) {
                                                            val list = editWorkflowSteps.toMutableList()
                                                            val temp = list[idx]
                                                            list[idx] = list[idx + 1]
                                                            list[idx + 1] = temp
                                                            editWorkflowSteps = list
                                                        }
                                                    },
                                                    enabled = idx < editWorkflowSteps.size - 1,
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", modifier = Modifier.size(14.dp))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val list = editWorkflowSteps.toMutableList()
                                                        list.add(idx + 1, step.copy(id = java.util.UUID.randomUUID().toString(), timestamp = System.currentTimeMillis()))
                                                        editWorkflowSteps = list
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Refresh, contentDescription = "Duplicate Step", modifier = Modifier.size(12.dp))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        editWorkflowSteps = editWorkflowSteps.filterIndexed { i, _ -> i != idx }
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete Step", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        // TextFields editing parameters
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = step.type,
                                                onValueChange = { newVal ->
                                                    editWorkflowSteps = editWorkflowSteps.mapIndexed { i, s ->
                                                        if (i == idx) s.copy(type = newVal.uppercase()) else s
                                                    }
                                                },
                                                label = { Text("Type", fontSize = 7.sp) },
                                                modifier = Modifier.weight(1f).height(40.dp),
                                                textStyle = TextStyle(fontSize = 10.sp),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = step.target,
                                                onValueChange = { newVal ->
                                                    editWorkflowSteps = editWorkflowSteps.mapIndexed { i, s ->
                                                        if (i == idx) s.copy(target = newVal) else s
                                                    }
                                                },
                                                label = { Text("Target CSS/Uri", fontSize = 7.sp) },
                                                modifier = Modifier.weight(1.5f).height(40.dp),
                                                textStyle = TextStyle(fontSize = 10.sp),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = step.value,
                                                onValueChange = { newVal ->
                                                    editWorkflowSteps = editWorkflowSteps.mapIndexed { i, s ->
                                                        if (i == idx) s.copy(value = newVal) else s
                                                    }
                                                },
                                                label = { Text("Value/Params", fontSize = 7.sp) },
                                                modifier = Modifier.weight(1.2f).height(40.dp),
                                                textStyle = TextStyle(fontSize = 10.sp),
                                                singleLine = true
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateSavedWorkflow(targetItem, editWorkflowName, editWorkflowUrl, editWorkflowSteps)
                        editWorkflowDialogTarget = null
                        Toast.makeText(context, "Successfully updated recipe!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save Changes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editWorkflowDialogTarget = null }) {
                    Text("Cancel", fontSize = 11.sp)
                }
            }
        )
    }

    // Modal runtime variables entry dialog for Script Template Mode execution
    if (activeRunTemplateWorkflow != null) {
        val workflowToRun = activeRunTemplateWorkflow!!
        AlertDialog(
            onDismissRequest = { activeRunTemplateWorkflow = null },
            shape = RoundedCornerShape(16.dp),
            title = {
                Column {
                    Text(
                        text = "Play Reusable Template",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Define variables to compile this automation recipe: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    templateVariablesMap.keys.toList().forEach { varKey ->
                        val currentValue = templateVariablesMap[varKey] ?: ""
                        OutlinedTextField(
                            value = currentValue,
                            onValueChange = { newValue ->
                                templateVariablesMap = templateVariablesMap + (varKey to newValue)
                            },
                            label = { Text(varKey.replace("_", " "), fontSize = 12.sp) },
                            placeholder = { Text("Value for $varKey") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.startWorkflowExecutionWithVariables(workflowToRun, templateVariablesMap)
                        activeRunTemplateWorkflow = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Execute Script")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeRunTemplateWorkflow = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}


