// Loaded with `node --require` before the Kotlin test bundle.
//
// The app bundle imports React Native and Expo modules at load time. Metro resolves them from
// client/expo, but they don't exist in the Gradle Node environment (and React Native doesn't load
// in plain Node anyway). The unit tests only exercise use cases, so every native module becomes a
// stub that lets the bundle load. If a test ends up calling one of them, the call is a no-op.
const Module = require("module");

const NATIVE_MODULES = [/^react-native(-.+)?(\/.*)?$/, /^expo(-.+)?(\/.*)?$/, /^@expo(-google-fonts)?\/.+$/];

function isNativeModule(request) {
    return NATIVE_MODULES.some((pattern) => pattern.test(request));
}

function createStub(name) {
    const target = function () {};
    return new Proxy(target, {
        get(_, property) {
            if (property === "__esModule") return true;
            if (property === "then" || typeof property === "symbol") return undefined;
            if (property === "toString") return () => `[stub ${name}]`;
            return createStub(`${name}.${String(property)}`);
        },
        apply: () => createStub(`${name}()`),
        construct: () => createStub(`new ${name}`),
    });
}

const stubs = new Map();
const originalLoad = Module._load;

Module._load = function (request, parent, isMain) {
    if (!isNativeModule(request)) return originalLoad.call(this, request, parent, isMain);
    if (!stubs.has(request)) stubs.set(request, createStub(request));
    return stubs.get(request);
};
