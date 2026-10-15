package com.rainweaver.cogscalc.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.rainweaver.cogscalc.data.local.AppSettingsEntity
import com.rainweaver.cogscalc.data.local.RecipeEntity
import com.rainweaver.cogscalc.domain.*
import java.io.File
import java.io.FileOutputStream
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

/** On-device, print-friendly recipe costing report with multi-page support. */
object CostReportPdf {

    private const val W = 595
    private const val H = 842
    private const val M = 40f
    private const val RIGHT = W - M
    private const val BOTTOM = 800f

    fun writePdf(context: Context, recipe: RecipeEntity, cost: RecipeCost, settings: AppSettingsEntity): File {
        val cur = settings.currency
        val dec = settings.decimals.coerceIn(0, 6)
        fun money(m: Money) = m.format(cur, dec)

        val title = Paint().apply { textSize = 20f; typeface = Typeface.DEFAULT_BOLD; color = Color.rgb(0x1D, 0x5B, 0x54) }
        val h = Paint().apply { textSize = 12f; typeface = Typeface.DEFAULT_BOLD; color = Color.DKGRAY }
        val body = Paint().apply { textSize = 11f; color = Color.BLACK }
        val small = Paint().apply { textSize = 9f; color = Color.GRAY }
        val dash = Paint().apply { textSize = 11f; color = Color.LTGRAY; textAlign = Paint.Align.RIGHT }

        val doc = PdfDocument()
        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNum).create())
        var canvas = page.canvas
        var y = M + 10f

        fun footer() {
            val f = Paint().apply { textSize = 8f; color = Color.GRAY; textAlign = Paint.Align.CENTER }
            canvas.drawText("Rainweaver Studio  •  COGS Calculator  •  Page $pageNum", W / 2f, H - 20f, f)
        }
        fun newPage() {
            footer(); doc.finishPage(page); pageNum++
            page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNum).create()); canvas = page.canvas; y = M + 10f
        }
        fun need(extra: Float) { if (y + extra > BOTTOM) newPage() }
        fun drawRight(text: String, x: Float, base: Paint) {
            val p = Paint(base).apply { textAlign = Paint.Align.RIGHT }; canvas.drawText(text, x, y, p)
        }

        // header
        canvas.drawText(settings.businessName.ifBlank { "Your Business" }, M, y, title); y += 22
        canvas.drawText("Costing Report — ${recipe.name}", M, y, h); y += 16
        canvas.drawText("Generated ${Date()}", M, y, small); y += 22

        // table header
        need(30f)
        canvas.drawText("INGREDIENT", M, y, h)
        canvas.drawText("QTY", 300f, y, h)
        canvas.drawText("COST/BASE", 390f, y, h)
        drawRight("LINE COST", RIGHT, h); y += 6
        canvas.drawLine(M, y, RIGHT, y, body); y += 16

        for (line in cost.lines) {
            need(30f)
            val qty = line.quantity.stripTrailingZeros().toPlainString()
            canvas.drawText(line.ingredientName.take(28), M, y, body)
            canvas.drawText("$qty ${line.unit.label}", 300f, y, body)
            canvas.drawText(money(line.costPerBase), 390f, y, body)
            drawRight(money(line.lineCost), RIGHT, body)
            y += 20
        }

        // totals
        y += 8; need(120f)
        canvas.drawLine(340f, y, RIGHT, y, body); y += 16
        fun row(label: String, v: String) { canvas.drawText(label, 360f, y, body); drawRight(v, RIGHT, body); y += 16 }
        row("Ingredient subtotal", money(cost.ingredientSubtotal))
        if (cost.wastageAmount.value.signum() != 0) row("Wastage", money(cost.wastageAmount))
        if (cost.additionalTotal.value.signum() != 0) row("Additional costs", money(cost.additionalTotal))
        y += 2; canvas.drawLine(340f, y, RIGHT, y, body); y += 18
        val gl = Paint().apply { textSize = 13f; typeface = Typeface.DEFAULT_BOLD; color = Color.rgb(0x1D, 0x5B, 0x54) }
        canvas.drawText("TOTAL BATCH COST", 340f, y, gl); drawRight(money(cost.batchCost), RIGHT, gl); y += 20
        row("Sellable portions", cost.portions.toString())
        val cp = cost.costPerPortion
        row("Cost per portion", cp?.let { money(it) } ?: "—")

        // suggested prices
        val base = cp ?: cost.batchCost
        y += 10; need(90f)
        canvas.drawText("Suggested selling price (per portion)", M, y, h); y += 16
        val markup = settings.defaultMarkup.toBigDecimalOrNull()
        val margin = settings.defaultMargin.toBigDecimalOrNull()
        if (markup != null) {
            val price = Pricing.roundToStep(Pricing.markupPrice(base, markup), settings.priceStep.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO)
            canvas.drawText("Markup ${markup.stripTrailingZeros().toPlainString()}%", M, y, body); drawRight(money(price), RIGHT, body); y += 16
        }
        if (margin != null) {
            Pricing.marginPrice(base, margin)?.let { mp ->
                val price = Pricing.roundToStep(mp, settings.priceStep.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO)
                canvas.drawText("Gross margin ${margin.stripTrailingZeros().toPlainString()}%", M, y, body); drawRight(money(price), RIGHT, body); y += 16
            }
        }

        if (recipe.notes.isNotBlank()) { y += 8; need(40f); canvas.drawText("Notes: ${recipe.notes}", M, y, small) }

        footer(); doc.finishPage(page)
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val safe = recipe.name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "recipe" }
        val file = File(dir, "COGS-$safe.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
