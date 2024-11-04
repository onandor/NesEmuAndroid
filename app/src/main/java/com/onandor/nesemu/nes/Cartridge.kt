package com.onandor.nesemu.nes

import android.util.Log
import okio.internal.commonToUtf8String
import java.io.ByteArrayInputStream

data class INesHeader(
    val name: ByteArray,
    val numPrgBank: Int,
    val numChrBank: Int,
    val control1: Int,
    val control2: Int,
    val prgRamSize: Int,
    val tvSystem: Int
)

enum class Mirroring {
    HORIZONTAL,
    VERTICAL,
    SINGLE_SCREEN,
    FOUR_SCREEN
}

class Cartridge {

    companion object {
        private const val TAG = "Cartridge"
    }

    object Bits {
        const val TRAINER = 4
        const val NAMETABLE = 1
        const val ALT_NAMETABLE = 8
    }

    lateinit var prgRom: IntArray
        private set
    lateinit var chrRom: IntArray
        private set
    var mirroring: Mirroring = Mirroring.HORIZONTAL
        private set
    var mapperId: Int = 0
        private set

    fun parseRom(rom: ByteArray) {
        val stream = rom.inputStream()
        val header = parseINesHeader(stream)

        if (header.name.commonToUtf8String(0, 3) != "NES") {
            stream.close()
            throw RomParseException(TAG, "Invalid ROM file")
        }

        if (header.control2 shr 2 == 0b11) {
            stream.close()
            throw RomParseException(TAG, "Unsupported iNES version")
        }

        if (header.control1 and Bits.TRAINER > 0) {
            stream.read(ByteArray(512)) // Discarding trainer
        }

        mapperId = (header.control1 shr 4) or ((header.control2 shr 4) shl 4)
        Log.i(TAG, "Using mapper $mapperId")
        if (mapperId != 0) {
            stream.close()
            throw RomParseException(TAG, "Unsupported mapper")
        }

        // The mappers currently supported only use horizontal or vertical mirroring
        if (header.control1 and Bits.NAMETABLE > 0) {
            mirroring = Mirroring.VERTICAL
            Log.i(TAG, "Using vertical nametable mirroring")
        } else {
            Log.i(TAG, "Using horizontal nametable mirroring")
        }

        val prgRomBytes = ByteArray(header.numPrgBank * 16384)
        stream.read(prgRomBytes)
        prgRom = prgRomBytes.toByteIntArray()

        if (header.numChrBank != 0) {
            val chrRomBytes = ByteArray(header.numChrBank * 8192)
            stream.read(chrRomBytes)
            chrRom = chrRomBytes.toByteIntArray()
        } else {
            stream.close()
            throw RomParseException(TAG, "Cartridge uses CHR RAM")
        }
        Log.i(TAG, "PRG ROM banks: ${header.numPrgBank}, CHR ROM banks: ${header.numChrBank}")
        stream.close()
    }

    private fun parseINesHeader(stream: ByteArrayInputStream): INesHeader {
        val header = INesHeader(
            name = (0..3).map { stream.read().toByte() }.toByteArray(),
            numPrgBank = stream.read(),
            numChrBank = stream.read(),
            control1 = stream.read(),
            control2 = stream.read(),
            prgRamSize = stream.read(),
            tvSystem = stream.read()
        )
        stream.read(ByteArray(6)) // Discarding padding
        return header
    }
}