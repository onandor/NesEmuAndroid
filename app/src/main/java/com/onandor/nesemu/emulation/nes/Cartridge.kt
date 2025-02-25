package com.onandor.nesemu.emulation.nes

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
    lateinit var header: INesHeader
        private set

    fun parseRom(rom: ByteArray) {
        val stream = rom.inputStream()
        header = parseINesHeader(stream)

        if (header.name.commonToUtf8String() != "NES${0x1A.toChar()}") {
            stream.close()
            Log.e(TAG, "Invalid ROM file")
            throw RomParseException("Invalid ROM file")
        }

        if (header.control2 shr 2 == 0b11) {
            stream.close()
            Log.e(TAG, "Unsupported iNES version")
            throw RomParseException("Unsupported iNES version")
        }

        if (header.control1 and Bits.TRAINER > 0) {
            stream.read(ByteArray(512)) // Discarding trainer
        }

        mapperId = ((header.control1 ushr 4) and 0x0F) or (header.control2 and 0xF0)
        Log.i(TAG, "Using mapper $mapperId")

        // The mappers currently supported only use horizontal or vertical mirroring
        if (header.control1 and Bits.NAMETABLE > 0) {
            mirroring = Mirroring.VERTICAL
            Log.i(TAG, "Using vertical nametable mirroring")
        } else {
            Log.i(TAG, "Using horizontal nametable mirroring")
        }

        val prgRomBytes = ByteArray(header.numPrgBank * 0x4000)
        stream.read(prgRomBytes)
        prgRom = prgRomBytes.toSigned8Array()

        if (header.numChrBank != 0) {
            val chrRomBytes = ByteArray(header.numChrBank * 0x2000)
            stream.read(chrRomBytes)
            chrRom = chrRomBytes.toSigned8Array()
        } else {
            chrRom = IntArray(0x2000)
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