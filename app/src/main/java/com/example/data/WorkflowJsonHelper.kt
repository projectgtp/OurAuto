package com.example.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class WorkflowDraft(
    val name: String,
    val initialUrl: String,
    val steps: List<WorkflowStep>
)

object WorkflowJsonHelper {
    
    fun stepsToJsonString(stepsList: List<WorkflowStep>): String {
        val stepsArray = JSONArray()
        for (step in stepsList) {
            val stepObj = JSONObject()
            stepObj.put("id", step.id)
            stepObj.put("type", step.type)
            stepObj.put("target", step.target)
            stepObj.put("value", step.value)
            stepObj.put("timestamp", step.timestamp)
            stepsArray.put(stepObj)
        }
        return stepsArray.toString()
    }

    fun stepsFromJsonString(jsonString: String): List<WorkflowStep> {
        if (jsonString.isBlank()) return emptyList()
        val stepsList = mutableListOf<WorkflowStep>()
        try {
            val stepsArray = JSONArray(jsonString)
            for (i in 0 until stepsArray.length()) {
                val stepObj = stepsArray.getJSONObject(i)
                val id = stepObj.optString("id", UUID.randomUUID().toString())
                val type = stepObj.getString("type")
                val target = stepObj.optString("target", "")
                val value = stepObj.optString("value", "")
                val timestamp = stepObj.optLong("timestamp", System.currentTimeMillis())
                stepsList.add(WorkflowStep(id = id, type = type, target = target, value = value, timestamp = timestamp))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return stepsList
    }

    fun exportToJson(name: String, initialUrl: String, stepsList: List<WorkflowStep>): String {
        val root = JSONObject()
        root.put("name", name)
        root.put("initialUrl", initialUrl)
        
        val stepsArray = JSONArray()
        for (step in stepsList) {
            val stepObj = JSONObject()
            stepObj.put("id", step.id)
            stepObj.put("type", step.type)
            stepObj.put("target", step.target)
            stepObj.put("value", step.value)
            stepObj.put("timestamp", step.timestamp)
            stepsArray.put(stepObj)
        }
        root.put("steps", stepsArray)
        return root.toString(4) // pretty print with 4 space indent
    }

    fun importFromJson(jsonString: String): WorkflowDraft {
        try {
            val root = JSONObject(jsonString)
            
            val name = root.optString("name", "Imported Script").ifBlank { "Imported Script" }
            val initialUrl = root.optString("initialUrl", "https://www.google.com").ifBlank { "https://www.google.com" }
            
            val stepsList = mutableListOf<WorkflowStep>()
            val stepsArray = root.optJSONArray("steps")
            if (stepsArray != null) {
                for (i in 0 until stepsArray.length()) {
                    val stepObj = stepsArray.getJSONObject(i)
                    val id = stepObj.optString("id", UUID.randomUUID().toString())
                    val type = stepObj.getString("type").uppercase()
                    val target = stepObj.optString("target", "")
                    val value = stepObj.optString("value", "")
                    val timestamp = stepObj.optLong("timestamp", System.currentTimeMillis())
                    
                    val allowedTypes = listOf(
                        "CLICK", "INPUT", "NAVIGATE", "WAIT", "SCREENSHOT", "LOOP", "CONDITIONAL", "OS_CLICK", "OS_SCROLL", "SCROLL", "BACK", "FORWARD",
                        // 1. Navigation
                        "OPEN_LINK", "OPEN_APP", "OPEN_URL", "RELOAD_PAGE", "GO_BACK", "GO_FORWARD", "SWITCH_TAB", "CLOSE_TAB", "OPEN_TAB", "NEW_TAB",
                        // 2. Interaction
                        "DOUBLE_CLICK", "LONG_PRESS", "INPUT_TEXT", "CLEAR_TEXT", "SUBMIT_FORM", "SELECT_OPTION", "TOGGLE_SWITCH",
                        // 3. Scroll & Gestures
                        "SCROLL_UP", "SCROLL_DOWN", "SCROLL_LEFT", "SCROLL_RIGHT", "SCROLL_TO_TOP", "SCROLL_TO_BOTTOM", "SCROLL_UNTIL_ELEMENT_VISIBLE", "SWIPE", "DRAG_AND_DROP",
                        // 4. Wait / Timing
                        "WAIT_FOR_ELEMENT_VISIBLE", "WAIT_FOR_ELEMENT_CLICKABLE", "WAIT_FOR_PAGE_LOAD", "WAIT_FOR_URL_CHANGE", "WAIT_FOR_CONDITION", "WAIT_UNTIL_STABLE",
                        // 5. Detection / Query
                        "FIND_ELEMENT", "ELEMENT_EXISTS", "ELEMENT_NOT_EXISTS", "GET_ELEMENT_TEXT", "GET_ATTRIBUTE", "GET_CURRENT_URL", "GET_DOMAIN", "DETECT_POPUP", "DETECT_AD",
                        // 6. Logic / Control
                        "IF_CONDITION", "ELSE", "BRANCH", "BREAK", "CONTINUE", "RETRY_STEP", "RETRY_BLOCK", "STOP_EXECUTION", "DELAY_WITH_CONDITION",
                        // 7. Verification / Security
                        "WAIT_FOR_VERIFICATION", "CHECK_VERIFICATION_STATUS", "HANDLE_CAPTCHA", "SESSION_VALID_CHECK",
                        // 8. Variable & Data
                        "SET_VARIABLE", "GET_VARIABLE", "REPLACE_VARIABLE", "STORE_VALUE", "LOAD_VALUE", "PARSE_URL_PARAMS", "GENERATE_DYNAMIC_VALUE", "CLIPBOARD_COPY", "CLIPBOARD_PASTE", "STORE_COOKIES", "LOAD_COOKIES", "CLEAR_COOKIES",
                        // 9. Smart / AI
                        "SMART_CLICK", "SMART_SCROLL", "AUTO_FIX_STEP", "SUGGEST_NEXT_ACTION",
                        // 10. Error Handling
                        "ON_ERROR", "FALLBACK_ACTION", "RESTART_FLOW", "SKIP_STEP", "LOG_ERROR", "CAPTURE_DEBUG_INFO",
                        // 11. Debug / Monitoring
                        "TAKE_SCREENSHOT", "RECORD_LOG", "HIGHLIGHT_ELEMENT", "STEP_PREVIEW", "EXECUTION_TRACE", "SAY_TEXT", "TTS", "SPEAK",
                        // 12. System / Device
                        "OPEN_APP_PACKAGE", "CLOSE_APP", "CHECK_NETWORK", "ENABLE_WIFI", "DISABLE_WIFI", "CHECK_PERMISSION", "REQUEST_PERMISSION"
                    )
                    
                    if (type !in allowedTypes) {
                        throw IllegalArgumentException("Unsupported step type '$type' at step #${i+1}. Standard and advanced types are supported.")
                    }
                    
                    stepsList.add(WorkflowStep(id = id, type = type, target = target, value = value, timestamp = timestamp))
                }
            }
            
            return WorkflowDraft(name = name, initialUrl = initialUrl, steps = stepsList)
        } catch (e: Exception) {
            throw IllegalArgumentException(e.message ?: "Invalid JSON syntax or schema fields are missing.")
        }
    }
    
    fun getExampleTemplateJson(): String {
        val steps = listOf(
            WorkflowStep("s1", "NAVIGATE", "https://news.ycombinator.com"),
            WorkflowStep("s2", "CLICK", "a.storylink, td.title > span > a"),
            WorkflowStep("s3", "WAIT", "", "2000")
        )
        return exportToJson("HackerNews Reader Guide", "https://news.ycombinator.com", steps)
    }

    fun extractVariables(initialUrl: String, steps: List<WorkflowStep>): Map<String, String> {
        val vars = mutableMapOf<String, String>()
        val regex = """\{\{([A-Z0-9_]+)(?::([^}]+))?\}\}""".toRegex()
        
        fun parseText(text: String) {
            regex.findAll(text).forEach { match ->
                val key = match.groupValues[1]
                val defaultVal = match.groupValues[2]
                vars[key] = defaultVal.ifBlank { "Placeholder value" }
            }
        }
        
        parseText(initialUrl)
        steps.forEach { step ->
            parseText(step.target)
            parseText(step.value)
        }
        return vars
    }

    private fun cleanPlaceholderForCode(str: String): String {
        val regex = """\{\{([A-Z0-9_]+)(?::([^}]+))?\}\}""".toRegex()
        var cleaned = str
        regex.findAll(str).forEach { match ->
            val varName = match.groupValues[1]
            val defaultVal = match.groupValues[2]
            val replacement = if (defaultVal.isNotEmpty()) defaultVal else varName
            cleaned = cleaned.replace(match.value, replacement)
        }
        return cleaned
    }

    private fun formatStringWithPlaceholdersPython(str: String): String {
        val regex = """\{\{([A-Z0-9_]+)(?::([^}]+))?\}\}""".toRegex()
        if (!regex.containsMatchIn(str)) {
            return "\"${str.replace("\"", "\\\"")}\""
        }
        val matchResult = regex.matchEntire(str)
        if (matchResult != null) {
            return matchResult.groupValues[1]
        }
        var formatted = str
        regex.findAll(str).forEach { match ->
            val placeholder = match.value
            val varName = match.groupValues[1]
            formatted = formatted.replace(placeholder, "{$varName}")
        }
        return "f\"${formatted.replace("\"", "\\\"")}\""
    }

    private fun formatStringWithPlaceholdersJS(str: String): String {
        val regex = """\{\{([A-Z0-9_]+)(?::([^}]+))?\}\}""".toRegex()
        if (!regex.containsMatchIn(str)) {
            return "\"${str.replace("\"", "\\\"")}\""
        }
        val matchResult = regex.matchEntire(str)
        if (matchResult != null) {
            return matchResult.groupValues[1]
        }
        var formatted = str
        regex.findAll(str).forEach { match ->
            val placeholder = match.value
            val varName = match.groupValues[1]
            formatted = formatted.replace(placeholder, "\${$varName}")
        }
        return "`" + formatted.replace("`", "\\`") + "`"
    }

    private fun formatStringWithPlaceholdersKotlin(str: String): String {
        val regex = """\{\{([A-Z0-9_]+)(?::([^}]+))?\}\}""".toRegex()
        if (!regex.containsMatchIn(str)) {
            return "\"${str.replace("\"", "\\\"").replace("$", "\\$")}\""
        }
        val matchResult = regex.matchEntire(str)
        if (matchResult != null) {
            return matchResult.groupValues[1]
        }
        var formatted = str
        regex.findAll(str).forEach { match ->
            val placeholder = match.value
            val varName = match.groupValues[1]
            formatted = formatted.replace(placeholder, "$$varName")
        }
        return "\"${formatted.replace("\"", "\\\"")}\""
    }

    private fun formatStringWithPlaceholdersGo(str: String): String {
        val regex = """\{\{([A-Z0-9_]+)(?::([^}]+))?\}\}""".toRegex()
        if (!regex.containsMatchIn(str)) {
            return "\"${str.replace("\"", "\\\"")}\""
        }
        val matchResult = regex.matchEntire(str)
        if (matchResult != null) {
            return matchResult.groupValues[1]
        }
        var formatted = str
        val args = mutableListOf<String>()
        regex.findAll(str).forEach { match ->
            val placeholder = match.value
            val varName = match.groupValues[1]
            formatted = formatted.replace(placeholder, "%s")
            args.add(varName)
        }
        return if (args.isEmpty()) {
            "\"${formatted.replace("\"", "\\\"")}\""
        } else {
            "fmt.Sprintf(\"${formatted.replace("\"", "\\\"")}\", ${args.joinToString(", ")})"
        }
    }

    private fun resolveSelectorForLang(selector: String, lang: String): String {
        val trimmed = selector.trim()
        return when {
            trimmed.startsWith("TEXT:", ignoreCase = true) -> {
                val content = trimmed.substring(5).trim()
                if (lang == "Go") {
                    "xpath=//*[contains(text(), '$content')]"
                } else {
                    "text=$content"
                }
            }
            trimmed.startsWith("PLACEHOLDER:", ignoreCase = true) -> {
                val content = trimmed.substring(12).trim()
                "[placeholder*='$content']"
            }
            trimmed.startsWith("NAME:", ignoreCase = true) -> {
                val content = trimmed.substring(5).trim()
                "[name='$content']"
            }
            trimmed.startsWith("XPATH:", ignoreCase = true) -> {
                val content = trimmed.substring(6).trim()
                "xpath=$content"
            }
            else -> selector
        }
    }

    fun generatePythonPlaywright(name: String, initialUrl: String, steps: List<WorkflowStep>): String {
        val vars = extractVariables(initialUrl, steps)
        val sb = java.lang.StringBuilder()
        sb.append("#!/usr/bin/env python3\n")
        sb.append("# Generated by OurAuto Automation Suite\n")
        sb.append("# Recipe: ${name.replace("\n", " ")}\n\n")
        sb.append("import os\n")
        sb.append("import asyncio\n")
        sb.append("from playwright.async_api import async_playwright\n\n")
        
        sb.append("# --- Reusable Template Variables (override via env) ---\n")
        val cleanUrl = cleanPlaceholderForCode(initialUrl)
        sb.append("INITIAL_URL = os.getenv(\"INITIAL_URL\", \"$cleanUrl\")\n")
        vars.forEach { (key, defaultVal) ->
            sb.append("${key} = os.getenv(\"${key}\", \"${defaultVal.replace("\"", "\\\"")}\")\n")
        }
        sb.append("\nasync def main():\n")
        sb.append("    async with async_playwright() as p:\n")
        sb.append("        print(\"Launching Chromium browser...\")\n")
        sb.append("        browser = await p.chromium.launch(headless=False, args=[\"--disable-blink-features=AutomationControlled\"])\n")
        sb.append("        context = await browser.new_context(\n")
        sb.append("            viewport={\"width\": 1280, \"height\": 720},\n")
        sb.append("            user_agent=\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36\"\n")
        sb.append("        )\n")
        sb.append("        page = await context.new_page()\n")
        sb.append("        \n")
        sb.append("        print(f\"Navigating to initial landing page: {INITIAL_URL}\")\n")
        sb.append("        await page.goto(INITIAL_URL, wait_until=\"domcontentloaded\")\n")
        sb.append("        \n")
        
        steps.forEachIndexed { index, step ->
            sb.append("        # Step ${index + 1}: ${step.type}\n")
            val targetExpr = formatStringWithPlaceholdersPython(resolveSelectorForLang(step.target, "Python"))
            val valExpr = formatStringWithPlaceholdersPython(step.value)
            sb.append("        try:\n")
            when (step.type.uppercase()) {
                "NAVIGATE", "OPEN_LINK", "OPEN_URL" -> {
                    sb.append("            print(f\"Navigating to: {INITIAL_URL if ($targetExpr) == '' else ($targetExpr)}\")\n")
                    sb.append("            await page.goto($targetExpr or INITIAL_URL, wait_until=\"domcontentloaded\", timeout=15000)\n")
                }
                "CLICK", "SMART_CLICK" -> {
                    sb.append("            print(f\"Clicking element: {$targetExpr}\")\n")
                    sb.append("            loc = page.locator($targetExpr).first\n")
                    sb.append("            await loc.wait_for(state=\"visible\", timeout=5000)\n")
                    sb.append("            await loc.scroll_into_view_if_needed()\n")
                    sb.append("            await loc.click(timeout=5000)\n")
                }
                "DOUBLE_CLICK" -> {
                    sb.append("            print(f\"Double clicking element: {$targetExpr}\")\n")
                    sb.append("            loc = page.locator($targetExpr).first\n")
                    sb.append("            await loc.wait_for(state=\"visible\", timeout=5000)\n")
                    sb.append("            await loc.dblclick(timeout=5000)\n")
                }
                "INPUT", "INPUT_TEXT" -> {
                    sb.append("            print(f\"Typing value into element: {$targetExpr}\")\n")
                    sb.append("            loc = page.locator($targetExpr).first\n")
                    sb.append("            await loc.wait_for(state=\"visible\", timeout=5000)\n")
                    sb.append("            await loc.fill(\"\")\n")
                    sb.append("            await loc.type($valExpr, delay=50)\n")
                }
                "CLEAR_TEXT" -> {
                    sb.append("            print(f\"Clearing text from: {$targetExpr}\")\n")
                    sb.append("            loc = page.locator($targetExpr).first\n")
                    sb.append("            await loc.wait_for(state=\"visible\", timeout=5000)\n")
                    sb.append("            await loc.fill(\"\")\n")
                }
                "SELECT_OPTION" -> {
                    sb.append("            print(f\"Selecting option {$valExpr} in: {$targetExpr}\")\n")
                    sb.append("            loc = page.locator($targetExpr).first\n")
                    sb.append("            await loc.wait_for(state=\"visible\", timeout=5000)\n")
                    sb.append("            await loc.select_option(value=$valExpr)\n")
                }
                "WAIT", "WAIT_FOR_CONDITION" -> {
                    val ms = step.value.toDoubleOrNull() ?: 2000.0
                    sb.append("            print(\"Waiting for ${ms / 1000.0} seconds...\")\n")
                    sb.append("            await asyncio.sleep(${ms / 1000.0})\n")
                }
                "SCREENSHOT", "TAKE_SCREENSHOT" -> {
                    sb.append("            print(\"Taking screenshot script_screenshot_${index + 1}.png\")\n")
                    sb.append("            await page.screenshot(path=\"script_screenshot_${index + 1}.png\")\n")
                }
                "BACK", "GO_BACK" -> {
                    sb.append("            print(\"Going back in browser history...\")\n")
                    sb.append("            await page.go_back()\n")
                }
                "FORWARD", "GO_FORWARD" -> {
                    sb.append("            print(\"Going forward in browser history...\")\n")
                    sb.append("            await page.go_forward()\n")
                }
                "SCROLL_DOWN" -> {
                    sb.append("            print(\"Scrolling page down...\")\n")
                    sb.append("            await page.evaluate(\"window.scrollBy(0, window.innerHeight)\")\n")
                }
                "SCROLL_UP" -> {
                    sb.append("            print(\"Scrolling page up...\")\n")
                    sb.append("            await page.evaluate(\"window.scrollBy(0, -window.innerHeight)\")\n")
                }
                "SCROLL_UNTIL_ELEMENT_VISIBLE" -> {
                    sb.append("            print(f\"Scrolling until element is visible: {$targetExpr}\")\n")
                    sb.append("            loc = page.locator($targetExpr).first\n")
                    sb.append("            await loc.scroll_into_view_if_needed(timeout=5000)\n")
                }
                "GET_ELEMENT_TEXT" -> {
                    sb.append("            loc = page.locator($targetExpr).first\n")
                    sb.append("            txt = await loc.inner_text()\n")
                    sb.append("            print(f\"Retrieved text from {$targetExpr}: {txt}\")\n")
                }
                "GET_CURRENT_URL" -> {
                    sb.append("            print(f\"Current URL is: {page.url}\")\n")
                }
                else -> {
                    sb.append("            print(\"Step action '${step.type}' passed as no-op.\")\n")
                }
            }
            sb.append("        except Exception as e:\n")
            sb.append("            print(f\"[STEP ERROR] Step ${index + 1} (${step.type}) failed: {e}\")\n")
            sb.append("        \n")
        }
        
        sb.append("        print(\"Playbook script completed successfully.\")\n")
        sb.append("        await context.close()\n")
        sb.append("        await browser.close()\n\n")
        sb.append("if __name__ == \"__main__\":\n")
        sb.append("    asyncio.run(main())\n")
        return sb.toString()
    }

    fun generateJSPlaywright(name: String, initialUrl: String, steps: List<WorkflowStep>): String {
        val vars = extractVariables(initialUrl, steps)
        val sb = java.lang.StringBuilder()
        sb.append("/**\n")
        sb.append(" * Generated by OurAuto Automation Suite\n")
        sb.append(" * Recipe: ${name.replace("\n", " ")}\n")
        sb.append(" */\n\n")
        sb.append("const { chromium } = require('playwright');\n\n")
        
        sb.append("// --- Reusable Template Variables (override via env) ---\n")
        val cleanUrl = cleanPlaceholderForCode(initialUrl)
        sb.append("const INITIAL_URL = process.env.INITIAL_URL || \"$cleanUrl\";\n")
        vars.forEach { (key, defaultVal) ->
            sb.append("const ${key} = process.env.${key} || \"${defaultVal.replace("\"", "\\\"")}\";\n")
        }
        sb.append("\nasync function main() {\n")
        sb.append("    console.log(\"Launching Chromium...\");\n")
        sb.append("    const browser = await chromium.launch({ headless: false, args: ['--disable-blink-features=AutomationControlled'] });\n")
        sb.append("    const context = await browser.newContext({\n")
        sb.append("        viewport: { width: 1280, height: 720 },\n")
        sb.append("        userAgent: \"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36\"\n")
        sb.append("    });\n")
        sb.append("    const page = await context.newPage();\n")
        sb.append("    \n")
        sb.append("    console.log(`Navigating to initial page: \${INITIAL_URL}`);\n")
        sb.append("    await page.goto(INITIAL_URL, { waitUntil: 'domcontentloaded' });\n")
        sb.append("    \n")
        
        steps.forEachIndexed { index, step ->
            sb.append("    // Step ${index + 1}: ${step.type}\n")
            val targetExpr = formatStringWithPlaceholdersJS(resolveSelectorForLang(step.target, "Node.js"))
            val valExpr = formatStringWithPlaceholdersJS(step.value)
            sb.append("    try {\n")
            when (step.type.uppercase()) {
                "NAVIGATE", "OPEN_LINK", "OPEN_URL" -> {
                    sb.append("        console.log(`Navigating to: \${$targetExpr || INITIAL_URL}`);\n")
                    sb.append("        await page.goto($targetExpr || INITIAL_URL, { waitUntil: 'domcontentloaded', timeout: 15000 });\n")
                }
                "CLICK", "SMART_CLICK" -> {
                    sb.append("        console.log(`Clicking element: \${$targetExpr}`);\n")
                    sb.append("        const loc = page.locator($targetExpr).first();\n")
                    sb.append("        await loc.waitFor({ state: 'visible', timeout: 5000 });\n")
                    sb.append("        await loc.scrollIntoViewIfNeeded();\n")
                    sb.append("        await loc.click({ timeout: 5000 });\n")
                }
                "DOUBLE_CLICK" -> {
                    sb.append("        console.log(`Double clicking element: \${$targetExpr}`);\n")
                    sb.append("        const loc = page.locator($targetExpr).first();\n")
                    sb.append("        await loc.waitFor({ state: 'visible', timeout: 5000 });\n")
                    sb.append("        await loc.dblclick({ timeout: 5000 });\n")
                }
                "INPUT", "INPUT_TEXT" -> {
                    sb.append("        console.log(`Typing value into element: \${$targetExpr}`);\n")
                    sb.append("        const loc = page.locator($targetExpr).first();\n")
                    sb.append("        await loc.waitFor({ state: 'visible', timeout: 5000 });\n")
                    sb.append("        await loc.fill('');\n")
                    sb.append("        await loc.type($valExpr, { delay: 50 });\n")
                }
                "CLEAR_TEXT" -> {
                    sb.append("        console.log(`Clearing text from: \${$targetExpr}`);\n")
                    sb.append("        const loc = page.locator($targetExpr).first();\n")
                    sb.append("        await loc.waitFor({ state: 'visible', timeout: 5000 });\n")
                    sb.append("        await loc.fill('');\n")
                }
                "SELECT_OPTION" -> {
                    sb.append("        console.log(`Selecting option \${$valExpr} in: \${$targetExpr}`);\n")
                    sb.append("        const loc = page.locator($targetExpr).first();\n")
                    sb.append("        await loc.waitFor({ state: 'visible', timeout: 5000 });\n")
                    sb.append("        await loc.selectOption({ value: $valExpr });\n")
                }
                "WAIT", "WAIT_FOR_CONDITION" -> {
                    val ms = step.value.toDoubleOrNull() ?: 2000.0
                    sb.append("        console.log(`Waiting for ${ms / 1000.0} seconds...`);\n")
                    sb.append("        await new Promise(resolve => setTimeout(resolve, $ms));\n")
                }
                "SCREENSHOT", "TAKE_SCREENSHOT" -> {
                    sb.append("        console.log('Taking screenshot...');\n")
                    sb.append("        await page.screenshot({ path: `screenshot_${index + 1}.png` });\n")
                }
                "BACK", "GO_BACK" -> {
                    sb.append("        console.log('Going back...');\n")
                    sb.append("        await page.goBack();\n")
                }
                "FORWARD", "GO_FORWARD" -> {
                    sb.append("        console.log('Going forward...');\n")
                    sb.append("        await page.goForward();\n")
                }
                "SCROLL_DOWN" -> {
                    sb.append("        console.log('Scrolling page down...');\n")
                    sb.append("        await page.evaluate(() => window.scrollBy(0, window.innerHeight));\n")
                }
                "SCROLL_UP" -> {
                    sb.append("        console.log('Scrolling page up...');\n")
                    sb.append("        await page.evaluate(() => window.scrollBy(0, -window.innerHeight));\n")
                }
                "SCROLL_UNTIL_ELEMENT_VISIBLE" -> {
                    sb.append("        console.log(`Scrolling until element: \${$targetExpr}`);\n")
                    sb.append("        const loc = page.locator($targetExpr).first();\n")
                    sb.append("        await loc.scrollIntoViewIfNeeded({ timeout: 5000 });\n")
                }
                "GET_ELEMENT_TEXT" -> {
                    sb.append("        const loc = page.locator($targetExpr).first();\n")
                    sb.append("        const txt = await loc.innerText();\n")
                    sb.append("        console.log(`Retrieved text from \${$targetExpr}: \${txt}`);\n")
                }
                "GET_CURRENT_URL" -> {
                    sb.append("        console.log(`Current URL is: \${page.url()}`);\n")
                }
                else -> {
                    sb.append("        console.log(`Step action '${step.type}' passed as system no-op.`);\n")
                }
            }
            sb.append("    } catch (err) {\n")
            sb.append("        console.error(`[STEP ERROR] Step ${index + 1} (${step.type}) failed:`, err.message);\n")
            sb.append("    }\n")
            sb.append("    \n")
        }
        
        sb.append("    console.log(\"Node.js Playwright script finished.\");\n")
        sb.append("    await context.close();\n")
        sb.append("    await browser.close();\n")
        sb.append("}\n\n")
        sb.append("main().catch(console.error);\n")
        return sb.toString()
    }

    fun generateKotlinPlaywright(name: String, initialUrl: String, steps: List<WorkflowStep>): String {
        val vars = extractVariables(initialUrl, steps)
        val sb = java.lang.StringBuilder()
        sb.append("import com.microsoft.playwright.*\n")
        sb.append("import java.nio.file.Paths\n\n")
        sb.append("/**\n")
        sb.append(" * Generated by OurAuto Automation Suite\n")
        sb.append(" * Run with Kotlin/Java JVM and Playwright SDK\n")
        sb.append(" */\n")
        sb.append("fun main() {\n")
        sb.append("    val env = System.getenv()\n")
        val cleanUrl = cleanPlaceholderForCode(initialUrl)
        sb.append("    val INITIAL_URL = env.getOrDefault(\"INITIAL_URL\", \"$cleanUrl\")\n")
        vars.forEach { (key, defaultVal) ->
            sb.append("    val ${key} = env.getOrDefault(\"${key}\", \"${defaultVal.replace("\"", "\\\"")}\")\n")
        }
        sb.append("\n")
        sb.append("    Playwright.create().use { playwright ->\n")
        sb.append("        println(\"Initializing JVM Playwright instance...\")\n")
        sb.append("        val browser = playwright.chromium().launch(\n")
        sb.append("            BrowserType.LaunchOptions().setHeadless(false)\n")
        sb.append("                .setArgs(listOf(\"--disable-blink-features=AutomationControlled\"))\n")
        sb.append("        )\n")
        sb.append("        val context = browser.newContext(\n")
        sb.append("            Browser.NewContextOptions().setViewportSize(1280, 720)\n")
        sb.append("                .setUserAgent(\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36\")\n")
        sb.append("        )\n")
        sb.append("        val page = context.newPage()\n\n")
        sb.append("        println(\"Opening landing url...\")\n")
        sb.append("        page.navigate(INITIAL_URL)\n\n")
        
        steps.forEachIndexed { index, step ->
            sb.append("        // Step ${index + 1}: ${step.type}\n")
            val targetExpr = formatStringWithPlaceholdersKotlin(resolveSelectorForLang(step.target, "Kotlin"))
            val valExpr = formatStringWithPlaceholdersKotlin(step.value)
            sb.append("        try {\n")
            when (step.type.uppercase()) {
                "NAVIGATE", "OPEN_LINK", "OPEN_URL" -> {
                    sb.append("            println(\"Navigating to: \" + $targetExpr)\n")
                    sb.append("            page.navigate($targetExpr, Page.NavigateOptions().setTimeout(15000.0))\n")
                }
                "CLICK", "SMART_CLICK" -> {
                    sb.append("            println(\"Clicking element: \" + $targetExpr)\n")
                    sb.append("            val loc = page.locator($targetExpr).first()\n")
                    sb.append("            loc.waitFor(Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE).setTimeout(5000.0))\n")
                    sb.append("            loc.scrollIntoViewIfNeeded()\n")
                    sb.append("            loc.click(Locator.ClickOptions().setTimeout(5000.0))\n")
                }
                "DOUBLE_CLICK" -> {
                    sb.append("            println(\"Double clicking element: \" + $targetExpr)\n")
                    sb.append("            val loc = page.locator($targetExpr).first()\n")
                    sb.append("            loc.waitFor(Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE).setTimeout(5000.0))\n")
                    sb.append("            loc.dblclick(Locator.DblclickOptions().setTimeout(5000.0))\n")
                }
                "INPUT", "INPUT_TEXT" -> {
                    sb.append("            println(\"Typing value into element: \" + $targetExpr)\n")
                    sb.append("            val loc = page.locator($targetExpr).first()\n")
                    sb.append("            loc.waitFor(Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE).setTimeout(5000.0))\n")
                    sb.append("            loc.fill(\"\")\n")
                    sb.append("            loc.type($valExpr, Locator.TypeOptions().setDelay(50.0))\n")
                }
                "CLEAR_TEXT" -> {
                    sb.append("            println(\"Clearing text from: \" + $targetExpr)\n")
                    sb.append("            val loc = page.locator($targetExpr).first()\n")
                    sb.append("            loc.waitFor(Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE).setTimeout(5000.0))\n")
                    sb.append("            loc.fill(\"\")\n")
                }
                "SELECT_OPTION" -> {
                    sb.append("            println(\"Selecting option \" + $valExpr + \" in: \" + $targetExpr)\n")
                    sb.append("            val loc = page.locator($targetExpr).first()\n")
                    sb.append("            loc.waitFor(Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE).setTimeout(5000.0))\n")
                    sb.append("            loc.selectOption($valExpr)\n")
                }
                "WAIT", "WAIT_FOR_CONDITION" -> {
                    val ms = step.value.toLongOrNull() ?: 2000L
                    sb.append("            println(\"Waiting for ${ms / 1000.0} seconds...\")\n")
                    sb.append("            Thread.sleep($ms)\n")
                }
                "SCREENSHOT", "TAKE_SCREENSHOT" -> {
                    sb.append("            println(\"Taking screenshot...\")\n")
                    sb.append("            page.screenshot(Page.ScreenshotOptions().setPath(Paths.get(\"screenshot_${index + 1}.png\")))\n")
                }
                "BACK", "GO_BACK" -> {
                    sb.append("            println(\"Going back...\")\n")
                    sb.append("            page.goBack()\n")
                }
                "FORWARD", "GO_FORWARD" -> {
                    sb.append("            println(\"Going forward...\")\n")
                    sb.append("            page.goForward()\n")
                }
                "SCROLL_DOWN" -> {
                    sb.append("            println(\"Scrolling page down...\")\n")
                    sb.append("            page.evaluate(\"() => window.scrollBy(0, window.innerHeight)\")\n")
                }
                "SCROLL_UP" -> {
                    sb.append("            println(\"Scrolling page up...\")\n")
                    sb.append("            page.evaluate(\"() => window.scrollBy(0, -window.innerHeight)\")\n")
                }
                "SCROLL_UNTIL_ELEMENT_VISIBLE" -> {
                    sb.append("            println(\"Scrolling until element visible: \" + $targetExpr)\n")
                    sb.append("            val loc = page.locator($targetExpr).first()\n")
                    sb.append("            loc.scrollIntoViewIfNeeded()\n")
                }
                "GET_ELEMENT_TEXT" -> {
                    sb.append("            val loc = page.locator($targetExpr).first()\n")
                    sb.append("            val txt = loc.innerText()\n")
                    sb.append("            println(\"Retrieved text from \" + $targetExpr + \": \" + txt)\n")
                }
                "GET_CURRENT_URL" -> {
                    sb.append("            println(\"Current URL is: \" + page.url())\n")
                }
                else -> {
                    sb.append("            println(\"Step action '${step.type}' passed as system no-op.\")\n")
                }
            }
            sb.append("        } catch (e: Exception) {\n")
            sb.append("            println(\"[STEP ERROR] Step ${index + 1} (${step.type}) failed: \" + e.getMessage())\n")
            sb.append("        }\n")
            sb.append("        \n")
        }
        
        sb.append("        println(\"Kotlin Playwright playbook sequence completed.\")\n")
        sb.append("        context.close()\n")
        sb.append("        browser.close()\n")
        sb.append("    }\n")
        sb.append("}\n")
        return sb.toString()
    }

    fun generateGoRod(name: String, initialUrl: String, steps: List<WorkflowStep>): String {
        val vars = extractVariables(initialUrl, steps)
        val sb = java.lang.StringBuilder()
        sb.append("package main\n\n")
        sb.append("import (\n")
        sb.append("\t\"fmt\"\n")
        sb.append("\t\"os\"\n")
        sb.append("\t\"time\"\n")
        sb.append("\t\"github.com/go-rod/rod\"\n")
        sb.append("\t\"github.com/go-rod/rod/lib/launcher\"\n")
        sb.append(")\n\n")
        sb.append("/**\n")
        sb.append(" * Generated by OurAuto Automation Suite\n")
        sb.append(" * Built for high-efficiency Go Rod framework\n")
        sb.append(" */\n")
        sb.append("func main() {\n")
        val cleanUrl = cleanPlaceholderForCode(initialUrl)
        sb.append("\tINITIAL_URL := os.Getenv(\"INITIAL_URL\")\n")
        sb.append("\tif INITIAL_URL == \"\" {\n")
        sb.append("\t\tINITIAL_URL = \"$cleanUrl\"\n")
        sb.append("\t}\n")
        
        vars.forEach { (key, defaultVal) ->
            sb.append("\t${key} := os.Getenv(\"${key}\")\n")
            sb.append("\tif ${key} == \"\" {\n")
            sb.append("\t\t${key} = \"${defaultVal.replace("\"", "\\\"")}\"\n")
            sb.append("\t}\n")
        }
        
        sb.append("\n\tfmt.Println(\"Launching browser instance...\")\n")
        sb.append("\tl := launcher.New().\n")
        sb.append("\t\tSet(\"disable-blink-features\", \"AutomationControlled\").\n")
        sb.append("\t\tSet(\"user-agent\", \"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36\").\n")
        sb.append("\t\tMustLaunch()\n\n")
        sb.append("\tbrowser := rod.New().ControlURL(l).MustConnect()\n")
        sb.append("\tdefer browser.MustClose()\n\n")
        sb.append("\tfmt.Printf(\"Opening target page: %s\\n\", INITIAL_URL)\n")
        sb.append("\tpage := browser.MustPage(INITIAL_URL)\n")
        sb.append("\tpage.MustWaitDOMStable()\n\n")
        
        steps.forEachIndexed { index, step ->
            sb.append("\t// Step ${index + 1}: ${step.type}\n")
            val targetExpr = formatStringWithPlaceholdersGo(resolveSelectorForLang(step.target, "Go"))
            val valExpr = formatStringWithPlaceholdersGo(step.value)
            sb.append("\tfunc() {\n")
            sb.append("\t\tdefer func() {\n")
            sb.append("\t\t\tif r := recover(); r != nil {\n")
            sb.append("\t\t\t\tfmt.Printf(\"[STEP ERROR] Step ${index + 1} (${step.type}) failed: %v\\n\", r)\n")
            sb.append("\t\t\t}\n")
            sb.append("\t\t}()\n\n")
            when (step.type.uppercase()) {
                "NAVIGATE", "OPEN_LINK", "OPEN_URL" -> {
                    sb.append("\t\tfmt.Printf(\"Navigating to: %s\\n\", $targetExpr)\n")
                    sb.append("\t\tpage.Timeout(15 * time.Second).MustNavigate($targetExpr)\n")
                    sb.append("\t\tpage.MustWaitDOMStable()\n")
                }
                "CLICK", "SMART_CLICK" -> {
                    sb.append("\t\tfmt.Printf(\"Clicking element: %s\\n\", $targetExpr)\n")
                    sb.append("\t\tel := page.Timeout(5 * time.Second).MustElement($targetExpr)\n")
                    sb.append("\t\tel.MustScrollIntoView()\n")
                    sb.append("\t\tel.MustClick()\n")
                }
                "DOUBLE_CLICK" -> {
                    sb.append("\t\tfmt.Printf(\"Double clicking element: %s\\n\", $targetExpr)\n")
                    sb.append("\t\tel := page.Timeout(5 * time.Second).MustElement($targetExpr)\n")
                    sb.append("\t\tel.MustScrollIntoView()\n")
                    sb.append("\t\tel.MustDoubleClick()\n")
                }
                "INPUT", "INPUT_TEXT" -> {
                    sb.append("\t\tfmt.Printf(\"Typing into element: %s\\n\", $targetExpr)\n")
                    sb.append("\t\tel := page.Timeout(5 * time.Second).MustElement($targetExpr)\n")
                    sb.append("\t\tel.MustScrollIntoView()\n")
                    sb.append("\t\tel.MustSelectAllText().MustInput($valExpr)\n")
                }
                "CLEAR_TEXT" -> {
                    sb.append("\t\tfmt.Printf(\"Clearing text from: %s\\n\", $targetExpr)\n")
                    sb.append("\t\tel := page.Timeout(5 * time.Second).MustElement($targetExpr)\n")
                    sb.append("\t\tel.MustSelectAllText().MustInput(\"\")\n")
                }
                "SELECT_OPTION" -> {
                    sb.append("\t\tfmt.Printf(\"Selecting option %s in dropdown: %s\\n\", $valExpr, $targetExpr)\n")
                    sb.append("\t\tpage.Timeout(5 * time.Second).MustElement($targetExpr).MustSelect($valExpr)\n")
                }
                "WAIT", "WAIT_FOR_CONDITION" -> {
                    val ms = step.value.toLongOrNull() ?: 2000L
                    sb.append("\t\tfmt.Printf(\"Waiting for %d ms...\\n\", $ms)\n")
                    sb.append("\t\ttime.Sleep($ms * time.Millisecond)\n")
                }
                "SCREENSHOT", "TAKE_SCREENSHOT" -> {
                    sb.append("\t\tfmt.Println(\"Taking screenshot...\")\n")
                    sb.append("\t\tpage.MustScreenshot(fmt.Sprintf(\"screenshot_%d.png\", ${index + 1}))\n")
                }
                "BACK", "GO_BACK" -> {
                    sb.append("\t\tfmt.Println(\"Going back...\")\n")
                    sb.append("\t\tpage.MustNavigateBack()\n")
                }
                "FORWARD", "GO_FORWARD" -> {
                    sb.append("\t\tfmt.Println(\"Going forward...\")\n")
                    sb.append("\t\tpage.MustNavigateForward()\n")
                }
                "SCROLL_DOWN" -> {
                    sb.append("\t\tfmt.Println(\"Scrolling page down...\")\n")
                    sb.append("\t\tpage.MustEval(\"() => window.scrollBy(0, window.innerHeight)\")\n")
                }
                "SCROLL_UP" -> {
                    sb.append("\t\tfmt.Println(\"Scrolling page up...\")\n")
                    sb.append("\t\tpage.MustEval(\"() => window.scrollBy(0, -window.innerHeight)\")\n")
                }
                "SCROLL_UNTIL_ELEMENT_VISIBLE" -> {
                    sb.append("\t\tfmt.Printf(\"Scrolling until element is visible: %s\\n\", $targetExpr)\n")
                    sb.append("\t\tpage.Timeout(5 * time.Second).MustElement($targetExpr).MustScrollIntoView()\n")
                }
                "GET_ELEMENT_TEXT" -> {
                    sb.append("\t\tel := page.Timeout(5 * time.Second).MustElement($targetExpr)\n")
                    sb.append("\t\ttxt := el.MustText()\n")
                    sb.append("\t\tfmt.Printf(\"Retrieved text from %s: %s\\n\", $targetExpr, txt)\n")
                }
                "GET_CURRENT_URL" -> {
                    sb.append("\t\tcurrURL := page.MustInfo().URL\n")
                    sb.append("\t\tfmt.Printf(\"Current URL is: %s\\n\", currURL)\n")
                }
                else -> {
                    sb.append("\t\tfmt.Printf(\"Step action '${step.type}' passed as system no-op.\\n\")\n")
                }
            }
            sb.append("\t}()\n")
            sb.append("\t\n")
        }
        
        sb.append("\tfmt.Println(\"Rod automated script completed.\")\n")
        sb.append("}\n")
        return sb.toString()
    }
}
