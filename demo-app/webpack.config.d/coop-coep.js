// Enable SharedArrayBuffer / OPFS for SQLite WASM.
// The dev server must send COOP + COEP headers so the browser
// exposes SharedArrayBuffer and Atomics to the page.
config.devServer = config.devServer || {};
config.devServer.headers = {
    ...(config.devServer.headers || {}),
    "Cross-Origin-Opener-Policy": "same-origin",
    "Cross-Origin-Embedder-Policy": "require-corp",
};
