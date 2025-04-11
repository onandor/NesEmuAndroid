package com.onandor.nesemu.domain.service

import com.onandor.nesemu.data.entity.LibraryEntry

interface CoverArtService {

    suspend fun sourceUrls(entries: List<LibraryEntry>)
}