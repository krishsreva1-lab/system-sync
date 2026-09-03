package com.krish.systemsync.vault

import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.krish.systemsync.security.CryptographyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

enum class StorageMode {
    MAXIMUM_PRIVACY, // Internal storage + AES-256 encryption
    STANDARD_HIDDEN  // App-private external storage + .nomedia (unreadable by Gallery/Files apps)
}

data class VaultFile(
    val name: String,
    val size: Long,
    val lastModified: Long,
    val mode: StorageMode,
    val type: FileType
)

data class TrashedFile(
    val trashName: String,
    val originalName: String,
    val size: Long,
    val deletedAt: Long,
    val mode: StorageMode,
    val type: FileType
)

enum class FileType {
    IMAGE, VIDEO, AUDIO, DOCUMENT, OTHER
}

/**
 * Result of attempting to remove the original (public) copy of a file that was just hidden.
 * Scoped storage on Android 10+ frequently requires explicit user consent to delete a file
 * the app didn't create itself (e.g. a photo from DCIM), so this models the possible outcomes.
 */
sealed class RemoveOriginalResult {
    object Removed : RemoveOriginalResult()
    data class NeedsConsent(val intentSender: IntentSender) : RemoveOriginalResult()
    object NotRemovable : RemoveOriginalResult()
}

class VaultRepository(private val context: Context, private val isDummy: Boolean = false) {
    private val cryptoManager = CryptographyManager()

    private val vaultFolderName = if (isDummy) "dummy_vault" else "vault"
    private val hiddenFolderName = if (isDummy) ".dummy_vault" else ".vault"

    private val internalVaultDir = File(context.filesDir, vaultFolderName).apply { mkdirs() }
    private val externalHiddenDir = File(context.getExternalFilesDir(null), hiddenFolderName).apply {
        mkdirs()
        File(this, ".nomedia").createNewFile()
    }
    private val trashDir = File(context.filesDir, if (isDummy) "dummy_trash" else "trash").apply { mkdirs() }
    private val playbackCacheDir = File(context.cacheDir, "playback").apply { mkdirs() }

    private fun dirFor(mode: StorageMode) =
        if (mode == StorageMode.MAXIMUM_PRIVACY) internalVaultDir else externalHiddenDir

    /** Copies (and encrypts, if required) the picked file into the vault. Does NOT touch the source. */
    suspend fun importFile(uri: Uri, fileName: String, mode: StorageMode): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val destDir = dirFor(mode)
            val destFile = uniqueFile(destDir, fileName)

