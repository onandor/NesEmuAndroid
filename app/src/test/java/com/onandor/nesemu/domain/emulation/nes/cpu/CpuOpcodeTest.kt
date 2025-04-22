package com.onandor.nesemu.domain.emulation.nes.cpu

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

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
    val cycles: List<CpuOpcodeTestCycle>
)

@Serializable(with = CycleSerializer::class)
data class CpuOpcodeTestCycle(
    val address: Int,
    val value: Int,
    val memoryAccessKind: String
)

object CycleSerializer : KSerializer<CpuOpcodeTestCycle> {

    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("CpuOpcodeTestCycle", StructureKind.LIST)

    override fun serialize(
        encoder: Encoder,
        value: CpuOpcodeTestCycle
    ) {}

    override fun deserialize(decoder: Decoder): CpuOpcodeTestCycle {
        val input = decoder as? JsonDecoder ?: error("Expected JsonDecoder")
        val jsonArray = input.decodeJsonElement().jsonArray
        return CpuOpcodeTestCycle(
            address = jsonArray[0].jsonPrimitive.int,
            value = jsonArray[1].jsonPrimitive.int,
            memoryAccessKind = jsonArray[2].jsonPrimitive.content
        )
    }
}
