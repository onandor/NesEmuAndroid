package com.onandor.nesemu.nes

import java.io.ByteArrayInputStream

data class INesHeader(
    val name: ByteArray,
    val numPrgBank: Int,
    val numChrBank: Int,
    val mapperFlags1: Int,
    val mapperFlags2: Int,
    val prgRamSize: Int,
    val tvSystem: Int
)

class Cartridge(rom: ByteArray) {

    private var prgRom: IntArray? = null
    private var chrRom: IntArray? = null
    private var mapperID: Int = -1

    init {
        parseRom(rom)
    }

    private fun parseRom(rom: ByteArray) {
        val stream = rom.inputStream()
        val header = parseINesHeader(stream)

        if (header.mapperFlags1 and 4 > 0) {
            stream.read(ByteArray(512)) // Discarding trainer
        }

        mapperID = (header.mapperFlags1 shr 4) or ((header.mapperFlags2 shr 4) shl 4)

        val prgRomBytes = ByteArray(header.numPrgBank * 16384)
        stream.read(prgRomBytes)
        prgRom = prgRomBytes.toIntArray()

        if (header.numChrBank != 0) {
            val chrRomBytes = ByteArray(header.numChrBank * 8192)
            stream.read(chrRomBytes)
            chrRom = chrRomBytes.toIntArray()
        } else {
            throw RuntimeException("CHR RAM is not supported")
        }

        stream.close()
    }

    private fun parseINesHeader(stream: ByteArrayInputStream): INesHeader {
        val header = INesHeader(
            name = (0..3).map { stream.read().toByte() }.toByteArray(),
            numPrgBank = stream.read(),
            numChrBank = stream.read(),
            mapperFlags1 = stream.read(),
            mapperFlags2 = stream.read(),
            prgRamSize = stream.read(),
            tvSystem = stream.read()
        )
        stream.read(ByteArray(6)) // Discarding padding
        return header
    }
}