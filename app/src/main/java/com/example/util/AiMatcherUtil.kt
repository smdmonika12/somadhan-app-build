package com.example.util

import com.example.data.entity.CategoryEntity
import com.example.data.entity.ProblemEntity

object AiMatcherUtil {

    /**
     * Scores how well a text matches a category's keywords and description.
     */
    fun getCategoryMatchScore(text: String, category: CategoryEntity): Double {
        if (text.isBlank()) return 0.0
        val normalizedText = text.lowercase()
        val keywords = category.keywords.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
        
        var matchCount = 0.0
        for (kw in keywords) {
            if (normalizedText.contains(kw)) {
                // Longer keywords give higher confidence
                matchCount += 1.0 + (kw.length * 0.1)
            }
        }

        // Check if category name matches
        if (normalizedText.contains(category.nameBangla.lowercase())) matchCount += 3.0
        if (normalizedText.contains(category.nameEnglish.lowercase())) matchCount += 3.0

        return matchCount
    }

    /**
     * Suggests the best matched category ID based on the title and description.
     */
    fun suggestCategory(title: String, description: String, categories: List<CategoryEntity>): CategoryEntity? {
        val combinedText = "$title $description"
        if (combinedText.trim().length < 3) return null

        var bestCategory: CategoryEntity? = null
        var maxScore = 0.0

        for (cat in categories) {
            val score = getCategoryMatchScore(combinedText, cat)
            if (score > maxScore && score >= 1.0) {
                maxScore = score
                bestCategory = cat
            }
        }
        return bestCategory
    }

    /**
     * AI-based similar search for Solver.
     * Checks title, description, category keywords similarity for a search query.
     */
    fun searchSimilarProblems(
        query: String,
        problems: List<ProblemEntity>,
        categories: List<CategoryEntity>
    ): List<ProblemEntity> {
        if (query.isBlank()) return problems
        val normalizedQuery = query.lowercase().trim()
        val queryTokens = normalizedQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }

        // Find which categories relate to the query
        val relatedCategoryIds = categories.filter { cat ->
            getCategoryMatchScore(normalizedQuery, cat) > 0.5 ||
                    cat.nameBangla.lowercase().contains(normalizedQuery) ||
                    cat.nameEnglish.lowercase().contains(normalizedQuery)
        }.map { it.id }.toSet()

        return problems.mapNotNull { problem ->
            var score = 0.0
            val titleLower = problem.title.lowercase()
            val descLower = problem.description.lowercase()
            val catNameLower = problem.categoryName.lowercase()

            // Exact query match
            if (titleLower.contains(normalizedQuery)) score += 5.0
            if (descLower.contains(normalizedQuery)) score += 3.0
            if (catNameLower.contains(normalizedQuery)) score += 4.0

            // Token overlap
            for (token in queryTokens) {
                if (titleLower.contains(token)) score += 2.0
                if (descLower.contains(token)) score += 1.0
                if (catNameLower.contains(token)) score += 2.0
            }

            // Semantic category match
            if (relatedCategoryIds.contains(problem.categoryId)) {
                score += 3.0
            }

            if (score > 0.0) Pair(problem, score) else null
        }
            .sortedByDescending { it.second }
            .map { it.first }
    }
}
