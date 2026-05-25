package dev.parez.sidekick.persistence

import kotlinx.browser.localStorage
import org.w3c.dom.get
import org.w3c.dom.set

private object JsMenuOrderStore : MenuOrderStore {
    override suspend fun read(): List<String> = decodeMenuOrder(localStorage[MENU_ORDER_KEY])

    override suspend fun write(ids: List<String>) {
        localStorage[MENU_ORDER_KEY] = encodeMenuOrder(ids)
    }
}

internal actual fun createMenuOrderStore(): MenuOrderStore = JsMenuOrderStore
