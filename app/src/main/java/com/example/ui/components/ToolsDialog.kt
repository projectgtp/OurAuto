package com.example.ui.components

import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.AutomationViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ToolsDialog(
    viewModel: AutomationViewModel,
    webViewRef: WebView?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
            var scriptEngineExpanded by remember { mutableStateOf(false) }
            var extensionsExpanded by remember { mutableStateOf(false) }
            var customScriptCode by remember { mutableStateOf("") }

            var showAddExtensionForm by remember { mutableStateOf(false) }
            var extName by remember { mutableStateOf("") }
            var extDesc by remember { mutableStateOf("") }
            var extPattern by remember { mutableStateOf("*") }
            var extJsCode by remember { mutableStateOf("") }

            var showSourceDialog by remember { mutableStateOf(false) }
            var userAgentExpanded by remember { mutableStateOf(false) }
            var cookiesPaneExpanded by remember { mutableStateOf(false) }
            
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Quick Tools and Utilities",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { onDismiss() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close tools modal")
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Content Blocker status item (Premium Gated)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!viewModel.isPremiumUnlocked) {
                                    onDismiss()
                                    viewModel.showPremiumUpgradeDialog = true
                                }
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Warning, contentDescription = "Ads shielding", tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Premium Ads & Pop-up Blocker", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (!viewModel.isPremiumUnlocked) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Default.Lock, contentDescription = "Locked Feature", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                    }
                                }
                                Text("Intercepts telemetry, pop-ups, and aggressive tracker elements.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                        }
                        if (viewModel.isPremiumUnlocked) {
                            Switch(
                                checked = viewModel.isAdblockEnabled,
                                onCheckedChange = { viewModel.isAdblockEnabled = it }
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // 📊 Realtime Automation Diagnostics & Variables HUD
                    var showDiagnosticsExpanded by remember { mutableStateOf(true) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDiagnosticsExpanded = !showDiagnosticsExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Diagnostics", tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "📊 Automation Diagnostics & HUD",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = if (showDiagnosticsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Toggle Section"
                                )
                            }
                            
                            if (showDiagnosticsExpanded) {
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                // Statistics Cards Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("TOTAL RUNS", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("${viewModel.totalRunsCount}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("ERRORS DETECTED", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("${viewModel.issuesList.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Active Registers (Variables) Listing
                                Text("📁 LIVE VARIABLES REGISTRY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                if (viewModel.variableRegistry.isEmpty()) {
                                    Text(
                                        "No values recorded yet. Launch standard macro routines or assign variables to view in real-time.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                            .padding(6.dp)
                                    ) {
                                        viewModel.variableRegistry.forEach { (key, value) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "{{$key}}",
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                                Text(
                                                    value,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 2,
                                                    modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Accessibility Service Direct manual controls
                                Text("ACCESSIBILITY MANUAL SERVICE DECK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(6.dp))
                                val isAccRunning = MyAccessibilityService.isServiceRunning()
                                if (!isAccRunning) {
                                    Text(
                                        "Service is offline. Turn on matching accessibility service in phone settings for global OS system control tests.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                MyAccessibilityService.performGlobalAction(1) // Back
                                                Toast.makeText(context, "Accessibility System Back gesture fired", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(1f).height(32.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Text("Back", fontSize = 10.sp)
                                        }
                                        Button(
                                            onClick = {
                                                MyAccessibilityService.performGlobalAction(2) // Home
                                                Toast.makeText(context, "Accessibility System Home gesture fired", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(1f).height(32.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Text("Home", fontSize = 10.sp)
                                        }
                                        Button(
                                            onClick = {
                                                MyAccessibilityService.performGlobalAction(3) // Recents
                                                Toast.makeText(context, "Accessibility System Recents gesture fired", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(1f).height(32.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Text("Recents", fontSize = 10.sp)
                                        }
                                        Button(
                                            onClick = {
                                                MyAccessibilityService.performGlobalAction(4) // Notifications
                                                Toast.makeText(context, "Accessibility System Expand Notifications gesture fired", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(1.2f).height(32.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Text("Notif Drawer", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // Quick utility: clear cache
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                webViewRef?.clearCache(true)
                                Toast.makeText(context, "WebView Cache cleaned fully", Toast.LENGTH_SHORT).show()
                                onDismiss()
                             }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Cache clearing", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Clear Cache & Storage", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Flushes cached elements and rendering resources", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                    }



                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // 1. COLLAPSIBLE BROWSER EXTENSIONS
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { extensionsExpanded = !extensionsExpanded }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Browser Extensions icon",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("UserScripts & Chrome Extensions (Tampermonkey)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Manage automatic background themes, performance HUD counters & live compiled custom userScripts", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                        Text(
                            text = if (extensionsExpanded) "▲" else "▼",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }

                    if (extensionsExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "💡 Standard Chrome Extensions (.crx) are emulated via inject-on-load automatic content scripts matching specified URL rules.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            // Render all currently loaded Extensions
                            viewModel.installedExtensions.forEach { ext ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
                                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ext.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text(ext.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Match Pattern: ${ext.urlMatchPattern}",
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Switch(
                                        checked = ext.isEnabled,
                                        onCheckedChange = { 
                                            viewModel.toggleExtension(ext.id)
                                            Toast.makeText(context, "${ext.name} modern injection status toggled!", Toast.LENGTH_SHORT).show()
                                        }
                                    )

                                    IconButton(
                                        onClick = {
                                            viewModel.deleteExtension(ext.id)
                                            Toast.makeText(context, "${ext.name} deleted successfully", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove Extension",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            if (!showAddExtensionForm) {
                                Button(
                                    onClick = { showAddExtensionForm = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Extension", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Install Custom Chrome Extension (Content Script)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                // Add custom chrome extension form
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Install New Content-Script Extension", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    
                                    OutlinedTextField(
                                        value = extName,
                                        onValueChange = { extName = it },
                                        label = { Text("Extension Name", fontSize = 9.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("e.g. My Custom Translator", fontSize = 9.sp) }
                                    )

                                    OutlinedTextField(
                                        value = extDesc,
                                        onValueChange = { extDesc = it },
                                        label = { Text("Extension Purpose / Description", fontSize = 9.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("e.g. Automatically alters elements on page finish", fontSize = 9.sp) }
                                    )

                                    OutlinedTextField(
                                        value = extPattern,
                                        onValueChange = { extPattern = it },
                                        label = { Text("Domain Match Pattern", fontSize = 9.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("e.g. wikipedia.org or '*' for all URLs", fontSize = 9.sp) }
                                    )

                                    OutlinedTextField(
                                        value = extJsCode,
                                        onValueChange = { extJsCode = it },
                                        label = { Text("JavaScript Content Script Executable", fontSize = 9.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("e.g. alert('Hello from Extension!');", fontSize = 9.sp) },
                                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 9.sp)
                                    )

                                    // Presets quick-load chips
                                    Text("Load Template Preset:", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val extensionPresets = listOf(
                                            "Auto Refresh 5s" to "setInterval(function() { window.location.reload(); }, 5000);",
                                            "Background Aqua" to "document.body.style.backgroundColor='#E0F2FE'; alert('Aqua Extension Activated!');",
                                            "DOM Link Checker" to "alert('Found ' + document.links.length + ' links on page!');"
                                        )

                                        extensionPresets.forEach { (pName, pJs) ->
                                            Button(
                                                onClick = {
                                                    extName = pName
                                                    extDesc = "Triggers helper automation scripts on target pages"
                                                    extJsCode = pJs
                                                    extPattern = "*"
                                                    Toast.makeText(context, "$pName preset template loaded!", Toast.LENGTH_SHORT).show()
                                                },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                                                modifier = Modifier.height(24.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(pName, fontSize = 8.sp)
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { 
                                                showAddExtensionForm = false 
                                                extName = ""
                                                extDesc = ""
                                                extPattern = "*"
                                                extJsCode = ""
                                            },
                                            modifier = Modifier.weight(1f).height(32.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("Cancel", fontSize = 10.sp)
                                        }

                                        Button(
                                            onClick = {
                                                if (extName.trim().isEmpty() || extJsCode.trim().isEmpty()) {
                                                    Toast.makeText(context, "Name and JS code are strictly required!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    viewModel.addNewExtension(
                                                        name = extName,
                                                        description = extDesc.ifBlank { "Custom user imported script integration" },
                                                        pattern = extPattern.ifBlank { "*" },
                                                        js = extJsCode
                                                    )
                                                    Toast.makeText(context, "$extName chrome script compiled & installed!", Toast.LENGTH_SHORT).show()
                                                    
                                                    // Reset states
                                                    extName = ""
                                                    extDesc = ""
                                                    extPattern = "*"
                                                    extJsCode = ""
                                                    showAddExtensionForm = false
                                                }
                                            },
                                            modifier = Modifier.weight(1.5f).height(32.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = "Install Extension", modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Install Extension", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // 2. COLLAPSIBLE CUSTOM SCRIPT ENGINE
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { scriptEngineExpanded = !scriptEngineExpanded }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Script Engine icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Advanced Script Engine", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Inject customizable JavaScript scripts live inside browser", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                        Text(
                            text = if (scriptEngineExpanded) "▲" else "▼",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }

                    if (scriptEngineExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Select Preset Sniper Script:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            // Preset microchips/buttons list
                            val presets = listOf(
                                "Highlight Images" to "Array.from(document.querySelectorAll('img')).forEach(img => img.style.border = '5px dotted #8B5CF6')",
                                "Auto-Scrape Links" to "alert('Discovered total ' + document.getElementsByTagName('a').length + ' links in active DOM tree.');",
                                "Vintage Sepia" to "document.documentElement.style.filter = 'sepia(0.85) contrast(1.15)';"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                presets.forEach { (name, code) ->
                                    Button(
                                        onClick = { 
                                            customScriptCode = code
                                            Toast.makeText(context, "Preset '$name' loaded!", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp).weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(name, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Custom Text field input console
                            OutlinedTextField(
                                value = customScriptCode,
                                onValueChange = { customScriptCode = it },
                                label = { Text("JavaScript Console Injection", fontSize = 10.sp) },
                                placeholder = { Text("e.g. alert(document.title)", fontSize = 10.sp) },
                                maxLines = 4,
                                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 10.sp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    if (customScriptCode.trim().isNotEmpty()) {
                                        webViewRef?.evaluateJavascript(customScriptCode, null)
                                        Toast.makeText(context, "Script successfully injected and executed in-page!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "Please write or choose a script code preset first", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Run Script", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Infiltrate & Inject Script Code", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 1. COLLAPSIBLE USER AGENT SPOOF-SWITCHER
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!viewModel.isPremiumUnlocked) {
                                    onDismiss()
                                    viewModel.showPremiumUpgradeDialog = true
                                } else {
                                    userAgentExpanded = !userAgentExpanded
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User Agent Configurations icon",
                            tint = Color(0xFF3B82F6)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("User Agent Configurations", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (!viewModel.isPremiumUnlocked) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.Lock, contentDescription = "Locked Feature", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                }
                            }
                            Text("Configure custom user agent profiles or custom headers", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                        Text(
                            text = if (userAgentExpanded) "▲" else "▼",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }

                    if (userAgentExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Choose Target Browser User-Agent Header Profile:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            
                            val profiles = listOf("Default", "Desktop Chrome", "iPhone iOS Safari", "Custom")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                profiles.forEach { profile ->
                                    val isSelected = viewModel.currentUserAgentType == profile
                                    Button(
                                        onClick = {
                                            viewModel.currentUserAgentType = profile
                                            Toast.makeText(context, "$profile User Agent profile activated! Page will reload.", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f).height(30.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(profile, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (viewModel.currentUserAgentType == "Custom") {
                                OutlinedTextField(
                                    value = viewModel.customUserAgentString,
                                    onValueChange = { viewModel.customUserAgentString = it },
                                    label = { Text("Custom User-Agent Header String", fontSize = 10.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 9.sp)
                                )
                                Button(
                                    onClick = {
                                        Toast.makeText(context, "Custom User Agent applied! Reloading webview", Toast.LENGTH_SHORT).show()
                                        webViewRef?.reload()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(32.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Apply & Reload Now", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 2. VIEW PAGE SOURCE (HTML/DOM EXTRACTOR)
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.isFetchingSource = true
                                webViewRef?.evaluateJavascript(
                                    "javascript:(function() { return document.documentElement.outerHTML; })();"
                                ) { rawHtml ->
                                    viewModel.isFetchingSource = false
                                    var formatted = rawHtml ?: ""
                                    if (formatted.startsWith("\"") && formatted.endsWith("\"") && formatted.length >= 2) {
                                        formatted = formatted.substring(1, formatted.length - 1)
                                    }
                                    formatted = formatted
                                        .replace("\\u003C", "<")
                                        .replace("\\u003E", ">")
                                        .replace("\\u0026", "&")
                                        .replace("\\\"", "\"")
                                        .replace("\\'", "'")
                                        .replace("\\\\", "\\")
                                        .replace("\\n", "\n")
                                        .replace("\\r", "\r")
                                        .replace("\\t", "\t")

                                    viewModel.pageSourceHtml = if (formatted.trim().isEmpty() || formatted == "null") {
                                        "<!-- Empty DOM or source blocked by cross-origin policies -->"
                                    } else {
                                        formatted
                                    }
                                    showSourceDialog = true
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "View Source Icon",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("View Page Source (HTML)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Extracts, inspects & copies raw HTML structure for target integrations", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                        if (viewModel.isFetchingSource) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Arrow", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // 3. COLLAPSIBLE COOKIE EDITOR & INJECTOR
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!viewModel.isPremiumUnlocked) {
                                    onDismiss()
                                    viewModel.showPremiumUpgradeDialog = true
                                } else {
                                    viewModel.refreshBrowserCookies(viewModel.activeBrowserUrl)
                                    cookiesPaneExpanded = !cookiesPaneExpanded
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Cookie Editor icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Cookie Editor & Injector", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (!viewModel.isPremiumUnlocked) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.Lock, contentDescription = "Locked Feature", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                }
                            }
                            Text("Inspect, modify, delete & manually inject active session cookies", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                        Text(
                            text = if (cookiesPaneExpanded) "▲" else "▼",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }

                    if (cookiesPaneExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            var showAddCookieForm by remember { mutableStateOf(false) }
                            var newCookieName by remember { mutableStateOf("") }
                            var newCookieVal by remember { mutableStateOf("") }

                            var cookieFilterText by remember { mutableStateOf("") }
                            var showBulkImportForm by remember { mutableStateOf(false) }
                            var bulkImportInputText by remember { mutableStateOf("") }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Active Cookies (${viewModel.activeBrowserCookies.size}):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Button(
                                    onClick = { showAddCookieForm = !showAddCookieForm },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Text(if (showAddCookieForm) "Cancel" else "+ Add", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // 1. Search Bar Filter box
                            OutlinedTextField(
                                value = cookieFilterText,
                                onValueChange = { cookieFilterText = it },
                                placeholder = { Text("Filter cookies by name or value...", fontSize = 9.sp) },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                maxLines = 1,
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp),
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon", modifier = Modifier.size(12.dp)) }
                            )

                            // 2. High-power utilities toolbar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val json = viewModel.activeBrowserCookies.joinToString(prefix = "{\n", postfix = "\n}", separator = ",\n") { "  \"${it.first}\": \"${it.second.replace("\"", "\\\"")}\"" }
                                        val clip = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        clip.setPrimaryClip(android.content.ClipData.newPlainText("JSON Cookies", json))
                                        Toast.makeText(context, "Domain cookies copied as JSON!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f).height(24.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Copy JSON", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                Button(
                                    onClick = {
                                        val header = viewModel.activeBrowserCookies.joinToString(separator = "; ") { "${it.first}=${it.second}" }
                                        val clip = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        clip.setPrimaryClip(android.content.ClipData.newPlainText("Cookie Header", header))
                                        Toast.makeText(context, "Domain cookies copied as header string!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1.1f).height(24.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Text("Copy Header", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { showBulkImportForm = !showBulkImportForm },
                                    modifier = Modifier.weight(1f).height(24.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                ) {
                                    Text(if (showBulkImportForm) "Close Bulk" else "Bulk Paste", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        viewModel.clearAllCookies(viewModel.activeBrowserUrl)
                                        Toast.makeText(context, "Cleared cookies for this domain!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(0.9f).height(24.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Clear All", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // 3. Bulk Import Form
                            if (showBulkImportForm) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(6.dp))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Bulk Paste Raw Cookie Values (JSON or Raw Semicolon)", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    OutlinedTextField(
                                        value = bulkImportInputText,
                                        onValueChange = { bulkImportInputText = it },
                                        placeholder = { Text("e.g. cookie1=val; cookie2=val\nor {\"c1\":\"v1\"}", fontSize = 9.sp) },
                                        modifier = Modifier.fillMaxWidth().height(80.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 9.sp)
                                    )
                                    Button(
                                        onClick = {
                                            if (bulkImportInputText.isNotBlank()) {
                                                val rawPairs = mutableListOf<Pair<String, String>>()
                                                if (bulkImportInputText.trim().startsWith("{")) {
                                                    val jsonMatches = """["']([^"']+)["']\s*:\s*["']([^"']*)["']""".toRegex()
                                                    jsonMatches.findAll(bulkImportInputText).forEach { match ->
                                                        rawPairs.add(match.groupValues[1] to match.groupValues[2])
                                                    }
                                                } else {
                                                    bulkImportInputText.split(";").forEach { item ->
                                                        val index = item.indexOf("=")
                                                        if (index != -1) {
                                                            rawPairs.add(item.substring(0, index).trim() to item.substring(index + 1).trim())
                                                        }
                                                    }
                                                }
                                                
                                                if (rawPairs.isNotEmpty()) {
                                                    rawPairs.forEach { (name, value) ->
                                                        viewModel.saveCookie(viewModel.activeBrowserUrl, name, value)
                                                    }
                                                    Toast.makeText(context, "Bulk imported ${rawPairs.size} domains successfully!", Toast.LENGTH_SHORT).show()
                                                    bulkImportInputText = ""
                                                    showBulkImportForm = false
                                                } else {
                                                    Toast.makeText(context, "No matches found. Assure name=value format.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(30.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text("Import and Inject", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (showAddCookieForm) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(6.dp))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Add New Session Cookie for Domain", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    OutlinedTextField(
                                        value = newCookieName,
                                        onValueChange = { newCookieName = it },
                                        label = { Text("Cookie Name/Key", fontSize = 9.sp) },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp)
                                    )
                                    OutlinedTextField(
                                        value = newCookieVal,
                                        onValueChange = { newCookieVal = it },
                                        label = { Text("Cookie Value", fontSize = 9.sp) },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp)
                                    )
                                    Button(
                                        onClick = {
                                            if (newCookieName.trim().isNotEmpty()) {
                                                viewModel.saveCookie(viewModel.activeBrowserUrl, newCookieName.trim(), newCookieVal)
                                                newCookieName = ""
                                                newCookieVal = ""
                                                showAddCookieForm = false
                                                Toast.makeText(context, "Cookie injected successfully! Tap Reload to apply.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Cookie Name cannot be blank!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(30.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text("Save Cookie", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            val filteredCookies = remember(viewModel.activeBrowserCookies, cookieFilterText) {
                                if (cookieFilterText.isBlank()) {
                                    viewModel.activeBrowserCookies
                                } else {
                                    viewModel.activeBrowserCookies.filter {
                                        it.first.contains(cookieFilterText, ignoreCase = true) ||
                                        it.second.contains(cookieFilterText, ignoreCase = true)
                                    }
                                }
                            }

                            if (filteredCookies.isEmpty()) {
                                Text(if (cookieFilterText.isBlank()) "No cookies found on this active domain. Interact or log in to trigger cookie cache." else "No cookies match search filter.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    filteredCookies.forEach { cookie ->
                                        var isEditing by remember { mutableStateOf(false) }
                                        var editValue by remember { mutableStateOf(cookie.second) }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(4.dp))
                                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(4.dp))
                                                .padding(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(cookie.first, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                                                if (isEditing) {
                                                    OutlinedTextField(
                                                        value = editValue,
                                                        onValueChange = { editValue = it },
                                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp),
                                                        modifier = Modifier.fillMaxWidth().height(40.dp)
                                                    )
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        modifier = Modifier.padding(top = 4.dp)
                                                    ) {
                                                        Button(
                                                            onClick = {
                                                                viewModel.saveCookie(viewModel.activeBrowserUrl, cookie.first, editValue)
                                                                isEditing = false
                                                                Toast.makeText(context, "${cookie.first} cookie updated!", Toast.LENGTH_SHORT).show()
                                                            },
                                                            modifier = Modifier.height(24.dp),
                                                            contentPadding = PaddingValues(horizontal = 6.dp),
                                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                                        ) {
                                                            Text("Update", fontSize = 8.sp)
                                                        }
                                                        OutlinedButton(
                                                            onClick = { isEditing = false },
                                                            modifier = Modifier.height(24.dp),
                                                            contentPadding = PaddingValues(horizontal = 6.dp)
                                                        ) {
                                                            Text("Cancel", fontSize = 8.sp)
                                                        }
                                                    }
                                                } else {
                                                    Text(cookie.second, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, style = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace))
                                                }
                                            }

                                            if (!isEditing) {
                                                IconButton(
                                                    onClick = { 
                                                        editValue = cookie.second
                                                        isEditing = true 
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit Cookie", modifier = Modifier.size(12.dp))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        viewModel.deleteCookie(viewModel.activeBrowserUrl, cookie.first)
                                                        Toast.makeText(context, "${cookie.first} cookie deleted!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete Cookie", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Render Developer Core Console / DOM explorer dialog POPUP
                    if (showSourceDialog) {
                        var sourceTabSelected by remember { mutableStateOf(0) }
                        var sourceSearchText by remember { mutableStateOf("") }
                        var replInputText by remember { mutableStateOf("") }
                        var replOutputText by remember { mutableStateOf("No output logs fetched. Type a JS block above and tap Execute.") }

                        val resourceLinksByRegex = remember(viewModel.pageSourceHtml) {
                            val html = viewModel.pageSourceHtml ?: ""
                            val scriptsList = mutableListOf<String>()
                            val stylesList = mutableListOf<String>()
                            
                            val scriptRegex = """<script\s+[^>]*src=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
                            scriptRegex.findAll(html).forEach { match ->
                                scriptsList.add(match.groupValues[1])
                            }
                            
                            val styleRegex = """<link\s+[^>]*href=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
                            styleRegex.findAll(html).forEach { match ->
                                val valMatch = match.value
                                if (valMatch.contains("stylesheet", ignoreCase = true)) {
                                    stylesList.add(match.groupValues[1])
                                }
                            }
                            Pair(scriptsList.distinct(), stylesList.distinct())
                        }

                        Dialog(onDismissRequest = { showSourceDialog = false }) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.85f)
                                    .padding(4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Web Inspector Console",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(onClick = { showSourceDialog = false }) {
                                            Icon(Icons.Default.Close, contentDescription = "Close Inspector View")
                                        }
                                    }
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf("DOM Source", "Scripts & Links", "Live JS Console").forEachIndexed { idx, name ->
                                            val isSel = sourceTabSelected == idx
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { sourceTabSelected = idx },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                                border = if (isSel) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(name, fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
                                                }
                                            }
                                        }
                                    }

                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                    
                                    when (sourceTabSelected) {
                                        0 -> {
                                            // DOM VIEW WITH SEARCH MATCHING
                                            Text(
                                                text = "Search contents inside XML/HTML body (case insensitive):",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            OutlinedTextField(
                                                value = sourceSearchText,
                                                onValueChange = { sourceSearchText = it },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp),
                                                placeholder = { Text("Filter DOM source text...") },
                                                maxLines = 1,
                                                singleLine = true,
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f)
                                                    .background(Color(0xFF0F172A), shape = RoundedCornerShape(8.dp))
                                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                                                    .padding(8.dp)
                                            ) {
                                                val sourceText = viewModel.pageSourceHtml ?: "Fetching page source HTML stream..."
                                                val filteredText = remember(sourceText, sourceSearchText) {
                                                    if (sourceSearchText.isNotBlank()) {
                                                        sourceText.lineSequence()
                                                            .filter { it.contains(sourceSearchText, ignoreCase = true) }
                                                            .take(150)
                                                            .joinToString(separator = "\n")
                                                    } else {
                                                        sourceText
                                                    }
                                                }

                                                LazyColumn(modifier = Modifier.matchParentSize()) {
                                                    item {
                                                        SelectionContainer {
                                                            Text(
                                                                text = filteredText.ifBlank { "No matching DOM segments found." },
                                                                color = Color(0xFFF1F5F9),
                                                                fontSize = 9.5.sp,
                                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        1 -> {
                                            // SCRIPTS EXTRACTION PANEL
                                            Text(
                                                text = "Extracted JS Files & CSS Elements:",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(bottom = 6.dp)
                                            )

                                            LazyColumn(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                item {
                                                    Text("📜 Embedded Scripts (${resourceLinksByRegex.first.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp)
                                                }
                                                if (resourceLinksByRegex.first.isEmpty()) {
                                                    item {
                                                        Text("No script directories referenced directly.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                                                    }
                                                } else {
                                                    items(resourceLinksByRegex.first) { path ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                                .padding(6.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(path, fontSize = 9.sp, modifier = Modifier.weight(1f), overflow = TextOverflow.Ellipsis, maxLines = 1)
                                                            IconButton(
                                                                onClick = {
                                                                    val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                                    clipboardManager.setPrimaryClip(android.content.ClipData.newPlainText("Script Path", path))
                                                                    Toast.makeText(context, "Url copied!", Toast.LENGTH_SHORT).show()
                                                                },
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Icon(Icons.Default.Share, contentDescription = "Copy script link", modifier = Modifier.size(12.dp))
                                                            }
                                                        }
                                                    }
                                                }

                                                item {
                                                    Text("🎨 Stylesheet Links (${resourceLinksByRegex.second.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                                                }
                                                if (resourceLinksByRegex.second.isEmpty()) {
                                                    item {
                                                        Text("No global stylesheets referenced directly.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                                                    }
                                                } else {
                                                    items(resourceLinksByRegex.second) { cssPath ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                                .padding(6.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(cssPath, fontSize = 9.sp, modifier = Modifier.weight(1f), overflow = TextOverflow.Ellipsis, maxLines = 1)
                                                            IconButton(
                                                                onClick = {
                                                                    val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                                    clipboardManager.setPrimaryClip(android.content.ClipData.newPlainText("CSS Path", cssPath))
                                                                    Toast.makeText(context, "Url copied!", Toast.LENGTH_SHORT).show()
                                                                },
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Icon(Icons.Default.Share, contentDescription = "Copy stylesheet link", modifier = Modifier.size(12.dp))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        2 -> {
                                            // JS REPL CONSOLE
                                            Text(
                                                text = "Browser Runtime JavaScript REPL Console:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(bottom = 6.dp)
                                            )
                                            OutlinedTextField(
                                                value = replInputText,
                                                onValueChange = { replInputText = it },
                                                modifier = Modifier.fillMaxWidth(),
                                                placeholder = { Text("e.g. document.title  or  document.cookie") },
                                                maxLines = 3,
                                                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp)
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        if (replInputText.isNotBlank()) {
                                                            webViewRef?.let { web ->
                                                                web.evaluateJavascript(replInputText) { callbackVal ->
                                                                    replOutputText = "Command executed successfully:\n$callbackVal"
                                                                }
                                                            } ?: run {
                                                                replOutputText = "Error: Core webView environment is unlinked."
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Run JS Expression", fontSize = 10.sp)
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        replInputText = ""
                                                        replOutputText = "Console stream cleared."
                                                    },
                                                    modifier = Modifier.weight(0.5f)
                                                ) {
                                                    Text("Clear", fontSize = 10.sp)
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f)
                                                    .background(Color(0xFF1E293B), shape = RoundedCornerShape(8.dp))
                                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                                                    .padding(10.dp)
                                            ) {
                                                LazyColumn(modifier = Modifier.matchParentSize()) {
                                                    item {
                                                        SelectionContainer {
                                                            Text(
                                                                text = replOutputText,
                                                                color = Color(0xFFA5F3FC),
                                                                fontSize = 10.sp,
                                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                val clipData = android.content.ClipData.newPlainText("HTML Source", viewModel.pageSourceHtml ?: "")
                                                clipboardManager.setPrimaryClip(clipData)
                                                Toast.makeText(context, "Full HTML source copied to clipboard!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text("Copy Full DOM", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }

                                        OutlinedButton(
                                            onClick = { showSourceDialog = false },
                                            modifier = Modifier.weight(0.5f)
                                        ) {
                                            Text("Dismiss", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}
