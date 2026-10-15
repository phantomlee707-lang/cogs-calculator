# COGS Calculator

**Offline-first cost-of-goods-sold calculator for small food businesses.** Know what your products really cost to make — ingredient costs, per-portion cost, and selling-price suggestions — entirely on your device. No account, no internet.

A [Rainweaver Studio](https://rainweaver.qd.je) app.

---

## What it does

- **Ingredient catalog** — enter purchase price & quantity, get an automatic **cost per base unit** (Rp16,000 / 1 kg flour → Rp16/g). Edit, search, archive.
- **Units & conversions** — weight (kg/g), volume (L/ml), count (pcs). Compatible units auto-convert; **incompatible units are rejected**, and cross-type use (e.g. cups of flour priced per gram) requires an explicit conversion factor.
- **Recipes** — build from your catalog with quantities, units, sellable portions, optional wastage, and additional costs (packaging, labor, fuel).
- **Cost engine** — ingredient cost, batch cost, and **cost per portion**, using exact decimal arithmetic (no floating-point).
- **Auto-recalculation** — change an ingredient price and every recipe using it updates. **Historical snapshots** are preserved and immutable.
- **Selling-price calculator** — **markup on cost** (cost × (1+markup%)) vs **target gross margin** (cost ÷ (1−margin%)), clearly distinguished, with price-step rounding.
- **PDF costing reports** — professional, print-friendly, multi-page; marks current vs snapshot.
- **Backup & restore** — local export/import of all data.

## Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- **Room** for local persistence with immutable snapshots
- **Android `PdfDocument`** for on-device reports
- **FileProvider + SAF** for sharing and backup
- **min SDK 24** (Android 7.0)

## Download

Grab the latest from [**Releases**](https://github.com/phantomlee707-lang/cogs-calculator/releases) (debug APK also hosted at rainweaver.qd.je).

## Build from source

```bash
git clone https://github.com/phantomlee707-lang/cogs-calculator.git
cd cogs-calculator
./gradlew assembleDebug
```

> Requires JDK 17 and the Android SDK (compileSdk 34).

## Tests

```bash
./gradlew testDebugUnitTest
```

Covers money arithmetic, unit conversions (incl. incompatible-unit rejection), ingredient unit-cost, the cost engine (line/batch/per-portion), and markup vs margin pricing.

## License

© Rainweaver Studio. All rights reserved.
