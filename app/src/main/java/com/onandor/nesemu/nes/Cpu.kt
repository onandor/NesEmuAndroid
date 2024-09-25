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
class Cpu(private var memory: Memory) {

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

            opcode = readByte(PC++)
            addressHandlerTable[opcode]()
            opcodeHandlerTable[opcode]()

            this.cycles += opcodeCyclesTable[opcode]
            if (addressingCycle && opcodeCycle) {
                this.cycles++
            }
        }
        return this.cycles
    }

    private fun readByte(address: Int): Int = memory[address and 0xFFFF]

    private fun read2Bytes(address: Int): Int =
        memory[address and 0xFFFF] or (memory[(address + 1) and 0xFFFF] shl 8)

    fun writeByte(address: Int, value: Int) {
        memory[address and 0xFFFF] = value
    }

    private fun setFlag(flag: Int, set: Boolean) {
        PS = if (set) PS or flag else PS and flag.inv()
    }

    private fun getFlag(flag: Int): Boolean = (PS and flag) > 0

    private fun incrPC(incrementBy: Int): Int {
        val oldPC = PC
        PC = (PC + incrementBy) and 0xFFFF
        return oldPC
    }

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

    // ------ Addressing mode functions ------

    // Accumulator
    private fun acc() {}

    // Absolute
    private fun abs() {
        eaddress = readByte(incrPC(1)) or (readByte(incrPC(1)) shl 8)
    }

    // Absolute,X
    private fun absx() {
        val address = readByte(PC)
        eaddress = address + X
        addressingCycle = (address xor eaddress) shr 8 > 0
        incrPC(2)
    }

    // Absolute,Y
    private fun absy() {
        val address = read2Bytes(PC);
        eaddress = address + Y
        addressingCycle = (address xor eaddress) shr 8 > 0
        incrPC(2)
    }

    // Immediate
    private fun imm() {
        eaddress = incrPC(1)
    }

    // Implied
    private fun impl() {}

    // Indirect
    private fun ind() {
        val address = read2Bytes(PC)
        incrPC(2)
        eaddress = if ((address and 0xFF) == 0xFF) {
            readByte(address) or (readByte(address and 0xFF00) shl 8)
        } else {
            read2Bytes(address)
        }
    }

    // Indexed Indirect
    private fun indx() {
        val pageZeroAddress = readByte(incrPC(1)) + X
        val nextPageZeroAddress = (pageZeroAddress + 1) and 0xFF // Zero page wrap-around
        eaddress = readByte(pageZeroAddress) or (readByte(nextPageZeroAddress) shl 8)
    }

    // Indirect Indexed
    private fun indy() {
        val pageZeroAddress = readByte(PC++)
        val nextPageZeroAddress = (pageZeroAddress + 1) and 0xFF // Zero page wrap around
        val address = readByte(pageZeroAddress) or (readByte(nextPageZeroAddress) shl 8)
        val addressY = address + Y
        addressingCycle = (address xor addressY) shr 8 > 0
        eaddress = addressY
    }

    // Relative
    private fun rel() {
        eaddress = PC++
    }

    // Zero Page
    private fun zpg() {
        eaddress = readByte(PC++)
    }

    // Zero Page,X
    private fun zpgx() {
        eaddress = (readByte(PC++) + X) and 0xFF
    }

    // Zero Page,Y
    private fun zpgy() {
        eaddress = (readByte(PC++) + Y) and 0xFF
    }

    // ------ Instruction handler functions ------

    private fun branch() {
        val displacement = readByte(eaddress)
        val oldPC = PC
        PC += displacement
        cycles += 1 + ((PC and 0xFF00) != (oldPC and 0xFF00)).toInt()
    }

    private fun ADC(operand: Int) {
        var sum: Int
        if (getFlag(Flags.DECIMAL)) {
            sum = (A and 0x0F) + (operand and 0x0F) + getFlag(Flags.CARRY).toInt()
            if (sum > 0x09)
                sum += 0x06
            sum = (A and 0xF0) + (operand and 0xF0) + (if (sum > 0x0F) 0x10 else 0) + (sum and 0x0F)
        } else {
            sum = A + operand + getFlag(Flags.CARRY).toInt()
        }

        setFlag(Flags.OVERFLOW, (operand xor sum) and (A xor sum) and 0x80 > 0)
        if (getFlag(Flags.DECIMAL)) {
            setZNFlags(sum)
            if (sum > 0x9F)
                sum += 0x60
        } else {
            setZNFlags(sum and 0xFF)
        }
        setFlag(Flags.CARRY, sum > 0xFF)
        A = sum and 0xFF
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
            A = A shl 1
            setZNFlags(A)
            return
        }
        var data = readByte(eaddress)
        setFlag(Flags.CARRY, data and Flags.NEGATIVE > 0)
        data = data shl 1
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
        PC++
        writeByte(SP-- or 0x100, PC shr 8)
        writeByte(SP-- or 0x100, PC)
        writeByte(SP-- or 0x100, PS or Flags.BREAK)
        PC = read2Bytes(0xFFFE)
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
        val data = readByte(eaddress) - 1
        writeByte(eaddress, data)
        setZNFlags(data)
    }

    private fun DEX() {
        X -= 1
        setZNFlags(X)
    }

    private fun DEY() {
        Y -= 1
        setZNFlags(Y)
    }

    private fun EOR() {
        val data = readByte(eaddress)
        A = A xor data
        setZNFlags(A)
        opcodeCycle = true
    }

    private fun INC() {
        val data = readByte(eaddress) + 1
        writeByte(eaddress, data)
        setZNFlags(data)
    }

    private fun INX() {
        X += 1
        setZNFlags(X)
    }

    private fun INY() {
        Y += 1
        setZNFlags(Y)
    }

    private fun JMP() {
        PC = eaddress
    }

    private fun JSR() {
        writeByte(SP-- or 0x100, --PC shr 0)
        writeByte(SP-- or 0x100, PC)
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
        writeByte(--SP or 0x100, A)
    }

    private fun PHP() {
        writeByte(SP or 0x100, PS or Flags.UNUSED or Flags.BREAK)
    }

    private fun PLA() {
        A = readByte(++SP or 0x100)
        setZNFlags(A)
    }

    private fun PLP() {
        PS = readByte(++SP or 0x100) and Flags.BREAK.inv() or Flags.UNUSED
    }

    private fun ROL() {
        val oldCarry = PS and Flags.CARRY
        if (addressHandlerTable[opcode] == ::acc) {
            setFlag(Flags.CARRY, A and Flags.NEGATIVE > 0)
            A = (A shl 1) or oldCarry
            setZNFlags(A)
            return
        }
        var data = readByte(eaddress)
        setFlag(Flags.CARRY, data and Flags.NEGATIVE > 0)
        data = (data shl 1) or oldCarry
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
        PS = readByte(++SP or 0x100) and Flags.BREAK.inv() or Flags.UNUSED
        PC = readByte(++SP or 0x100) or (readByte(++ SP or 0x100) shl 8)
    }

    private fun RTS() {
        val pcLow = readByte(++SP or 0x100)
        val pcHigh = readByte(++SP or 0x100)
        PC = (pcLow or (pcHigh shl 8)) + 1
    }

    private fun SBC() {
        val data = readByte(eaddress)
        ADC(data.inv())
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
