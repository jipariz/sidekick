package dev.parez.sidekick.network

private fun dateNow(): Double = js("Date.now()")

public actual fun currentTimeMillis(): Long = dateNow().toLong()
