// Metro, the bundler Expo uses on Android and on the web. It packs index.js, which loads the
// compiled Kotlin bundle, together with the npm packages of this folder.
const { getDefaultConfig } = require("expo/metro-config");
const fs = require("fs");
const path = require("path");

const projectRoot = __dirname;
const config = getDefaultConfig(projectRoot);

// The bundle lives outside this folder, in client/app/build. Watching it lets Metro reload the app
// when scripts/start-metro.js rebuilds it.
const uiBundle = path.resolve(
  projectRoot,
  "../app/build/kotlin-webpack/js/productionExecutable",
);
config.watchFolders = [...(config.watchFolders ?? []), uiBundle];

// npm packages come only from client/expo/node_modules, even for the Kotlin bundle's imports.
config.resolver.nodeModulesPaths = [path.resolve(projectRoot, "node_modules")];

// Gradle installs its own node_modules for Kotlin/JS; Metro must only see the client ones.
const kotlinNodeModules = /[\\/]build[\\/]js[\\/]node_modules[\\/].*/;

const existingBlockList = config.resolver.blockList;
config.resolver.blockList = [
  ...(Array.isArray(existingBlockList) ? existingBlockList : [existingBlockList].filter(Boolean)),
  kotlinNodeModules,
];

// Dropbox on Windows turns every file and folder it holds into a cloud file (a reparse point with
// tag IO_REPARSE_TAG_CLOUD_1), even inside folders marked com.dropbox.ignored. Windows hides that
// reparse point when it lists a folder that is itself a cloud file, but the ignored folders
// (node_modules, .expo) stay plain folders, so listing them shows it and Node reports each entry as
// a symlink. Metro's crawler skips symlinks: it never indexes node_modules and fails with "Unable to
// resolve module", and in .expo it fails with "EINVAL: readlink". A cloud file is no link (readlink
// fails with EINVAL), so its directory entry gets the type lstat gives.
if (process.platform === "win32") {
  const { isSymbolicLink, isDirectory, isFile } = fs.Dirent.prototype;
  const cloudFileStats = new WeakMap();
  const cloudFileStat = (entry) => {
    if (!isSymbolicLink.call(entry)) return null;
    if (!cloudFileStats.has(entry)) {
      const file = path.join(entry.parentPath, entry.name);
      let stat = null;
      try {
        fs.readlinkSync(file);
      } catch (error) {
        if (error.code === "EINVAL") stat = fs.lstatSync(file, { throwIfNoEntry: false }) ?? null;
      }
      cloudFileStats.set(entry, stat);
    }
    return cloudFileStats.get(entry);
  };
  fs.Dirent.prototype.isSymbolicLink = function () {
    return cloudFileStat(this) ? false : isSymbolicLink.call(this);
  };
  fs.Dirent.prototype.isDirectory = function () {
    return cloudFileStat(this)?.isDirectory() ?? isDirectory.call(this);
  };
  fs.Dirent.prototype.isFile = function () {
    return cloudFileStat(this)?.isFile() ?? isFile.call(this);
  };
}

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
