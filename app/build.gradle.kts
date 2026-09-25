plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  // Supabase migration (ধাপ ১): DTO ক্লাসগুলোর @Serializable এর জন্য
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.somadhan.bdapp"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    manifestPlaceholders["MAPS_API_KEY"] = "DEMO_KEY_REPLACE_LATER"
  }

  buildTypes {
    debug {
      // NOTE: previously `signingConfig = signingConfigs.getByName("debugConfig")` here pointed
      // at a `debug.keystore` file that isn't included in this exported project (see README.md
      // step 5), which made `:app:validateSigningDebug` fail with "Keystore file ... not found".
      // Removed so Android Studio uses its own auto-generated debug keystore instead -- this
      // only affects how the debug APK is signed, not any app behavior/feature/data.
    }
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    // Supabase migration (ধাপ ২০): supabase-kt এর সরকারি README অনুযায়ী "minimum Android SDK
    // version is 26, for lower versions you need to enable core library desugaring" -- এই
    // প্রজেক্টের minSdk 24 (উপরে দেখুন), আর supabase-kt (Postgrest/Auth/Storage/Realtime)
    // ধাপ ১ থেকেই ব্যবহৃত হচ্ছে। এতদিন এটা কোনো Gradle build/sync দিয়ে ধরা পড়েনি (এই migration-এর
    // কোনো session-ই network/gradle সুবিধা পায়নি) -- এই ধাপে (Realtime যোগ করার সময়) সরকারি
    // ডকুমেন্টেশন যাচাই করে এই গ্যাপ প্রথমবার আবিষ্কৃত হলো। নিচের dependencies ব্লকে
    // coreLibraryDesugaring যোগ করা হয়েছে।
    isCoreLibraryDesugaringEnabled = true
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
// Secrets configuration for environment variables
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  // [ধাপ ৩৩.৫] FIREBASE_APPCHECK_DEBUG_TOKEN ignoreList এন্ট্রি সরানো হলো — Firebase সম্পূর্ণ
  // অপসারিত হওয়ায় .env/.env.example-এ এই key আর নেই, তাই ignore করার কিছু নেই।
  ignoreList.add("sdk.dir")
}


// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  // [SUPABASE-MIGRATED - ধাপ ৩৩.৩ (চূড়ান্ত অংশ)] firebase.firestore সরানো হয়েছে — এই সেশনে
  // `AdminFirestoreExplorerView.kt`/`CsvImportUtil.kt`/`FirebaseConfigDialog.kt`/
  // `FirebaseConfigHelper.kt` (একমাত্র caller-চেইন) সম্পূর্ণ dead/orphaned হিসেবে re-verify করে
  // ডিলিট করা হয়েছে (আসল active UI আগেই `AdminSupabaseExplorerView` দিয়ে প্রতিস্থাপিত ছিল,
  // ধাপ ১৯ থেকে) — এখন প্রজেক্টে কোনো Firebase SDK dependency/import অবশিষ্ট নেই।
  // implementation(libs.firebase.firestore)
  // [SUPABASE-MIGRATED - ধাপ ৩৩.৩ কাজ ১] firebase.storage সরানো হয়েছে — কোথাও সক্রিয় ব্যবহার
  // ছিল না (SomadhanViewModel.kt-এর FirebaseStorage import dead ছিল, uploadProfileImageToFirebase()
  // আসলে ImageStorageUtil-এর মধ্য দিয়ে Supabase Storage ব্যবহার করে, ধাপ ১৫ থেকেই)।
  // implementation(libs.firebase.storage)

  // [SUPABASE-MIGRATED - ধাপ ৩৩.৩ কাজ ১] firebase.auth সরানো হয়েছে — কোথাও কোনো import/ব্যবহার
  // পাওয়া যায়নি পুরো কোডবেসে।
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  // implementation(libs.firebase.appcheck.recaptcha)
  // Password hashing (bcrypt) for user credentials
  implementation(libs.jbcrypt)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.maps.compose)
  implementation(libs.play.services.maps)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)

  // --- Supabase migration (ধাপ ১): নতুন dependency, Firebase dependency গুলো অক্ষত রেখে পাশাপাশি যোগ করা হলো ---
  implementation(platform(libs.supabase.bom))
  implementation(libs.supabase.postgrest.kt)
  implementation(libs.supabase.auth.kt)
  implementation(libs.supabase.storage.kt)
  implementation(libs.supabase.realtime.kt)
  implementation(libs.ktor.client.android)
  implementation(libs.kotlinx.serialization.json)
  // Supabase migration (ধাপ ৩১ ঘ): kyc_submission_date (timestamptz) সঠিকভাবে ISO-8601 এ
  // এনকোড করার জন্য -- আগে SimpleDateFormat-ভিত্তিক হেল্পার দিয়ে হচ্ছিল, master prompt-এ এই
  // dependency-ই ব্যবহার করতে বলা হয়েছিল।
  implementation(libs.kotlinx.datetime)
  // Supabase migration (ধাপ ২০): উপরে compileOptions-এ isCoreLibraryDesugaringEnabled=true এর
  // জন্য দরকার -- supabase-kt এর minSdk 26 রিকোয়ারমেন্ট এই প্রজেক্টের minSdk 24-এ মেটানোর জন্য।
  coreLibraryDesugaring(libs.android.desugar.jdk.libs)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)
}
