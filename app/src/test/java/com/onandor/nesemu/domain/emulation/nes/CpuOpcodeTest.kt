package com.onandor.nesemu.domain.emulation.nes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class CpuTest(
    val name: String,
    val opcode: Int
)

@Suppress("PropertyName")
@Serializable
data class TestCpuState(
    @SerialName("pc") val PC: Int,
    @SerialName("s") val SP: Int,
    @SerialName("a") val A: Int,
    @SerialName("x") val X: Int,
    @SerialName("y") val Y: Int,
    @SerialName("p") val PS: Int,
    @SerialName("ram") val memory: List<List<Int>>
)

@Serializable
data class CpuOpcodeTest(
    val name: String,
    @SerialName("initial") val initialState: TestCpuState,
    @SerialName("final") val finalState: TestCpuState,
    val cycles: List<List<Unit>>
)
