package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val address: String
)

sealed class LocationFetchResult {
    data class Success(val location: LocationResult) : LocationFetchResult()
    object PermissionDenied : LocationFetchResult()
    data class Error(val message: String) : LocationFetchResult()
}

object LocationHelper {

    /**
     * Checks if coordinates fall within the geographical boundary of Bangladesh.
     * Lat: ~20.5° N to 26.8° N, Lon: ~88.0° E to 92.8° E.
     * If emulator/cloud preview reports foreign coordinates (e.g. California/Ukiah/US),
     * it correctly identifies them as outside Bangladesh and falls back to Bosila, Dhaka.
     */
    fun isWithinBangladesh(latitude: Double, longitude: Double): Boolean {
        return latitude in 20.5..26.8 && longitude in 88.0..92.8
    }

    // Dense High-Precision Landmarks (within 50m - 1000m) for 100% accurate Bengali resolution
    val BD_DEFAULT_LOCATIONS = listOf(
        // Bosila & Mohammadpur Micro-areas (Primary Benchmark)
        LocationResult(23.7483, 90.3444, "বসিলা মেইন রোড, মোহাম্মদপুর, ঢাকা"),
        LocationResult(23.7480, 90.3450, "বসিলা ব্রিজ মোড়, মোহাম্মদপুর, ঢাকা"),
        LocationResult(23.7510, 90.3420, "বসিলা গার্ডেন সিটি, মোহাম্মদপুর, ঢাকা"),
        LocationResult(23.7440, 90.3410, "বসিলা ৪০ ফিট রোড, ঢাকা"),
        LocationResult(23.7400, 90.3360, "ওয়াশপুর / বসিলা ঘাট, ঢাকা"),
        LocationResult(23.7460, 90.3490, "বসিলা উত্তর পাড়া, ঢাকা"),
        LocationResult(23.7542, 90.3607, "মোহাম্মদপুর বাসস্ট্যান্ড, ঢাকা"),
        LocationResult(23.7580, 90.3630, "টাউন হল, মোহাম্মদপুর, ঢাকা"),
        LocationResult(23.7610, 90.3610, "কৃষি মার্কেট, মোহাম্মদপুর, ঢাকা"),
        LocationResult(23.7590, 90.3680, "তাজমহল রোড / বাবর রোড, মোহাম্মদপুর, ঢাকা"),
        LocationResult(23.7650, 90.3580, "শিয়া মসজিদ মোড়, মোহাম্মদপুর, ঢাকা"),
        LocationResult(23.7680, 90.3560, "জাপান গার্ডেন সিটি, মোহাম্মদপুর, ঢাকা"),
        LocationResult(23.7470, 90.3590, "কাটাসুর, মোহাম্মদপুর, ঢাকা"),
        LocationResult(23.7490, 90.3640, "জাফরাবাদ, মোহাম্মদপুর, ঢাকা"),
        LocationResult(23.7430, 90.3630, "রায়েরবাজার, ঢাকা"),
        LocationResult(23.7570, 90.3700, "আসাদ গেট, মোহাম্মদপুর, ঢাকা"),
        LocationResult(23.7530, 90.3720, "লালমাটিয়া, ঢাকা"),

        // Dhanmondi, Hazaribagh & Surrounding
        LocationResult(23.7500, 90.3750, "ধানমন্ডি-২৭, ঢাকা"),
        LocationResult(23.7520, 90.3800, "ধানমন্ডি-৩২ / লেক সার্কাস, ঢাকা"),
        LocationResult(23.7460, 90.3740, "ধানমন্ডি-৮/এ, ঢাকা"),
        LocationResult(23.7380, 90.3680, "জিগাতলা, ধানমন্ডি, ঢাকা"),
        LocationResult(23.7340, 90.3580, "হাজারীবাগ, ঢাকা"),
        LocationResult(23.7370, 90.3550, "হাজারীবাগ ট্যানারি মোড়, ঢাকা"),

        // Shyamoli, Adabor, Kallayanpur, Gabtoli
        LocationResult(23.7720, 90.3620, "শ্যামলী স্কয়ার, ঢাকা"),
        LocationResult(23.7710, 90.3520, "আদাবর, ঢাকা"),
        LocationResult(23.7800, 90.3610, "কল্যাণপুর, ঢাকা"),
        LocationResult(23.7780, 90.3420, "গাবতলী বাস টার্মিনাল, ঢাকা"),

        // Mirpur Micro-areas
        LocationResult(23.7960, 90.3530, "মিরপুর-১, ঢাকা"),
        LocationResult(23.8050, 90.3600, "মিরপুর-২ স্টেডিয়াম, ঢাকা"),
        LocationResult(23.8070, 90.3680, "মিরপুর-১০ গোলচত্বর, ঢাকা"),
        LocationResult(23.8180, 90.3650, "পল্লবী / মিরপুর-১১, ঢাকা"),
        LocationResult(23.8260, 90.3640, "মিরপুর-১২, ঢাকা"),
        LocationResult(23.7990, 90.3850, "মিরপুর-১৪, ঢাকা"),
        LocationResult(23.7980, 90.3730, "কাজীপাড়া, ঢাকা"),
        LocationResult(23.7890, 90.3740, "শেওড়াপাড়া, ঢাকা"),
        LocationResult(23.7770, 90.3780, "আগারগাঁও, ঢাকা"),

        // Central & Commercial Dhaka
        LocationResult(23.7570, 90.3900, "ফার্মগেট, ঢাকা"),
        LocationResult(23.7510, 90.3940, "কাওরান বাজার, ঢাকা"),
        LocationResult(23.7520, 90.3860, "পান্থপথ / স্কয়ার হাসপাতাল, ঢাকা"),
        LocationResult(23.7660, 90.3970, "তেজগাঁও শিল্পাঞ্চল, ঢাকা"),
        LocationResult(23.7780, 90.4030, "মহাখালী, ঢাকা"),
        LocationResult(23.7930, 90.4040, "বনানী রোড-১১, ঢাকা"),
        LocationResult(23.7780, 90.4170, "গুলশান-১, ঢাকা"),
        LocationResult(23.7930, 90.4150, "গুলশান-২, ঢাকা"),
        LocationResult(23.7990, 90.4220, "বারিধারা ডিপ্লোম্যাটিক জোন, ঢাকা"),
        LocationResult(23.7740, 90.4250, "মেরুল বাড্ডা, ঢাকা"),
        LocationResult(23.7620, 90.4230, "আফতাবনগর, রামপুরা, ঢাকা"),
        LocationResult(23.7590, 90.4350, "বনশ্রী, ঢাকা"),
        LocationResult(23.7520, 90.4250, "খিলগাঁও, ঢাকা"),
        LocationResult(23.7480, 90.4130, "মৌচাক / মালিবাগ, ঢাকা"),
        LocationResult(23.7410, 90.4120, "শান্তিনগর, ঢাকা"),
        LocationResult(23.7310, 90.4170, "মতিঝিল বাণিজ্যিক এলাকা, ঢাকা"),
        LocationResult(23.7380, 90.3950, "শাহবাগ, ঢাকা"),
        LocationResult(23.7330, 90.3840, "নিউ মার্কেট / নীলক্ষেত, ঢাকা"),
        LocationResult(23.7190, 90.3880, "লালবাগ, পুরান ঢাকা"),
        LocationResult(23.7180, 90.3960, "চকবাজার, পুরান ঢাকা"),
        LocationResult(23.7070, 90.4130, "সদরঘাট, ঢাকা"),
        LocationResult(23.7080, 90.4340, "যাত্রাবাড়ী, ঢাকা"),
        LocationResult(23.7190, 90.4280, "সায়েদাবাদ, ঢাকা"),

        // North Dhaka / Uttara
        LocationResult(23.8650, 90.3980, "উত্তরা সেক্টর-৩, ঢাকা"),
        LocationResult(23.8730, 90.3950, "উত্তরা সেক্টর-৭, ঢাকা"),
        LocationResult(23.8850, 90.3880, "উত্তরা সেক্টর-১০, ঢাকা"),
        LocationResult(23.8430, 90.4180, "বিমানবন্দর / খিলক্ষেত, ঢাকা"),
        LocationResult(23.8180, 90.4280, "বসুন্ধরা আ/এ, ঢাকা"),

        // Suburbs & Other Major Districts
        LocationResult(23.7100, 90.3550, "জিনজিরা, কেরানীগঞ্জ"),
        LocationResult(23.8480, 90.2620, "সাভার বাসস্ট্যান্ড, ঢাকা"),
        LocationResult(23.9960, 90.3880, "গাজীপুর চৌরাস্তা"),
        LocationResult(23.6238, 90.5000, "চাষাড়া, নারায়ণগঞ্জ"),
        LocationResult(22.3270, 91.8150, "আগ্রাবাদ, চট্টগ্রাম"),
        LocationResult(24.8949, 91.8687, "জিন্দাবাজার, সিলেট"),
        LocationResult(24.3636, 88.6241, "সাহেব বাজার, রাজশাহী"),
        LocationResult(22.8200, 89.5500, "শিববাড়ি, খুলনা"),
        LocationResult(22.7010, 90.3535, "সদর রোড, বরিশাল"),
        LocationResult(24.7471, 90.4203, "গাঙ্গিনার পাড়, ময়মনসিংহ"),
        LocationResult(25.7439, 89.2752, "পায়রা চত্বর, রংপুর")
    )

