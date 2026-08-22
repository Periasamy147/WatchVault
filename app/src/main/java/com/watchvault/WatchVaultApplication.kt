package com.watchvault

import android.app.Application
import com.watchvault.di.AppContainer

class WatchVaultApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
