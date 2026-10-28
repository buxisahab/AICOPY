package com.buxi.music.models

import com.buxi.music.innertube.models.YTItem
import com.buxi.music.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
