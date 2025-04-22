package com.onandor.nesemu.domain.emulation.nes.cpu

import com.onandor.nesemu.domain.emulation.nes.Cartridge
import com.onandor.nesemu.domain.emulation.nes.InvalidOperationException
import com.onandor.nesemu.domain.emulation.nes.Nes
import com.onandor.nesemu.domain.emulation.nes.plus16
import com.onandor.nesemu.domain.emulation.nes.plus8
import com.onandor.nesemu.domain.emulation.nes.toHexString
import com.onandor.nesemu.domain.emulation.nes.toSigned8
import org.junit.Test
import java.io.File
import java.io.FileWriter
import java.util.StringJoiner

// https://www.nesdev.org/wiki/Emulator_tests
// nestest under CPU Tests
class NesTestCpu {

    companion object {
        private const val TEST_ROM_PATH = "src/test/res/nestest.nes"
        private const val RESULT_PATH = "src/test/res/nestest_own.log"
        private const val NUM_TEST_INSTRUCTIONS = 8991
    }

    private val nes = Nes({ 0 }, { 0 })
    private val traceList = mutableListOf<String>()

    private fun createNesTestTrace(PC: Int, SP: Int, A: Int, X: Int, Y: Int, PS: Int, cycles: Int) {
        val pc = "${PC.toHexString(4)}  "

        val nextInstruction = nes.dbgCpuReadMemory(PC)
        val nextInstructionName = Cpu.INSTRUCTION_NAME_TABLE[nextInstruction]

        val opcodeHexJoiner = StringJoiner(" ").add(nextInstruction.toHexString(2))
        val opcodeAsmJoiner = StringJoiner(" ").add(nextInstructionName)

        when (Cpu.ADDRESSING_MODE_TABLE[nextInstruction]) {
            "ACC" -> {
                opcodeAsmJoiner.add("A")
            }
            "ABS" -> {
                val low = nes.dbgCpuReadMemory(PC.plus16(1))
                val high = nes.dbgCpuReadMemory(PC.plus16(2))
                opcodeHexJoiner
                    .add(low.toHexString(2))
                    .add(high.toHexString(2))
                val valueAtLocation = nes.dbgCpuReadMemory(low or (high shl 8))
                opcodeAsmJoiner.add("$${high.toHexString(2)}${low.toHexString(2)}")
                if (nextInstructionName != "JMP" && nextInstructionName != "JSR") {
                    opcodeAsmJoiner.add("= ${valueAtLocation.toHexString(2)}")
                }
            }
            "ABSX" -> {
                val low = nes.dbgCpuReadMemory(PC.plus16(1))
                val high = nes.dbgCpuReadMemory(PC.plus16(2))
                opcodeHexJoiner
                    .add(low.toHexString(2))
                    .add(high.toHexString(2))
                val eaddress = (low or (high shl 8)).plus16(X)
                val valueAtLocation = nes.dbgCpuReadMemory(eaddress)
                opcodeAsmJoiner
                    .add("$${high.toHexString(2)}${low.toHexString(2)},X")
                    .add("@ ${eaddress.toHexString(4)}")
                    .add("= ${valueAtLocation.toHexString(2)}")
            }
            "ABSY" -> {
                val low = nes.dbgCpuReadMemory(PC.plus16(1))
                val high = nes.dbgCpuReadMemory(PC.plus16(2))
                opcodeHexJoiner
                    .add(low.toHexString(2))
                    .add(high.toHexString(2))
                val eaddress = (low or (high shl 8)).plus16(Y)
                val valueAtLocation = nes.dbgCpuReadMemory(eaddress)
                opcodeAsmJoiner
                    .add("$${high.toHexString(2)}${low.toHexString(2)},Y")
                    .add("@ ${eaddress.toHexString(4)}")
                    .add("= ${valueAtLocation.toHexString(2)}")
            }
            "IMM" -> {
                val operand = nes.dbgCpuReadMemory(PC.plus16(1))
                opcodeHexJoiner.add(operand.toHexString(2))
                opcodeAsmJoiner.add("#$${operand.toHexString(2)}")
            }
            "IMPL" -> {}
            "IND" -> {
                val high = nes.dbgCpuReadMemory(PC.plus16(2))
                val low = nes.dbgCpuReadMemory(PC.plus16(1))
                opcodeHexJoiner
                    .add(low.toHexString(2))
                    .add(high.toHexString(2))
                val address = low or (high shl 8)
                val eaddress = if ((address and 0xFF) == 0xFF) {
                    nes.dbgCpuReadMemory(address) or (nes.dbgCpuReadMemory(address and 0xFF00) shl 8)
                } else {
                    nes.dbgCpuReadMemory(address) or (nes.dbgCpuReadMemory(address.plus16(1)) shl 8)
                }
                opcodeAsmJoiner
                    .add("($${high.toHexString(2)}${low.toHexString(2)})")
                    .add("= ${eaddress.toHexString(4)}")
            }
            "INDX" -> {
                val operand = nes.dbgCpuReadMemory(PC.plus16(1))
                opcodeHexJoiner.add(operand.toHexString(2))
                val pageZeroAddress = operand.plus8(X)
                val eaddress = nes.dbgCpuReadMemory(pageZeroAddress) or
                        (nes.dbgCpuReadMemory(pageZeroAddress.plus8(1)) shl 8)
                val valueAtAddress = nes.dbgCpuReadMemory(eaddress)
                opcodeAsmJoiner
                    .add("($${operand.toHexString(2)},X)")
                    .add("@ ${pageZeroAddress.toHexString(2)}")
                    .add("= ${eaddress.toHexString(4)}")
                    .add("= ${valueAtAddress.toHexString(2)}")
            }
            "INDY" -> {
                val operand = nes.dbgCpuReadMemory(PC.plus16(1))
                opcodeHexJoiner.add(operand.toHexString(2))
                val pageZeroAddress = nes.dbgCpuReadMemory(operand) or (nes.dbgCpuReadMemory(operand.plus8(1)) shl 8)
                val valueAtAddress = nes.dbgCpuReadMemory(pageZeroAddress.plus16(Y))
                opcodeAsmJoiner
                    .add("($${operand.toHexString(2)}),Y")
                    .add("= ${pageZeroAddress.toHexString(4)}")
                    .add("@ ${pageZeroAddress.plus16(Y).toHexString(4)}")
                    .add("= ${valueAtAddress.toHexString(2)}")
            }
            "REL" -> {
                val operand = nes.dbgCpuReadMemory(PC.plus16(1))
                opcodeHexJoiner.add(operand.toHexString(2))
                opcodeAsmJoiner.add("$${PC.plus16(2 + operand.toSigned8()).toHexString(2)}")
            }
            "ZPG" -> {
                val operand = nes.dbgCpuReadMemory(PC.plus16(1))
                opcodeHexJoiner.add(operand.toHexString(2))
                opcodeAsmJoiner
                    .add("$${operand.toHexString(2)}")
                    .add("= ${nes.dbgCpuReadMemory(operand).toHexString(2)}")
            }
            "ZPGX" -> {
                val operand = nes.dbgCpuReadMemory(PC.plus16(1))
                opcodeHexJoiner.add(operand.toHexString(2))
                val eaddress = operand.plus8(X)
                val valueAtLocation = nes.dbgCpuReadMemory(eaddress)
                opcodeAsmJoiner
                    .add("$${operand.toHexString(2)},X")
                    .add("@ ${eaddress.toHexString(2)}")
                    .add("= ${valueAtLocation.toHexString(2)}")
            }
            "ZPGY" -> {
                val operand = nes.dbgCpuReadMemory(PC.plus16(1))
                opcodeHexJoiner.add(operand.toHexString(2))
                val eaddress = operand.plus8(Y)
                val valueAtLocation = nes.dbgCpuReadMemory(eaddress)
                opcodeAsmJoiner
                    .add("$${operand.toHexString(2)},Y")
                    .add("@ ${eaddress.toHexString(2)}")
                    .add("= ${valueAtLocation.toHexString(2)}")
            }
        }

        val opcodeHex = if (Cpu.ILLEGAL_INSTRUCTION_TABLE[nextInstruction]) {
            opcodeHexJoiner.toString().padEnd(9) + "*"
        } else {
            opcodeHexJoiner.toString().padEnd(10)
        }
        val opcodeAsm = opcodeAsmJoiner.toString().padEnd(32)

        val registerStatus = StringJoiner(" ")
            .add("A:${A.toHexString(2)}")
            .add("X:${X.toHexString(2)}")
            .add("Y:${Y.toHexString(2)}")
            .add("P:${PS.toHexString(2)}")
            .add("SP:${SP.toHexString(2)}")
            .toString()

        val cpuCycles = " CYC:$cycles"

        traceList.add(pc + opcodeHex + opcodeAsm + registerStatus + cpuCycles)
    }

