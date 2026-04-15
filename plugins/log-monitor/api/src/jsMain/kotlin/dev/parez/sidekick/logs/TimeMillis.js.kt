package dev.parez.sidekick.logs

actual fun currentTimeMillis(): Long = js("Date.now()").toString().toLong()
