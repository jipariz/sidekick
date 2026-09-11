package dev.parez.sidekick.log

public actual fun currentTimeMillis(): Long = js("Date.now()").toString().toLong()