    private fun createMesenTrace(PC: Int, SP: Int, A: Int, X: Int, Y: Int, PS: Int, cycles: Int) {
        val pc = "${PC.toHexString(4)}  "

        val nextInstruction = nes.dbgCpuReadMemory(PC)
        val nextInstructionName = Cpu.INSTRUCTION_NAME_TABLE[nextInstruction]

        val opcodeAsmJoiner = StringJoiner(" ").add(nextInstructionName)

        when (Cpu.ADDRESSING_MODE_TABLE[nextInstruction]) {
            "ACC" -> {
                opcodeAsmJoiner.add("A")
            }
            "ABS" -> {
                val low = nes.dbgCpuReadMemory(PC.plus16(1))
                val high = nes.dbgCpuReadMemory(PC.plus16(2))
                val valueAtLocation = nes.dbgCpuReadMemory(low or (high shl 8))
                opcodeAsmJoiner.add("$${high.toHexString(2)}${low.toHexString(2)}")
                if (nextInstructionName != "JMP" && nextInstructionName != "JSR") {
                    opcodeAsmJoiner.add("= $${valueAtLocation.toHexString(4)}")
                }
            }
            "ABSX" -> {
                val low = nes.dbgCpuReadMemory(PC.plus16(1))
                val high = nes.dbgCpuReadMemory(PC.plus16(2))
                val eaddress = (low or (high shl 8)).plus16(X)
                val valueAtLocation = nes.dbgCpuReadMemory(eaddress)
                opcodeAsmJoiner
                    .add("$${high.toHexString(2)}${low.toHexString(2)},X")
                    .add("@ ${eaddress.toHexString(4)}")
                    .add("= ${valueAtLocation.toHexString(2)}")
            }
            "ABSY" -> {
                val low = nes.dbgCpuReadMemory(PC.plus16(1))
                val high = nes.dbgCpuReadMemory(PC.plus16(2))
                val eaddress = (low or (high shl 8)).plus16(Y)
                val valueAtLocation = nes.dbgCpuReadMemory(eaddress)
                opcodeAsmJoiner
                    .add("$${high.toHexString(2)}${low.toHexString(2)},Y")
                    .add("@ ${eaddress.toHexString(4)}")
                    .add("= ${valueAtLocation.toHexString(2)}")
            }
            "IMM" -> {
                val operand = nes.dbgCpuReadMemory(PC.plus16(1))
                opcodeAsmJoiner.add("#$${operand.toHexString(2)}")
            }
            "IMPL" -> {}
            "IND" -> {
                val high = nes.dbgCpuReadMemory(PC.plus16(2))
                val low = nes.dbgCpuReadMemory(PC.plus16(1))
                val address = low or (high shl 8)
                val eaddress = if ((address and 0xFF) == 0xFF) {
                    nes.dbgCpuReadMemory(address) or (nes.dbgCpuReadMemory(address and 0xFF00) shl 8)
                } else {
                    nes.dbgCpuReadMemory(address) or (nes.dbgCpuReadMemory(address.plus16(1)) shl 8)
                }
                opcodeAsmJoiner
                    .add("($${high.toHexString(2)}${low.toHexString(2)})")
                    .add("= $${eaddress.toHexString(4)}")
            }
            "INDX" -> {
                val operand = nes.dbgCpuReadMemory(PC.plus16(1))
                val pageZeroAddress = operand.plus8(X)
                val eaddress = nes.dbgCpuReadMemory(pageZeroAddress) or
                        (nes.dbgCpuReadMemory(pageZeroAddress.plus8(1)) shl 8)
                val valueAtAddress = nes.dbgCpuReadMemory(eaddress)
                opcodeAsmJoiner
                    .add("($${operand.toHexString(2)},X)")
                    .add("@ ${pageZeroAddress.toHexString(2)}")
                    .add("= ${eaddress.toHexString(4)}")
                    .add("= $${valueAtAddress.toHexString(4)}")
            }
            "INDY" -> {
                val operand = nes.dbgCpuReadMemory(PC.plus16(1))
                val pageZeroAddress = nes.dbgCpuReadMemory(operand) or (nes.dbgCpuReadMemory(operand.plus8(1)) shl 8)
                val valueAtAddress = nes.dbgCpuReadMemory(pageZeroAddress.plus16(Y))
                opcodeAsmJoiner
                    .add("($${operand.toHexString(2)}),Y")
                    .add("[$${pageZeroAddress.plus16(Y).toHexString(4)}]")
                    .add("= $${valueAtAddress.toHexString(4)}")
            }
            "REL" -> {
                val operand = nes.dbgCpuReadMemory(PC.plus16(1))
                opcodeAsmJoiner.add("$${PC.plus16(2 + operand.toSigned8()).toHexString(2)}")
            }
            "ZPG" -> {
                val operand = nes.dbgCpuReadMemory(PC.plus16(1))
                opcodeAsmJoiner
                    .add("$${operand.toHexString(2)}")
                    .add("= $${nes.dbgCpuReadMemory(operand).toHexString(4)}")
            }
            "ZPGX" -> {
                val operand = nes.dbgCpuReadMemory(PC.plus16(1))
                val eaddress = operand.plus8(X)
                val valueAtLocation = nes.dbgCpuReadMemory(eaddress)
                opcodeAsmJoiner
                    .add("$${operand.toHexString(2)},X")
                    .add("@ ${eaddress.toHexString(2)}")
                    .add("= $${valueAtLocation.toHexString(4)}")
            }
            "ZPGY" -> {
                val operand = nes.dbgCpuReadMemory(PC.plus16(1))
                val eaddress = operand.plus8(Y)
                val valueAtLocation = nes.dbgCpuReadMemory(eaddress)
                opcodeAsmJoiner
                    .add("$${operand.toHexString(2)},Y")
                    .add("@ ${eaddress.toHexString(2)}")
                    .add("= $${valueAtLocation.toHexString(4)}")
            }
        }
        val opcodeAsm = opcodeAsmJoiner.toString().padEnd(33)

        val registerStatus = StringJoiner(" ")
            .add("A:${A.toHexString(4)}")
            .add("X:${X.toHexString(4)}")
            .add("Y:${Y.toHexString(4)}")
            .add("S:${SP.toHexString(4)}")
            .add("P:${PS.toHexString(4)}")
            .toString()

        traceList.add(pc + opcodeAsm + registerStatus)
    }

