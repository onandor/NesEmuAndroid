package com.onandor.nesemu.viewmodels

import com.onandor.nesemu.data.preferences.PreferenceManager
import com.onandor.nesemu.domain.service.InputService
import com.onandor.nesemu.domain.service.LibraryService
import com.onandor.nesemu.navigation.NavigationManager
import io.mockk.every
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PreferencesViewModelTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    val navManager = mockk<NavigationManager>()

    val inputService = mockk<InputService> {
        every { state } returns MutableStateFlow(InputService.State())
    }

    val libraryService = mockk<LibraryService> {
        every { state } returns MutableStateFlow(LibraryService.State())
    }

    val prefManager = mockk<PreferenceManager> {
        every { observeSteamGridDBApiKey() } returns flowOf("")
        every { observeUseDarkTheme() } returns flowOf(false)
    }

    lateinit var viewModel: PreferencesViewModel

    @Before
    fun setup() {
        viewModel = PreferencesViewModel(navManager, inputService, libraryService, prefManager)
    }

    @Test
    fun testWorks() {

    }
}