package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BrowserDatabase
import com.example.data.preferences.SettingsRepository
import com.example.data.repository.BrowserDataRepository
import com.example.data.repository.CredentialRepository
import com.example.data.repository.DownloadBridge
import com.example.data.repository.SearchSuggestionRepository
import com.example.data.repository.ToolsRepository
import com.example.ui.state.BrowserViewModel
import com.example.ui.theme.BrowserXTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BrowserUiRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: BrowserDatabase
    private lateinit var viewModel: BrowserViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, BrowserDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dataRepo = BrowserDataRepository(
            bookmarkDao = db.bookmarkDao(),
            historyDao = db.historyDao(),
            downloadDao = db.downloadDao(),
            sitePermissionDao = db.sitePermissionDao(),
            speedDialDao = db.speedDialDao(),
            readingListDao = db.readingListDao(),
            extensionDao = db.extensionDao(),
            dictionaryDao = db.dictionaryDao()
        )
        viewModel = BrowserViewModel(
            browserEngine = FakeBrowserEngine(),
            dataRepository = dataRepo,
            settingsRepository = SettingsRepository(context),
            suggestionRepository = SearchSuggestionRepository(dataRepo),
            downloadBridge = DownloadBridge(context, dataRepo),
            credentialRepository = CredentialRepository(context, db.credentialDao()),
            toolsRepository = ToolsRepository(context)
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `start page shows single Opera search bar, navigates tabs, settings, passwords and formal about screen`() {
        composeRule.setContent {
            BrowserXTheme {
                BrowserXRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // Branding & single search box on Start Page (top address bar hidden when Start Page active)
        composeRule.onNodeWithText("MσdLU 1APK BROWSER X").assertIsDisplayed()
        assertEquals(1, composeRule.onAllNodesWithTag("start_page_search_box").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("address_bar_field").fetchSemanticsNodes().size)

        // Open Tab Switcher & create Private tab (Turkish default: "Gizli Oturum")
        composeRule.onNodeWithTag("tabs_switcher_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("new_private_tab_button").assertIsDisplayed()
        composeRule.onNodeWithTag("new_private_tab_button").performClick()
        composeRule.mainClock.advanceTimeBy(600)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Gizli Oturum").assertIsDisplayed()

        // Open Opera Menu -> Settings -> Password Manager
        composeRule.onNodeWithTag("browser_menu_button").performClick()
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("menu_settings").performClick()
        composeRule.mainClock.advanceTimeBy(600)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Ayarlar").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("setting_passwords"))
        composeRule.onNodeWithTag("setting_passwords").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Şifre Yöneticisi").assertIsDisplayed()
        composeRule.onNodeWithTag("pw_autofill_switch").assertIsDisplayed()

        // Navigate back to Settings and open Formal About screen
        composeRule.onNodeWithContentDescription("Geri git").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("setting_about"))
        composeRule.onNodeWithTag("setting_about").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Hakkında").assertIsDisplayed()
        composeRule.onNodeWithTag("about_screen_list")
            .performScrollToNode(hasTestTag("about_official_portal_btn"))
        composeRule.onNodeWithText("SeRhAt GüNeR").assertIsDisplayed()
        composeRule.onNodeWithText("Telif Hakkı Bildirimleri (Madde 1–9)").assertIsDisplayed()
    }
}
