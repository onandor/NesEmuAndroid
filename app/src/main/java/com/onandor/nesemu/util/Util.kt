package com.onandor.nesemu.util

import java.security.MessageDigest

fun ByteArray.sha1Hash(fromIndex: Int, toIndex: Int = size): String {
    val hashBytes = MessageDigest.getInstance("SHA-1").run {
        digest(copyOfRange(fromIndex, toIndex))
    }
    return hashBytes.joinToString("") { byte -> "%02x".format(byte) }
}