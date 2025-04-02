package com.onandor.nesemu.emulation.nes

import android.util.Log
import com.onandor.nesemu.emulation.savestate.CartridgeState
import com.onandor.nesemu.emulation.savestate.Savable
import okio.internal.commonToUtf8String
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import kotlin.math.ceil
import kotlin.math.pow

// https://www.nesdev.org/wiki/INES
private data class INesHeader(
    val prgRomBanks: Int,       // byte 4
    val chrRomBanks: Int,       // byte 5
    val flags6: Int,            // byte 6
    val flags7: Int,            // byte 7
    val prgRamBanks: Int         // byte 8
)

// https://www.nesdev.org/wiki/NES_2.0
private data class Nes2Header(
    val prgRomSizeLow: Int,     // byte 4
    val chrRomSizeLow: Int,     // byte 5
    val flags6: Int,            // byte 6
    val flags7: Int,            // byte 7
    val mapper: Int,            // byte 8
    val romSizeHigh: Int,       // byte 9
    val prgRamSize: Int,        // byte 10
    val chrRamSize: Int,        // byte 11
    val timing: Int             // byte 12
)

enum class Mirroring {
    Horizontal,
    Vertical,
    SingleScreen,
    FourScreen
}

private enum class RomFormat {
    INes,
    Nes2
}

class Cartridge : Savable<CartridgeState> {

    // A cartridge has:
    // - PRG ROM chip
    // - optional PRG RAM chip
    // - PRG ROM or PRG RAM chip (or both in rare cases)
    // https://www.nesdev.org/wiki/CHR_ROM_vs._CHR_RAM

    lateinit var prgRom: IntArray
        private set
    lateinit var chrRom: IntArray
        private set
    var prgRam: IntArray? = null
        private set
    var chrRam: IntArray? = null
        private set
    var prgRomBanks: Int = 0
        private set
    var chrRomBanks: Int = 0
        private set
    lateinit var mirroring: Mirroring
        private set
    var mapperId: Int = 0
        private set
    var isPrgRamBatteryBacked: Boolean = false
        private set

    private lateinit var initialPrgRom: IntArray
    private lateinit var initialChrRom: IntArray

    fun reset() {
        prgRom = initialPrgRom.copyOf()
        if (chrRomBanks > 0) {
            chrRom = initialChrRom.copyOf()
        }
        if (prgRam != null) {
            prgRam = IntArray(prgRam!!.size)
        }
        if (chrRam != null) {
            chrRam = IntArray(chrRam!!.size)
        }
    }

    fun parseRom(rom: ByteArray) {
        val stream = rom.inputStream()
        val romFormat = getRomFormat(stream)
        Log.d(TAG, "Cartridge information:")

        try {
            if (romFormat == RomFormat.INes) {
                Log.d(TAG, "\tformat: iNES")
                val header = parseINesHeader(stream)
                prepareCartridge(header, stream)
            } else {
                Log.d(TAG, "\tformat: NES 2.0")
                val header = parseNes2Header(stream)
                prepareCartridge(header, stream)
            }
        } finally {
            stream.close()
        }

        initialPrgRom = prgRom.copyOf()
        if (chrRomBanks > 0) {
            initialChrRom = chrRom.copyOf()
        }
    }

