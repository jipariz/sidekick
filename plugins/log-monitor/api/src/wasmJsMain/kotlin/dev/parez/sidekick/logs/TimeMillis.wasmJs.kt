package dev.parez.sidekick.logs

private fun dateNow(): Double = js("Date.now()")

actual fun currentTimeMillis(): Long = dateNow().toLong()
