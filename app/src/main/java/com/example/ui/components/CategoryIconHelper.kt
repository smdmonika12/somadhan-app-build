package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.School
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.SomadhanOrange

object CategoryIconHelper {
    fun getIcon(iconName: String): ImageVector {
        return when (iconName) {
            "Bolt" -> Icons.Default.Bolt
            "Plumbing" -> Icons.Default.Plumbing
            "Computer" -> Icons.Default.Computer
            "Laptop" -> Icons.Default.Laptop
            "AcUnit" -> Icons.Default.AcUnit
            "HomeRepairService" -> Icons.Default.HomeRepairService
            "DesignServices" -> Icons.Default.DesignServices
            "School" -> Icons.Default.School
            "Brush" -> Icons.Default.Brush
            "FormatPaint" -> Icons.Default.FormatPaint
            "LocalShipping" -> Icons.Default.LocalShipping
            "Palette" -> Icons.Default.Palette
            "Article" -> Icons.Default.Article
            "Code" -> Icons.Default.Code
            "Campaign" -> Icons.Default.Campaign
            "MovieFilter" -> Icons.Default.MovieFilter
            else -> Icons.Default.Build
        }
    }

    /**
     * Provides category-specific soft pastel container background and vibrant icon tint
     * strictly matching the modern reference UI aesthetic.
     */
    fun getCategoryColors(categoryName: String, iconName: String = ""): Pair<Color, Color> {
        return when {
            categoryName.contains("ইলেকট্র") || iconName == "Bolt" ->
                Pair(Color(0xFFFFF4E5), Color(0xFFFF8A00)) // Soft amber/orange
            categoryName.contains("প্লাম্বিং") || iconName == "Plumbing" ->
                Pair(Color(0xFFEBF5FF), Color(0xFF2563EB)) // Soft sky blue
            categoryName.contains("কার্পেন্ট") || iconName == "HomeRepairService" ->
                Pair(Color(0xFFFBF0EA), Color(0xFFB45309)) // Soft warm brown/tan
            categoryName.contains("রং") || categoryName.contains("পেইন্ট") || iconName == "Brush" || iconName == "FormatPaint" || iconName == "Palette" ->
                Pair(Color(0xFFFDF2F8), Color(0xFFDB2777)) // Soft rose/pink
            categoryName.contains("কম্পিউটার") || categoryName.contains("আইটি") || iconName == "Computer" || iconName == "Laptop" || iconName == "Code" ->
                Pair(Color(0xFFEFF6FF), Color(0xFF3B82F6)) // Soft tech blue
            categoryName.contains("মোবাইল") || categoryName.contains("ইলেকট্রনিক্স") || iconName == "Build" || iconName == "DesignServices" ->
                Pair(Color(0xFFECFDF5), Color(0xFF10B981)) // Soft emerald/mint
            categoryName.contains("এসি") || iconName == "AcUnit" ->
                Pair(Color(0xFFF0F9FF), Color(0xFF0284C7)) // Soft cyan
            categoryName.contains("ডেলিভারি") || iconName == "LocalShipping" ->
                Pair(Color(0xFFFFFBEB), Color(0xFFD97706)) // Soft amber/gold
            else ->
                Pair(Color(0xFFFFF4E5), SomadhanOrange)
        }
    }
}
