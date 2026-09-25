package com.example.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object DistanceUtil {

    /**
     * Calculates the great-circle distance between two points on the Earth's surface
     * using the Haversine formula in Kilometers.
     */
    fun calculateDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(rLat1) * cos(rLat2) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    /**
     * Converts English numbers to Bengali numbers.
     */
    fun toBengaliDigits(input: String): String {
        val bengaliDigits = mapOf(
            '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
            '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯', '.' to '.'
        )
        return input.map { bengaliDigits[it] ?: it }.joinToString("")
    }

    fun roundTaka(amount: Double): Long {
        return Math.round(amount)
    }

    fun toBengaliTaka(amount: Double): String {
        return toBengaliDigits(Math.round(amount).toString())
    }

    fun toBengaliDigits(amount: Double): String {
        return toBengaliDigits(Math.round(amount).toString())
    }

    fun toBengaliDigits(amount: Float): String {
        return toBengaliDigits(Math.round(amount.toDouble()).toString())
    }

    fun toBengaliDigits(amount: Long): String {
        return toBengaliDigits(amount.toString())
    }

    fun toBengaliDigits(amount: Int): String {
        return toBengaliDigits(amount.toString())
    }

    fun formatDistance(distanceKm: Double): String {
        return if (distanceKm < 1.0) {
            val meters = (distanceKm * 1000).toInt()
            "${toBengaliDigits(meters.toString())} মিটার দূরে"
        } else {
            val formatted = String.format("%.1f", distanceKm)
            "${toBengaliDigits(formatted)} কিমি দূরে"
        }
    }
}
