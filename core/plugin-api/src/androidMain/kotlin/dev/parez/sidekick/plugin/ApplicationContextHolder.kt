package dev.parez.sidekick.plugin

import android.content.Context

object ApplicationContextHolder {
    lateinit var context: Context
        private set

    /** `true` once [initialize] has been called. */
    val isInitialized: Boolean get() = ::context.isInitialized

    fun initialize(ctx: Context) {
        context = ctx.applicationContext
    }
}
