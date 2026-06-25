package com.example.ui

internal fun cleanUrlForComparison(url: String?): String {
    if (url == null) return ""
    return url.trimEnd('/').lowercase()
        .removePrefix("https://www.")
        .removePrefix("http://www.")
        .removePrefix("https://")
        .removePrefix("http://")
}
