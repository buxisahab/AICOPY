package com.musifly.android.ui.utils

/**
 * YouTube thumbnail quality levels
 */
enum class ThumbnailQuality(val fileName: String, val width: Int, val height: Int) {
    MAX_RES("maxresdefault.jpg", 1280, 720),
    SD("sddefault.jpg", 640, 480),
    HQ("hqdefault.jpg", 480, 360),
    MQ("mqdefault.jpg", 320, 180),
    DEFAULT("default.jpg", 120, 90)
}

/**
 * Extract YouTube video ID from various URL formats
 */
fun String.extractYouTubeVideoId(): String? {
    return when {
        contains("youtube.com/watch?v=") -> substringAfter("v=").substringBefore("&")
        contains("youtu.be/") -> substringAfter("youtu.be/").substringBefore("?")
        contains("youtube.com/embed/") -> substringAfter("embed/").substringBefore("?")
        contains("youtube.com/v/") -> substringAfter("v/").substringBefore("?")
        contains("img.youtube.com/vi/") -> substringAfter("/vi/").substringBefore("/")
        contains("i.ytimg.com/vi/") -> substringAfter("/vi/").substringBefore("/")
        // Check if it's already a video ID (11 characters)
        length == 11 && matches(Regex("[a-zA-Z0-9_-]{11}")) -> this
        else -> null
    }
}

/**
 * Get HD YouTube thumbnail URL (maxresdefault)
 */
fun getYouTubeHdThumbnailUrl(videoId: String): String {
    return "https://img.youtube.com/vi/$videoId/${ThumbnailQuality.MAX_RES.fileName}"
}

/**
 * Get list of fallback thumbnail URLs in quality order
 */
fun getYouTubeThumbnailUrls(videoId: String): List<String> {
    return listOf(
        "https://img.youtube.com/vi/$videoId/${ThumbnailQuality.MAX_RES.fileName}",
        "https://img.youtube.com/vi/$videoId/${ThumbnailQuality.HQ.fileName}",
        "https://img.youtube.com/vi/$videoId/${ThumbnailQuality.MQ.fileName}"
    )
}

/**
 * Convert any thumbnail URL to HD quality with fallback URLs
 * Returns a Pair of (primaryUrl, fallbackUrls)
 */
fun String.toHdThumbnailWithFallback(): Pair<String, List<String>> {
    val videoId = extractYouTubeVideoId()
    return if (videoId != null) {
        val urls = getYouTubeThumbnailUrls(videoId)
        urls.first() to urls.drop(1)
    } else {
        this to emptyList()
    }
}

/**
 * Convert any thumbnail URL to maxresdefault quality
 * Falls back to original URL if video ID cannot be extracted
 */
fun String.toHdThumbnail(): String {
    val videoId = extractYouTubeVideoId()
    return if (videoId != null) {
        getYouTubeHdThumbnailUrl(videoId)
    } else {
        this
    }
}

/**
 * Get the best available thumbnail for a video ID
 * Useful for generating HD thumbnail URLs from song/video IDs
 */
fun String.toVideoThumbnailUrl(quality: ThumbnailQuality = ThumbnailQuality.MAX_RES): String {
    return "https://img.youtube.com/vi/$this/${quality.fileName}"
}

fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this
    "https://lh3\\.googleusercontent\\.com/.*=w(\\d+)-h(\\d+).*".toRegex()
        .matchEntire(this)?.groupValues?.let { group ->
            val (W, H) = group.drop(1).map { it.toInt() }
            var w = width
            var h = height
            if (w != null && h == null) h = (w / W) * H
            if (w == null && h != null) w = (h / H) * W
            return "${split("=w")[0]}=w$w-h$h-p-l90-rj"
        }
    if (this matches "https://yt3\\.ggpht\\.com/.*=s(\\d+)".toRegex()) {
        return "$this-s${width ?: height}"
    }
    return this
}
