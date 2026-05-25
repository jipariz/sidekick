package dev.parez.sidekick.persistence

import java.util.prefs.Preferences

private object JvmMenuOrderStore : MenuOrderStore {
    private val node: Preferences = Preferences.userRoot().node("dev/parez/sidekick/menu-order")

    override suspend fun read(): List<String> = decodeMenuOrder(node.get(MENU_ORDER_KEY, null))

    override suspend fun write(ids: List<String>) {
        node.put(MENU_ORDER_KEY, encodeMenuOrder(ids))
        node.flush()
    }
}

internal actual fun createMenuOrderStore(): MenuOrderStore = JvmMenuOrderStore
