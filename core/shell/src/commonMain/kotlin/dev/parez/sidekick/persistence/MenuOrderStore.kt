package dev.parez.sidekick.persistence

internal interface MenuOrderStore {
    suspend fun read(): List<String>

    suspend fun write(ids: List<String>)
}

internal expect fun createMenuOrderStore(): MenuOrderStore

internal const val MENU_ORDER_STORE_NAME = "sidekick_menu_order"
internal const val MENU_ORDER_KEY = "sidekick.menu_order"

internal fun encodeMenuOrder(ids: List<String>): String = ids.joinToString("\n")

internal fun decodeMenuOrder(raw: String?): List<String> =
    raw?.split('\n')?.filter { it.isNotEmpty() } ?: emptyList()
