package com.nothing.vault

import android.app.Application
import com.nothing.vault.data.repository.VaultRepository

class VaultApp : Application() {

    lateinit var repository: VaultRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = VaultRepository(this)
    }
}
