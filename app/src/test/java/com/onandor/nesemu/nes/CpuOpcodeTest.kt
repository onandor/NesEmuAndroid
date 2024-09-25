package com.onandor.nesemu.nes

import com.google.gson.annotations.SerializedName

data class CpuTest(
    val name: String,
    val opcode: Int
)

@Suppress("PropertyName")
data class TestCpuState(
    @SerializedName("pc") val PC: Int,
    @SerializedName("s") val SP: Int,
    @SerializedName("a") val A: Int,
    @SerializedName("x") val X: Int,
    @SerializedName("y") val Y: Int,
    @SerializedName("p") val PS: Int,
    @SerializedName("ram") val memory: List<List<Int>>
)

data class CpuOpcodeTest(
    val name: String,
    @SerializedName("initial") val initialState: TestCpuState,
    @SerializedName("final") val finalState: TestCpuState,
    val cycles: List<List<Any>>
)
