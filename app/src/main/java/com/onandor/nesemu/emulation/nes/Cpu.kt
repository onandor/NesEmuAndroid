package com.onandor.nesemu.emulation.nes

import android.util.Log

@Suppress("PropertyName")
data class CpuState(
    val PC: Int,
    val SP: Int,
    val A: Int,
    val X: Int,
    val Y: Int,
    val PS: Int
)

@Suppress("FunctionName", "PrivatePropertyName", "LocalVariableName")
class Cpu(
    private val readMemory: (address: Int) -> Int,
    private val writeMemory: (address: Int, value: Int) -> Unit
) {

    private object Flags {
        const val CARRY: Int = 0b00000001
        const val ZERO: Int = 0b00000010
        const val INTERRUPT_DISABLE: Int = 0b00000100
        const val DECIMAL: Int = 0b00001000
        const val BREAK: Int = 0b00010000
        const val UNUSED: Int = 0b00100000
        const val OVERFLOW: Int = 0b01000000
        const val NEGATIVE: Int = 0b10000000

        const val BIT_5: Int = UNUSED
        const val BIT_6: Int = OVERFLOW
    }

    var debugCallback: (PC: Int, SP: Int, A: Int, X: Int, Y: Int, PS: Int, cycles: Int) -> Unit =
        EMPTY_DEBUG_CALLBACK

    private var PC: Int = 0                 // Program Counter - 16 bits
    private var SP: Int = 0                 // Stack Pointer - 8 bits
    private var A: Int = 0                  // Accumulator - 8 bits
    private var X: Int = 0                  // Index Register X - 8 bits
    private var Y: Int = 0                  // Index Register Y - 8 bits
    private var PS: Int = 0b00100100        // Processor Status - 8 bits (flags)

    private var instruction: Int = 0        // Currently executed instruction
    private var eaddress: Int = 0           // Effective address for the current instruction

    // Set to true whenever the current addressing mode or instruction could result in a page boundary
    // being crossed, resulting in an additional cycle. The bonus cycle occurs when both are set to
    // true at the end of the execution.
    private var addressingCycle = false
    private var instructionCycle = false

    private var totalCycles: Int = 7
    private var interruptCycles: Int = 0

    fun reset() {
        PC = read2Bytes(0xFFFC)
        SP = 0xFD
        A = 0
        X = 0
        Y = 0
        PS = 0b00000100
        totalCycles = 7 // https://www.pagetable.com/?p=410
        interruptCycles = 0
    }

    fun step(): Int {
        if (debugCallback !== EMPTY_DEBUG_CALLBACK) {
            this.debugCallback(PC, SP, A, X, Y, PS, totalCycles)
        }

        var stepCycles = interruptCycles
        addressingCycle = false
        instructionCycle = false

        instruction = readByte(PC)
        PC = PC.plus16(1)
        ADDRESS_HANDLER_TABLE[instruction]()
        INSTRUCTION_HANDLER_TABLE[instruction]()

        stepCycles += INSTRUCTION_CYCLES_TABLE[instruction]
        if (addressingCycle && instructionCycle) {
            stepCycles++
        }

        this.totalCycles += stepCycles
        interruptCycles = 0
        return stepCycles
    }

    fun executeCycles(cycles: Int): Int {
        this.totalCycles = 0
        while (this.totalCycles < cycles) {
            if (debugCallback !== EMPTY_DEBUG_CALLBACK) {
                this.debugCallback(PC, SP, A, X, Y, PS, totalCycles)
            }

            addressingCycle = false
            instructionCycle = false

            instruction = readByte(PC)
            PC = PC.plus16(1)
            ADDRESS_HANDLER_TABLE[instruction]()
            INSTRUCTION_HANDLER_TABLE[instruction]()

            this.totalCycles += INSTRUCTION_CYCLES_TABLE[instruction]
            if (addressingCycle && instructionCycle) {
                this.totalCycles++
            }
        }
        return this.totalCycles
    }

    // For testing only
    fun setPC(PC: Int) {
        this.PC = PC
    }

    private fun readByte(address: Int): Int = readMemory(address and 0xFFFF)

    private fun read2Bytes(address: Int): Int =
        readMemory(address and 0xFFFF) or (readMemory((address and 0xFFFF).plus16(1)) shl 8)

    fun writeByte(address: Int, value: Int) = writeMemory(address and 0xFFFF, value and 0xFF)

    private fun setFlag(flag: Int, set: Boolean) {
        PS = if (set) PS or flag else PS and flag.inv()
    }

    private fun getFlag(flag: Int): Boolean = (PS and flag) > 0

    private fun setZNFlags(data: Int) {
        setFlag(Flags.ZERO, data == 0)
        setFlag(Flags.NEGATIVE, (data and Flags.NEGATIVE) > 0)
    }

    fun getState(): CpuState {
        return CpuState(
            PC = this.PC,
            SP = this.SP,
            A = this.A,
            X = this.X,
            Y = this.Y,
            PS = this.PS
        )
    }

    fun setState(state: CpuState) {
        PC = state.PC
        SP = state.SP
        A = state.A
        X = state.X
        Y = state.Y
        PS = state.PS
    }

    // ------ Stack functions ------

    private fun pushByte(value: Int) {
        writeByte(SP or 0x100, value)
        SP = (SP - 1) and 0xFF
    }

    private fun push2Bytes(value: Int) {
        pushByte(value shr 8)
        pushByte(value)
    }

    private fun pullByte(): Int {
        SP = (SP + 1) and 0xFF
        return readByte(SP or 0x100)
    }

    private fun pull2Bytes(): Int {
        return pullByte() or (pullByte() shl 8)
    }

    // ------ Addressing mode functions ------

    // Accumulator
    private fun acc() {}

    // Absolute
    private fun abs() {
        eaddress = read2Bytes(PC)
        PC = PC.plus16(2)
    }

    // Absolute,X
    private fun absx() {
        val address = read2Bytes(PC)
        eaddress = address + X
        addressingCycle = (address xor eaddress) shr 8 > 0
        PC = PC.plus16(2)
    }

    // Absolute,Y
    private fun absy() {
        val address = read2Bytes(PC);
        eaddress = address + Y
        addressingCycle = (address xor eaddress) shr 8 > 0
        PC = PC.plus16(2)
    }

    // Immediate
    private fun imm() {
        eaddress = PC
        PC = PC.plus16(1)
    }

    // Implicit
    private fun impl() {}

    // Indirect
    private fun ind() {
        val address = read2Bytes(PC)
        eaddress = if ((address and 0xFF) == 0xFF) {
            readByte(address) or (readByte(address and 0xFF00) shl 8)
        } else {
            read2Bytes(address)
        }
        PC = PC.plus16(2)
    }

    // Indexed Indirect
    private fun indx() {
        val pageZeroAddress = readByte(PC).plus8(X)
        eaddress = readByte(pageZeroAddress) or (readByte(pageZeroAddress.plus8(1)) shl 8)
        PC = PC.plus16(1)
    }

    // Indirect Indexed
    private fun indy() {
        val pageZeroAddress = readByte(PC)
        val nextPageZeroAddress = (pageZeroAddress + 1) and 0xFF // Zero page wrap around
        val address = readByte(pageZeroAddress) or (readByte(nextPageZeroAddress) shl 8)
        val addressY = address + Y
        addressingCycle = (address xor addressY) shr 8 > 0
        eaddress = addressY
        PC = PC.plus16(1)
    }

    // Relative
    private fun rel() {
        eaddress = PC
        PC = PC.plus16(1)
    }

    // Zero Page
    private fun zpg() {
        eaddress = readByte(PC)
        PC = PC.plus16(1)
    }

    // Zero Page,X
    private fun zpgx() {
        eaddress = readByte(PC).plus8(X)
        PC = PC.plus16(1)
    }

    // Zero Page,Y
    private fun zpgy() {
        eaddress = readByte(PC).plus8(Y)
        PC = PC.plus16(1)
    }

    // ------- Interrupt handler functions -------

    fun IRQ() {
        if (getFlag(Flags.INTERRUPT_DISABLE)) {
            return
        }
        push2Bytes(PC)
        PHP()
        PC = read2Bytes(0xFFFE)
        setFlag(Flags.INTERRUPT_DISABLE, true)
        interruptCycles = 7
    }

    fun NMI() {
        push2Bytes(PC)
        PHP()
        PC = read2Bytes(0xFFFA)
        setFlag(Flags.INTERRUPT_DISABLE, true)
        interruptCycles = 7
    }

    // ------ Instruction handler functions ------

    // ----------- Legal instructions ------------

    private fun branch() {
        val offset = readByte(eaddress)
        val oldPC = PC
        PC = PC.plus16(offset.toSigned8())
        totalCycles += 1 + ((PC and 0xFF00) != (oldPC and 0xFF00)).toInt()
    }

    private fun ADC_binary(operand: Int) {
        var sum = A + operand + getFlag(Flags.CARRY).toInt()
        setZNFlags(sum and 0xFF)
        setFlag(Flags.OVERFLOW, (operand xor sum) and (A xor sum) and 0x80 > 0)
        setFlag(Flags.CARRY, sum > 0xFF)
        A = sum and 0xFF
    }

    // Not perfect
    private fun ADC_decimal(operand: Int) {
        var sum = (A and 0x0F) + (operand and 0x0F) + getFlag(Flags.CARRY).toInt()
        if (sum > 0x09)
            sum += 0x06
        sum = (A and 0xF0) + (operand and 0xF0) + (if (sum > 0x0F) 0x10 else 0) + (sum and 0x0F)

        setFlag(Flags.OVERFLOW, (operand xor sum) and (A xor sum) and 0x80 > 0)
        setZNFlags(sum)
        if (sum > 0x9F)
            sum += 0x60
        setFlag(Flags.CARRY, sum > 0xFF)
        A = sum and 0xFF
    }

    private fun ADC(operand: Int) {
        if (getFlag(Flags.DECIMAL)) {
            Log.w(TAG, "ADC in decimal mode is not supported, using binary mode")
            ADC_binary(operand)
        } else {
            ADC_binary(operand)
        }
        instructionCycle = true
    }

    private fun ADC() {
        ADC(readByte(eaddress))
    }

    private fun AND() {
        val operand = readByte(eaddress)
        A = A and operand
        setZNFlags(A)
        instructionCycle = true
    }

    private fun ASL() {
        if (ADDRESS_HANDLER_TABLE[instruction] == ::acc) {
            setFlag(Flags.CARRY, A and Flags.NEGATIVE > 0)
            A = (A shl 1) and 0xFF
            setZNFlags(A)
            return
        }
        var data = readByte(eaddress)
        setFlag(Flags.CARRY, data and Flags.NEGATIVE > 0)
        data = (data shl 1) and 0xFF
        writeByte(eaddress, data)
        setZNFlags(data)
    }

    private fun BCC() {
        if (!getFlag(Flags.CARRY)) branch()
    }

    private fun BCS() {
        if (getFlag(Flags.CARRY)) branch()
    }

    private fun BEQ() {
        if (getFlag(Flags.ZERO)) branch()
    }

    private fun BIT() {
        val data = readByte(eaddress)
        setFlag(Flags.ZERO, (A and data) == 0)
        setFlag(Flags.NEGATIVE, data and Flags.NEGATIVE > 0)
        setFlag(Flags.OVERFLOW, data and Flags.OVERFLOW > 0)
    }

    private fun BMI() {
        if (getFlag(Flags.NEGATIVE)) branch()
    }

    private fun BNE() {
        if (!getFlag(Flags.ZERO)) branch()
    }

    private fun BPL() {
        if (!getFlag(Flags.NEGATIVE)) branch()
    }

    private fun BRK() {
        PC = PC.plus16(1)
        push2Bytes(PC)
        pushByte(PS or Flags.BREAK)
        PC = read2Bytes(0xFFFE)
        setFlag(Flags.INTERRUPT_DISABLE, true)
    }

    private fun BVC() {
        if (!getFlag(Flags.OVERFLOW)) branch()
    }

    private fun BVS() {
        if (getFlag(Flags.OVERFLOW)) branch()
    }

    private fun CLC() {
        setFlag(Flags.CARRY, false)
    }

    private fun CLD() {
        setFlag(Flags.DECIMAL, false)
    }

    private fun CLI() {
        setFlag(Flags.INTERRUPT_DISABLE, false)
    }

    private fun CLV() {
        setFlag(Flags.OVERFLOW, false)
    }

    private fun CMP(register: Int) {
        val data = readByte(eaddress)
        setFlag(Flags.CARRY, register >= data)
        setFlag(Flags.ZERO, register == data)
        setFlag(Flags.NEGATIVE, (register - data) and Flags.NEGATIVE > 0)
    }

    private fun CMP() {
        CMP(A)
        instructionCycle = true
    }

    private fun CPX() {
        CMP(X)
    }

    private fun CPY() {
        CMP(Y)
    }

    private fun DEC() {
        val data = readByte(eaddress).plus8(-1)
        writeByte(eaddress, data)
        setZNFlags(data)
    }

    private fun DEX() {
        X = X.plus8(-1)
        setZNFlags(X)
    }

    private fun DEY() {
        Y = Y.plus8(-1)
        setZNFlags(Y)
    }

    private fun EOR() {
        val data = readByte(eaddress)
        A = A xor data
        setZNFlags(A)
        instructionCycle = true
    }

    private fun INC() {
        val data = readByte(eaddress).plus8(1)
        writeByte(eaddress, data)
        setZNFlags(data)
    }

    private fun INX() {
        X = X.plus8(1)
        setZNFlags(X)
    }

    private fun INY() {
        Y = Y.plus8(1)
        setZNFlags(Y)
    }

    private fun JMP() {
        PC = eaddress
    }

    private fun JSR() {
        push2Bytes(PC.plus16(-1))
        PC = eaddress
    }

    private fun LDA() {
        A = readByte(eaddress)
        setZNFlags(A)
        instructionCycle = true
    }

    private fun LDX() {
        X = readByte(eaddress)
        setZNFlags(X)
        instructionCycle = true
    }

    private fun LDY() {
        Y = readByte(eaddress)
        setZNFlags(Y)
        instructionCycle = true
    }

    private fun LSR_A() {
        setFlag(Flags.CARRY, A and Flags.CARRY > 0)
        A = A shr 1
        setZNFlags(A)
    }

    private fun LSR() {
        if (ADDRESS_HANDLER_TABLE[instruction] == ::acc) {
            LSR_A()
            return
        }
        var data = readByte(eaddress)
        setFlag(Flags.CARRY, data and Flags.CARRY > 0)
        data = data shr 1
        writeByte(eaddress, data)
        setZNFlags(data)
    }

    private fun NOP() {
        instructionCycle = true
    }

    private fun ORA() {
        val data = readByte(eaddress)
        A = A or data
        setZNFlags(A)
        instructionCycle = true
    }

    private fun PHA() {
        pushByte(A)
    }

    private fun PHP() {
        pushByte(PS or Flags.UNUSED or Flags.BREAK)
    }

    private fun PLA() {
        A = pullByte()
        setZNFlags(A)
    }

    private fun PLP() {
        PS = pullByte() and Flags.BREAK.inv() or Flags.UNUSED
    }

    private fun ROL() {
        val oldCarry = PS and Flags.CARRY
        if (ADDRESS_HANDLER_TABLE[instruction] == ::acc) {
            setFlag(Flags.CARRY, A and Flags.NEGATIVE > 0)
            A = ((A shl 1) and 0xFF) or oldCarry
            setZNFlags(A)
            return
        }
        var data = readByte(eaddress)
        setFlag(Flags.CARRY, data and Flags.NEGATIVE > 0)
        data = ((data shl 1) and 0xFF) or oldCarry
        writeByte(eaddress, data)
        setZNFlags(data)
    }

    private fun ROR_A() {
        val oldCarry = PS and Flags.CARRY
        setFlag(Flags.CARRY, A and Flags.CARRY > 0)
        A = (A shr 1) or (oldCarry shl 7)
        setZNFlags(A)
    }

    private fun ROR() {
        if (ADDRESS_HANDLER_TABLE[instruction] == ::acc) {
            ROR_A()
            return
        }
        val oldCarry = PS and Flags.CARRY
        var data = readByte(eaddress)
        setFlag(Flags.CARRY, data and Flags.CARRY > 0)
        data = (data shr 1) or (oldCarry shl 7)
        writeByte(eaddress, data)
        setZNFlags(data)
    }

    private fun RTI() {
        PS = pullByte() and Flags.BREAK.inv() or Flags.UNUSED
        PC = pull2Bytes()
    }

    private fun RTS() {
        PC = pull2Bytes().plus16(1)
    }

    private fun SBC() {
        val data = readByte(eaddress)
        if (getFlag(Flags.DECIMAL)) {
            Log.w(TAG, "SBC in decimal mode is not supported, using binary mode")
            ADC(data.inv() and 0xFF)
        } else {
            ADC(data.inv() and 0xFF)
        }
    }

    private fun SEC() {
        setFlag(Flags.CARRY, true)
    }

    private fun SED() {
        setFlag(Flags.DECIMAL, true)
    }

    private fun SEI() {
        setFlag(Flags.INTERRUPT_DISABLE, true)
    }

    private fun STA() {
        writeByte(eaddress, A)
    }

    private fun STX() {
        writeByte(eaddress, X)
    }

    private fun STY() {
        writeByte(eaddress, Y)
    }

    private fun TAX() {
        X = A
        setZNFlags(X)
    }

    private fun TAY() {
        Y = A
        setZNFlags(Y)
    }

    private fun TSX() {
        X = SP
        setZNFlags(X)
    }

    private fun TXA() {
        A = X
        setZNFlags(A)
    }

    private fun TXS() {
        SP = X
    }

    private fun TYA() {
        A = Y
        setZNFlags(A)
    }

    // ---------- Illegal instructions -----------

    private fun ALR() {
        AND()
        LSR_A()
    }

    private fun ANC() {
        AND()
        setFlag(Flags.CARRY, (A and Flags.NEGATIVE) > 0)
        instructionCycle = false
    }

    private fun ARR() {
        AND()
        ROR_A()
        setFlag(Flags.OVERFLOW, (A and Flags.BIT_6) xor ((A and Flags.BIT_5) shl 1) > 0)
        setFlag(Flags.CARRY, A and Flags.BIT_6 > 0)
    }

    private fun DCP() {
        DEC()
        CMP(A)
    }

    private fun ISB() {
        INC()
        SBC()
        instructionCycle = false
    }

    private fun LAS() {
        val data = readByte(eaddress)
        val result = SP and data
        A = result
        X = result
        SP = result
        setZNFlags(result)
        instructionCycle = true
    }

    private fun LAX() {
        LDA()
        LDX()
    }

    private fun RLA() {
        ROL()
        AND()
        instructionCycle = false
    }

    private fun RRA() {
        ROR()
        ADC()
        instructionCycle = false
    }

    private fun SAX() {
        writeByte(eaddress, A and X)
    }

    private fun SBX() {
        val oldX = X
        X = (A and X).plus8(-readByte(eaddress))
        setFlag(Flags.CARRY, (A and oldX) >= X)
        setZNFlags(X)
    }

    private fun SLO() {
        ASL()
        ORA()
        instructionCycle = false
    }

    private fun SRE(){
        LSR()
        EOR()
        instructionCycle = false
    }

    private val ADDRESS_HANDLER_TABLE: Array<() -> Unit> = arrayOf(
        /*      |   0   |   1   |   2   |   3   |   4   |   5   |   6   |   7   |   8   |   9   |   A   |   B   |   C   |   D   |   E   |   F   */
        /* 0 */  ::impl, ::indx, ::impl, ::indx,  ::zpg,  ::zpg,  ::zpg,  ::zpg, ::impl,  ::imm,  ::acc,  ::imm,  ::abs,  ::abs,  ::abs,  ::abs,
        /* 1 */   ::rel, ::indy, ::impl, ::indy, ::zpgx, ::zpgx, ::zpgx, ::zpgx, ::impl, ::absy, ::impl, ::absy, ::absx, ::absx, ::absx, ::absx,
        /* 2 */   ::abs, ::indx, ::impl, ::indx,  ::zpg,  ::zpg,  ::zpg,  ::zpg, ::impl,  ::imm,  ::acc,  ::imm,  ::abs,  ::abs,  ::abs,  ::abs,
        /* 3 */   ::rel, ::indy, ::impl, ::indy, ::zpgx, ::zpgx, ::zpgx, ::zpgx, ::impl, ::absy, ::impl, ::absy, ::absx, ::absx, ::absx, ::absx,
        /* 4 */  ::impl, ::indx, ::impl, ::indx,  ::zpg,  ::zpg,  ::zpg,  ::zpg, ::impl,  ::imm,  ::acc,  ::imm,  ::abs,  ::abs,  ::abs,  ::abs,
        /* 5 */   ::rel, ::indy, ::impl, ::indy, ::zpgx, ::zpgx, ::zpgx, ::zpgx, ::impl, ::absy, ::impl, ::absy, ::absx, ::absx, ::absx, ::absx,
        /* 6 */  ::impl, ::indx, ::impl, ::indx,  ::zpg,  ::zpg,  ::zpg,  ::zpg, ::impl,  ::imm,  ::acc,  ::imm,  ::ind,  ::abs,  ::abs,  ::abs,
        /* 7 */   ::rel, ::indy, ::impl, ::indy, ::zpgx, ::zpgx, ::zpgx, ::zpgx, ::impl, ::absy, ::impl, ::absy, ::absx, ::absx, ::absx, ::absx,
        /* 8 */   ::imm, ::indx,  ::imm, ::indx,  ::zpg,  ::zpg,  ::zpg,  ::zpg, ::impl,  ::imm, ::impl,  ::imm,  ::abs,  ::abs,  ::abs,  ::abs,
        /* 9 */   ::rel, ::indy, ::impl, ::indy, ::zpgx, ::zpgx, ::zpgy, ::zpgy, ::impl, ::absy, ::impl, ::absy, ::absx, ::absx, ::absy, ::absy,
        /* A */   ::imm, ::indx,  ::imm, ::indx,  ::zpg,  ::zpg,  ::zpg,  ::zpg, ::impl,  ::imm, ::impl,  ::imm,  ::abs,  ::abs,  ::abs,  ::abs,
        /* B */   ::rel, ::indy, ::impl, ::indy, ::zpgx, ::zpgx, ::zpgy, ::zpgy, ::impl, ::absy, ::impl, ::absy, ::absx, ::absx, ::absy, ::absy,
        /* C */   ::imm, ::indx,  ::imm, ::indx,  ::zpg,  ::zpg,  ::zpg,  ::zpg, ::impl,  ::imm, ::impl,  ::imm,  ::abs,  ::abs,  ::abs,  ::abs,
        /* D */   ::rel, ::indy, ::impl, ::indy, ::zpgx, ::zpgx, ::zpgx, ::zpgx, ::impl, ::absy, ::impl, ::absy, ::absx, ::absx, ::absx, ::absx,
        /* E */   ::imm, ::indx,  ::imm, ::indx,  ::zpg,  ::zpg,  ::zpg,  ::zpg, ::impl,  ::imm, ::impl,  ::imm,  ::abs,  ::abs,  ::abs,  ::abs,
        /* F */   ::rel, ::indy, ::impl, ::indy, ::zpgx, ::zpgx, ::zpgx, ::zpgx, ::impl, ::absy, ::impl, ::absy, ::absx, ::absx, ::absx, ::absx
    )

    // Using NOP instead of unstable instructions (ANE, LXA, SHA, SHX, SHY, TAS) and JAM
    private val INSTRUCTION_HANDLER_TABLE: Array<() -> Unit> = arrayOf(
        /*     |   0   |   1   |   2   |   3   |   4   |   5   |   6   |   7   |   8   |   9   |   A   |   B   |   C   |   D   |   E   |   F   | */
        /* 0 */  ::BRK,  ::ORA,  ::NOP,  ::SLO,  ::NOP,  ::ORA,  ::ASL,  ::SLO,  ::PHP,  ::ORA,  ::ASL,  ::ANC,  ::NOP,  ::ORA,  ::ASL,  ::SLO,
        /* 1 */  ::BPL,  ::ORA,  ::NOP,  ::SLO,  ::NOP,  ::ORA,  ::ASL,  ::SLO,  ::CLC,  ::ORA,  ::NOP,  ::SLO,  ::NOP,  ::ORA,  ::ASL,  ::SLO,
        /* 2 */  ::JSR,  ::AND,  ::NOP,  ::RLA,  ::BIT,  ::AND,  ::ROL,  ::RLA,  ::PLP,  ::AND,  ::ROL,  ::ANC,  ::BIT,  ::AND,  ::ROL,  ::RLA,
        /* 3 */  ::BMI,  ::AND,  ::NOP,  ::RLA,  ::NOP,  ::AND,  ::ROL,  ::RLA,  ::SEC,  ::AND,  ::NOP,  ::RLA,  ::NOP,  ::AND,  ::ROL,  ::RLA,
        /* 4 */  ::RTI,  ::EOR,  ::NOP,  ::SRE,  ::NOP,  ::EOR,  ::LSR,  ::SRE,  ::PHA,  ::EOR,  ::LSR,  ::ALR,  ::JMP,  ::EOR,  ::LSR,  ::SRE,
        /* 5 */  ::BVC,  ::EOR,  ::NOP,  ::SRE,  ::NOP,  ::EOR,  ::LSR,  ::SRE,  ::CLI,  ::EOR,  ::NOP,  ::SRE,  ::NOP,  ::EOR,  ::LSR,  ::SRE,
        /* 6 */  ::RTS,  ::ADC,  ::NOP,  ::RRA,  ::NOP,  ::ADC,  ::ROR,  ::RRA,  ::PLA,  ::ADC,  ::ROR,  ::ARR,  ::JMP,  ::ADC,  ::ROR,  ::RRA,
        /* 7 */  ::BVS,  ::ADC,  ::NOP,  ::RRA,  ::NOP,  ::ADC,  ::ROR,  ::RRA,  ::SEI,  ::ADC,  ::NOP,  ::RRA,  ::NOP,  ::ADC,  ::ROR,  ::RRA,
        /* 8 */  ::NOP,  ::STA,  ::NOP,  ::SAX,  ::STY,  ::STA,  ::STX,  ::SAX,  ::DEY,  ::NOP,  ::TXA,  ::NOP,  ::STY,  ::STA,  ::STX,  ::SAX,
        /* 9 */  ::BCC,  ::STA,  ::NOP,  ::NOP,  ::STY,  ::STA,  ::STX,  ::SAX,  ::TYA,  ::STA,  ::TXS,  ::NOP,  ::NOP,  ::STA,  ::NOP,  ::NOP,
        /* A */  ::LDY,  ::LDA,  ::LDX,  ::LAX,  ::LDY,  ::LDA,  ::LDX,  ::LAX,  ::TAY,  ::LDA,  ::TAX,  ::NOP,  ::LDY,  ::LDA,  ::LDX,  ::LAX,
        /* B */  ::BCS,  ::LDA,  ::NOP,  ::LAX,  ::LDY,  ::LDA,  ::LDX,  ::LAX,  ::CLV,  ::LDA,  ::TSX,  ::LAS,  ::LDY,  ::LDA,  ::LDX,  ::LAX,
        /* C */  ::CPY,  ::CMP,  ::NOP,  ::DCP,  ::CPY,  ::CMP,  ::DEC,  ::DCP,  ::INY,  ::CMP,  ::DEX,  ::SBX,  ::CPY,  ::CMP,  ::DEC,  ::DCP,
        /* D */  ::BNE,  ::CMP,  ::NOP,  ::DCP,  ::NOP,  ::CMP,  ::DEC,  ::DCP,  ::CLD,  ::CMP,  ::NOP,  ::DCP,  ::NOP,  ::CMP,  ::DEC,  ::DCP,
        /* E */  ::CPX,  ::SBC,  ::NOP,  ::ISB,  ::CPX,  ::SBC,  ::INC,  ::ISB,  ::INX,  ::SBC,  ::NOP,  ::SBC,  ::CPX,  ::SBC,  ::INC,  ::ISB,
        /* F */  ::BEQ,  ::SBC,  ::NOP,  ::ISB,  ::NOP,  ::SBC,  ::INC,  ::ISB,  ::SED,  ::SBC,  ::NOP,  ::ISB,  ::NOP,  ::SBC,  ::INC,  ::ISB
    )

    private val INSTRUCTION_CYCLES_TABLE: IntArray = intArrayOf(
        /*     |  0  |  1  |  2  |  3  |  4  |  5  |  6  |  7  |  8  |  9  |  A  |  B  |  C  |  D  |  E  |  F  | */
        /* 0 */   7,    6,    2,    8,    3,    3,    5,    5,    3,    2,    2,    2,    4,    4,    6,    6,
        /* 1 */   2,    5,    2,    8,    4,    4,    6,    6,    2,    4,    2,    7,    4,    4,    7,    7,
        /* 2 */   6,    6,    2,    8,    3,    3,    5,    5,    4,    2,    2,    2,    4,    4,    6,    6,
        /* 3 */   2,    5,    2,    8,    4,    4,    6,    6,    2,    4,    2,    7,    4,    4,    7,    7,
        /* 4 */   6,    6,    2,    8,    3,    3,    5,    5,    3,    2,    2,    2,    3,    4,    6,    6,
        /* 5 */   2,    5,    2,    8,    4,    4,    6,    6,    2,    4,    2,    7,    4,    4,    7,    7,
        /* 6 */   6,    6,    2,    8,    3,    3,    5,    5,    4,    2,    2,    2,    5,    4,    6,    6,
        /* 7 */   2,    5,    2,    8,    4,    4,    6,    6,    2,    4,    2,    7,    4,    4,    7,    7,
        /* 8 */   2,    6,    2,    6,    3,    3,    3,    3,    2,    2,    2,    2,    4,    4,    4,    4,
        /* 9 */   2,    6,    2,    6,    4,    4,    4,    4,    2,    5,    2,    5,    5,    5,    5,    5,
        /* A */   2,    6,    2,    6,    3,    3,    3,    3,    2,    2,    2,    2,    4,    4,    4,    4,
        /* B */   2,    5,    2,    5,    4,    4,    4,    4,    2,    4,    2,    4,    4,    4,    4,    4,
        /* C */   2,    6,    2,    8,    3,    3,    5,    5,    2,    2,    2,    2,    4,    4,    6,    6,
        /* D */   2,    5,    2,    8,    4,    4,    6,    6,    2,    4,    2,    7,    4,    4,    7,    7,
        /* E */   2,    6,    2,    8,    3,    3,    5,    5,    2,    2,    2,    2,    4,    4,    6,    6,
        /* F */   2,    5,    2,    8,    4,    4,    6,    6,    2,    4,    2,    7,    4,    4,    7,    7
    )

    companion object {
        private const val TAG = "Cpu"
        const val FREQUENCY_HZ = 1_789_773

        private val EMPTY_DEBUG_CALLBACK: (
            PC: Int,
            SP: Int,
            A: Int,
            X: Int,
            Y: Int,
            PS: Int,
            cycles: Int
        ) -> Unit = { _, _, _, _, _, _, _ -> }

        val INSTRUCTION_NAME_TABLE: Array<String> = arrayOf(
            /*     |   0   |   1   |   2   |   3   |   4   |   5   |   6   |   7   |   8   |   9   |   A   |   B   |   C   |   D   |   E   |   F   | */
            /* 0 */  "BRK",  "ORA",  "NOP",  "SLO",  "NOP",  "ORA",  "ASL",  "SLO",  "PHP",  "ORA",  "ASL",  "ANC",  "NOP",  "ORA",  "ASL",  "SLO",
            /* 1 */  "BPL",  "ORA",  "NOP",  "SLO",  "NOP",  "ORA",  "ASL",  "SLO",  "CLC",  "ORA",  "NOP",  "SLO",  "NOP",  "ORA",  "ASL",  "SLO",
            /* 2 */  "JSR",  "AND",  "NOP",  "RLA",  "BIT",  "AND",  "ROL",  "RLA",  "PLP",  "AND",  "ROL",  "ANC",  "BIT",  "AND",  "ROL",  "RLA",
            /* 3 */  "BMI",  "AND",  "NOP",  "RLA",  "NOP",  "AND",  "ROL",  "RLA",  "SEC",  "AND",  "NOP",  "RLA",  "NOP",  "AND",  "ROL",  "RLA",
            /* 4 */  "RTI",  "EOR",  "NOP",  "SRE",  "NOP",  "EOR",  "LSR",  "SRE",  "PHA",  "EOR",  "LSR",  "ALR",  "JMP",  "EOR",  "LSR",  "SRE",
            /* 5 */  "BVC",  "EOR",  "NOP",  "SRE",  "NOP",  "EOR",  "LSR",  "SRE",  "CLI",  "EOR",  "NOP",  "SRE",  "NOP",  "EOR",  "LSR",  "SRE",
            /* 6 */  "RTS",  "ADC",  "NOP",  "RRA",  "NOP",  "ADC",  "ROR",  "RRA",  "PLA",  "ADC",  "ROR",  "ARR",  "JMP",  "ADC",  "ROR",  "RRA",
            /* 7 */  "BVS",  "ADC",  "NOP",  "RRA",  "NOP",  "ADC",  "ROR",  "RRA",  "SEI",  "ADC",  "NOP",  "RRA",  "NOP",  "ADC",  "ROR",  "RRA",
            /* 8 */  "NOP",  "STA",  "NOP",  "SAX",  "STY",  "STA",  "STX",  "SAX",  "DEY",  "NOP",  "TXA",  "NOP",  "STY",  "STA",  "STX",  "SAX",
            /* 9 */  "BCC",  "STA",  "NOP",  "NOP",  "STY",  "STA",  "STX",  "SAX",  "TYA",  "STA",  "TXS",  "NOP",  "NOP",  "STA",  "NOP",  "NOP",
            /* A */  "LDY",  "LDA",  "LDX",  "LAX",  "LDY",  "LDA",  "LDX",  "LAX",  "TAY",  "LDA",  "TAX",  "NOP",  "LDY",  "LDA",  "LDX",  "LAX",
            /* B */  "BCS",  "LDA",  "NOP",  "LAX",  "LDY",  "LDA",  "LDX",  "LAX",  "CLV",  "LDA",  "TSX",  "LAS",  "LDY",  "LDA",  "LDX",  "LAX",
            /* C */  "CPY",  "CMP",  "NOP",  "DCP",  "CPY",  "CMP",  "DEC",  "DCP",  "INY",  "CMP",  "DEX",  "SBX",  "CPY",  "CMP",  "DEC",  "DCP",
            /* D */  "BNE",  "CMP",  "NOP",  "DCP",  "NOP",  "CMP",  "DEC",  "DCP",  "CLD",  "CMP",  "NOP",  "DCP",  "NOP",  "CMP",  "DEC",  "DCP",
            /* E */  "CPX",  "SBC",  "NOP",  "ISB",  "CPX",  "SBC",  "INC",  "ISB",  "INX",  "SBC",  "NOP",  "SBC",  "CPX",  "SBC",  "INC",  "ISB",
            /* F */  "BEQ",  "SBC",  "NOP",  "ISB",  "NOP",  "SBC",  "INC",  "ISB",  "SED",  "SBC",  "NOP",  "ISB",  "NOP",  "SBC",  "INC",  "ISB"
        )

        val ADDRESSING_MODE_TABLE: Array<String> = arrayOf(
            /*      |   0   |   1   |   2   |   3   |   4   |   5   |   6   |   7   |   8   |   9   |   A   |   B   |   C   |   D   |   E   |   F   */
            /* 0 */  "IMPL", "INDX", "IMPL", "INDX",  "ZPG",  "ZPG",  "ZPG",  "ZPG", "IMPL",  "IMM",  "ACC",  "IMM",  "ABS",  "ABS",  "ABS",  "ABS",
            /* 1 */   "REL", "INDY", "IMPL", "INDY", "ZPGX", "ZPGX", "ZPGX", "ZPGX", "IMPL", "ABSY", "IMPL", "ABSY", "ABSX", "ABSX", "ABSX", "ABSX",
            /* 2 */   "ABS", "INDX", "IMPL", "INDX",  "ZPG",  "ZPG",  "ZPG",  "ZPG", "IMPL",  "IMM",  "ACC",  "IMM",  "ABS",  "ABS",  "ABS",  "ABS",
            /* 3 */   "REL", "INDY", "IMPL", "INDY", "ZPGX", "ZPGX", "ZPGX", "ZPGX", "IMPL", "ABSY", "IMPL", "ABSY", "ABSX", "ABSX", "ABSX", "ABSX",
            /* 4 */  "IMPL", "INDX", "IMPL", "INDX",  "ZPG",  "ZPG",  "ZPG",  "ZPG", "IMPL",  "IMM",  "ACC",  "IMM",  "ABS",  "ABS",  "ABS",  "ABS",
            /* 5 */   "REL", "INDY", "IMPL", "INDY", "ZPGX", "ZPGX", "ZPGX", "ZPGX", "IMPL", "ABSY", "IMPL", "ABSY", "ABSX", "ABSX", "ABSX", "ABSX",
            /* 6 */  "IMPL", "INDX", "IMPL", "INDX",  "ZPG",  "ZPG",  "ZPG",  "ZPG", "IMPL",  "IMM",  "ACC",  "IMM",  "IND",  "ABS",  "ABS",  "ABS",
            /* 7 */   "REL", "INDY", "IMPL", "INDY", "ZPGX", "ZPGX", "ZPGX", "ZPGX", "IMPL", "ABSY", "IMPL", "ABSY", "ABSX", "ABSX", "ABSX", "ABSX",
            /* 8 */   "IMM", "INDX",  "IMM", "INDX",  "ZPG",  "ZPG",  "ZPG",  "ZPG", "IMPL",  "IMM", "IMPL",  "IMM",  "ABS",  "ABS",  "ABS",  "ABS",
            /* 9 */   "REL", "INDY", "IMPL", "INDY", "ZPGX", "ZPGX", "ZPGY", "ZPGY", "IMPL", "ABSY", "IMPL", "ABSY", "ABSX", "ABSX", "ABSY", "ABSY",
            /* A */   "IMM", "INDX",  "IMM", "INDX",  "ZPG",  "ZPG",  "ZPG",  "ZPG", "IMPL",  "IMM", "IMPL",  "IMM",  "ABS",  "ABS",  "ABS",  "ABS",
            /* B */   "REL", "INDY", "IMPL", "INDY", "ZPGX", "ZPGX", "ZPGY", "ZPGY", "IMPL", "ABSY", "IMPL", "ABSY", "ABSX", "ABSX", "ABSY", "ABSY",
            /* C */   "IMM", "INDX",  "IMM", "INDX",  "ZPG",  "ZPG",  "ZPG",  "ZPG", "IMPL",  "IMM", "IMPL",  "IMM",  "ABS",  "ABS",  "ABS",  "ABS",
            /* D */   "REL", "INDY", "IMPL", "INDY", "ZPGX", "ZPGX", "ZPGX", "ZPGX", "IMPL", "ABSY", "IMPL", "ABSY", "ABSX", "ABSX", "ABSX", "ABSX",
            /* E */   "IMM", "INDX",  "IMM", "INDX",  "ZPG",  "ZPG",  "ZPG",  "ZPG", "IMPL",  "IMM", "IMPL",  "IMM",  "ABS",  "ABS",  "ABS",  "ABS",
            /* F */   "REL", "INDY", "IMPL", "INDY", "ZPGX", "ZPGX", "ZPGX", "ZPGX", "IMPL", "ABSY", "IMPL", "ABSY", "ABSX", "ABSX", "ABSX", "ABSX"
        )

        val ILLEGAL_INSTRUCTION_TABLE: Array<Boolean> = arrayOf(
            /*     |   0   |   1   |   2   |   3   |   4   |   5   |   6   |   7   |   8   |   9   |   A   |   B   |   C   |   D   |   E   |   F   | */
            /* 0 */  false,  false,   true,   true,   true,  false,  false,   true,  false,  false,  false,   true,   true,  false,  false,  true,
            /* 1 */  false,  false,   true,   true,   true,  false,  false,   true,  false,  false,   true,   true,   true,  false,  false,  true,
            /* 2 */  false,  false,   true,   true,  false,  false,  false,   true,  false,  false,  false,   true,  false,  false,  false,  true,
            /* 3 */  false,  false,   true,   true,   true,  false,  false,   true,  false,  false,   true,   true,   true,  false,  false,  true,
            /* 4 */  false,  false,   true,   true,   true,  false,  false,   true,  false,  false,  false,   true,  false,  false,  false,  true,
            /* 5 */  false,  false,   true,   true,   true,  false,  false,   true,  false,  false,   true,   true,   true,  false,  false,  true,
            /* 6 */  false,  false,   true,   true,   true,  false,  false,   true,  false,  false,  false,   true,  false,  false,  false,  true,
            /* 7 */  false,  false,   true,   true,   true,  false,  false,   true,  false,  false,   true,   true,   true,  false,  false,  true,
            /* 8 */   true,  false,   true,   true,  false,  false,  false,   true,  false,   true,  false,   true,  false,  false,  false,  true,
            /* 9 */  false,  false,   true,   true,  false,  false,  false,   true,  false,  false,  false,   true,   true,  false,   true,  true,
            /* A */  false,  false,  false,   true,  false,  false,  false,   true,  false,  false,  false,   true,  false,  false,  false,  true,
            /* B */  false,  false,   true,   true,  false,  false,  false,   true,  false,  false,  false,   true,  false,  false,  false,  true,
            /* C */  false,  false,   true,   true,  false,  false,  false,   true,  false,  false,  false,   true,  false,  false,  false,  true,
            /* D */  false,  false,   true,   true,   true,  false,  false,   true,  false,  false,   true,   true,   true,  false,  false,  true,
            /* E */  false,  false,   true,   true,  false,  false,  false,   true,  false,  false,  false,   true,  false,  false,  false,  true,
            /* F */  false,  false,   true,   true,   true,  false,  false,   true,  false,  false,   true,   true,   true,  false,  false,  true
        )
    }
}