    private fun prepareCartridge(header: INesHeader, stream: ByteArrayInputStream) {
        if (header.flags6 and 0x04 > 0) {
            stream.read(ByteArray(512)) // Discarding trainer
        }

        mapperId = ((header.flags6 ushr 4) and 0x0F) or (header.flags7 and 0xF0)
        Log.d(TAG, "\tmapper: ${mapperId.toString().padStart(3, '0')}")

        // The mappers currently supported only use horizontal or vertical mirroring
        mirroring = if (header.flags6 and 0x01 > 0) Mirroring.Vertical else Mirroring.Horizontal
        Log.d(TAG, "\tnametable mirroring: ${mirroring.name.lowercase()}")

        prgRomBanks = header.prgRomBanks
        Log.d(TAG, "\tPRG ROM banks: $prgRomBanks")
        val prgRomBytes = ByteArray(prgRomBanks * 0x4000)    // Each PRG ROM bank is 16 KiB
        stream.read(prgRomBytes)
        prgRom = prgRomBytes.toSigned8Array()

        isPrgRamBatteryBacked = header.flags6 and 0x02 > 0
        if (header.prgRamBanks > 0) {
            // PRG RAM size is defined in 8 KiB banks
            val prgRamSize = header.prgRamBanks * 0x2000
            prgRam = IntArray(prgRamSize)
            Log.d(TAG, "\tPRG RAM: $prgRamSize bytes")
        } else {
            prgRam = IntArray(0x2000)
            Log.d(TAG, "\tPRG RAM: 8192 bytes (default)")
        }
        Log.d(TAG, "\tPRG RAM type: ${if (isPrgRamBatteryBacked) "battery backed" else "volatile"}")

        chrRomBanks = header.chrRomBanks
        Log.d(TAG, "\tCHR ROM banks: $chrRomBanks")
        if (chrRomBanks != 0) {
            val chrRomBytes = ByteArray(chrRomBanks * 0x2000)    // Each CHR ROM bank is 8 KiB
            stream.read(chrRomBytes)
            chrRom = chrRomBytes.toSigned8Array()
            Log.d(TAG, "\tCHR RAM: not present")
        } else {
            chrRam = IntArray(0x2000)
            Log.d(TAG, "\tCHR RAM: 8192 bytes (default)")
        }
    }

    private fun prepareCartridge(header: Nes2Header, stream: ByteArrayInputStream) {
        // Check if rom needs a special console
        if (header.flags7 and 0x03 != 0) {
            Log.e(TAG, "The cartridge ROM requires a special console")
            throw RomParseException("The cartridge ROM requires a special console")
        }

        Log.d(TAG, "\ttiming/region: ${TIMING_REGIONS[header.timing and 0x03]}")

        if (header.flags6 and 0x04 > 0) {
            stream.read(ByteArray(512)) // Discarding trainer
        }

        mapperId = ((header.flags6 and 0xF0) ushr 4) or
                (header.flags7 and 0xF0) or
                ((header.mapper and 0x0F) shl 4)
        Log.d(TAG, "\tmapper: ${mapperId.toString().padStart(3, '0')}")

        mirroring = if (header.flags6 and 0x01 > 0) Mirroring.Vertical else Mirroring.Horizontal
        Log.d(TAG, "\tnametable mirroring: ${mirroring.name.lowercase()}")

        var prgRomLength: Int = 0
        if (header.romSizeHigh and 0x0F == 0x0F) {
            // PRG ROM size is specified in bytes using an exponent-multiplier notation
            val multiplier = (header.prgRomSizeLow and 0x03) * 2 + 1
            val exponent = (header.prgRomSizeLow and 0xFC) ushr 2
            val bytes = 2.0.pow(exponent) * multiplier
            prgRomLength = bytes.toInt()
            prgRomBanks = (ceil(bytes / 0x4000)).toInt()
        } else {
            // PRG ROM size is specified in 16 KiB banks
            prgRomBanks = ((header.romSizeHigh and 0x0F) shl 8) or header.prgRomSizeLow
            prgRomLength = prgRomBanks * 0x4000
        }
        Log.d(TAG, "\tPRG ROM banks: $prgRomBanks")

        val prgRomBytes = ByteArray(prgRomLength)
        stream.read(prgRomBytes)
        prgRom = prgRomBytes.toSigned8Array()

        val prgRamShiftCount = if (header.prgRamSize and 0xF0 > 0) {
            isPrgRamBatteryBacked = true
            header.prgRamSize and 0xF0
        } else {
            header.prgRamSize and 0x0F
        }

        if (prgRamShiftCount > 0) {
            val prgRamSize = 64 shl prgRamShiftCount
            prgRam = IntArray(prgRamSize)
            Log.d(TAG, "\tPRG RAM: $prgRamSize bytes")
            Log.d(TAG, "\tPRG RAM type: ${if (isPrgRamBatteryBacked) "battery backed" else "volatile"}")
        } else {
            Log.d(TAG, "\tPRG RAM: not present")
        }

        var chrRomLength: Int = 0
        if (header.romSizeHigh and 0xF0 == 0xF0) {
            // CHR ROM size is specified in bytes using an exponent-multiplier notation
            val multiplier = (header.chrRomSizeLow and 0x03) * 2 + 1
            val exponent = (header.chrRomSizeLow and 0xFC) ushr 2
            val bytes = 2.0.pow(exponent) * multiplier
            chrRomLength = bytes.toInt()
            chrRomLength = (ceil(bytes / 0x2000)).toInt()
        } else {
            // CHR ROM size is specified in 16 KiB banks
            chrRomBanks = ((header.romSizeHigh and 0x0F) shl 4) or header.chrRomSizeLow
            chrRomLength = chrRomBanks * 0x2000
        }
        Log.d(TAG, "\tCHR ROM banks: $chrRomBanks")

        if (chrRomBanks > 0) {
            val chrRomBytes = ByteArray(chrRomLength)
            stream.read(chrRomBytes)
            chrRom = chrRomBytes.toSigned8Array()
        }

        val chrRamShiftCount = (header.chrRamSize and 0x0F)
        if (chrRamShiftCount > 0) {
            val chrRamSize = 64 shl chrRamShiftCount
            chrRam = IntArray(chrRamSize)
            Log.d(TAG, "\tCHR RAM: $chrRamSize bytes")
        } else {
            Log.d(TAG, "\tCHR RAM: not present")
        }
    }