    private fun dumpPrgRom(cartridge: Cartridge) {
        val logWriter = FileWriter("src/test/res/prgromdump.log")
        for (row in 0 ..< cartridge.prgRom.size step 16) {
            logWriter.write(row.toHexString(4) + ": ")
            for (col in 0 ..< 16) {
                logWriter.write(cartridge.prgRom[row + col].toHexString(2) + " ")
            }
            logWriter.write("\n")
        }
        logWriter.close()
    }

    private fun dumpNametable(nes: Nes) {
        val logWriter = FileWriter("src/test/res/nametabledump.log")
        for (row in 0 ..< 30) {
            for (col in 0 ..< 32) {
                val id = nes.ppuReadMemory(0x2000 + row * 32 + col)
                logWriter.write(id.toHexString(2) + " ")
            }
            logWriter.write("\n")
        }
        logWriter.close()
    }

    @Test
    fun runTest() {
        val rom = File(TEST_ROM_PATH).readBytes()
        val cartridge = Cartridge()
        cartridge.parseRom(rom)
        nes.insertCartridge(cartridge)

        nes.cpu.reset()
        nes.ppu.reset()
        nes.cpu.setPC(0xC000)
        nes.cpu.debugCallback = ::createNesTestTrace

        for (i in 0 until NUM_TEST_INSTRUCTIONS) {
            try {
                nes.cpu.clock()
            } catch (e: InvalidOperationException) {
                println(e.message)
            }
        }

        val logWriter = FileWriter(RESULT_PATH)
        traceList.forEach { trace ->
            logWriter.write(trace + "\n")
        }
        logWriter.close()
    }

    @Test
    fun runDonkeyKong() {
        val rom = File("src/test/res/donkeykong.nes").readBytes()
        val cartridge = Cartridge()
        cartridge.parseRom(rom)
        nes.insertCartridge(cartridge)
        //dumpPrgRom(cartridge)

        nes.cpu.reset()
        nes.ppu.reset()
        nes.cpu.debugCallback = ::createMesenTrace

        for (i in 0 until 32000) {
            try {
                val cycles = nes.cpu.clock()
                for (j in 0 ..< cycles * 3) {
                    nes.ppu.tick()
                }
            } catch (e: InvalidOperationException) {
                println(e.message)
            }
        }

        val logWriter = FileWriter(RESULT_PATH)
        traceList.forEach { trace ->
            logWriter.write(trace + "\n")
        }
        logWriter.close()

        //dumpNametable(nes)
    }
}