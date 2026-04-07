package dev.parez.sidekick.network

import android.content.Context

object ApplicationContextHolder {
    lateinit var context: Context
        private set

    fun initialize(ctx: Context) {
        context = ctx.applicationContext
    }
}
