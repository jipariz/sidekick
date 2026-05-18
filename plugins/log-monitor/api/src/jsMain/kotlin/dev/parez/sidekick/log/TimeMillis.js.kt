package dev.parez.sidekick.log

actual fun currentTimeMillis(): Long = js("Date.now()").toString().toLong()
