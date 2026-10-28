package com.musifly.android.models

import com.musifly.innertube.models.YTItem
import com.musifly.android.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
