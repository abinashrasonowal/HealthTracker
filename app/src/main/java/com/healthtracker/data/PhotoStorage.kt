package com.healthtracker.data

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Keeps profile photos inside app-private storage so they survive the
 * original image being moved or deleted, and never leave the device.
 */
class PhotoStorage(private val context: Context) {

    private val dir: File get() = File(context.filesDir, "photos").apply { mkdirs() }

    /** Copies the picked image into app storage and returns its file Uri string. */
    suspend fun import(source: Uri): String? = withContext(Dispatchers.IO) {
        val target = File(dir, "${UUID.randomUUID()}.jpg")
        val copied = context.contentResolver.openInputStream(source)?.use { input ->
            target.outputStream().use { input.copyTo(it) }
        }
        if (copied == null) null else Uri.fromFile(target).toString()
    }

    suspend fun delete(photoUri: String?) = withContext(Dispatchers.IO) {
        val path = photoUri?.toUri()?.path ?: return@withContext
        val file = File(path)
        // Only ever delete files we own.
        if (file.parentFile == dir) file.delete()
    }
}
