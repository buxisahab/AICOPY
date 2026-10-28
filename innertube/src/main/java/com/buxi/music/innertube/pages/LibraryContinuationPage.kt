package com.buxi.music.innertube.pages

import com.buxi.music.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
