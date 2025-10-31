// Copy sql-wasm.wasm next to the bundle so WebWorkerDriver can load it.
// Wrapped in try-catch because this config is shared with wasmJs, which doesn't use sql.js.
try {
    const CopyPlugin = require("copy-webpack-plugin");

    config.plugins.push(
        new CopyPlugin({
            patterns: [
                {
                    from: require.resolve("sql.js/dist/sql-wasm.wasm"),
                    to: "sql-wasm.wasm",
                },
            ],
        })
    );

    config.resolve = config.resolve || {};
    config.resolve.fallback = {
        ...(config.resolve.fallback || {}),
        fs: false,
        path: false,
        crypto: false,
    };
} catch (e) {
    // copy-webpack-plugin / sql.js not available in this build target (e.g. wasmJs)
}
