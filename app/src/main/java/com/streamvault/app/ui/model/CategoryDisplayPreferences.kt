package com.streamvault.app.ui.model

import com.streamvault.domain.model.Category
import com.streamvault.domain.model.CategorySortMode

fun applyProviderCategoryDisplayPreferences(
    categories: List<Category>,
    hiddenCategoryIds: Set<Long>,
    sortMode: CategorySortMode
): List<Category> {
    return categories.filterNot { it.id in hiddenCategoryIds }
}
