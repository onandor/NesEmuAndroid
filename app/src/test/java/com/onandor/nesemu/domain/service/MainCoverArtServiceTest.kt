package com.onandor.nesemu.domain.service

import com.onandor.nesemu.data.entity.CoverArt
import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.data.network.sgdb.Game
import com.onandor.nesemu.data.network.sgdb.QueryResult
import com.onandor.nesemu.data.preferences.PreferenceManager
import com.onandor.nesemu.data.repository.CoverArtRepository
import com.onandor.nesemu.data.repository.LibraryEntryRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals

class MainCoverArtServiceTest {

    val mockHttpClientFactory: (String) -> HttpClient = {
        val mockEngine = MockEngine { request ->
            respond(
                content = Json.encodeToString(testGameQuery),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    val testScope = CoroutineScope(Dispatchers.Unconfined)

    val mockPrefManager = mockk<PreferenceManager> {
        every { observeSteamGridDBApiKey() } returns flowOf("api_key")
    }

    val mockLibraryEntryRepository = mockk<LibraryEntryRepository> {
        coEvery { findAllNotDirectory() } returns listOf(testEntry1, testEntry2)
    }

    val mockCoverArtRepository = mockk<CoverArtRepository> {
        coEvery { existsByRomHash(any()) } returns false
        coJustRun { upsert(any()) }
    }

    @Test
    fun coverArtService_SourceUrlsCalledThrice_RanOnce() {
        val mockCoverArtRepository = mockk<CoverArtRepository> {
            coEvery { existsByRomHash(any()) } coAnswers {
                delay(100)
                true
            }
            coJustRun { upsert(any()) }
        }

        val coverArtService = MainCoverArtService(mockHttpClientFactory, mockPrefManager,
            testScope, mockLibraryEntryRepository, mockCoverArtRepository)

        testScope.launch { coverArtService.sourceUrls(listOf(testEntry1, testEntry2)) }
        testScope.launch { coverArtService.sourceUrls(listOf(testEntry1, testEntry2)) }

        Thread.sleep(500)

        coVerify(exactly = 2) { mockCoverArtRepository.existsByRomHash(any()) }
    }

    @Test
    fun coverArtService_GivenTwoEntries_UpsertCalledTwice() {
        val capturedCoverArts = mutableListOf<CoverArt>()
        val mockCoverArtRepository = mockk<CoverArtRepository> {
            coEvery { existsByRomHash(any()) } returns false
            coEvery { upsert(capture(capturedCoverArts)) } just Runs
        }

        val coverArtService = MainCoverArtService(mockHttpClientFactory, mockPrefManager,
            testScope, mockLibraryEntryRepository, mockCoverArtRepository)

        Thread.sleep(500)

        coVerify(exactly = 2) { mockCoverArtRepository.upsert(any<CoverArt>()) }
        assertEquals(2, capturedCoverArts.size)
        assertEquals("romHash1", capturedCoverArts[0].romHash)
        assertEquals("romHash2", capturedCoverArts[1].romHash)
    }

    @Test
    fun coverArtService_GivenNoApiKey_NothingHappens() {
        val mockPrefManager = mockk<PreferenceManager> {
            every { observeSteamGridDBApiKey() } returns flowOf("")
        }

        val coverArtService = MainCoverArtService(mockHttpClientFactory, mockPrefManager,
            testScope, mockLibraryEntryRepository, mockCoverArtRepository)

        Thread.sleep(500)

        coVerify(exactly = 0) { mockCoverArtRepository.existsByRomHash(any()) }
        coVerify(exactly = 0) { mockCoverArtRepository.upsert(any()) }
    }

    @Test
    fun coverArtService_CoverArtExists_DidNotFetchAPI() {
        val capturedCoverArts = mutableListOf<CoverArt>()
        val mockCoverArtRepository = mockk<CoverArtRepository> {
            coEvery { existsByRomHash("romHash1") } returns true
            coEvery { existsByRomHash("romHash2") } returns false
            coEvery { upsert(capture(capturedCoverArts)) } just Runs
        }

        val coverArtService = MainCoverArtService(mockHttpClientFactory, mockPrefManager,
            testScope, mockLibraryEntryRepository, mockCoverArtRepository)

        Thread.sleep(500)

        coVerify(exactly = 2) { mockCoverArtRepository.existsByRomHash(any()) }
        coVerify(exactly = 1) { mockCoverArtRepository.upsert(any()) }
        assertEquals(1, capturedCoverArts.size)
        assertEquals("romHash2", capturedCoverArts[0].romHash)
    }

    companion object {
        private val testEntry1 = LibraryEntry(
            romHash = "romHash1",
            name = "name1",
            uri = "uri1",
            isDirectory = false
        )
        private val testEntry2 = LibraryEntry(
            romHash = "romHash2",
            name = "name2",
            uri = "uri2",
            isDirectory = false
        )
        private val testGameQuery = QueryResult<List<Game>>(
            success = true,
            data = emptyList()
        )
    }
}