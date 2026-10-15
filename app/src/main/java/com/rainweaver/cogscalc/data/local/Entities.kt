package com.rainweaver.cogscalc.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * All money/quantity/unit/enum values are stored as Strings and mapped to exact
 * domain types (BigDecimal, Unit) in the repository — never floats.
 */

@Entity(tableName = "ingredients", indices = [Index("name")])
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "",
    val purchaseQty: String,     // BigDecimal
    val purchaseUnit: String,    // Unit.name
    val purchasePrice: String,   // BigDecimal
    val baseUnit: String,        // Unit.name
    val supplier: String = "",
    val notes: String = "",
    val archived: Boolean = false,
    val updatedAt: Long = 0,
)

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "",
    val portions: Int = 1,
    val wastagePercent: String = "0",  // BigDecimal
    val notes: String = "",
    val prepNotes: String = "",
    val archived: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

@Entity(
    tableName = "recipe_lines",
    foreignKeys = [ForeignKey(RecipeEntity::class, ["id"], ["recipeId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("recipeId"), Index("ingredientId")],
)
data class RecipeLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val ingredientId: Long,
    val ingredientName: String,   // snapshot so a line survives ingredient deletion
    val quantity: String,         // BigDecimal
    val unit: String,             // Unit.name
    val conversionFactor: String? = null,  // BigDecimal (base units per line unit)
)

@Entity(
    tableName = "additional_costs",
    foreignKeys = [ForeignKey(RecipeEntity::class, ["id"], ["recipeId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("recipeId")],
)
data class AdditionalCostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val label: String,
    val amount: String,           // BigDecimal
)

/** A frozen historical costing. Immutable once saved. */
@Entity(
    tableName = "cost_snapshots",
    foreignKeys = [ForeignKey(RecipeEntity::class, ["id"], ["recipeId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("recipeId")],
)
data class CostSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val createdAt: Long,
    val portions: Int,
    val ingredientSubtotal: String,  // BigDecimal
    val additionalTotal: String,
    val wastageAmount: String,
    val batchCost: String,
    val costPerPortion: String?,     // null if portions invalid
    val currency: String,
)

@Entity(
    tableName = "snapshot_lines",
    foreignKeys = [ForeignKey(CostSnapshotEntity::class, ["id"], ["snapshotId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("snapshotId")],
)
data class SnapshotLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val snapshotId: Long,
    val ingredientName: String,
    val quantity: String,
    val unit: String,
    val costPerBase: String,
    val lineCost: String,
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val businessName: String = "",
    val currency: String = "Rp",
    val decimals: Int = 0,
    val defaultMarkup: String = "30",
    val defaultMargin: String = "30",
    val priceStep: String = "500",
)
