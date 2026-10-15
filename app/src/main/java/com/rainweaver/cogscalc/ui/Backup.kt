package com.rainweaver.cogscalc.ui

import android.content.Context
import android.net.Uri
import com.rainweaver.cogscalc.data.local.CogsDatabase
import java.io.File
import java.io.FileOutputStream

/** Local backup/restore of the whole database. Nothing is ever uploaded. */
object Backup {

    fun export(context: Context, uri: Uri): Boolean = try {
        val db = CogsDatabase.get(context)
        val tmp = File(context.cacheDir, "backup_export.db").apply { if (exists()) delete() }
        db.openHelper.writableDatabase.execSQL("VACUUM INTO '${tmp.absolutePath}'")
        val out = context.contentResolver.openOutputStream(uri) ?: return false
        out.use { o -> tmp.inputStream().use { it.copyTo(o) } }
        tmp.delete(); true
    } catch (e: Exception) { false }

    fun restore(context: Context, uri: Uri): Boolean = try {
        val dbFile = context.getDatabasePath("cogs.db")
        val input = context.contentResolver.openInputStream(uri) ?: return false
        input.use { i -> FileOutputStream(dbFile).use { i.copyTo(it) } }
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
        CogsDatabase.resetInstance(); true
    } catch (e: Exception) { false }
}
