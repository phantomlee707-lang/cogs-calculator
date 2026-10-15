package com.rainweaver.cogscalc.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        IngredientEntity::class,
        RecipeEntity::class,
        RecipeLineEntity::class,
        AdditionalCostEntity::class,
        CostSnapshotEntity::class,
        SnapshotLineEntity::class,
        AppSettingsEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class CogsDatabase : RoomDatabase() {
    abstract fun ingredientDao(): IngredientDao
    abstract fun recipeDao(): RecipeDao
    abstract fun snapshotDao(): SnapshotDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile private var INSTANCE: CogsDatabase? = null
        fun get(context: Context): CogsDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, CogsDatabase::class.java, "cogs.db")
                    .fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }

        fun resetInstance() { INSTANCE?.close(); INSTANCE = null }
    }
}
