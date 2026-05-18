package dev.parez.sidekick.log

private fun dateNow(): Double = js("Date.now()")

actual fun currentTimeMillis(): Long = dateNow().toLong()
