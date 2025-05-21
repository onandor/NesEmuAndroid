package com.onandor.nesemu

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.ui.model.UiLibraryEntry
import com.onandor.nesemu.ui.screens.LibraryBrowser
import com.onandor.nesemu.ui.screens.LibraryScreen
import com.onandor.nesemu.ui.theme.NesEmuTheme
import com.onandor.nesemu.viewmodels.LibraryViewModel
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LibraryScreenTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun libraryBrowserTest_TestEvents() {
        var navigateUpEventFired = false
        var clickedEntryDisplayName = ""

        composeTestRule.setContent {
            LibraryBrowser(
                entries = TEST_ENTRIES,
                coverArtUrls = emptyMap(),
                path = "",
                inSubdirectory = true,
                slideBackwards = false,
                onEvent = { event ->
                    if (event is LibraryViewModel.Event.OnNavigateUp) {
                        navigateUpEventFired = true
                    } else if (event is LibraryViewModel.Event.OnOpenLibraryEntry) {
                        clickedEntryDisplayName = event.entry.displayName
                    }
                }
            )
        }

        composeTestRule.onNodeWithContentDescription("Navigate up").performClick()
        assertTrue(navigateUpEventFired)

        composeTestRule.onNodeWithText("test_file_1").performClick()
        assertEquals("test_file_1", clickedEntryDisplayName)

        composeTestRule.onNodeWithText("test_file_2").performClick()
        assertEquals("test_file_2", clickedEntryDisplayName)
    }

    companion object {
        private val TEST_ENTRIES = listOf<UiLibraryEntry>(
            UiLibraryEntry(
                entity = LibraryEntry(
                    id = 0L,
                    romHash = "",
                    name = "test_directory",
                    uri = "",
                    isDirectory = true,
                    parentDirectoryUri = null
                ),
                displayName = "test_directory",
                lastPlayedDate = ""
            ),
            UiLibraryEntry(
                entity = LibraryEntry(
                    id = 1L,
                    romHash = "",
                    name = "test_file_1",
                    uri = "",
                    isDirectory = true,
                    parentDirectoryUri = null
                ),
                displayName = "test_file_1",
                lastPlayedDate = ""
            ),
            UiLibraryEntry(
                entity = LibraryEntry(
                    id = 2L,
                    romHash = "",
                    name = "test_file_2",
                    uri = "",
                    isDirectory = true,
                    parentDirectoryUri = null
                ),
                displayName = "test_file_2",
                lastPlayedDate = ""
            ),
        )
    }
}