            val input = context.contentResolver.openInputStream(uri)
                ?: throw java.io.IOException("Unable to open selected file")
            input.use { inStream ->
                FileOutputStream(destFile).use { output ->
                    if (mode == StorageMode.MAXIMUM_PRIVACY) {
                        cryptoManager.encrypt(inStream, output)
                    } else {
                        inStream.copyTo(output)
                    }
                }
            }
            Unit
        }
    }

    /** Attempts to remove the original public file so it disappears from Gallery/Files apps. */
    fun requestRemoveOriginal(uri: Uri): RemoveOriginalResult {
        // 1) Try deleting straight through the document/media provider (works for most
        // SAF document URIs and for files the app already owns).
        try {
            val deleted = DocumentsContract.deleteDocument(context.contentResolver, uri)
            if (deleted) return RemoveOriginalResult.Removed
        } catch (_: Exception) {
            // fall through to MediaStore path below
        }
        try {
            val rows = context.contentResolver.delete(uri, null, null)
            if (rows > 0) return RemoveOriginalResult.Removed
        } catch (_: SecurityException) {
            // Android 10+ requires explicit consent for files the app doesn't own.
        } catch (_: Exception) {
        }

        // Android 11+ (API 30+): MediaStore.createDeleteRequest gives a system consent dialog.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return try {
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                RemoveOriginalResult.NeedsConsent(pendingIntent.intentSender)
            } catch (_: Exception) {
                RemoveOriginalResult.NotRemovable
            }
        }
        return RemoveOriginalResult.NotRemovable
    }

    suspend fun unhideFile(fileName: String, mode: StorageMode, type: FileType): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val srcFile = File(dirFor(mode), fileName)
            if (!srcFile.exists()) throw Exception("File not found in vault")

            val mimeType = getMimeType(fileName)
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val folder = when (type) {
                        FileType.IMAGE -> "Pictures/SystemSync"
                        FileType.VIDEO -> "Movies/SystemSync"
                        FileType.AUDIO -> "Music/SystemSync"
                        else -> "Download/SystemSync"
                    }
                    put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val collection = when (type) {
                FileType.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                FileType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                FileType.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Files.getContentUri("external")
                }
            }

            val uri = context.contentResolver.insert(collection, contentValues)
                ?: throw Exception("Failed to create MediaStore entry")

            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    FileInputStream(srcFile).use { input ->
                        if (mode == StorageMode.MAXIMUM_PRIVACY) {
                            cryptoManager.decrypt(input, output)
                        } else {
                            input.copyTo(output)
                        }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)
                }
                uri
            } catch (e: Exception) {
                context.contentResolver.delete(uri, null, null)
                throw e
            }
        }
    }

    private fun getMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
    }

    suspend fun exportFile(fileName: String, mode: StorageMode, destUri: Uri) = withContext(Dispatchers.IO) {
        val srcFile = File(dirFor(mode), fileName)
        if (!srcFile.exists()) return@withContext

        context.contentResolver.openOutputStream(destUri)?.use { output ->
            FileInputStream(srcFile).use { input ->
                if (mode == StorageMode.MAXIMUM_PRIVACY) {
                    cryptoManager.decrypt(input, output)
                } else {
                    input.copyTo(output)
                }
            }
            Unit
        }
    }

    suspend fun deleteFile(fileName: String, mode: StorageMode, permanent: Boolean = false) = withContext(Dispatchers.IO) {
        val srcFile = File(dirFor(mode), fileName)
        if (permanent) {
            srcFile.delete()
        } else {
            val trashFile = File(trashDir, "${System.currentTimeMillis()}_${mode.name}_$fileName")
            srcFile.renameTo(trashFile)
        }
    }

    suspend fun restoreFromTrash(trashName: String): Boolean = withContext(Dispatchers.IO) {
        val trashFile = File(trashDir, trashName)
        val parsed = parseTrashName(trashName) ?: return@withContext false
        val destDir = dirFor(parsed.mode)
        val destFile = uniqueFile(destDir, parsed.originalName)
        trashFile.renameTo(destFile)
    }

    suspend fun permanentlyDeleteFromTrash(trashName: String) = withContext(Dispatchers.IO) {
        File(trashDir, trashName).delete()
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        trashDir.listFiles()?.forEach { it.delete() }
    }

    fun getTrashFiles(): List<TrashedFile> {
        return trashDir.listFiles()?.mapNotNull { f ->
            val parsed = parseTrashName(f.name) ?: return@mapNotNull null
            TrashedFile(
                trashName = f.name,
                originalName = parsed.originalName,
                size = f.length(),
                deletedAt = parsed.deletedAt,
                mode = parsed.mode,
                type = getFileType(parsed.originalName)
            )
        }?.sortedByDescending { it.deletedAt } ?: emptyList()
    }

    suspend fun renameFile(oldName: String, newName: String, mode: StorageMode) = withContext(Dispatchers.IO) {
        val dir = dirFor(mode)
        val oldFile = File(dir, oldName)
        val newFile = File(dir, newName)
        oldFile.renameTo(newFile)
    }

    fun getFiles(mode: StorageMode): List<VaultFile> {
        val dir = dirFor(mode)
        return dir.listFiles()?.filter { it.isFile && it.name != ".nomedia" }?.map {
            VaultFile(
                name = it.name,
                size = it.length(),
                lastModified = it.lastModified(),
                mode = mode,
                type = getFileType(it.name)
            )
        } ?: emptyList()
    }

    /** All vault files across both storage modes, newest first — used for the Home/Recent list and the player queue. */
    fun getAllFiles(): List<VaultFile> =
        (getFiles(StorageMode.MAXIMUM_PRIVACY) + getFiles(StorageMode.STANDARD_HIDDEN))
            .sortedByDescending { it.lastModified }

    /**
     * Resolves a playable local file for a vault entry. Encrypted (MAXIMUM_PRIVACY) files are
     * decrypted into a private cache file first since the player needs random/streamable access;
     * STANDARD_HIDDEN files are already plain and are returned directly.
     */
    suspend fun preparePlaybackFile(file: VaultFile): File = withContext(Dispatchers.IO) {
        val src = File(dirFor(file.mode), file.name)
        if (file.mode == StorageMode.STANDARD_HIDDEN) return@withContext src

        val cacheFile = File(playbackCacheDir, "play_${file.mode.name}_${file.name}")
        if (!cacheFile.exists() || cacheFile.lastModified() < src.lastModified()) {
            FileInputStream(src).use { input ->
                FileOutputStream(cacheFile).use { output ->
                    cryptoManager.decrypt(input, output)
                }
            }
        }
        cacheFile
    }

    fun clearPlaybackCache() {
        playbackCacheDir.listFiles()?.forEach { it.delete() }
    }

    private fun uniqueFile(dir: File, desiredName: String): File {
        var candidate = File(dir, desiredName)
        if (!candidate.exists()) return candidate
        val dot = desiredName.lastIndexOf('.')
        val base = if (dot > 0) desiredName.substring(0, dot) else desiredName
        val ext = if (dot > 0) desiredName.substring(dot) else ""
        var i = 1
        while (candidate.exists()) {
            candidate = File(dir, "${base}_$i$ext")
            i++
        }
        return candidate
    }

    private data class ParsedTrashName(val deletedAt: Long, val mode: StorageMode, val originalName: String)

    private fun parseTrashName(trashName: String): ParsedTrashName? {
        val firstUnderscore = trashName.indexOf('_')
        if (firstUnderscore < 0) return null
        val timestamp = trashName.substring(0, firstUnderscore).toLongOrNull() ?: return null
        val rest = trashName.substring(firstUnderscore + 1)
        val mode = when {
            rest.startsWith(StorageMode.MAXIMUM_PRIVACY.name + "_") -> StorageMode.MAXIMUM_PRIVACY
            rest.startsWith(StorageMode.STANDARD_HIDDEN.name + "_") -> StorageMode.STANDARD_HIDDEN
            else -> return null
        }
        val originalName = rest.substring(mode.name.length + 1)
        return ParsedTrashName(timestamp, mode, originalName)
    }

    private fun getFileType(fileName: String): FileType {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic" -> FileType.IMAGE
            "mp4", "mkv", "avi", "mov", "webm", "3gp" -> FileType.VIDEO
            "mp3", "wav", "m4a", "ogg", "flac", "aac" -> FileType.AUDIO
            "pdf", "doc", "docx", "txt", "xlsx" -> FileType.DOCUMENT
            else -> FileType.OTHER
        }
    }
}
