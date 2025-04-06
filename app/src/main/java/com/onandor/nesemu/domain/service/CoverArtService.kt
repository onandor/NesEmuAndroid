package com.onandor.nesemu.domain.service

import android.util.Log
import com.onandor.nesemu.data.entity.CoverArt
import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.data.network.sgdb.Game
import com.onandor.nesemu.data.network.sgdb.Grid
import com.onandor.nesemu.data.network.sgdb.QueryResult
import com.onandor.nesemu.data.preferences.PreferenceManager
import com.onandor.nesemu.data.repository.CoverArtRepository
import com.onandor.nesemu.data.repository.LibraryEntryRepository
import com.onandor.nesemu.di.IODispatcher
import com.onandor.nesemu.di.SteamGridDB
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoverArtService @Inject constructor(
    @SteamGridDB private val httpClientFactory: (String) -> HttpClient,
    private val prefManager: PreferenceManager,
    @IODispatcher private val ioScope: CoroutineScope,
    private val libraryEntryRepository: LibraryEntryRepository,
    private val coverArtRepository: CoverArtRepository
) {

    private var httpClient: HttpClient? = null
    private val isSyncing = AtomicBoolean(false)

    init {
        ioScope.launch {
            prefManager
                .observeSteamGridDBApiKey()
                .collect {
                    httpClient = if (it.isNotEmpty()) httpClientFactory(it) else null
                    val entries = libraryEntryRepository.findAllNotDirectory()
                    sourceUrls(entries)
                }
        }
    }

    suspend fun sourceUrls(entries: List<LibraryEntry>) {
        httpClient?.let {
            if (!isSyncing.compareAndSet(false, true)) {
                return
            }

            Log.d(TAG, "Sourcing cover art URLs for new library entries...")

            var numSuccessful = 0
            var numFailed = 0
            for (i in entries.indices) {
                val fileName = entries[i].name
                val romHash = entries[i].romHash

                if (coverArtRepository.existsByRomHash(romHash)) {
                    continue
                }

                try {
                    val coverArt = CoverArt(
                        romHash = romHash,
                        imageUrl = fetchUrlForRom(fileName)
                    )
                    coverArtRepository.upsert(coverArt)
                    numSuccessful += 1
                } catch (e: Exception) {
                    Log.e(TAG, e.message, e)
                    numFailed += 1
                }
            }

            Log.d(TAG, "Finished: $numSuccessful/$numFailed/${numSuccessful + numFailed} " +
                    "(successful/failed/total)")

            isSyncing.set(false)
        }
    }

    private suspend fun fetchUrlForRom(fileName: String): String? = httpClient?.let { client ->
        val fileName = fileName
            .removeSuffix(".nes")
            // Remove (..) and [..] parts from the file name
            .replace("[(\\[][^]^)]+[)\\]]".toRegex(), "")
            .trim()

        val games: QueryResult<List<Game>> = client.get("$GAME_SEARCH/$fileName").body()
        if (games.data.isEmpty()) {
            return null
        }

        val grids: QueryResult<List<Grid>> = client.get("$GRID_SEARCH/${games.data[0].id}").body()
        if (grids.data.isEmpty()) {
            return null
        }

        return grids.data[0].thumb
    }

    companion object {
        private const val TAG = "CoverArtService"
        private const val GAME_SEARCH = "search/autocomplete"
        private const val GRID_SEARCH = "grids/game"
    }
}