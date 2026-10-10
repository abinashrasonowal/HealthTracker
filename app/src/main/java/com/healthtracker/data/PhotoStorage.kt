package com.healthtracker.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.max

/**
 * Keeps profile photos inside app-private storage so they survive the
 * original image being moved or deleted, and never leave the device.
 */
class PhotoStorage(private val context: Context) {

    private companion object {
        /** Plenty for a profile picture, even full-screen; keeps backups small. */
        const val MAX_SIDE = 1024
        const val JPEG_QUALITY = 85
    }

    private val dir: File get() = File(context.filesDir, "photos").apply { mkdirs() }

    /**
     * Saves a picked image into app storage, shrunk to at most [MAX_SIDE] pixels and turned
     * upright (camera photos are often stored sideways with a rotation tag). Returns its file
     * Uri string, or null if the image can't be read.
     */
    suspend fun import(source: Uri): String? = withContext(Dispatchers.IO) {
        val bitmap = decodeUpright(source) ?: return@withContext null
        val target = File(dir, "${UUID.randomUUID()}.jpg")
        target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        bitmap.recycle()
        Uri.fromFile(target).toString()
    }

    private fun decodeUpright(source: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // With inJustDecodeBounds the decode always returns null; only the size in [bounds] matters.
        val stream = resolver.openInputStream(source) ?: return null
        stream.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longest = max(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) return null

        // Decode at a power-of-two reduction first so a 12-megapixel photo never sits in memory whole.
        var sample = 1
        while (longest / (sample * 2) >= MAX_SIDE) sample *= 2
        val decoded = resolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null

        val degrees = resolver.openInputStream(source)?.use { rotationOf(ExifInterface(it)) } ?: 0
        val scale = (MAX_SIDE.toFloat() / max(decoded.width, decoded.height)).coerceAtMost(1f)
        if (degrees == 0 && scale == 1f) return decoded
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(degrees.toFloat())
        }
        return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            .also { if (it !== decoded) decoded.recycle() }
    }

    private fun rotationOf(exif: ExifInterface): Int =
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

    /** Saves raw image bytes (e.g. from a backup) and returns the new file Uri string. */
    suspend fun write(bytes: ByteArray): String? = withContext(Dispatchers.IO) {
        val target = File(dir, "${UUID.randomUUID()}.jpg")
        target.writeBytes(bytes)
        Uri.fromFile(target).toString()
    }

    /** Reads a photo we own, or null if it's missing. */
    suspend fun read(photoUri: String): ByteArray? = withContext(Dispatchers.IO) {
        val file = photoUri.toUri().path?.let(::File)
        file?.takeIf { it.parentFile == dir && it.exists() }?.readBytes()
    }

    suspend fun delete(photoUri: String?) = withContext(Dispatchers.IO) {
        val path = photoUri?.toUri()?.path ?: return@withContext
        val file = File(path)
        // Only ever delete files we own.
        if (file.parentFile == dir) file.delete()
    }
}
