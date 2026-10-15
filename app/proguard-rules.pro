# R8 / release optimization rules

# --- Room: keep generated impl + entities/DAOs ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class com.rainweaver.cogscalc.data.** { *; }
-dontwarn androidx.room.paging.**

# --- Keep annotations & signatures ---
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# --- Kotlin coroutines ---
-dontwarn kotlinx.coroutines.**

# Keep native methods and enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
