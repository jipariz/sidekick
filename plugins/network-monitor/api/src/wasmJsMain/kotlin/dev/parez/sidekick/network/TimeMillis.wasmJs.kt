package dev.parez.sidekick.network

private fun dateNow(): Double = js("Date.now()")

actual fun currentTimeMillis(): Long = dateNow().toLong()
