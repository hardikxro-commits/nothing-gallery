package com.nothing.vault.data.model

import org.json.JSONObject

data class VaultFolder(
    val id: String,
    val name: String,
    val pinHash: String,
    val pinSalt: String,
    val isBiometricEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("pinHash", pinHash)
        put("pinSalt", pinSalt)
        put("isBiometricEnabled", isBiometricEnabled)
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(json: JSONObject): VaultFolder = VaultFolder(
            id = json.getString("id"),
            name = json.getString("name"),
            pinHash = json.getString("pinHash"),
            pinSalt = json.getString("pinSalt"),
            isBiometricEnabled = json.optBoolean("isBiometricEnabled", false),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }
}
