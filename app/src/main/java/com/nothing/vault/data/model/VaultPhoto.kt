package com.nothing.vault.data.model

import org.json.JSONObject

data class VaultPhoto(
    val id: String,
    val folderId: String,
    val encryptedFileName: String,
    val thumbnailFileName: String,
    val originalName: String,
    val mimeType: String,
    val ivHex: String,
    val fileSize: Long,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("folderId", folderId)
        put("encryptedFileName", encryptedFileName)
        put("thumbnailFileName", thumbnailFileName)
        put("originalName", originalName)
        put("mimeType", mimeType)
        put("ivHex", ivHex)
        put("fileSize", fileSize)
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(json: JSONObject): VaultPhoto = VaultPhoto(
            id = json.getString("id"),
            folderId = json.getString("folderId"),
            encryptedFileName = json.getString("encryptedFileName"),
            thumbnailFileName = json.optString("thumbnailFileName", ""),
            originalName = json.optString("originalName", ""),
            mimeType = json.optString("mimeType", "image/jpeg"),
            ivHex = json.getString("ivHex"),
            fileSize = json.optLong("fileSize", 0),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }
}
