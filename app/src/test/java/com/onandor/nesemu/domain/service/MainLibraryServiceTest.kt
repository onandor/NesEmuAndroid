package com.onandor.nesemu.domain.service

import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.data.entity.LibraryEntryWithDate
import com.onandor.nesemu.data.preferences.PreferenceManager
import com.onandor.nesemu.data.repository.LibraryEntryRepository
import com.onandor.nesemu.util.Document
import com.onandor.nesemu.util.DocumentAccessor
import com.onandor.nesemu.util.sha1Hash
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.junit4.MockKRule
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MainLibraryServiceTest {

    val testScope = CoroutineScope(Dispatchers.Unconfined)

    val mockPrefManager = mockk<PreferenceManager> {
        coEvery { getLibraryUri() } returns "test:/library"
        every { observeLibraryUri() } returns flowOf("test:/library")
    }

    val mockDocumentAccessor = mockk<DocumentAccessor> {
        coEvery { getDocumentName("test:/library") } returns "library"
        coEvery { readBytes(any()) } returns ByteArray(0)
    }

    val mockLibraryEntryRepository = mockk<LibraryEntryRepository> {
        coEvery { getLibraryRoot() } returns libraryRootEntry
        coEvery { findByUri("test:/library") } returns libraryRootEntry
        every { observeAllByParentDirectoryUri("test:/library") } returns flowOf(testRootEntries)
        every { observeAllByParentDirectoryUri("test:/library/subdirectory") } returns
                flowOf(testSubdirectoryEntries)
        coEvery { upsertLibraryDirectory(any(), any()) } returns libraryRootEntry
        coEvery { upsert(any<List<LibraryEntry>>()) } just Runs
        coEvery { deleteAll() } just Runs
    }

    val mockCoverArtService = mockk<CoverArtService> {
        coJustRun { sourceUrls(any()) }
    }

    @Test
    fun libraryServiceTest_NoLibraryUri_NothingHappens() {
        coEvery { mockPrefManager.getLibraryUri() } returns ""
        every { mockPrefManager.observeLibraryUri() } returns flowOf("")

        val libraryService = MainLibraryService(testScope, mockPrefManager, mockDocumentAccessor,
            mockLibraryEntryRepository, mockCoverArtService)

        Thread.sleep(100)

        coVerify(exactly = 1) { mockPrefManager.getLibraryUri() }
        coVerify(exactly = 0) { mockLibraryEntryRepository.getLibraryRoot() }
        assertEquals(null, libraryService.state.value.libraryDirectory)
    }

    @Test
    fun libraryServiceTest_GivenLibraryUri_DisplayedEntriesCorrect() {
        val libraryService = MainLibraryService(testScope, mockPrefManager, mockDocumentAccessor,
            mockLibraryEntryRepository, mockCoverArtService)

        Thread.sleep(100)

        coVerify(exactly = 1) { mockLibraryEntryRepository.getLibraryRoot() }
        assertEquals(libraryRootEntry, libraryService.state.value.libraryDirectory)
        assertEquals(testRootEntries.size, libraryService.displayedEntries.value.size)
        assertTrue(libraryService.displayedEntries.value.containsAll(testRootEntries))
    }

    @Test
    fun libraryServiceTest_LibraryUriSet_DisplayedEntriesUpdated() {
        val libraryUriFlow = MutableStateFlow("")

        coEvery { mockPrefManager.getLibraryUri() } returns libraryUriFlow.value
        every { mockPrefManager.observeLibraryUri() } returns libraryUriFlow
        coEvery { mockPrefManager.updateLibraryUri(any()) } coAnswers {
            libraryUriFlow.value = firstArg<String>()
        }
        every { mockDocumentAccessor.traverseDirectory(any()) } returns testRootDocuments

        val libraryService = MainLibraryService(testScope, mockPrefManager, mockDocumentAccessor,
            mockLibraryEntryRepository, mockCoverArtService)

        Thread.sleep(100)

        assertEquals(emptyList<LibraryEntryWithDate>(), libraryService.displayedEntries.value)

        runBlocking { libraryService.changeLibraryUri("test:/library") }

        Thread.sleep(100)

        coVerify(exactly = 1) { mockLibraryEntryRepository.deleteAll() }
        coVerify(exactly = 1) { mockLibraryEntryRepository.upsert(any<List<LibraryEntry>>()) }
        coVerifyOrder {
            mockLibraryEntryRepository.deleteAll()
            mockLibraryEntryRepository.upsert(any<List<LibraryEntry>>())
        }

        assertEquals(libraryRootEntry, libraryService.state.value.libraryDirectory)
        assertEquals(testRootEntries.size, libraryService.displayedEntries.value.size)
        assertTrue(libraryService.displayedEntries.value.containsAll(testRootEntries))
    }

    @Test
    fun libraryServiceTest_OnNavigation_DisplayedEntriesUpdated() {
        val libraryService = MainLibraryService(testScope, mockPrefManager, mockDocumentAccessor,
            mockLibraryEntryRepository, mockCoverArtService)

        Thread.sleep(100)

        runBlocking { libraryService.navigateToDirectory(subdirectoryEntry) }

        Thread.sleep(100)

        assertEquals(testSubdirectoryEntries.size, libraryService.displayedEntries.value.size)
        assertTrue(libraryService.displayedEntries.value.containsAll(testSubdirectoryEntries))

        runBlocking { libraryService.navigateUpOneDirectory() }

        assertEquals(testRootEntries.size, libraryService.displayedEntries.value.size)
        assertTrue(libraryService.displayedEntries.value.containsAll(testRootEntries))
    }

    companion object {

        @JvmStatic
        @BeforeClass
        fun setupClass() {
            mockkStatic("com.onandor.nesemu.util.UtilKt")
            every { any<ByteArray>().sha1Hash(any()) } returns "romHash"
        }

        @JvmStatic
        @AfterClass
        fun teardownClass() {
            unmockkStatic("com.onandor.nesemu.util.UtilKt")
        }

        private val testRootDocuments = listOf<Document>(
            Document(
                uri = "test:/library/entry1.nes",
                name = "entry1.nes",
                isDirectory = false,
                parentDirectoryUri = "test:/library"
            ),
            Document(
                uri = "test:/library/entry2.nes",
                name = "entry2.nes",
                isDirectory = false,
                parentDirectoryUri = "test:/library"
            ),
            Document(
                uri = "test:/library/subdirectory",
                name = "subdirectory",
                isDirectory = true,
                parentDirectoryUri = "test:/library"
            ),
            Document(
                uri = "test:/library/entry3.nes",
                name = "entry3.nes",
                isDirectory = false,
                parentDirectoryUri = "test:/library/subdirectory"
            )
        )

        private val libraryRootEntry = LibraryEntry(
            romHash = "library_root",
            name = "library",
            uri = "test:/library",
            isDirectory = true,
            parentDirectoryUri = null
        )

        private val subdirectoryEntry = LibraryEntry(
            romHash = "",
            name = "subdirectory",
            uri = "test:/library/subdirectory",
            isDirectory = true,
            parentDirectoryUri = "test:/library"
        )

        private val testRootEntries = listOf<LibraryEntryWithDate>(
            LibraryEntryWithDate(
                entry = LibraryEntry(
                    romHash = "romHash1",
                    name = "entry1.nes",
                    uri = "test:/library/entry1.nes",
                    isDirectory = false,
                    parentDirectoryUri = "test:/library"
                ),
                lastPlayedDate = null
            ),
            LibraryEntryWithDate(
                entry = LibraryEntry(
                    romHash = "romHash2",
                    name = "entry2.nes",
                    uri = "test:/library/entry2.nes",
                    isDirectory = false,
                    parentDirectoryUri = "test:/library"
                ),
                lastPlayedDate = null
            ),
            LibraryEntryWithDate(
                entry = subdirectoryEntry,
                lastPlayedDate = null
            )
        )

        private val testSubdirectoryEntries = listOf<LibraryEntryWithDate>(
            LibraryEntryWithDate(
                entry = LibraryEntry(
                    romHash = "romHash3",
                    name = "entry3.nes",
                    uri = "test:/library/subdirectory/entry3.nes",
                    isDirectory = false,
                    parentDirectoryUri = "test:/library/subdirectory"
                ),
                lastPlayedDate = null
            )
        )
    }
}