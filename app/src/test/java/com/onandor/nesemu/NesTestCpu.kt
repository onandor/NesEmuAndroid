package com.onandor.nesemu

import com.onandor.nesemu.nes.Cartridge
import com.onandor.nesemu.nes.Cpu
import com.onandor.nesemu.nes.InvalidOperationException
import com.onandor.nesemu.nes.Nes
import com.onandor.nesemu.nes.plus16
import com.onandor.nesemu.nes.plus8
import com.onandor.nesemu.nes.toHexString
import com.onandor.nesemu.nes.toSigned8
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

    private val nes = Nes()
    private val traceList = mutableListOf<String>()

    private fun createNesTestTrace(PC: Int, SP: Int, A: Int, X: Int, Y: Int, PS: Int, cycles: Int) {
        val pc = "${PC.toHexString(4)}  "

        val nextInstruction = nes.cpuReadMemory(PC)
        val nextInstructionName = Cpu.INSTRUCTION_NAME_TABLE[nextInstruction]

        val opcodeHexJoiner = StringJoiner(" ").add(nextInstruction.toHexString(2))
        val opcodeAsmJoiner = StringJoiner(" ").add(nextInstructionName)

        when (Cpu.ADDRESSING_MODE_TABLE[nextInstruction]) {
            "ACC" -> {
                opcodeAsmJoiner.add("A")
            }
            "ABS" -> {
                val low = nes.cpuReadMemory(PC.plus16(1))
                val high = nes.cpuReadMemory(PC.plus16(2))
                opcodeHexJoiner
                    .add(low.toHexString(2))
                    .add(high.toHexString(2))
                val valueAtLocation = nes.cpuReadMemory(low or (high shl 8))
                opcodeAsmJoiner.add("$${high.toHexString(2)}${low.toHexString(2)}")
                if (nextInstructionName != "JMP" && nextInstructionName != "JSR") {
                    opcodeAsmJoiner.add("= ${valueAtLocation.toHexString(2)}")
                }
            }
            "ABSX" -> {
                val low = nes.cpuReadMemory(PC.plus16(1))
                val high = nes.cpuReadMemory(PC.plus16(2))
                opcodeHexJoiner
                    .add(low.toHexString(2))
                    .add(high.toHexString(2))
                val eaddress = (low or (high shl 8)).plus16(X)
                val valueAtLocation = nes.cpuReadMemory(eaddress)
                opcodeAsmJoiner
                    .add("$${high.toHexString(2)}${low.toHexString(2)},X")
                    .add("@ ${eaddress.toHexString(4)}")
                    .add("= ${valueAtLocation.toHexString(2)}")
            }
            "ABSY" -> {
                val low = nes.cpuReadMemory(PC.plus16(1))
                val high = nes.cpuReadMemory(PC.plus16(2))
                opcodeHexJoiner
                    .add(low.toHexString(2))
                    .add(high.toHexString(2))
                val eaddress = (low or (high shl 8)).plus16(Y)
                val valueAtLocation = nes.cpuReadMemory(eaddress)
                opcodeAsmJoiner
                    .add("$${high.toHexString(2)}${low.toHexString(2)},Y")
                    .add("@ ${eaddress.toHexString(4)}")
                    .add("= ${valueAtLocation.toHexString(2)}")
            }
            "IMM" -> {
                val operand = nes.cpuReadMemory(PC.plus16(1))
                opcodeHexJoiner.add(operand.toHexString(2))
                opcodeAsmJoiner.add("#$${operand.toHexString(2)}")
            }
            "IMPL" -> {}
            "IND" -> {
                val high = nes.cpuReadMemory(PC.plus16(2))
                val low = nes.cpuReadMemory(PC.plus16(1))
                opcodeHexJoiner
                    .add(low.toHexString(2))
                    .add(high.toHexString(2))
                val address = low or (high shl 8)
                val eaddress = if ((address and 0xFF) == 0xFF) {
                    nes.cpuReadMemory(address) or (nes.cpuReadMemory(address and 0xFF00) shl 8)
                } else {
                    nes.cpuReadMemory(address) or (nes.cpuReadMemory(address.plus16(1)) shl 8)
                }
                opcodeAsmJoiner
                    .add("($${high.toHexString(2)}${low.toHexString(2)})")
                    .add("= ${eaddress.toHexString(4)}")
            }
            "INDX" -> {
                val operand = nes.cpuReadMemory(PC.plus16(1))
                opcodeHexJoiner.add(operand.toHexString(2))
                val pageZeroAddress = operand.plus8(X)
                val eaddress = nes.cpuReadMemory(pageZeroAddress) or
                        (nes.cpuReadMemory(pageZeroAddress.plus8(1)) shl 8)
                val valueAtAddress = nes.cpuReadMemory(eaddress)
                opcodeAsmJoiner
                    .add("($${operand.toHexString(2)},X)")
                    .add("@ ${pageZeroAddress.toHexString(2)}")
                    .add("= ${eaddress.toHexString(4)}")
                    .add("= ${valueAtAddress.toHexString(2)}")
            }
            "INDY" -> {
                val operand = nes.cpuReadMemory(PC.plus16(1))
                opcodeHexJoiner.add(operand.toHexString(2))
                val pageZeroAddress = nes.cpuReadMemory(operand) or (nes.cpuReadMemory(operand.plus8(1)) shl 8)
                val valueAtAddress = nes.cpuReadMemory(pageZeroAddress.plus16(Y))
                opcodeAsmJoiner
                    .add("($${operand.toHexString(2)}),Y")
                    .add("= ${pageZeroAddress.toHexString(4)}")
                    .add("@ ${pageZeroAddress.plus16(Y).toHexString(4)}")
                    .add("= ${valueAtAddress.toHexString(2)}")
            }
            "REL" -> {
                val operand = nes.cpuReadMemory(PC.plus16(1))
                opcodeHexJoiner.add(operand.toHexString(2))
                opcodeAsmJoiner.add("$${PC.plus16(2 + operand.toSigned8()).toHexString(2)}")
            }
            "ZPG" -> {
                val operand = nes.cpuReadMemory(PC.plus16(1))
                opcodeHexJoiner.add(operand.toHexString(2))
                opcodeAsmJoiner
                    .add("$${operand.toHexString(2)}")
                    .add("= ${nes.cpuReadMemory(operand).toHexString(2)}")
            }
            "ZPGX" -> {
                val operand = nes.cpuReadMemory(PC.plus16(1))
                opcodeHexJoiner.add(operand.toHexString(2))
                val eaddress = operand.plus8(X)
                val valueAtLocation = nes.cpuReadMemory(eaddress)
                opcodeAsmJoiner
                    .add("$${operand.toHexString(2)},X")
                    .add("@ ${eaddress.toHexString(2)}")
                    .add("= ${valueAtLocation.toHexString(2)}")
            }
            "ZPGY" -> {
                val operand = nes.cpuReadMemory(PC.plus16(1))
                opcodeHexJoiner.add(operand.toHexString(2))
                val eaddress = operand.plus8(Y)
                val valueAtLocation = nes.cpuReadMemory(eaddress)
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

    @Test
    fun runTest() {
        val rom = File(TEST_ROM_PATH).readBytes()
        val cartridge = Cartridge()
        cartridge.parseRom(rom)
        nes.insertCartridge(cartridge)
        nes.reset()

        nes.cpu.setPC(0xC000)
        nes.cpu.debugCallback = ::createNesTestTrace

        for (i in 0 until NUM_TEST_INSTRUCTIONS) {
            try {
                nes.cpu.step()
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
}