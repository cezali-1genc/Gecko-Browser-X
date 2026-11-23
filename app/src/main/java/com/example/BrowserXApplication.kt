package com.example

import android.app.Application
import com.example.data.local.BrowserDatabase
import com.example.data.preferences.SettingsRepository
import com.example.data.repository.BrowserDataRepository
import com.example.data.repository.CredentialRepository
import com.example.data.repository.DownloadBridge
import com.example.data.repository.SearchSuggestionRepository
import com.example.data.repository.ToolsRepository
import com.example.engine.BrowserEngine
import com.example.engine.GeckoEngineManager

class AppContainer(application: Application) {
    val database: BrowserDatabase by lazy { BrowserDatabase.getInstance(application) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(application) }
    val dataRepository: BrowserDataRepository by lazy {
        BrowserDataRepository(
            bookmarkDao = database.bookmarkDao(),
            historyDao = database.historyDao(),
            downloadDao = database.downloadDao(),
            sitePermissionDao = database.sitePermissionDao(),
            speedDialDao = database.speedDialDao(),
            readingListDao = database.readingListDao(),
            extensionDao = database.extensionDao(),
            dictionaryDao = database.dictionaryDao()
        )
    }
    val credentialRepository: CredentialRepository by lazy {
        CredentialRepository(application, database.credentialDao())
    }
    val toolsRepository: ToolsRepository by lazy {
        ToolsRepository(application)
    }
    val searchSuggestionRepository: SearchSuggestionRepository by lazy {
        SearchSuggestionRepository(dataRepository)
    }
    val downloadBridge: DownloadBridge by lazy {
        DownloadBridge(application, dataRepository)
    }
    val browserEngine: BrowserEngine by lazy {
        GeckoEngineManager(application)
    }
}

class BrowserXApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
