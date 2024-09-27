package com.onandor.nesemu

import com.onandor.nesemu.nes.CpuOpcodeTestRunner
import com.onandor.nesemu.nes.CpuTest
import org.junit.Test
import kotlin.collections.listOf

class IllegalOpcodeTests {

    private object Verbosities {
        const val FAIL = CpuOpcodeTestRunner.Verbosity.BASIC_INFO or CpuOpcodeTestRunner.Verbosity.FAIL
        const val NONE = CpuOpcodeTestRunner.Verbosity.NONE
        const val ALL = CpuOpcodeTestRunner.Verbosity.ALL
    }

    private val testRunner = CpuOpcodeTestRunner(
        verbosity = Verbosities.NONE,
        testsToRun = 9999,
        skipDecimalMode = true,
        stopOnFail = false
    )

    @Test
    fun testALR() {
        testRunner.run("ALR Immediate", 0x4B)
    }

    @Test
    fun testANC() {
        testRunner.run("ANC Immediate", 0x0B)
    }

    @Test
    fun testARR() {
        testRunner.run("ARR Immediate", 0x6B)
    }

    @Test
    fun testDCP() {
        val tests = listOf<CpuTest>(
            CpuTest("DCP ZeroPage", 0xC7),
            CpuTest("DCP ZeroPage,X", 0xD7),
            CpuTest("DCP Absolute", 0xCF),
            CpuTest("DCP Absolute,X", 0xDF),
            CpuTest("DCP Absolute,Y", 0xDB),
            CpuTest("DCP Indirect,X", 0xC3),
            CpuTest("DCP Indirect,Y", 0xD3)
        )
        testRunner.runAll(tests)
    }

    @Test
    fun testISC() {
        val tests = listOf<CpuTest>(
            CpuTest("ISC ZeroPage", 0xE7),
            CpuTest("ISC ZeroPage,X", 0xF7),
            CpuTest("ISC Absolute", 0xEF),
            CpuTest("ISC Absolute,X", 0xFF),
            CpuTest("ISC Absolute,Y", 0xFB),
            CpuTest("ISC Indirect,X", 0xE3),
            CpuTest("ISC Indirect,Y", 0xF3)
        )
        testRunner.runAll(tests)
    }

    @Test
    fun testLAS() {
        testRunner.run("LAS Absolute,Y", 0xBB)
    }

    @Test
    fun testLAX() {
        val tests = listOf<CpuTest>(
            CpuTest("LAX ZeroPage", 0xA7),
            CpuTest("LAX ZeroPage,Y", 0xB7),
            CpuTest("LAX Absolute", 0xAF),
            CpuTest("LAX Absolute,Y", 0xBF),
            CpuTest("LAX Indirect,X", 0xA3),
            CpuTest("LAX Indirect,Y", 0xB3)
        )
        testRunner.runAll(tests)
    }

    @Test
    fun testRLA() {
        val tests = listOf<CpuTest>(
            CpuTest("RLA ZeroPage", 0x27),
            CpuTest("RLA ZeroPage,X", 0x37),
            CpuTest("RLA Absolute", 0x2F),
            CpuTest("RLA Absolute,X", 0x3F),
            CpuTest("RLA Absolute,Y", 0x3B),
            CpuTest("RLA Indirect,X", 0x23),
            CpuTest("RLA Indirect,Y", 0x33)
        )
        testRunner.runAll(tests)
    }

    @Test
    fun testRRA() {
        val tests = listOf<CpuTest>(
            CpuTest("RRA ZeroPage", 0x67),
            CpuTest("RRA ZeroPage,X", 0x77),
            CpuTest("RRA Absolute", 0x6F),
            CpuTest("RRA Absolute,X", 0x7F),
            CpuTest("RRA Absolute,Y", 0x7B),
            CpuTest("RRA Indirect,X", 0x63),
            CpuTest("RRA Indirect,Y", 0x73)
        )
        testRunner.runAll(tests)
    }

    @Test
    fun testSAX() {
        val tests = listOf<CpuTest>(
            CpuTest("SAX ZeroPage", 0x87),
            CpuTest("SAX ZeroPage,Y", 0x97),
            CpuTest("SAX Absolute", 0x8F),
            CpuTest("SAX Indirect,X", 0x83),
        )
        testRunner.runAll(tests)
    }

    @Test
    fun testSBX() {
        testRunner.run("SBX Immediate", 0xCB)
    }

    @Test
    fun testSLO() {
        val tests = listOf<CpuTest>(
            CpuTest("SLO ZeroPage", 0x07),
            CpuTest("SLO ZeroPage,X", 0x17),
            CpuTest("SLO Absolute", 0x0F),
            CpuTest("SLO Absolute,X", 0x1F),
            CpuTest("SLO Absolute,Y", 0x1B),
            CpuTest("SLO Indirect,X", 0x03),
            CpuTest("SLO Indirect,Y", 0x13)
        )
        testRunner.runAll(tests)
    }

    @Test
    fun testSRE() {
        val tests = listOf<CpuTest>(
            CpuTest("SRE ZeroPage", 0x47),
            CpuTest("SRE ZeroPage,X", 0x57),
            CpuTest("SRE Absolute", 0x4F),
            CpuTest("SRE Absolute,X", 0x5F),
            CpuTest("SRE Absolute,Y", 0x5B),
            CpuTest("SRE Indirect,X", 0x43),
            CpuTest("SRE Indirect,Y", 0x53)
        )
        testRunner.runAll(tests)
    }
}