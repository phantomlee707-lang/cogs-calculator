package com.rainweaver.cogscalc.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientDao {
    @Query("SELECT * FROM ingredients WHERE archived = 0 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<IngredientEntity>

    @Query("SELECT * FROM ingredients WHERE id = :id")
    suspend fun getById(id: Long): IngredientEntity?

    @Query("SELECT * FROM ingredients WHERE name LIKE '%' || :q || '%' AND archived = 0 ORDER BY name COLLATE NOCASE")
    fun search(q: String): Flow<List<IngredientEntity>>

    @Insert suspend fun insert(e: IngredientEntity): Long
    @Update suspend fun update(e: IngredientEntity): Int
    @Query("DELETE FROM ingredients WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("UPDATE ingredients SET archived = :archived WHERE id = :id") suspend fun setArchived(id: Long, archived: Boolean)
}

data class RecipeWithDetails(
    @Embedded val recipe: RecipeEntity,
    @Relation(parentColumn = "id", entityColumn = "recipeId") val lines: List<RecipeLineEntity>,
    @Relation(parentColumn = "id", entityColumn = "recipeId") val additionalCosts: List<AdditionalCostEntity>,
)

@Dao
interface RecipeDao {
    @Transaction @Query("SELECT * FROM recipes ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<RecipeWithDetails>>

    @Transaction @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getById(id: Long): RecipeWithDetails?

    @Transaction @Query("SELECT * FROM recipes WHERE id = :id")
    fun observeById(id: Long): Flow<RecipeWithDetails?>

    @Insert suspend fun insertRecipe(r: RecipeEntity): Long
    @Update suspend fun updateRecipe(r: RecipeEntity): Int
    @Query("DELETE FROM recipes WHERE id = :id") suspend fun deleteById(id: Long)

    @Insert suspend fun insertLines(lines: List<RecipeLineEntity>)
    @Insert suspend fun insertAdditional(costs: List<AdditionalCostEntity>)
    @Query("DELETE FROM recipe_lines WHERE recipeId = :rid") suspend fun deleteLines(rid: Long)
    @Query("DELETE FROM additional_costs WHERE recipeId = :rid") suspend fun deleteAdditional(rid: Long)

    @Transaction
    suspend fun saveRecipe(r: RecipeEntity, lines: List<RecipeLineEntity>, additional: List<AdditionalCostEntity>): Long {
        val id = if (r.id == 0L) insertRecipe(r) else { updateRecipe(r); r.id }
        deleteLines(id); deleteAdditional(id)
        insertLines(lines.map { it.copy(recipeId = id) })
        insertAdditional(additional.map { it.copy(recipeId = id) })
        return id
    }
}

data class SnapshotWithLines(
    @Embedded val snapshot: CostSnapshotEntity,
    @Relation(parentColumn = "id", entityColumn = "snapshotId") val lines: List<SnapshotLineEntity>,
)

@Dao
interface SnapshotDao {
    @Transaction @Query("SELECT * FROM cost_snapshots WHERE recipeId = :rid ORDER BY createdAt DESC")
    fun observeByRecipe(rid: Long): Flow<List<SnapshotWithLines>>

    @Transaction @Query("SELECT * FROM cost_snapshots WHERE id = :id")
    suspend fun getById(id: Long): SnapshotWithLines?

    @Insert suspend fun insertSnapshot(s: CostSnapshotEntity): Long
    @Insert suspend fun insertLines(lines: List<SnapshotLineEntity>)

    @Transaction
    suspend fun save(s: CostSnapshotEntity, lines: List<SnapshotLineEntity>): Long {
        val id = insertSnapshot(s)
        insertLines(lines.map { it.copy(snapshotId = id) })
        return id
    }
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observe(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun get(): AppSettingsEntity?

    @Insert suspend fun insert(s: AppSettingsEntity)
    @Update suspend fun update(s: AppSettingsEntity): Int

    @Transaction
    suspend fun upsert(s: AppSettingsEntity) { if (update(s) == 0) insert(s) }
}
