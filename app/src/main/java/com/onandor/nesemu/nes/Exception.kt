package com.onandor.nesemu.nes

abstract class NesException(val tag: String, message: String) : Exception(message)

class InvalidOperationException(tag: String, message: String) : NesException(tag, message)
class RomParseException(tag: String, message: String) : NesException(tag, message)