package dev.parez.sidekick.plugin

import android.content.Context

public object ApplicationContextHolder {
    public lateinit var context: Context
        private set

    /** `true` once [initialize] has been called. */
    public val isInitialized: Boolean get() = ::context.isInitialized

    public fun initialize(ctx: Context) {
        context = ctx.applicationContext
    }
}
