package com.example.ui

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class MyAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("MyAccessibilityService", "Accessibility service connected successfully!")
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Can monitor runtime layout hierarchies for cross-app automation recording & execution
    }

    override fun onInterrupt() {
        Log.e("MyAccessibilityService", "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    companion object {
        private var instance: MyAccessibilityService? = null

        fun isServiceRunning(): Boolean {
            return instance != null
        }

        fun performClickAt(x: Float, y: Float, callback: (Boolean) -> Unit = {}) {
            val service = instance ?: return callback(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val path = Path().apply {
                    moveTo(x, y)
                }
                val builder = GestureDescription.Builder()
                val stroke = GestureDescription.StrokeDescription(path, 0L, 50L)
                builder.addStroke(stroke)
                service.dispatchGesture(builder.build(), object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        super.onCompleted(gestureDescription)
                        callback(true)
                    }
                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        super.onCancelled(gestureDescription)
                        callback(false)
                    }
                }, null)
            } else {
                callback(false)
            }
        }

        fun performScroll(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long = 300L, callback: (Boolean) -> Unit = {}) {
            val service = instance ?: return callback(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val path = Path().apply {
                    moveTo(startX, startY)
                    lineTo(endX, endY)
                }
                val builder = GestureDescription.Builder()
                val stroke = GestureDescription.StrokeDescription(path, 0L, duration)
                builder.addStroke(stroke)
                service.dispatchGesture(builder.build(), object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        super.onCompleted(gestureDescription)
                        callback(true)
                    }
                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        super.onCancelled(gestureDescription)
                        callback(false)
                    }
                }, null)
            } else {
                callback(false)
            }
        }

        fun performGlobalAction(actionId: Int, callback: (Boolean) -> Unit = {}) {
            val service = instance ?: return callback(false)
            val result = service.performGlobalAction(actionId)
            callback(result)
        }

        fun findAndClickByText(text: String, callback: (Boolean) -> Unit = {}) {
            val service = instance ?: return callback(false)
            val root = service.rootInActiveWindow ?: return callback(false)
            val nodes = root.findAccessibilityNodeInfosByText(text)
            if (nodes.isNullOrEmpty()) { root.recycle(); return callback(false) }
            val node = nodes.first()
            val clickable = findClickableAncestor(node) ?: node
            val result = clickable.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
            nodes.forEach { runCatching { it.recycle() } }
            root.recycle()
            callback(result)
        }

        fun findAndClickById(viewId: String, callback: (Boolean) -> Unit = {}) {
            val service = instance ?: return callback(false)
            val root = service.rootInActiveWindow ?: return callback(false)
            val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
            if (nodes.isNullOrEmpty()) { root.recycle(); return callback(false) }
            val node = nodes.first()
            val clickable = findClickableAncestor(node) ?: node
            val result = clickable.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
            nodes.forEach { runCatching { it.recycle() } }
            root.recycle()
            callback(result)
        }

        fun findAndClickByDesc(desc: String, callback: (Boolean) -> Unit = {}) {
            val service = instance ?: return callback(false)
            val root = service.rootInActiveWindow ?: return callback(false)
            val node = findNodeByContentDesc(root, desc)
            if (node == null) { root.recycle(); return callback(false) }
            val clickable = findClickableAncestor(node) ?: node
            val result = clickable.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
            runCatching { node.recycle() }
            root.recycle()
            callback(result)
        }

        fun scrollToText(text: String, callback: (Boolean) -> Unit = {}) {
            val service = instance ?: return callback(false)
            val root = service.rootInActiveWindow ?: return callback(false)
            val found = root.findAccessibilityNodeInfosByText(text)
            if (!found.isNullOrEmpty()) {
                found.first().performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
                found.forEach { runCatching { it.recycle() } }
                root.recycle()
                return callback(true)
            }
            val scrollable = findScrollableNode(root)
            val scrolled = scrollable?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) ?: false
            runCatching { scrollable?.recycle() }
            root.recycle()
            callback(scrolled)
        }

        fun launchApp(packageName: String, context: android.content.Context, callback: (Boolean) -> Unit = {}) {
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                    ?: return callback(false)
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                callback(true)
            } catch (e: Exception) { callback(false) }
        }

        private fun findClickableAncestor(node: android.view.accessibility.AccessibilityNodeInfo): android.view.accessibility.AccessibilityNodeInfo? {
            var cur: android.view.accessibility.AccessibilityNodeInfo? = node
            while (cur != null) {
                if (cur.isClickable) return cur
                cur = cur.parent
            }
            return null
        }

        private fun findNodeByContentDesc(root: android.view.accessibility.AccessibilityNodeInfo, desc: String): android.view.accessibility.AccessibilityNodeInfo? {
            if (root.contentDescription?.toString()?.contains(desc, ignoreCase = true) == true) return root
            for (i in 0 until root.childCount) {
                val child = root.getChild(i) ?: continue
                val result = findNodeByContentDesc(child, desc)
                if (result != null) return result
                runCatching { child.recycle() }
            }
            return null
        }

        private fun findScrollableNode(root: android.view.accessibility.AccessibilityNodeInfo): android.view.accessibility.AccessibilityNodeInfo? {
            if (root.isScrollable) return root
            for (i in 0 until root.childCount) {
                val child = root.getChild(i) ?: continue
                val result = findScrollableNode(child)
                if (result != null) return result
                runCatching { child.recycle() }
            }
            return null
        }
    }
}
