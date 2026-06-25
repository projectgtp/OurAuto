package com.example.ui.components

import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Workflow
import com.example.data.WorkflowJsonHelper
import com.example.data.WorkflowStep
import com.example.ui.AppTab
import com.example.ui.AutomationViewModel

@Composable
fun TabsManagementDialog(
    viewModel: AutomationViewModel,
    homeWebViewRef: WebView?,
    onDismiss: () -> Unit
) {
    var newTabUrlInput by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🌐 Active Browser Tabs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { onDismiss() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close sheet")
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp)
                    ) {
                        items(viewModel.browserTabs) { tab ->
                            val isActive = tab.id == viewModel.activeTabId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable {
                                        viewModel.selectTab(tab.id)
                                        homeWebViewRef?.loadUrl(tab.url)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Info,
                                        contentDescription = "Active info status",
                                        tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = tab.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = tab.url,
                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                // Disable closing if it's the last tab
                                if (viewModel.browserTabs.size > 1) {
                                    IconButton(
                                        onClick = {
                                            viewModel.removeTab(tab.id)
                                            homeWebViewRef?.loadUrl(viewModel.activeBrowserUrl)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Close tab icon",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    OutlinedTextField(
                        value = newTabUrlInput,
                        onValueChange = { newTabUrlInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://www.google.com") },
                        label = { Text("Open URL in new Tab") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                if (!viewModel.isPremiumUnlocked && viewModel.browserTabs.size >= 3) {
                                    onDismiss()
                                    viewModel.showPremiumUpgradeDialog = true
                                    Toast.makeText(context, "Free tier is limited to 3 active tabs.", Toast.LENGTH_LONG).show()
                                } else {
                                    val target = if (newTabUrlInput.isNotBlank()) newTabUrlInput else "https://www.google.com"
                                    val formatted = viewModel.formatSearchQuery(target)
                                    viewModel.addNewTab(formatted)
                                    homeWebViewRef?.loadUrl(formatted)
                                    newTabUrlInput = ""
                                    onDismiss()
                                }
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Open positive button", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                }
            }
    }
}

@Composable
fun DownloadsDialog(viewModel: AutomationViewModel, webViewRef: WebView?, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
                    .padding(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Downloads List",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Browser Downloads Log",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { onDismiss() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Downloads",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Review downloaded files, client APKs, raw videos, and dynamic test bundles.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    if (viewModel.downloadsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                Icon(
                                    imageVector = Icons.Default.DownloadForOffline,
                                    contentDescription = "Empty downloads log",
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    "No active or past downloads",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Trigger links, media, and raw assets in the web browser above to record download events.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(viewModel.downloadsList) { download ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                                Text(
                                                    text = download.filename,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.5.sp,
                                                    maxLines = 1,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = download.mimeType,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 9.sp
                                                )
                                            }
                                            Badge(
                                                containerColor = when (download.status) {
                                                    "SUCCESSFUL" -> Color(0xFF00C9A7)
                                                    "RUNNING" -> MaterialTheme.colorScheme.primary
                                                    "FAILED" -> MaterialTheme.colorScheme.error
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            ) {
                                                Text(
                                                    text = download.status,
                                                    color = Color.White,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Progress bar or size display
                                        if (download.status == "RUNNING" || download.status == "PENDING") {
                                            LinearProgressIndicator(
                                                progress = download.progress,
                                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.outlineVariant
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "${(download.progress * 100).toInt()}% loaded",
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (download.totalBytes > 0) {
                                                    val sizeMb = download.totalBytes.toFloat() / (1024f * 1024f)
                                                    Text(
                                                        text = String.format("%.2f MB", sizeMb),
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (download.totalBytes > 0) {
                                                    val sizeMb = download.totalBytes.toFloat() / (1024f * 1024f)
                                                    Text(
                                                        text = "Size: " + String.format("%.2f MB", sizeMb),
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                } else {
                                                    Text("Size: Unknown", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }

                                                if (download.status == "SUCCESSFUL") {
                                                    Button(
                                                        onClick = {
                                                            try {
                                                                if (download.localUri != null) {
                                                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                                                        setDataAndType(Uri.parse(download.localUri), download.mimeType)
                                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                                    }
                                                                    context.startActivity(intent)
                                                                } else {
                                                                    viewModel.triggerOpenDownloadedFile(context, download)
                                                                }
                                                            } catch (e: Exception) {
                                                                Toast.makeText(context, "Cannot open: ${e.message}. Use system downloads folder.", Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(26.dp)
                                                    ) {
                                                        Text("View File", fontSize = 9.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            try {
                                val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Unable to launch system downloads", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "System Downloads folder", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open System Downloads Folder", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryDialog(viewModel: AutomationViewModel, homeWebViewRef: WebView?, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "📜 Visited History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { onDismiss() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close history dialog")
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    if (viewModel.historyList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No browser logs available. Start exploring websites!", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                        ) {
                            items(viewModel.historyList) { history ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.activeBrowserUrl = history.url
                                            homeWebViewRef?.loadUrl(history.url)
                                            onDismiss()
                                        }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = history.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = history.url,
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            onClick = {
                                viewModel.clearHistory()
                                Toast.makeText(context, "History wiped safely", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clean absolute")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Wipe HistoryLogs")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImportDialog(viewModel: AutomationViewModel, onDismiss: () -> Unit) {
    var importTextState by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Dialog(onDismissRequest = { onDismiss() }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Import JSON Automation Workflow",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "You can write and modify template configurations outside this application using standard text editors, then import them below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = importTextState,
                        onValueChange = { importTextState = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .testTag("import_text_field"),
                        textStyle = TextStyle(
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        placeholder = {
                            Text(
                                "{\n  \"name\": \"Hacker News Reader\",\n  \"initialUrl\": \"https://news.ycombinator.com\",\n  \"steps\": [\n    {\"type\": \"NAVIGATE\", \"target\": \"https://news.ycombinator.com\"}\n  ]\n}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    )

                    if (viewModel.importErrorMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Error: " + viewModel.importErrorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                importTextState = WorkflowJsonHelper.getExampleTemplateJson()
                                viewModel.importErrorMessage = null
                            }
                        ) {
                            Text("Autoload Sample")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { onDismiss() }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val success = viewModel.loadAndImportTemplate(importTextState)
                                if (success) {
                                    onDismiss()
                                    Toast.makeText(context, "Successfully loaded script template!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("import_validate_button")
                        ) {
                            Text("Import")
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
fun ExportDialog(
    workflow: Workflow,
    viewModel: AutomationViewModel,
    copyToClipboard: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    // Modal Sheet DIALOG: Export Template
    showExportDialog?.let { workflow ->
        val recordSteps = remember(workflow) {
            WorkflowJsonHelper.stepsFromJsonString(workflow.stepsJson)
        }
        val jsonString = remember(workflow, recordSteps) {
            WorkflowJsonHelper.exportToJson(workflow.name, workflow.initialUrl, recordSteps)
        }
        var selectedLang by remember { mutableStateOf("JSON") }
        val displayedCode = remember(workflow, recordSteps, selectedLang) {
            when (selectedLang) {
                "Python" -> WorkflowJsonHelper.generatePythonPlaywright(workflow.name, workflow.initialUrl, recordSteps)
                "Node.js" -> WorkflowJsonHelper.generateJSPlaywright(workflow.name, workflow.initialUrl, recordSteps)
                "Kotlin" -> WorkflowJsonHelper.generateKotlinPlaywright(workflow.name, workflow.initialUrl, recordSteps)
                "Go" -> WorkflowJsonHelper.generateGoRod(workflow.name, workflow.initialUrl, recordSteps)
                else -> jsonString
            }
        }

        Dialog(onDismissRequest = { onDismiss() }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export recipe icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Export Automation Recipe",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Generate optimized scripts from your workflow action sequence.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Multi-Language Selection Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("JSON", "Python", "Node.js", "Kotlin", "Go").forEach { lang ->
                            val isSel = selectedLang == lang
                            Surface(
                                onClick = { selectedLang = lang },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isSel) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = lang,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        LazyColumn(modifier = Modifier.padding(10.dp)) {
                            item {
                                Text(
                                    text = displayedCode,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { copyToClipboard("Custom Script Block", displayedCode) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Copy icon check", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Code Structure")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { onDismiss() }) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddManualStepDialog(viewModel: AutomationViewModel, onDismiss: () -> Unit) {
    if (showAddManualStepDialog) {
        var manualType by remember { mutableStateOf("CLICK") }
        var manualTarget by remember { mutableStateOf("") }
        var manualValue by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { onDismiss() }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "➕ Build Manual Steps Action",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("CLICK", "INPUT", "NAVIGATE", "WAIT", "SCREENSHOT").forEach { option ->
                                val isSelected = manualType == option
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { manualType = option },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(option, fontWeight = FontWeight.Bold, fontSize = 8.5.sp)
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("LOOP", "CONDITIONAL", "OS_CLICK", "OS_SCROLL").forEach { option ->
                                val isSelected = manualType == option
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { manualType = option },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(option, fontWeight = FontWeight.Bold, fontSize = 8.5.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when (manualType) {
                            "NAVIGATE" -> "Target Link URL"
                            "WAIT" -> "Target (Unused)"
                            "SCREENSHOT" -> "Screenshot Label Name"
                            "LOOP" -> "Loop Repetition Count (e.g. 3)"
                            "CONDITIONAL" -> "Check Text Existence (e.g. TEXT:Submit or .navbar)"
                            "OS_CLICK" -> "Physical Screen Coordinate 'x,y' (e.g. 500,1000)"
                            "OS_SCROLL" -> "Physical Screen Path 'startX,startY,endX,endY' (e.g. 500,1500,500,500)"
                            else -> "CSS Element Query Selector (.btn, #input)"
                        },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = manualTarget,
                        onValueChange = { manualTarget = it },
                        modifier = Modifier.fillMaxWidth().testTag("manual_target_input"),
                        enabled = manualType != "WAIT",
                        placeholder = { 
                            Text(
                                when (manualType) {
                                    "NAVIGATE" -> "https://..."
                                    "SCREENSHOT" -> "e.g. after_login"
                                    "LOOP" -> "e.g. 5"
                                    "CONDITIONAL" -> "e.g. TEXT:Welcome or button.primary"
                                    "OS_CLICK" -> "e.g. 450,1200"
                                    "OS_SCROLL" -> "e.g. 500,1500,500,300"
                                    else -> "e.g. td.title > a"
                                }
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when (manualType) {
                            "WAIT" -> "Delay duration in Milliseconds"
                            "LOOP" -> "Steps displacement count to repeat (e.g., 2 repeat last 2 steps)"
                            "CONDITIONAL" -> "Failure Skip count (skip next X steps if not found)"
                            "OS_CLICK" -> "Verification Timing delay in ms (Unused)"
                            "OS_SCROLL" -> "Duration of gesture slide in ms"
                            "SCREENSHOT" -> "Save Destination (Unused)"
                            else -> "Argument Value (text query to type)"
                        },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = manualValue,
                        onValueChange = { manualValue = it },
                        modifier = Modifier.fillMaxWidth().testTag("manual_value_input"),
                        enabled = manualType != "SCREENSHOT" && manualType != "OS_CLICK",
                        placeholder = { 
                            Text(
                                when (manualType) {
                                    "WAIT" -> "1500"
                                    "LOOP" -> "e.g. 2"
                                    "CONDITIONAL" -> "e.g. 1"
                                    "OS_SCROLL" -> "300"
                                    else -> "e.g. Input value text string"
                                }
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { onDismiss() }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (manualType != "WAIT" && manualType != "SCREENSHOT" && manualTarget.isBlank()) {
                                    Toast.makeText(context, "Field target is obligatory", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.addManualStep(manualType, manualTarget, manualValue)
                                onDismiss()
                            },
                            modifier = Modifier.testTag("manual_save_button")
                        ) {
                            Text("Add Instruction")
                        }
                    }
                }
            }
        }
    }
}

fun isDownloadableUrl(url: String): Boolean {
    val cleanUrl = url.lowercase(java.util.Locale.US).split("?")[0].split("#")[0]
    return cleanUrl.endsWith(".apk") ||
            cleanUrl.endsWith(".mp4") ||
            cleanUrl.endsWith(".zip") ||
            cleanUrl.endsWith(".rar") ||
            cleanUrl.endsWith(".bin") ||
            cleanUrl.endsWith(".mov") ||
            cleanUrl.endsWith(".mkv") ||
            cleanUrl.endsWith(".avi") ||
            cleanUrl.endsWith(".pdf") ||
            cleanUrl.endsWith(".exe") ||
            cleanUrl.endsWith(".dmg") ||
            cleanUrl.endsWith(".tar") ||
            cleanUrl.endsWith(".gz") ||
            cleanUrl.endsWith(".7z")
}

fun handleWebViewScheme(view: WebView?, context: android.content.Context, url: String): Boolean {
    val uri = try { android.net.Uri.parse(url) } catch (e: Exception) { null } ?: return false
    val scheme = uri.scheme?.lowercase(java.util.Locale.US) ?: return false

    // Web and embedded data URLs are handled natively by the WebView
    if (scheme == "http" || scheme == "https" || scheme == "data") {
        return false
    }

    // Specially handle Android intents
    if (scheme == "intent") {
        try {
            val intent = Intent.parseUri(url, 1)
            if (intent != null) {
                // Try launching the resolved activity
                val pm = context.packageManager
                val info = pm.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                if (info != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return true
                } else {
                    // Try fallback URL if present
                    val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                    if (!fallbackUrl.isNullOrEmpty()) {
                        val fallbackUri = android.net.Uri.parse(fallbackUrl)
                        val fbScheme = fallbackUri.scheme?.lowercase(java.util.Locale.US)
                        if (fbScheme == "http" || fbScheme == "https") {
                            view?.loadUrl(fallbackUrl)
                            return true
                        }
                    }
                    // Try package name fallback to play store
                    val pack = intent.`package`
                    if (!pack.isNullOrEmpty()) {
                        val marketIntent = Intent(
                            Intent.ACTION_VIEW,
                            android.net.Uri.parse("market://details?id=$pack")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(marketIntent)
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    // Android App links
    if (scheme == "android-app") {
        try {
            val intent = Intent.parseUri(url, 2)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    // Explicit custom routing for specific schemes to optimize OS actions
    try {
        val intent = when (scheme) {
            "tel" -> Intent(Intent.ACTION_DIAL, uri)
            "sms", "smsto" -> Intent(Intent.ACTION_SENDTO, uri)
            "mailto" -> Intent(Intent.ACTION_SENDTO, uri)
            "geo" -> Intent(Intent.ACTION_VIEW, uri)
            "market" -> Intent(Intent.ACTION_VIEW, uri)
            "ftp" -> Intent(Intent.ACTION_VIEW, uri)
            else -> null
        }

        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return true
        }
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "No app available to handle: ${scheme}://", android.widget.Toast.LENGTH_SHORT).show()
        return true
    }

    // Supported lists of other known custom external protocols (Communication, Social, App Stores, etc.)
    val handledSchemes = setOf(
        "tel", "sms", "smsto", "mailto", "geo", "market",
        "whatsapp", "tg", "fb", "instagram", "youtube", "twitter", "snapchat",
        "sip", "maps", "zoommtg", "file", "content", "ftp", "ws", "wss"
    )

    if (scheme in handledSchemes || scheme.startsWith("whatsapp") || scheme.startsWith("fb") || scheme.startsWith("twit") || scheme.startsWith("tg")) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "App not installed to open link: $scheme://", android.widget.Toast.LENGTH_SHORT).show()
        }
        return true
    }

    // Fallback for any other custom URL scheme (e.g., specific app deep links)
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return true
    } catch (e: Exception) {
        // Ignored or fallback
    }

    return false
}

}

@Composable
fun DownloadConfirmInline(viewModel: AutomationViewModel, onDismiss: () -> Unit) {
    val confirmRequest = viewModel.pendingDownloadRequest ?: return
        Dialog(onDismissRequest = {
            onDismiss()
            viewModel.pendingDownloadRequest = null
        }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DownloadForOffline,
                            contentDescription = "Confirm download and install",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Download File?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Do you want to download this file with the automation browser?",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "FILE NAME",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = confirmRequest.filename,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (confirmRequest.contentLength > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "ESTIMATED SIZE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                val megaBytes = confirmRequest.contentLength.toFloat() / (1024f * 1024f)
                                Text(
                                    text = String.format("%.2f MB", megaBytes),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!confirmRequest.mimetype.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "MIME TYPE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = confirmRequest.mimetype,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                onDismiss()
                                viewModel.pendingDownloadRequest = null
                            }
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.confirmAndEnqueueDownload()
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Download", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumUpgradeDialogContent(viewModel: AutomationViewModel) {
    val context = LocalContext.current
    if (viewModel.showPremiumUpgradeDialog) {
        Dialog(onDismissRequest = { viewModel.showPremiumUpgradeDialog = false }) {
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "System configuration icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Extended Features Required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Activate advanced features to configure additional system capabilities:",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val featuresList = listOf(
                        "Script Templates & JSON Import",
                        "Auto Navigation Repeat Cycles",
                        "Smart Wait Page Load Detection",
                        "Automatic Execution Recovery",
                        "Network Ad & Pop-up Blocker",
                        "Custom User Agent Configuration",
                        "Session Persistence Support",
                        "Unlimited Concurrent Browser Tabs"
                    )
                    
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        featuresList.forEach { feat ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Enabled feature",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(feat, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.showPremiumUpgradeDialog = false },
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel", fontSize = 11.sp)
                        }
                        
                        Button(
                            onClick = {
                                viewModel.showPremiumUpgradeDialog = false
                                viewModel.currentTab = AppTab.SETTINGS
                                Toast.makeText(context, "Enter activation code 'Goodxvampire' to unlock.", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.weight(1.2f).height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Activate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
}
