package com.nothing.vault.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.util.concurrent.Semaphore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nothing.vault.data.crypto.AesEncryption
import com.nothing.vault.data.crypto.KeyStoreManager
import com.nothing.vault.data.model.VaultFolder
import com.nothing.vault.data.model.VaultPhoto
import com.nothing.vault.util.SecurityUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.LinkedHashMap
import java.util.UUID

class VaultRepository(private val context: Context) {

    private val vaultDir: File get() = File(context.filesDir, "vault").also { it.mkdirs() }
    private val foldersFile: File get() = File(context.filesDir, "folders.json")
    private fun photosFile(folderId: String) = File(context.filesDir, "photos_${folderId}.json")

    private var foldersCache: List<VaultFolder>? = null
    private var foldersCacheDirty = true

    private val decryptSemaphore = Semaphore(2)

    private val thumbnailCache = object : LinkedHashMap<String, Bitmap>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>): Boolean =
            size > 128
    }

    private val photoCache = object : LinkedHashMap<String, Bitmap>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>): Boolean =
            size > 16
    }

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "vault_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun isFirstLaunch(): Boolean = !foldersFile.exists()

    fun getFolders(): List<VaultFolder> {
        if (foldersCache != null && !foldersCacheDirty) {
            return foldersCache!!
        }
        if (!foldersFile.exists()) return emptyList()
        val json = JSONArray(foldersFile.readText())
        val folders = (0 until json.length()).map { VaultFolder.fromJson(json.getJSONObject(it)) }
        foldersCache = folders
        foldersCacheDirty = false
        return folders
    }

    fun saveFolder(folder: VaultFolder) {
        foldersCacheDirty = true
        val folders = getFolders().toMutableList()
        val existing = folders.indexOfFirst { it.id == folder.id }
        if (existing >= 0) folders[existing] = folder
        else folders.add(folder)
        writeFolders(folders)
    }

    fun deleteFolder(folderId: String) {
        foldersCacheDirty = true
        val folders = getFolders().filter { it.id != folderId }
        writeFolders(folders)

        vaultDir.listFiles()?.filter {
            it.name.startsWith(folderId)
        }?.forEach { it.delete() }

        photosFile(folderId).delete()
    }

    private fun writeFolders(folders: List<VaultFolder>) {
        val arr = JSONArray()
        folders.forEach { arr.put(it.toJson()) }
        foldersFile.writeText(arr.toString())
    }

    fun findFolderByPin(pin: String): VaultFolder? {
        return getFolders().firstOrNull { folder ->
            SecurityUtils.verifyPin(pin, folder.pinHash, folder.pinSalt)
        }
    }

    fun getBiometricFolder(): VaultFolder? {
        val folderId = prefs.getString("biometric_folder_id", null) ?: return null
        return getFolders().firstOrNull { it.id == folderId }
    }

    fun setBiometricFolder(folderId: String) {
        prefs.edit().putString("biometric_folder_id", folderId).apply()
        getFolders().firstOrNull { it.id == folderId }?.let {
            saveFolder(it.copy(isBiometricEnabled = true))
        }
    }

    fun clearBiometricFolder() {
        prefs.edit().remove("biometric_folder_id").apply()
        getFolders().firstOrNull()?.let {
            saveFolder(it.copy(isBiometricEnabled = false))
        }
    }

    fun createFolder(name: String, pin: String): VaultFolder {
        val salt = SecurityUtils.generateSalt()
        val saltHex = SecurityUtils.bytesToHex(salt)
        val pinHash = SecurityUtils.hashPin(pin, salt)
        val folder = VaultFolder(
            id = UUID.randomUUID().toString(),
            name = name,
            pinHash = pinHash,
            pinSalt = saltHex
        )
        saveFolder(folder)
        return folder
    }

    fun getPhotos(folderId: String): List<VaultPhoto> {
        val file = photosFile(folderId)
        if (!file.exists()) return emptyList()
        val json = JSONArray(file.readText())
        return (0 until json.length()).map { VaultPhoto.fromJson(json.getJSONObject(it)) }
    }

    private fun writePhotos(folderId: String, photos: List<VaultPhoto>) {
        val arr = JSONArray()
        photos.forEach { arr.put(it.toJson()) }
        photosFile(folderId).writeText(arr.toString())
    }

    fun importPhoto(
        folderId: String,
        uri: Uri,
        originalName: String,
        onProgress: (Int) -> Unit = {}
    ): Result<VaultPhoto> {
        return try {
            val key = KeyStoreManager.getSecretKey()
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("Cannot open file"))

            val tempFile = File(vaultDir, "temp_${UUID.randomUUID()}")
            inputStream.use { it?.copyTo(tempFile.outputStream()) }

            val encryptedFile = File(vaultDir, "${folderId}_${UUID.randomUUID()}.enc")
            val iv = AesEncryption.encrypt(tempFile, encryptedFile, key)

            val thumbnailFile = File(vaultDir, "${folderId}_thumb_${UUID.randomUUID()}.jpg")
            createThumbnail(tempFile, thumbnailFile)

            tempFile.delete()

            val photo = VaultPhoto(
                id = UUID.randomUUID().toString(),
                folderId = folderId,
                encryptedFileName = encryptedFile.name,
                thumbnailFileName = thumbnailFile.name,
                originalName = originalName,
                mimeType = "image/jpeg",
                ivHex = SecurityUtils.bytesToHex(iv),
                fileSize = encryptedFile.length()
            )

            val photos = getPhotos(folderId).toMutableList()
            photos.add(photo)
            writePhotos(folderId, photos)

            Result.success(photo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deletePhoto(photo: VaultPhoto) {
        File(vaultDir, photo.encryptedFileName).delete()
        File(vaultDir, photo.thumbnailFileName).delete()
        val photos = getPhotos(photo.folderId).filter { it.id != photo.id }
        writePhotos(photo.folderId, photos)
        synchronized(photoCache) { photoCache.remove(photo.id) }
        synchronized(thumbnailCache) { thumbnailCache.remove(photo.id) }
    }

    fun getCachedBitmap(photoId: String): Bitmap? {
        synchronized(photoCache) {
            return photoCache[photoId]
        }
    }

    fun cacheBitmap(photoId: String, bitmap: Bitmap) {
        synchronized(photoCache) {
            photoCache[photoId] = bitmap
        }
    }

    fun clearBitmapCache() {
        synchronized(photoCache) {
            photoCache.values.forEach { it.recycle() }
            photoCache.clear()
        }
    }

    fun decryptPhotoToBitmap(photo: VaultPhoto): Bitmap? {
        getCachedBitmap(photo.id)?.let { return it }
        decryptSemaphore.acquire()
        return try {
            val key = KeyStoreManager.getSecretKey()
            val encryptedFile = File(vaultDir, photo.encryptedFileName)
            val iv = SecurityUtils.hexToBytes(photo.ivHex)
            val bytes = AesEncryption.decryptToBytes(encryptedFile, key, iv)

            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
                cacheBitmap(photo.id, bitmap)
                return bitmap
            }

            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            if (opts.outWidth > 0 && opts.outHeight > 0) {
                var sampleSize = 1
                while (opts.outWidth / sampleSize > 2048 || opts.outHeight / sampleSize > 2048) {
                    sampleSize *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val sampled = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
                if (sampled != null) {
                    cacheBitmap(photo.id, sampled)
                    return sampled
                }
            }
            null
        } catch (e: Exception) {
            null
        } finally {
            decryptSemaphore.release()
        }
    }

    fun decryptPhotoToBitmapSampled(photo: VaultPhoto, maxDimension: Int = 2048): Bitmap? {
        return try {
            val key = KeyStoreManager.getSecretKey()
            val encryptedFile = File(vaultDir, photo.encryptedFileName)
            val iv = SecurityUtils.hexToBytes(photo.ivHex)
            val bytes = AesEncryption.decryptToBytes(encryptedFile, key, iv)

            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)

            if (opts.outWidth <= 0 || opts.outHeight <= 0) {
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }

            val sampleSize = calculateSampleSize(opts.outWidth, opts.outHeight, maxDimension)

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
        } catch (e: Exception) {
            null
        }
    }

    fun warmPhotoCache(photos: List<VaultPhoto>, centerIndex: Int = -1, count: Int = 30, preDecrypt: Boolean = false) {
        if (photos.isEmpty()) return
        val start = if (centerIndex < 0) 0 else (centerIndex - count / 2).coerceAtLeast(0)
        val end = (start + count).coerceAtMost(photos.size)
        for (i in start until end) {
            val photo = photos[i]
            if (!thumbnailCache.containsKey(photo.id)) {
                getThumbnailBitmap(photo)
            }
        }
        if (preDecrypt) {
            val first = photos.firstOrNull()
            if (first != null && !photoCache.containsKey(first.id)) {
                decryptPhotoToBitmap(first)
            }
            if (photos.size > 1) {
                val second = photos[1]
                if (!photoCache.containsKey(second.id)) {
                    decryptPhotoToBitmap(second)
                }
            }
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        while (width / sampleSize > maxDimension || height / sampleSize > maxDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }

    fun getCachedThumbnail(photoId: String): Bitmap? {
        synchronized(thumbnailCache) {
            return thumbnailCache[photoId]
        }
    }

    fun getThumbnailBitmap(photo: VaultPhoto): Bitmap? {
        synchronized(thumbnailCache) {
            thumbnailCache[photo.id]?.let { return it }
        }
        val thumbFile = File(vaultDir, photo.thumbnailFileName)
        val bitmap = if (thumbFile.exists()) {
            BitmapFactory.decodeFile(thumbFile.absolutePath)
        } else null
        if (bitmap != null) {
            synchronized(thumbnailCache) {
                thumbnailCache[photo.id] = bitmap
            }
        }
        return bitmap
    }

    private fun createThumbnail(source: File, output: File, maxSize: Int = 512) {
        val bitmap = BitmapFactory.decodeFile(source.absolutePath)
        if (bitmap != null) {
            val scale = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height, 1f)
            val thumb = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
            output.outputStream().use { thumb.compress(Bitmap.CompressFormat.JPEG, 80, it) }
            if (thumb != bitmap) thumb.recycle()
            bitmap.recycle()
        }
    }

    fun recoverPhoto(photo: VaultPhoto): Boolean {
        return try {
            val key = KeyStoreManager.getSecretKey()
            val encryptedFile = File(vaultDir, photo.encryptedFileName)
            val iv = SecurityUtils.hexToBytes(photo.ivHex)
            val bytes = AesEncryption.decryptToBytes(encryptedFile, key, iv)

            val filename = "${photo.id}.jpg"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/recovere")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                )
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { out ->
                        out.write(bytes)
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(it, values, null, null)
                    true
                } ?: false
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                )
                val recoverDir = File(dir, "recovere")
                recoverDir.mkdirs()
                File(recoverDir, filename).writeBytes(bytes)
                val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                intent.data = Uri.fromFile(File(recoverDir, filename))
                context.sendBroadcast(intent)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun clearDecoyFlag() {
        prefs.edit().putBoolean("in_decoy", false).apply()
    }

    fun isInDecoy(): Boolean = prefs.getBoolean("in_decoy", false)

    fun setInDecoy(value: Boolean) {
        prefs.edit().putBoolean("in_decoy", value).apply()
    }
}
