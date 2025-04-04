package com.onandor.nesemu.domain.emulation.nes

abstract class NesException(message: String) : Exception(message)

class InvalidOperationException(message: String) : NesException(message)
class RomParseException(message: String) : NesException(message)