    private fun parseINesHeader(stream: ByteArrayInputStream): INesHeader {
        stream.read(ByteArray(4))   // Discarding identification
        val header = INesHeader(
            prgRomBanks = stream.read(),
            chrRomBanks = stream.read(),
            flags6 = stream.read(),
            flags7 = stream.read(),
            prgRamBanks = stream.read()
        )
        stream.read(ByteArray(7))   // Discarding unused bytes and padding
        return header
    }

    private fun parseNes2Header(stream: ByteArrayInputStream): Nes2Header {
        stream.read(ByteArray(4))
        val header = Nes2Header(
            prgRomSizeLow = stream.read(),
            chrRomSizeLow = stream.read(),
            flags6 = stream.read(),
            flags7 = stream.read(),
            mapper = stream.read(),
            romSizeHigh = stream.read(),
            prgRamSize = stream.read(),
            chrRamSize = stream.read(),
            timing = stream.read()
        )
        stream.read(ByteArray(3))
        return header
    }

    private fun getRomFormat(stream: ByteArrayInputStream): RomFormat {
        val identification = (0 ..< 4).map { stream.read().toByte() }.toByteArray()
        if (identification.commonToUtf8String() != "NES${0x1A.toChar()}") {
            Log.e(TAG, "Invalid ROM file")
            throw RomParseException("Invalid ROM file")
        }

        stream.read(ByteArray(3))
        val flags7 = stream.read()
        stream.reset()

        return if ((flags7 and 0x0C) ushr 2 == 0x02) RomFormat.Nes2 else RomFormat.INes
    }

    override fun createSaveState(): CartridgeState {
        return CartridgeState(
            prgRom = prgRom,
            chrRom = if (chrRomBanks > 0) chrRom else null,
            prgRam = prgRam,
            chrRam = chrRam
        )
    }

    override fun loadState(state: CartridgeState) {
        prgRom = state.prgRom
        if (chrRomBanks > 0) {
            chrRom = state.chrRom!!
        }
        prgRam = state.prgRam
        chrRam = state.chrRam
    }

    companion object {
        private const val TAG = "Cartridge"
        private val TIMING_REGIONS: Map<Int, String> = mapOf(
            0 to "NTSC (RP2C02)",
            1 to "PAL (RP2C07)",
            2 to "multiple",
            3 to "Dendy (UA6538)"
        )

        fun calculateRomHash(rom: ByteArray): String {
            val hashBytes = MessageDigest.getInstance("SHA-1").run {
                digest(rom.copyOfRange(16, rom.size))
            }
            return hashBytes.joinToString("") { byte -> "%02x".format(byte) }
        }
    }
}