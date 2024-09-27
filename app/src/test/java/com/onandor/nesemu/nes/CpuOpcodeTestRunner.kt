package com.onandor.nesemu.nes

import com.google.gson.Gson
import java.io.File

class CpuOpcodeTestRunner(
    private val verbosity: Int,
    private val testsToRun: Int,
    private val skipDecimalMode: Boolean,
    private val stopOnFail: Boolean
) {

    companion object {
        private const val TESTS_DIR = "src/test/res/6502-tests/"
    }

    object Verbosity {
        const val NONE = 0b000
        const val FAIL = 0b001
        const val OK = 0b010
        const val BASIC_INFO = 0b100
        const val ALL = 0b111
    }

    private val cpu: Cpu = Cpu(this::cpuReadMemory, this::cpuWriteMemory)
    private val memory: IntArray = IntArray(65536)

    fun runAll(tests: List<CpuTest>): Boolean {
        var allPassed = true
        run breaking@ {
            tests.forEach { test ->
                val testPassed = run(test.name, test.opcode)
                if (allPassed && !testPassed) {
                    allPassed = false
                }
                if (stopOnFail && !testPassed) {
                    return@breaking
                }
            }
        }
        return allPassed
    }

    fun run(instructionName: String, opcode: Int): Boolean {
        val opcodeStr = opcode.toString(16).padStart(2, '0')
        val file = File("$TESTS_DIR$opcodeStr.json")
        val br = file.bufferedReader()

        println("Running tests for instruction: $instructionName (opcode: $opcodeStr)")

        var passedCount = 0
        var totalTests = 0
        var testIdx = 0
        while (testIdx < testsToRun) {
            val testJson = br.readLine()
            if (testJson == null) {
                return false
            }

            testIdx++

            val opcodeTest: CpuOpcodeTest =
                Gson().fromJson<CpuOpcodeTest>(testJson, CpuOpcodeTest::class.java)

            if (skipDecimalMode && opcodeTest.initialState.PS and 0b00001000 > 0) {
                continue
            }

            val cpuState = CpuState(
                PC = opcodeTest.initialState.PC,
                SP = opcodeTest.initialState.SP,
                A = opcodeTest.initialState.A,
                X = opcodeTest.initialState.X,
                Y = opcodeTest.initialState.Y,
                PS = opcodeTest.initialState.PS
            )
            cpu.setState(cpuState)
            opcodeTest.initialState.memory.forEach { value ->
                cpuWriteMemory(value[0], value[1])
            }

            if (canPrint(Verbosity.BASIC_INFO)) {
                println("Test $testIdx of $testsToRun - ${opcodeTest.name}")
            }

            val expectedCycles = opcodeTest.cycles.size
            val actualCycles = cpu.executeCycles(expectedCycles)

            val passed = evaluateAndPrintResult(opcodeTest.finalState, expectedCycles, actualCycles)
            if (passed) {
                if (canPrint(Verbosity.BASIC_INFO)) println("PASSED\n")
                passedCount++
            } else if (canPrint(Verbosity.BASIC_INFO)) {
                println("FAILED\n")
            }

            totalTests++

            if (stopOnFail && !passed) {
                break
            }
        }

        val successRate = passedCount / totalTests.toFloat() * 100
        println("Passed: $passedCount/$totalTests - $successRate%\n")

        br.close()

        return successRate == 100f
    }

    private fun cpuReadMemory(address: Int): Int {
        return memory[address]
    }

    private fun cpuWriteMemory(address: Int, value: Int) {
        memory[address] = value
    }

    private fun printMemoryResult(finalState: TestCpuState) {
        println("+-Err-+------Addr------+----Exp-----+----Got-----+")
        finalState.memory.forEach { value ->
            val error = if (memory[value[0]] == value[1]) "     " else "  x  "
            val address = StringBuilder()
                .append(value[0].toString())
                .append(" (${value[0].toString(16).padStart(4, '0')})  ")
                .padStart(16, ' ')
                .toString()
            val expected = StringBuilder()
                .append(value[1].toString())
                .append(" (${value[1].toString(16).padStart(2, '0')})  ")
                .padStart(12, ' ')
                .toString()
            val got = StringBuilder()
                .append(memory[value[0]].toString())
                .append(" (${memory[value[0]].toString(16).padStart(2, '0')})  ")
                .padStart(12, ' ')
                .toString()

            println("|$error|$address|$expected|$got|")
        }
        println("+-----+----------------+------------+------------+")
    }

    private fun evaluateAndPrintResult(
        finalState: TestCpuState,
        expectedCycles: Int,
        actualCycles: Int
    ): Boolean {
        var pass = true
        val cpuState = cpu.getState()

        if (cpuState.PC == finalState.PC) {
            if (canPrint(Verbosity.OK)) println("PC: OK")
        } else {
            if (canPrint(Verbosity.FAIL)) {
                println("PC: FAIL\tExpected: ${finalState.PC}, Got: ${cpuState.PC}")
            }
            pass = false
        }

        if (cpuState.SP == finalState.SP) {
            if (canPrint(Verbosity.OK)) println("SP: OK")
        } else {
            if (canPrint(Verbosity.FAIL)) {
                println("SP: FAIL\tExpected: ${finalState.SP}, Got: ${cpuState.SP}")
            }
            pass = false
        }

        if (cpuState.A == finalState.A) {
            if (canPrint(Verbosity.OK)) println("A: OK")
        } else {
            if (canPrint(Verbosity.FAIL)) {
                println("A: FAIL\tExpected: ${finalState.A}, Got: ${cpuState.A}")
            }
            pass = false
        }

        if (cpuState.X == finalState.X) {
            if (canPrint(Verbosity.OK)) println("X: OK")
        } else {
            if (canPrint(Verbosity.FAIL)) {
                println("X: FAIL\tExpected: ${finalState.X}, Got: ${cpuState.X}")
            }
            pass = false
        }

        if (cpuState.Y == finalState.Y) {
            if (canPrint(Verbosity.OK)) println("Y: OK")
        } else {
            if (canPrint(Verbosity.FAIL)) {
                println("Y: FAIL\tExpected: ${finalState.Y}, Got: ${cpuState.Y}")
            }
            pass = false
        }

        if (cpuState.PS == finalState.PS) {
            if (canPrint(Verbosity.OK)) println("PS: OK")
        } else {
            if (canPrint(Verbosity.FAIL)) {
                val expectedPS = finalState.PS.toString(2).padStart(8, '0')
                val gotPS = cpuState.PS.toString(2).padStart(8, '0')
                println("PS: FAIL\t\t\t  NV BDIZC")
                println("\t\t\tExpected: $expectedPS")
                println("\t\t\t\t Got: $gotPS")
            }
            pass = false
        }

        var memoryPass = true
        finalState.memory.forEach { value ->
            if (memory[value[0]] != value[1]) {
                memoryPass = false
            }
        }
        if (memoryPass) {
            if (canPrint(Verbosity.OK)) println("Mem: OK")
        } else if (canPrint(Verbosity.FAIL)) {
            println("Mem: FAIL")
            printMemoryResult(finalState)
        }

        if (actualCycles == expectedCycles) {
            if (canPrint(Verbosity.OK)) println("Cycles: OK")
        } else {
            if (canPrint(Verbosity.FAIL)) {
                println("Cycles: FAIL\tExpected: $expectedCycles, Got: $actualCycles")
                pass = false
            }
        }

        return pass && memoryPass
    }

    private fun canPrint(verbosity: Int): Boolean = this.verbosity and verbosity > 0
}