package dev.parez.sidekick.network

actual fun currentTimeMillis(): Long = js("Date.now()").toString().toLong()
