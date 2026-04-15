package dev.parez.sidekick.logs

import kotlin.random.Random

internal fun randomUuid(): String {
    val bytes = Random.nextBytes(16)
    bytes[6] = (bytes[6].toInt() and 0x0F or 0x40).toByte() // version 4
    bytes[8] = (bytes[8].toInt() and 0x3F or 0x80).toByte() // variant 1
    return buildString(36) {
        for (i in bytes.indices) {
            val b = bytes[i].toInt() and 0xFF
            append(HEX[b shr 4])
            append(HEX[b and 0x0F])
            if (i == 3 || i == 5 || i == 7 || i == 9) append('-')
        }
    }
}

private val HEX = "0123456789abcdef".toCharArray()
