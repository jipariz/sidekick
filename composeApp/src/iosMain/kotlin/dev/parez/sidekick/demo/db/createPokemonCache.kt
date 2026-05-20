package dev.parez.sidekick.demo.db

actual fun createPokemonCache(): PokemonCache = InMemoryPokemonCache()