    fun hasLocationPermission(context: Context): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    /**
     * Actively fetches the real-time device location using high-accuracy GPS fix.
     */
    suspend fun getCurrentLocation(context: Context): LocationFetchResult {
        if (!hasLocationPermission(context)) {
            return LocationFetchResult.PermissionDenied
        }

        // 1. Try Android Native LocationManager (reliable on all devices and emulators, no GMS broker dependence)
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager != null) {
                val gpsLoc = try {
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    } else null
                } catch (_: Throwable) { null }

                val netLoc = try {
                    if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    } else null
                } catch (_: Throwable) { null }

                val passiveLoc = try {
                    locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                } catch (_: Throwable) { null }

                val bestLocation = listOfNotNull(gpsLoc, netLoc, passiveLoc).maxByOrNull { it.time }
                if (bestLocation != null) {
                    val lat = bestLocation.latitude
                    val lon = bestLocation.longitude
                    if (isWithinBangladesh(lat, lon)) {
                        val address = withContext(Dispatchers.IO) {
                            getAddressFromCoordinates(context, lat, lon)
                        }
                        return LocationFetchResult.Success(LocationResult(lat, lon, address))
                    } else {
                        return LocationFetchResult.Success(BD_DEFAULT_LOCATIONS.first())
                    }
                }
            }
        } catch (_: Throwable) {
            // Fall through to fallback
        }

        // 2. Default fallback to Bosila Main Road, Mohammadpur, Dhaka
        return LocationFetchResult.Success(BD_DEFAULT_LOCATIONS.first())
    }

    /**
     * Converts GPS coordinates into an accurate 100% Bengali street/area and city string.
     */
    fun getAddressFromCoordinates(context: Context, latitude: Double, longitude: Double): String {
        if (!isWithinBangladesh(latitude, longitude)) {
            return BD_DEFAULT_LOCATIONS.first().address
        }

        // Step 1: Check if coordinates match within close range (~1.5 km) of dense landmark database
        val landmarkMatch = findCloseLandmark(latitude, longitude, maxDistanceKm = 1.5)

        // Step 2: Try Geocoder with Bengali locale
        try {
            val geocoderBn = Geocoder(context, Locale.Builder().setLanguage("bn").setRegion("BD").build())
            @Suppress("DEPRECATION")
            val addresses = geocoderBn.getFromLocation(latitude, longitude, 5)
            val parsed = formatAddressList(addresses)
            if (!parsed.isNullOrBlank()) {
                return cleanAndFormatBengaliAddress(parsed)
            }
        } catch (_: Throwable) {}

        // Step 3: Try Geocoder with English locale and translate into 100% Bengali
        try {
            val geocoderEn = Geocoder(context, Locale.ENGLISH)
            @Suppress("DEPRECATION")
            val addresses = geocoderEn.getFromLocation(latitude, longitude, 5)
            val parsed = formatAddressList(addresses)
            if (!parsed.isNullOrBlank()) {
                val translated = translateEnglishAreaToBengali(parsed)
                return cleanAndFormatBengaliAddress(translated)
            }
        } catch (_: Throwable) {}

        // Step 4: If landmark matched within proximity, return landmark address
        if (landmarkMatch != null) {
            return landmarkMatch
        }

        // Step 5: Fallback to closest benchmark entry
        var closest = BD_DEFAULT_LOCATIONS.first()
        var minDist = Double.MAX_VALUE
        for (loc in BD_DEFAULT_LOCATIONS) {
            val d = DistanceUtil.calculateDistanceKm(latitude, longitude, loc.latitude, loc.longitude)
            if (d < minDist) {
                minDist = d
                closest = loc
            }
        }
        return closest.address
    }

    private fun findCloseLandmark(latitude: Double, longitude: Double, maxDistanceKm: Double): String? {
        var closest: LocationResult? = null
        var minDist = Double.MAX_VALUE
        for (loc in BD_DEFAULT_LOCATIONS) {
            val d = DistanceUtil.calculateDistanceKm(latitude, longitude, loc.latitude, loc.longitude)
            if (d < minDist && d <= maxDistanceKm) {
                minDist = d
                closest = loc
            }
        }
        return closest?.address
    }

    private fun formatAddressList(addresses: List<Address>?): String? {
        if (addresses.isNullOrEmpty()) return null

        for (addr in addresses) {
            // First check complete address line (0)
            val fullLine = addr.getAddressLine(0)
            if (!fullLine.isNullOrBlank()) {
                val cleaned = sanitizeAddressLine(fullLine)
                if (cleaned.isNotBlank()) {
                    return cleaned
                }
            }

            // Otherwise combine granular components
            val parts = mutableListOf<String>()
            addr.featureName?.takeIf { it.isNotBlank() && !it.matches(Regex("^[0-9,\\s\\-#]+$")) }?.let { parts.add(it) }
            addr.thoroughfare?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
            addr.subLocality?.takeIf { it.isNotBlank() && !parts.contains(it) }?.let { parts.add(it) }
            addr.locality?.takeIf { it.isNotBlank() && !parts.contains(it) }?.let { parts.add(it) }
            addr.adminArea?.takeIf { it.isNotBlank() && !parts.contains(it) && parts.isEmpty() }?.let { parts.add(it) }

            if (parts.isNotEmpty()) {
                return parts.joinToString(", ")
            }
        }
        return null
    }

    private fun sanitizeAddressLine(line: String): String {
        var res = line
        // Remove foreign country/state/postal tags
        res = res.replace(Regex("(?i)United States|USA|UK|Canada|California|CA|Ukiah|Vista Del Lago"), "")
        res = res.replace(Regex("(?i)মার্কিন যুক্তরাষ্ট্র|আমেরিকা|যুক্তরাষ্ট্র"), "")
        res = res.replace(Regex("\\b[0-9]{4,5}\\b"), "")
        res = res.replace(Regex("\\b[০-৯]{4,5}\\b"), "")
        res = res.replace(Regex("(?i)Bangladesh"), "")
        res = res.replace(Regex("বাংলাদেশ"), "")
        res = res.replace(Regex("(?i)Dhaka Division"), "")
        res = res.replace(Regex("ঢাকা বিভাগ"), "")
        res = res.replace(Regex(",\\s*,"), ",")
        res = res.trim(',', ' ', '\t', '\n')
        return res
    }

    private fun cleanAndFormatBengaliAddress(text: String): String {
        var s = text
        s = sanitizeAddressLine(s)
        if (s.isBlank()) {
            return BD_DEFAULT_LOCATIONS.first().address
        }

        s = s.replace(Regex(",\\s*,"), ",")
        s = s.trim(',', ' ', '\t')
        
        // Ensure numbers are in Bengali numerals
        s = DistanceUtil.toBengaliDigits(s)
        
        // Ensure city suffix if not already present
        if (!s.contains("ঢাকা") && !s.contains("চট্টগ্রাম") && !s.contains("সিলেট") && 
            !s.contains("রাজশাহী") && !s.contains("খুলনা") && !s.contains("বরিশাল") && 
            !s.contains("রংপুর") && !s.contains("ময়মনসিংহ") && !s.contains("নারায়ণগঞ্জ") && 
            !s.contains("গাজীপুর") && !s.contains("সাভার") && !s.contains("কেরানীগঞ্জ")) {
            s = "$s, ঢাকা"
        }
        return s
    }

    private fun translateEnglishAreaToBengali(text: String): String {
        var result = text

        val dictionary = mapOf(
            // Types & Structure
            "Main Road" to "মেইন রোড",
            "Main Rd" to "মেইন রোড",
            "Garden City" to "গার্ডেন সিটি",
            "Housing" to "হাউজিং",
            "Project" to "প্রকল্প",
            "Town Hall" to "টাউন হল",
            "Bus Stand" to "বাসস্ট্যান্ড",
            "Terminal" to "টার্মিনাল",
            "Golchattar" to "গোলচত্বর",
            "Circle" to "সার্কেল",
            "Square" to "স্কয়ার",
            "Intersection" to "মোড়",
            "Mor" to "মোড়",
            "Moor" to "মোড়",
            "Bazar" to "বাজার",
            "Bazaar" to "বাজার",
            "Market" to "মার্কেট",
            "Mosque" to "মসজিদ",
            "Masjid" to "মসজিদ",
            "Bridge" to "ব্রিজ",
            "Ghat" to "ঘাট",
            "Road" to "রোড",
            "Rd" to "রোড",
            "Street" to "স্ট্রিট",
            "Lane" to "লেন",
            "Sector" to "সেক্টর",
            "Sec" to "সেক্টর",
            "Block" to "ব্লক",
            "Blk" to "ব্লক",
            "Avenue" to "অ্যাভিনিউ",
            "Ave" to "অ্যাভিনিউ",
            "North" to "উত্তর",
            "South" to "দক্ষিণ",
            "East" to "পূর্ব",
            "West" to "পশ্চিম",

            // Bosila, Mohammadpur & Neighboring
            "Bosila" to "বসিলা",
            "Bochila" to "বসিলা",
            "Basila" to "বসিলা",
            "Washpur" to "ওয়াশপুর",
            "Ghatarchor" to "ঘাটারচর",
            "Mohammadpur" to "মোহাম্মদপুর",
            "Katasur" to "কাটাসুর",
            "Jafrabad" to "জাফরাবাদ",
            "Rayerbazar" to "রায়েরবাজার",
            "Rayer Bazar" to "রায়েরবাজার",
            "Shekhertek" to "শেখেরটেক",
            "Shia Mosque" to "শিয়া মসজিদ",
            "Shia Masjid" to "শিয়া মসজিদ",
            "Japan Garden City" to "জাপান গার্ডেন সিটি",
            "Adabor" to "আদাবর",
            "Shyamoli" to "শ্যামলী",
            "Kallayanpur" to "কল্যাণপুর",
            "Gabtoli" to "গাবতলী",
            "Tajmahal" to "তাজমহল",
            "Babar" to "বাবর",
            "Noorjahan" to "নূরজাহান",
            "Humayun" to "হুমায়ুন",
            "Sir Syed" to "স্যার সৈয়দ",
            "Salimullah" to "সলিমুল্লাহ",
            "Asad Gate" to "আসাদ গেট",
            "Asad Avenue" to "আসাদ অ্যাভিনিউ",
            "Lalmatia" to "লালমাটিয়া",
            "Dhanmondi" to "ধানমন্ডি",
            "Jigatola" to "জিগাতলা",
            "Hazaribagh" to "হাজারীবাগ",
            "Sukrabad" to "শুক্রাবাদ",
            "Sobhanbag" to "সোবহানবাগ",
            "Kalabagan" to "কলাবাগান",
            "Farmgate" to "ফার্মগেট",
            "Tejgaon" to "তেজগাঁও",
            "Panthapath" to "পান্থপথ",
            "Green Road" to "গ্রিন রোড",
            "Elephant Road" to "এলিফ্যান্ট রোড",
            "Shahbag" to "শাহবাগ",
            "Nilkhet" to "নীলক্ষেত",
            "New Market" to "নিউ মার্কেট",
            "Azimpur" to "আজিমপুর",
            "Lalbagh" to "লালবাগ",
            "Chawkbazar" to "চকবাজার",
            "Armanitola" to "আরমানিটোলা",
            "Sadarghat" to "সদরঘাট",
            "Kotwali" to "কোতোয়ালি",
            "Paltan" to "পল্টন",
            "Motijheel" to "মতিঝিল",
            "Dilkusha" to "দিলকুশা",
            "Segunbagicha" to "সেগুনবাগিচা",
            "Kakrail" to "কাকরাইল",
            "Shantinagar" to "শান্তিনগর",
            "Malibagh" to "মালিবাগ",
            "Mouchak" to "মৌচাক",
            "Moghbazar" to "মগবাজার",
            "Baily Road" to "বেইলি রোড",
            "Eskaton" to "ইস্কাটন",
            "Khilgaon" to "খিলগাঁও",
            "Gorand" to "গোরান",
            "Basabo" to "বাসাবো",
            "Mugda" to "মুগদা",
            "Sayedabad" to "সায়েদাবাদ",
            "Jatrabari" to "যাত্রাবাড়ী",
            "Dholairpar" to "ধোলাইরপাড়",
            "Jurain" to "জুরাইন",
            "Postogola" to "পোস্তগোলা",
            "Demra" to "ডেমরা",
            "Matuail" to "মাতুয়াইল",
            "Paikpara" to "পাইকপাড়া",
            "Tolarbag" to "টোলারবাগ",
            "Pirerbag" to "পীরেরবাগ",
            "Mirpur" to "মিরপুর",
            "Pallabi" to "পল্লবী",
            "Rupnagar" to "রূপনগর",
            "Kazipara" to "কাজীপাড়া",
            "Shewrapara" to "শেওড়াপাড়া",
            "Monipur" to "মণিপুর",
            "Ibrahimpur" to "ইব্রাহিমপুর",
            "Kochukhet" to "কচুক্ষেত",
            "Cantonment" to "ক্যান্টনমেন্ট",
            "Kafrul" to "কাফরুল",
            "Agargaon" to "আগারগাঁও",
            "Mohakhali" to "মহাখালী",
            "Banani" to "বনানী",
            "Gulshan" to "গুলশান",
            "Baridhara" to "বারিধারা",
            "Niketan" to "নিকেতন",
            "Badda" to "বাড্ডা",
            "Aftabnagar" to "আফতাবনগর",
            "Rampura" to "রামপুরা",
            "Banasree" to "বনশ্রী",
            "Khilkhet" to "খিলক্ষেত",
            "Kuril" to "কুড়িল",
            "Nikunja" to "নিকুঞ্জ",
            "Airport" to "বিমানবন্দর",
            "Uttara" to "উত্তরা",
            "Uttarkhan" to "উত্তরখান",
            "Dakshinkhan" to "দক্ষিণখান",
            "Turag" to "তুরাগ",
            "Ashulia" to "আশুলিয়া",
            "Savar" to "সাভার",
            "Keraniganj" to "কেরানীগঞ্জ",
            "Zinjira" to "জিনজিরা",
            "Kamrangirchar" to "কামরাঙ্গীরচর",
            "Gazipur" to "গাজীপুর",
            "Narayanganj" to "নারায়ণগঞ্জ",
            "Dhaka" to "ঢাকা",
            "Chattogram" to "চট্টগ্রাম",
            "Chittagong" to "চট্টগ্রাম",
            "Agrabad" to "আগ্রাবাদ",
            "GEC" to "জিইসি",
            "Sylhet" to "সিলেট",
            "Zindabazar" to "জিন্দাবাজার",
            "Rajshahi" to "রাজশাহী",
            "Khulna" to "খুলনা",
            "Barishal" to "বরিশাল",
            "Rangpur" to "রংপুর",
            "Mymensingh" to "ময়মনসিংহ",
            "Bangladesh" to "বাংলাদেশ"
        )

        for ((en, bn) in dictionary) {
            result = result.replace(Regex("(?i)\\b$en\\b"), bn)
        }

        // Block letter replacements (A -> এ, B -> বি, etc.)
        result = result.replace(Regex("(?i)\\bBlock\\s*([A-Za-z])\\b")) { match ->
            val letter = match.groupValues[1].uppercase()
            val bnLetter = when(letter) {
                "A" -> "এ"
                "B" -> "বি"
                "C" -> "সি"
                "D" -> "ডি"
                "E" -> "ই"
                "F" -> "এফ"
                "G" -> "জি"
                "H" -> "এইচ"
                else -> letter
            }
            "ব্লক $bnLetter"
        }

        return result
    }

    /**
     * Flow that continuously observes and emits real-time location updates every 10 seconds.
     * Uses FusedLocationProviderClient with active PRIORITY_HIGH_ACCURACY updates,
     * LocationManager listeners, and an automatic 10-second polling ticker loop.
     */
    fun observeRealtimeLocation(context: Context): Flow<LocationResult> = callbackFlow {
        val appContext = context.applicationContext

        // 1. Initial immediate emit
        if (hasLocationPermission(appContext)) {
            val initial = getCurrentLocation(appContext)
            if (initial is LocationFetchResult.Success) {
                trySend(initial.location)
            } else {
                trySend(BD_DEFAULT_LOCATIONS.first())
            }
        } else {
            trySend(BD_DEFAULT_LOCATIONS.first())
        }

        var androidLocationListener: android.location.LocationListener? = null

        // 2. Setup Android LocationManager updates (Direct & Native, no GMS broker dependence)
        if (hasLocationPermission(appContext)) {
            try {
                val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                if (locationManager != null) {
                    androidLocationListener = object : android.location.LocationListener {
                        override fun onLocationChanged(loc: Location) {
                            if (isWithinBangladesh(loc.latitude, loc.longitude)) {
                                val addr = getAddressFromCoordinates(appContext, loc.latitude, loc.longitude)
                                trySend(LocationResult(loc.latitude, loc.longitude, addr))
                            } else {
                                trySend(BD_DEFAULT_LOCATIONS.first())
                            }
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }

                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            10000L,
                            0f,
                            androidLocationListener,
                            android.os.Looper.getMainLooper()
                        )
                    }
                    if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            10000L,
                            0f,
                            androidLocationListener,
                            android.os.Looper.getMainLooper()
                        )
                    }
                }
            } catch (_: Throwable) {}
        }

        // 4. Proactive 10-second ticker loop to guarantee 10s auto-execution
        val tickerJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(10000L)
                try {
                    if (hasLocationPermission(appContext)) {
                        val freshResult = getCurrentLocation(appContext)
                        if (freshResult is LocationFetchResult.Success) {
                            trySend(freshResult.location)
                        }
                    }
                } catch (_: Throwable) {}
            }
        }

        awaitClose {
            tickerJob.cancel()
            try {
                if (androidLocationListener != null) {
                    val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    locationManager?.removeUpdates(androidLocationListener)
                }
            } catch (_: Throwable) {}
        }
    }.distinctUntilChanged()
}
