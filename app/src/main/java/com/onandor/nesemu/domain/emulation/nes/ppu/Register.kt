package com.onandor.nesemu.domain.emulation.nes.ppu

interface Register {

    val address: Int
    var value: Int
}

class Control : Register {

    override val address: Int = 0x2000
    override var value: Int = 0

    var nametableSelect: Int
        get() { return value and 0x03 }
        set(value) { this.value = (this.value and 0xFC) or (value and 0x03) }
    var vramAddrIncrement: Int
        get() { return (value and 0x04) ushr 2 }
        set(value) { this.value = if (value > 0) this.value or 0x04 else this.value and 0x04.inv() }
    var spritePatternTableSelect: Int
        get() { return (value and 0x08) ushr 3 }
        set(value) { this.value = if (value > 0) this.value or 0x08 else this.value and 0x08.inv() }
    var bgPatternTableSelect: Int
        get() { return (value and 0x10) ushr 4 }
        set(value) { this.value = if (value > 0) this.value or 0x10 else this.value and 0x10.inv() }
    var tallSprites: Int
        get() { return (value and 0x20) ushr 5 }
        set(value) { this.value = if (value > 0) this.value or 0x20 else this.value and 0x20.inv() }
    var masterSlaveSelect: Int
        get() { return (value and 0x40) ushr 6 }
        set(value) { this.value = if (value > 0) this.value or 0x40 else this.value and 0x40.inv() }
    var enableVBlankNmi: Int
        get() { return (value and 0x80) ushr 7 }
        set(value) { this.value = if (value > 0) this.value or 0x80 else this.value and 0x80.inv() }
}

class Mask : Register {

    override val address: Int = 0x2001
    override var value: Int = 0

    var grayscale: Int
        get() { return value and 0x01 }
        set(value) { this.value = if (value > 0) this.value or 0x01 else this.value and 0x01.inv() }
    var showBgInLeft: Int
        get() { return (value and 0x02) ushr 1 }
        set(value) { this.value = if (value > 0) this.value or 0x02 else this.value and 0x02.inv() }
    var showSpritesInLeft: Int
        get() { return (value and 0x04) ushr 2 }
        set(value) { this.value = if (value > 0) this.value or 0x04 else this.value and 0x04.inv() }
    var backgroundRenderingOn: Int
        get() { return (value and 0x08) ushr 3 }
        set(value) { this.value = if (value > 0) this.value or 0x08 else this.value and 0x08.inv() }
    var spriteRenderingOn: Int
        get() { return (value and 0x10) ushr 4 }
        set(value) { this.value = if (value > 0) this.value or 0x10 else this.value and 0x10.inv() }
    var emphasizeRed: Int
        get() { return (value and 0x20) ushr 5 }
        set(value) { this.value = if (value > 0) this.value or 0x20 else this.value and 0x20.inv() }
    var emphasizeGreen: Int
        get() { return (value and 0x40) ushr 6 }
        set(value) { this.value = if (value > 0) this.value or 0x40 else this.value and 0x40.inv() }
    var emphasizeBlue: Int
        get() { return (value and 0x80) ushr 7 }
        set(value) { this.value = if (value > 0) this.value or 0x80 else this.value and 0x80.inv() }
}

class Status : Register {

    override val address: Int = 0x2002
    override var value: Int = 0

    var spriteOverflow: Int
        get() { return (value and 0x20) ushr 5 }
        set(value) { this.value = if (value > 0) this.value or 0x20 else this.value and 0x20.inv() }
    var spriteZeroHit: Int
        get() { return (value and 0x40) ushr 6 }
        set(value) { this.value = if (value > 0) this.value or 0x40 else this.value and 0x40.inv() }
    var vblank: Int
        get() { return (value and 0x80) ushr 7 }
        set(value) { this.value = if (value > 0) this.value or 0x80 else this.value and 0x80.inv() }
}

class OAMAddress : Register {

    override val address: Int = 0x2003
    override var value: Int = 0
}

class OAMData : Register {

    override val address: Int = 0x2004
    override var value: Int = 0
}

class Scroll : Register {
    override val address: Int = 0x2005
    override var value: Int = 0
}

class Address : Register {
    override val address: Int = 0x2006
    override var value: Int = 0
}

class Data : Register {
    override val address: Int = 0x2007
    override var value: Int = 0
}