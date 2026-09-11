package dev.parez.sidekick.network

public actual fun currentTimeMillis(): Long = js("Date.now()").toString().toLong()
