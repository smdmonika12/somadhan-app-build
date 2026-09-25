# ProGuard & R8 Optimization Rules for Somadhan

# Keep data classes and entities
-keep class com.example.data.entity.** { *; }
-keep class com.example.data.model.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class androidx.room.** { *; }

# [ধাপ ৩৩.৫] Firebase-নির্দিষ্ট keep/dontwarn rule সরানো হলো — Firebase SDK dependency
# সম্পূর্ণ অপসারিত (ধাপ ৩৩.৩), তাই এই rule গুলো এখন কোনো matching class-ই খুঁজে পেত না (dead
# config, R8-কে কোনো ক্ষতি করত না কিন্তু বিভ্রান্তিকর ছিল)। com.google.android.gms.** এখনো
# ব্যবহৃত হতে পারে (Google Maps/Play Services location ইত্যাদির জন্য, Firebase-নির্দিষ্ট নয়),
# তাই সেই dontwarn আলাদাভাবে নিচে রাখা হলো।
-dontwarn com.google.android.gms.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep line numbers for accurate crash stacktraces in production
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

