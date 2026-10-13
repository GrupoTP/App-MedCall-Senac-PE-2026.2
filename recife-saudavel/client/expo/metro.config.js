const { getDefaultConfig } = require("expo/metro-config");
const path = require("path");

const projectRoot = __dirname;
const uiBundle = path.resolve(
  projectRoot,
  "../app/build/kotlin-webpack/js/productionExecutable",
);

const config = getDefaultConfig(projectRoot);

config.watchFolders = [...(config.watchFolders ?? []), uiBundle];
config.resolver.nodeModulesPaths = [path.resolve(projectRoot, "node_modules")];

// Gradle installs its own node_modules for Kotlin/JS; Metro must only see the client ones.
const kotlinNodeModules = /[\\/]build[\\/]js[\\/]node_modules[\\/].*/;
const existingBlockList = config.resolver.blockList;
config.resolver.blockList = Array.isArray(existingBlockList)
  ? [...existingBlockList, kotlinNodeModules]
  : [existingBlockList, kotlinNodeModules].filter(Boolean);

// On the web the signed-in account lives in the Kotlin service worker, not in SQLite.
// An empty module keeps the SQLite WebAssembly build out of the PWA.
const webOnlyEmpty = new Set(["expo-sqlite"]);
const defaultResolveRequest = config.resolver.resolveRequest;
config.resolver.resolveRequest = (context, moduleName, platform) => {
  if (platform === "web" && webOnlyEmpty.has(moduleName)) {
    return { type: "empty" };
  }
  return (defaultResolveRequest ?? context.resolveRequest)(context, moduleName, platform);
};

module.exports = config;
