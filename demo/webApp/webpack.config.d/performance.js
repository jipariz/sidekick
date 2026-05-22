// Raise webpack's asset size thresholds. The default 244 KiB entrypoint
// budget targets hand-coded SPAs; a Compose Multiplatform wasm/JS bundle is
// inherently large (skiko alone is ~8 MiB, plus the app wasm and SQLite
// worker bundles), so the default warning is just noise. Keep `hints` on
// `warning` so genuinely surprising regressions still surface.
config.performance = {
    hints: 'warning',
    maxAssetSize: 32 * 1024 * 1024,    // 32 MiB — large enough for Sidekick-demo-webApp.wasm + skiko.wasm
    maxEntrypointSize: 4 * 1024 * 1024, // 4 MiB — accommodates the JS-target bundle
};
