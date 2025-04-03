package com.onandor.nesemu

import com.onandor.nesemu.domain.emulation.nes.CpuOpcodeTestRunner
import com.onandor.nesemu.domain.emulation.nes.CpuTest
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class LegalOpcodeTests {

    private object Verbosities {
        const val FAIL = CpuOpcodeTestRunner.Verbosity.BASIC_INFO or CpuOpcodeTestRunner.Verbosity.FAIL
        const val NONE = CpuOpcodeTestRunner.Verbosity.NONE
        const val ALL = CpuOpcodeTestRunner.Verbosity.ALL
    }

    private val testRunner = CpuOpcodeTestRunner(
        verbosity = Verbosities.FAIL,
        testsToRun = 5000,
        skipDecimalMode = false,
        stopOnFail = false
    )

    @Test
    fun testADC() {
        val tests = listOf<CpuTest>(
            CpuTest("ADC Immediate", 0x69),
            CpuTest("ADC ZeroPage", 0x65),
            CpuTest("ADC ZeroPage,X", 0x75),
            CpuTest("ADC Absolute", 0x6D),
            CpuTest("ADC Absolute,X", 0x7D),
            CpuTest("ADC Absolute,Y", 0x79),
            CpuTest("ADC Indirect,X", 0x61),
            CpuTest("ADC Indirect,Y", 0x71)
        )
        testRunner.runAll(tests)
    }

    @Test
    fun testAND() {
        val tests = listOf<CpuTest>(
            CpuTest("AND Immediate", 0x29),
            CpuTest("AND ZeroPage", 0x25),
            CpuTest("AND ZeroPage,X", 0x35),
            CpuTest("AND Absolute", 0x2D),
            CpuTest("AND Absolute,X", 0x3D),
            CpuTest("AND Absolute,Y", 0x39),
            CpuTest("AND Indirect,X", 0x21),
            CpuTest("AND Indirect,Y", 0x31)
        )
        testRunner.runAll(tests)
    }

    @Test
    fun testASL() {
        val tests = listOf<CpuTest>(
            CpuTest("ASL Accumulator", 0x0A),
            CpuTest("ASL ZeroPage", 0x06),
            CpuTest("ASL ZeroPage,X", 0x16),
            CpuTest("ASL Absolute", 0x0E),
            CpuTest("ASL Absolute,X", 0x1E),
        )
        testRunner.runAll(tests)
    }

    @Test
    fun testBCC() {
        testRunner.run("BCC Relative", 0x90)
    }

    @Test
    fun testBCS() {
        testRunner.run("BCS Relative", 0xB0)
    }

    @Test
    fun testBEQ() {
        testRunner.run("BEQ Relative", 0xF0)
    }

    @Test
    fun testBMI() {
        testRunner.run("BMI Relative", 0x30)
    }

    @Test
    fun testBNE() {
        testRunner.run("BNE Relative", 0xD0)
    }

    @Test
    fun testBPL() {
        testRunner.run("BPL Relative", 0x10)
    }

    @Test
    fun testBRK() {
        testRunner.run("BRK Implicit", 0x00)
    }

    @Test
    fun testBVC() {
        testRunner.run("BVC Relative", 0x50)
    }

    @Test
    fun testBVS() {
        testRunner.run("BVS Relative", 0x70)
    }

    @Test
    fun testBIT() {
        testRunner.run("BIT ZeroPage", 0x24)
        testRunner.run("BIT Absolute", 0x2C)
    }

    @Test
    fun testCLC() {
        testRunner.run("CLC Implicit", 0x18)
    }

    @Test
    fun testCLD() {
        testRunner.run("CLD Implicit", 0xD8)
    }

    @Test
    fun testCLI() {
        testRunner.run("CLI Implicit", 0x58)
    }

    @Test
    fun testCLV() {
        testRunner.run("CLV Implicit", 0xB8)
    }

    @Test
    fun testCMP() {
        val tests = listOf<CpuTest>(
            CpuTest("CMP Immediate", 0xC9),
            CpuTest("CMP ZeroPage", 0xC5),
            CpuTest("CMP ZeroPage,X", 0xD5),
            CpuTest("CMP Absolute", 0xCD),
            CpuTest("CMP Absolute,X", 0xDD),
            CpuTest("CMP Absolute,Y", 0xD9),
            CpuTest("CMP Indirect,X", 0xC1),
            CpuTest("CMP Indirect,Y", 0xD1)
        )
        testRunner.runAll(tests)
    }

    @Test
    fun testCPX() {
        testRunner.run("CPX Immediate", 0xE0)
        testRunner.run("CPX ZeroPage", 0xE4)
        testRunner.run("CPX Absolute", 0xEC)
    }

    @Test
    fun testCPY() {
        testRunner.run("CPY Immediate", 0xC0)
        testRunner.run("CPY ZeroPage", 0xC4)
        testRunner.run("CPY Absolute", 0xCC)
    }

    @Test
    fun testDEC() {
        val tests = listOf<CpuTest>(
            CpuTest("DEC ZeroPage", 0xC6),
            CpuTest("DEC ZeroPage,X", 0xD6),
            CpuTest("DEC Absolute", 0xCE),
            CpuTest("DEC Absolute,X", 0xDE)
        )
        testRunner.runAll(tests)
    }

    @Test
    fun testDEX() {
        testRunner.run("DEX Implicit", 0xCA)
    }

    @Test
    fun testDEY() {
        testRunner.run("DEY Implicit", 0x88)
    }

    @Test
    fun testEOR() {
        val tests = listOf<CpuTest>(
            CpuTest("EOR Immediate", 0x49),
            CpuTest("EOR ZeroPage", 0x45),
            CpuTest("EOR ZeroPage,X", 0x55),
            CpuTest("EOR Absolute", 0x4D),
            CpuTest("EOR Absolute,X", 0x5D),
            CpuTest("EOR Absolute,Y", 0x59),
            CpuTest("EOR Indirect,X", 0x41),
            CpuTest("EOR Indirect,Y", 0x51)
        )
        testRunner.runAll(tests)
    }

    @Test
    fun testINC() {
        val tests = listOf<CpuTest>(
            CpuTest("INC ZeroPage", 0xE6),
            CpuTest("INC ZeroPage,X", 0xF6),
            CpuTest("INC Absolute", 0xEE),
            CpuTest("INC Absolute,X", 0xFE),
        )
        testRunner.runAll(tests)
    }

    @Test
    fun testINX() {
        testRunner.run("INX Implicit", 0xE8)
    }

    @Test
    fun testINY() {
        testRunner.run("INY Implicit", 0xC8)
    }

    @Test
    fun testJMP() {
        testRunner.run("JMP Absolute", 0x4C)
        testRunner.run("JMP Indirect", 0x6C)
    }

    @Test
    fun testJSR() {
        testRunner.run("JSR Absolute", 0x20)
    }

    @Test
    fun testLDA() {
        val tests = listOf<CpuTest>(
            CpuTest("LDA Immediate", 0xA9),
            CpuTest("LDA ZeroPage", 0xA5),
            CpuTest("LDA ZeroPage,X", 0xB5),
            CpuTest("LDA Absolute", 0xAD),
            CpuTest("LDA Absolute,X", 0xBD),
            CpuTest("LDA Absolute,Y", 0xB9),
            CpuTest("LDA Indirect,X", 0xA1),
            CpuTest("LDA Indirect,Y", 0xB1)
        )
        testRunner.runAll(tests)
    }

    @Test
    fun testLDX() {
        val tests = listOf<CpuTest>(
            CpuTest("LDX Immediate", 0xA2),
            CpuTest("LDX ZeroPage", 0xA6),
            CpuTest("LDX ZeroPage,Y", 0xB6),
            CpuTest("LDA Absolute", 0xAE),
            CpuTest("LDA Absolute,Y", 0xBE)
        )
        testRunner.runAll(tests)
    }

    @Test
    fun testLDY() {
        val tests = listOf<CpuTest>(
            CpuTest("LDY Immediate", 0xA0),
            CpuTest("LDY ZeroPage", 0xA4),
            CpuTest("LDY ZeroPage,X", 0xB4),
            CpuTest("LDY Absolute", 0xAC),
            CpuTest("LDY Absolute,X", 0xBC)
        )
        testRunner.runAll(tests)
    }

    @Test
    fun testLSR() {
        val tests = listOf<CpuTest>(
            CpuTest("LSR Accumulator", 0x4A),
            CpuTest("LSR ZeroPage", 0x46),
            CpuTest("LSR ZeroPage,X", 0x56),
            CpuTest("LSR Absolute", 0x4E),
            CpuTest("LSR Absolute,X", 0x5E)
        )
        testRunner.runAll(tests)
    }

    @Test
    fun testNOP() {
        testRunner.run("NOP Implicit", 0xEA)
    }

    @Test
    fun testORA() {
        val tests = listOf<CpuTest>(
            CpuTest("ORA Immediate", 0x09),
            CpuTest("ORA ZeroPage", 0x05),
            CpuTest("ORA ZeroPage,X", 0x15),
            CpuTest("ORA Absolute", 0x0D),
            CpuTest("ORA Absolute,X", 0x1D),
            CpuTest("ORA Absolute,Y", 0x19),
            CpuTest("ORA Indirect,X", 0x01),
            CpuTest("ORA Indirect,Y", 0x11)
        )
        testRunner.runAll(tests)
    }

    @Test
    fun testPHA() {
        testRunner.run("PHA Implicit", 0x48)
    }

    @Test
    fun testPHP() {
        testRunner.run("PHP Implicit", 0x08)
    }

    @Test
    fun testPLA() {
        testRunner.run("PLA Implicit", 0x68)
    }

    @Test
    fun testPLP() {
        testRunner.run("PLP Implicit", 0x28)
    }

    @Test
    fun testROL() {
        val tests = listOf<CpuTest>(
            CpuTest("ROL Accumulator", 0x2A),
            CpuTest("ROL ZeroPage", 0x26),
            CpuTest("ROL ZeroPage,X", 0x36),
            CpuTest("ROL Absolute", 0x2E),
            CpuTest("ROL Absolute,X", 0x3E),
        )
        testRunner.runAll(tests)
    }

    @Test
    fun testROR() {
        val tests = listOf<CpuTest>(
            CpuTest("ROR Accumulator", 0x6A),
            CpuTest("ROR ZeroPage", 0x66),
            CpuTest("ROR ZeroPage,X", 0x76),
            CpuTest("ROR Absolute", 0x6E),
            CpuTest("ROR Absolute,X", 0x7E),
        )
        testRunner.runAll(tests)
    }

    @Test
    fun testRTI() {
        testRunner.run("RTI Implicit", 0x40)
    }

    @Test
    fun testRTS() {
        testRunner.run("RTS Implicit", 0x60)
    }

    @Test
    fun testSBC() {
        val tests = listOf<CpuTest>(
            CpuTest("SBC Immediate", 0xE9),
            CpuTest("SBC ZeroPage", 0xE5),
            CpuTest("SBC ZeroPage,X", 0xF5),
            CpuTest("SBC Absolute", 0xED),
            CpuTest("SBC Absolute,X", 0xFD),
            CpuTest("SBC Absolute,Y", 0xF9),
            CpuTest("SBC Indirect,X", 0xE1),
            CpuTest("SBC Indirect,Y", 0xF1)
        )
        testRunner.runAll(tests)
    }

    @Test
    fun testSEC() {
        testRunner.run("SEC Implicit", 0x38)
    }

    @Test
    fun testSED() {
        testRunner.run("SED Implicit", 0xF8)
    }

    @Test
    fun testSEI() {
        testRunner.run("SEI Implicit", 0x78)
    }

    @Test
    fun testSTA() {
        val tests = listOf<CpuTest>(
            CpuTest("STA ZeroPage", 0x85),
            CpuTest("STA ZeroPage,X", 0x95),
            CpuTest("STA Absolute", 0x8D),
            CpuTest("STA Absolute,X", 0x9D),
            CpuTest("STA Absolute,Y", 0x99),
            CpuTest("STA Indirect,X", 0x81),
            CpuTest("STA Indirect,Y", 0x91)
        )
        testRunner.runAll(tests)
    }

    @Test
    fun testSTX() {
        testRunner.run("STX ZeroPage", 0x86)
        testRunner.run("STX ZeroPage,Y", 0x96)
        testRunner.run("STX Absolute", 0x8E)
    }

    @Test
    fun testSTY() {
        testRunner.run("STY ZeroPage", 0x84)
        testRunner.run("STY ZeroPage,Y", 0x94)
        testRunner.run("STY Absolute", 0x8C)
    }

    @Test
    fun testTAX() {
        testRunner.run("TAX Implicit", 0xAA)
    }

    @Test
    fun testTAY() {
        testRunner.run("TAY Implicit", 0xA8)
    }

    @Test
    fun testTSX() {
        testRunner.run("TSX Implicit", 0xBA)
    }

    @Test
    fun testTXA() {
        testRunner.run("TXA Implicit", 0x8A)
    }

    @Test
    fun testTXS() {
        testRunner.run("TXS Implicit", 0x9A)
    }

    @Test
    fun testTYA() {
        testRunner.run("TYA Implicit", 0x98)
    }
}