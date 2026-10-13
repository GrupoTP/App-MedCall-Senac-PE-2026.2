const { getDefaultConfig } = require("expo/metro-config");
const fs = require("fs");
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

// Dropbox turns synced folders into Windows cloud files (reparse points). Node lists those as
// symlinks, so Metro never indexes them, and a change event inside one (even a read that updates
// the last access time of public/worker.js) crashes it with "TreeFS: Failed to make parent directory
// entry". Nothing Metro bundles lives in them: public/ is served from disk and assets/ is read by
// the Expo config, so Metro can skip them.
const isRealSymlink = (file) => {
  try {
    fs.readlinkSync(file);
    return true;
  } catch {
    return false;
  }
};
const escapeRegExp = (text) => text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
const cloudFolders = fs
  .readdirSync(projectRoot, { withFileTypes: true })
  .filter((entry) => entry.isSymbolicLink() && !isRealSymlink(path.join(projectRoot, entry.name)))
  .map((entry) => new RegExp(`^${escapeRegExp(path.join(projectRoot, entry.name))}([\\\\/].*)?$`));

const existingBlockList = config.resolver.blockList;
config.resolver.blockList = [
  ...(Array.isArray(existingBlockList) ? existingBlockList : [existingBlockList].filter(Boolean)),
  kotlinNodeModules,
  ...cloudFolders,
];

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
