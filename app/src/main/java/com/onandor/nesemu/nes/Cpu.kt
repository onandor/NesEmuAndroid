package com.onandor.nesemu.nes

@Suppress("PropertyName")
data class CpuState(
    val PC: Int,
    val SP: Int,
    val A: Int,
    val X: Int,
    val Y: Int,
    val PS: Int
)

@Suppress("FunctionName", "PrivatePropertyName")
class Cpu(
    private val onReadMemory: (Int) -> Int,
    private val onWriteMemory: (Int, Int) -> Unit
) {

    private object Flags {
        const val CARRY: Int = 0b00000001
        const val ZERO: Int = 0b00000010
        const val INTERRUPT: Int = 0b00000100
        const val DECIMAL: Int = 0b00001000
        const val BREAK: Int = 0b00010000
        const val UNUSED: Int = 0b00100000
        const val OVERFLOW: Int = 0b01000000
        const val NEGATIVE: Int = 0b10000000
    }

    private var PC: Int = 0xFFFC            // Program Counter - 16 bits
    private var SP: Int = 0xFD              // Stack Pointer - 8 bits
    private var A: Int = 0                  // Accumulator - 8 bits
    private var X: Int = 0                  // Index Register X - 8 bits
    private var Y: Int = 0                  // Index Register Y - 8 bits
    private var PS: Int = 0b00010100        // Processor Status - 8 bits (flags)

    private var opcode: Int = 0             // Currently executed opcode
    private var eaddress: Int = 0           // Effective address for the current instruction

    private var addressingCycle = false
    private var opcodeCycle = false

    private var cycles: Int = 0

    init {
        reset()
    }

    fun reset() {
        SP = 0xFD
        A = 0
        X = 0
        Y = 0
        cycles = 0
    }

    fun execute(cycles: Int): Int {
        this.cycles = 0
        while (this.cycles < cycles) {
            addressingCycle = false
            opcodeCycle = false

            opcode = readByte(PC)
            PC = PC.plus16(1)
            addressHandlerTable[opcode]()
            opcodeHandlerTable[opcode]()

            this.cycles += opcodeCyclesTable[opcode]
            if (addressingCycle && opcodeCycle) {
                this.cycles++
            }
        }
        return this.cycles
    }

    private fun readByte(address: Int): Int = onReadMemory(address and 0xFFFF)

    private fun read2Bytes(address: Int): Int =
        onReadMemory(address and 0xFFFF) or (onReadMemory((address + 1) and 0xFFFF) shl 8)

    fun writeByte(address: Int, value: Int) = onWriteMemory(address and 0xFFFF, value and 0xFF)

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

    // Implied
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
        //eaddress = (readByte(PC) + X) and 0xFF
        eaddress = readByte(PC).plus8(X)
        PC = PC.plus16(1)
    }

    // Zero Page,Y
    private fun zpgy() {
        eaddress = readByte(PC).plus8(Y)
        PC = PC.plus16(1)
    }

    // ------ Instruction handler functions ------

    private fun branch() {
        val offset = readByte(eaddress)
        val oldPC = PC
        PC = PC.plus16(offset.toSigned8())
        cycles += 1 + ((PC and 0xFF00) != (oldPC and 0xFF00)).toInt()
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
            //ADC_decimal(operand)
            throw RuntimeException("ADC in decimal mode is not supported")
        } else {
            ADC_binary(operand)
        }
        opcodeCycle = true
    }

    private fun ADC() {
        ADC(readByte(eaddress))
    }

    private fun AND() {
        val operand = readByte(eaddress)
        A = A and operand
        setZNFlags(A)
        opcodeCycle = true
    }

    private fun ASL() {
        if (addressHandlerTable[opcode] == ::acc) {
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
        setFlag(Flags.INTERRUPT, true)
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
        setFlag(Flags.INTERRUPT, false)
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
        opcodeCycle = true
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
        opcodeCycle = true
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
        opcodeCycle = true
    }

    private fun LDX() {
        X = readByte(eaddress)
        setZNFlags(X)
        opcodeCycle = true
    }

    private fun LDY() {
        Y = readByte(eaddress)
        setZNFlags(Y)
        opcodeCycle = true
    }

    private fun LSR() {
        if (addressHandlerTable[opcode] == ::acc) {
            setFlag(Flags.CARRY, A and Flags.CARRY > 0)
            A = A shr 1
            setZNFlags(A)
            return
        }
        var data = readByte(eaddress)
        setFlag(Flags.CARRY, data and Flags.CARRY > 0)
        data = data shr 1
        writeByte(eaddress, data)
        setZNFlags(data)
    }

    private fun NOP() {}

    private fun ORA() {
        val data = readByte(eaddress)
        A = A or data
        setZNFlags(A)
        opcodeCycle = true
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
        if (addressHandlerTable[opcode] == ::acc) {
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

    private fun ROR() {
        val oldCarry = PS and Flags.CARRY
        if (addressHandlerTable[opcode] == ::acc) {
            setFlag(Flags.CARRY, A and Flags.CARRY > 0)
            A = (A shr 1) or (oldCarry shl 7)
            setZNFlags(A)
            return
        }
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
            throw RuntimeException("SBC in decimal mode is not supported")
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
        setFlag(Flags.INTERRUPT, true)
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

    private val addressHandlerTable: Array<() -> Unit> = arrayOf(
        /*      |   0   |   1   |   2   |   3   |   4   |   5   |   6   |   7   |   8   |   9   |   A   |   B   |   C   |   D   |   E   |   F   */
        /* 0 */	 ::impl, ::indx, ::impl, ::impl, ::impl,  ::zpg,  ::zpg, ::impl, ::impl,  ::imm,  ::acc, ::impl, ::impl,  ::abs,  ::abs, ::impl,
        /* 1 */   ::rel, ::indy, ::impl, ::impl, ::impl, ::zpgx, ::zpgx, ::impl, ::impl, ::absy, ::impl, ::impl, ::impl, ::absx, ::absx, ::impl,
        /* 2 */   ::abs, ::indx, ::impl, ::impl,  ::zpg,  ::zpg,  ::zpg, ::impl, ::impl,  ::imm,  ::acc, ::impl,  ::abs,  ::abs,  ::abs, ::impl,
        /* 3 */   ::rel, ::indy, ::impl, ::impl, ::impl, ::zpgx, ::zpgx, ::impl, ::impl, ::absy, ::impl, ::impl, ::impl, ::absx, ::absx, ::impl,
        /* 4 */  ::impl, ::indx, ::impl, ::impl, ::impl,  ::zpg,  ::zpg, ::impl, ::impl,  ::imm,  ::acc, ::impl,  ::abs,  ::abs,  ::abs, ::impl,
        /* 5 */   ::rel, ::indy, ::impl, ::impl, ::impl, ::zpgx, ::zpgx, ::impl, ::impl, ::absy, ::impl, ::impl, ::impl, ::absx, ::absx, ::impl,
        /* 6 */  ::impl, ::indx, ::impl, ::impl, ::impl,  ::zpg,  ::zpg, ::impl, ::impl,  ::imm,  ::acc, ::impl,  ::ind,  ::abs,  ::abs, ::impl,
        /* 7 */   ::rel, ::indy, ::impl, ::impl, ::impl, ::zpgx, ::zpgx, ::impl, ::impl, ::absy, ::impl, ::impl, ::impl, ::absx, ::absx, ::impl,
        /* 8 */  ::impl, ::indx, ::impl, ::impl,  ::zpg,  ::zpg,  ::zpg, ::impl, ::impl, ::impl, ::impl, ::impl,  ::abs,  ::abs,  ::abs, ::impl,
        /* 9 */   ::rel, ::indy, ::impl, ::impl, ::zpgx, ::zpgx, ::zpgy, ::impl, ::impl, ::absy, ::impl, ::impl, ::impl, ::absx, ::impl, ::impl,
        /* A */   ::imm, ::indx,  ::imm, ::impl,  ::zpg,  ::zpg,  ::zpg, ::impl, ::impl,  ::imm, ::impl, ::impl,  ::abs,  ::abs,  ::abs, ::impl,
        /* B */   ::rel, ::indy, ::impl, ::impl, ::zpgx, ::zpgx, ::zpgy, ::impl, ::impl, ::absy, ::impl, ::impl, ::absx, ::absx, ::absy, ::impl,
        /* C */   ::imm, ::indx, ::impl, ::impl,  ::zpg,  ::zpg,  ::zpg, ::impl, ::impl,  ::imm, ::impl, ::impl,  ::abs,  ::abs,  ::abs, ::impl,
        /* D */   ::rel, ::indy, ::impl, ::impl, ::impl, ::zpgx, ::zpgx, ::impl, ::impl, ::absy, ::impl, ::impl, ::impl, ::absx, ::absx, ::impl,
        /* E */   ::imm, ::indx, ::impl, ::impl,  ::zpg,  ::zpg,  ::zpg, ::impl, ::impl,  ::imm, ::impl, ::impl,  ::abs,  ::abs,  ::abs, ::impl,
        /* F */   ::rel, ::indy, ::impl, ::impl, ::impl, ::zpgx, ::zpgx, ::impl, ::impl, ::absy, ::impl, ::impl, ::impl, ::absx, ::absx, ::impl
    )

    private val opcodeHandlerTable: Array<() -> Unit> = arrayOf(
        /*     |   0   |   1   |   2   |   3   |   4   |   5   |   6   |   7   |   8   |   9   |   A   |   B   |   C   |   D   |   E   |   F   | */
        /* 0 */  ::BRK,  ::ORA,  ::NOP,  ::NOP,  ::NOP,  ::ORA,  ::ASL,  ::NOP,  ::PHP,  ::ORA,  ::ASL,  ::NOP,  ::NOP,  ::ORA,  ::ASL,  ::NOP,
        /* 1 */  ::BPL,  ::ORA,  ::NOP,  ::NOP,  ::NOP,  ::ORA,  ::ASL,  ::NOP,  ::CLC,  ::ORA,  ::NOP,  ::NOP,  ::NOP,  ::ORA,  ::ASL,  ::NOP,
        /* 2 */  ::JSR,  ::AND,  ::NOP,  ::NOP,  ::BIT,  ::AND,  ::ROL,  ::NOP,  ::PLP,  ::AND,  ::ROL,  ::NOP,  ::BIT,  ::AND,  ::ROL,  ::NOP,
        /* 3 */  ::BMI,  ::AND,  ::NOP,  ::NOP,  ::NOP,  ::AND,  ::ROL,  ::NOP,  ::SEC,  ::AND,  ::NOP,  ::NOP,  ::NOP,  ::AND,  ::ROL,  ::NOP,
        /* 4 */  ::RTI,  ::EOR,  ::NOP,  ::NOP,  ::NOP,  ::EOR,  ::LSR,  ::NOP,  ::PHA,  ::EOR,  ::LSR,  ::NOP,  ::JMP,  ::EOR,  ::LSR,  ::NOP,
        /* 5 */  ::BVC,  ::EOR,  ::NOP,  ::NOP,  ::NOP,  ::EOR,  ::LSR,  ::NOP,  ::CLI,  ::EOR,  ::NOP,  ::NOP,  ::NOP,  ::EOR,  ::LSR,  ::NOP,
        /* 6 */  ::RTS,  ::ADC,  ::NOP,  ::NOP,  ::NOP,  ::ADC,  ::ROR,  ::NOP,  ::PLA,  ::ADC,  ::ROR,  ::NOP,  ::JMP,  ::ADC,  ::ROR,  ::NOP,
        /* 7 */  ::BVS,  ::ADC,  ::NOP,  ::NOP,  ::NOP,  ::ADC,  ::ROR,  ::NOP,  ::SEI,  ::ADC,  ::NOP,  ::NOP,  ::NOP,  ::ADC,  ::ROR,  ::NOP,
        /* 8 */  ::NOP,  ::STA,  ::NOP,  ::NOP,  ::STY,  ::STA,  ::STX,  ::NOP,  ::DEY,  ::NOP,  ::TXA,  ::NOP,  ::STY,  ::STA,  ::STX,  ::NOP,
        /* 9 */  ::BCC,  ::STA,  ::NOP,  ::NOP,  ::STY,  ::STA,  ::STX,  ::NOP,  ::TYA,  ::STA,  ::TXS,  ::NOP,  ::NOP,  ::STA,  ::NOP,  ::NOP,
        /* A */  ::LDY,  ::LDA,  ::LDX,  ::NOP,  ::LDY,  ::LDA,  ::LDX,  ::NOP,  ::TAY,  ::LDA,  ::TAX,  ::NOP,  ::LDY,  ::LDA,  ::LDX,  ::NOP,
        /* B */  ::BCS,  ::LDA,  ::NOP,  ::NOP,  ::LDY,  ::LDA,  ::LDX,  ::NOP,  ::CLV,  ::LDA,  ::TSX,  ::NOP,  ::LDY,  ::LDA,  ::LDX,  ::NOP,
        /* C */  ::CPY,  ::CMP,  ::NOP,  ::NOP,  ::CPY,  ::CMP,  ::DEC,  ::NOP,  ::INY,  ::CMP,  ::DEX,  ::NOP,  ::CPY,  ::CMP,  ::DEC,  ::NOP,
        /* D */  ::BNE,  ::CMP,  ::NOP,  ::NOP,  ::NOP,  ::CMP,  ::DEC,  ::NOP,  ::CLD,  ::CMP,  ::NOP,  ::NOP,  ::NOP,  ::CMP,  ::DEC,  ::NOP,
        /* E */  ::CPX,  ::SBC,  ::NOP,  ::NOP,  ::CPX,  ::SBC,  ::INC,  ::NOP,  ::INX,  ::SBC,  ::NOP,  ::NOP,  ::CPX,  ::SBC,  ::INC,  ::NOP,
        /* F */  ::BEQ,  ::SBC,  ::NOP,  ::NOP,  ::NOP,  ::SBC,  ::INC,  ::NOP,  ::SED,  ::SBC,  ::NOP,  ::NOP,  ::NOP,  ::SBC,  ::INC,  ::NOP
    )

    private val opcodeCyclesTable: IntArray = intArrayOf(
        /*     |  0  |  1  |  2  |  3  |  4  |  5  |  6  |  7  |  8  |  9  |  A  |  B  |  C  |  D  |  E  |  F  | */
        /* 0 */   7,    6,    2,    2,    2,    3,    5,    2,    3,    2,    2,    2,    2,    4,    6,    2,
        /* 1 */   2,    5,    2,    2,    2,    4,    6,    2,    2,    4,    2,    2,    2,    4,    7,    2,
        /* 2 */   6,    6,    2,    2,    3,    3,    5,    2,    4,    2,    2,    2,    4,    4,    6,    2,
        /* 3 */   2,    5,    2,    2,    2,    4,    6,    2,    2,    4,    2,    2,    2,    4,    7,    2,
        /* 4 */   6,    6,    2,    2,    2,    3,    5,    2,    3,    2,    2,    2,    3,    4,    6,    2,
        /* 5 */   2,    5,    2,    2,    2,    4,    6,    2,    2,    4,    2,    2,    2,    4,    7,    2,
        /* 6 */   6,    6,    2,    2,    2,    3,    5,    2,    4,    2,    2,    2,    5,    4,    6,    2,
        /* 7 */   2,    5,    2,    2,    2,    4,    6,    2,    2,    4,    2,    2,    2,    4,    7,    2,
        /* 8 */   2,    6,    2,    2,    3,    3,    3,    2,    2,    2,    2,    2,    4,    4,    4,    2,
        /* 9 */   2,    6,    2,    2,    4,    4,    4,    2,    2,    5,    2,    2,    2,    5,    2,    2,
        /* A */   2,    6,    2,    2,    3,    3,    3,    2,    2,    2,    2,    2,    4,    4,    4,    2,
        /* B */   2,    5,    2,    2,    4,    4,    4,    2,    2,    4,    2,    2,    4,    4,    4,    2,
        /* C */   2,    6,    2,    2,    3,    3,    5,    2,    2,    2,    2,    2,    4,    4,    6,    2,
        /* D */   2,    5,    2,    2,    2,    4,    6,    2,    2,    4,    2,    2,    2,    4,    7,    2,
        /* E */   2,    6,    2,    2,    3,    3,    5,    2,    2,    2,    2,    2,    4,    4,    6,    2,
        /* F */   2,    5,    2,    2,    2,    4,    6,    2,    2,    4,    2,    2,    2,    4,    7,    2
    )
}
