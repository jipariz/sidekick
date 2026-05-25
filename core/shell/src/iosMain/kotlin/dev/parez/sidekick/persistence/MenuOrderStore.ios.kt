package dev.parez.sidekick.persistence

import platform.Foundation.NSUserDefaults

private object IosMenuOrderStore : MenuOrderStore {
    private val defaults: NSUserDefaults
        get() = NSUserDefaults.standardUserDefaults

    override suspend fun read(): List<String> =
        decodeMenuOrder(defaults.stringForKey(MENU_ORDER_KEY))

    override suspend fun write(ids: List<String>) {
        defaults.setObject(encodeMenuOrder(ids), MENU_ORDER_KEY)
    }
}

internal actual fun createMenuOrderStore(): MenuOrderStore = IosMenuOrderStore
