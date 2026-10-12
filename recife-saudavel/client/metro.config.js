const { getDefaultConfig } = require("expo/metro-config");
const path = require("path");

const projectRoot = __dirname;
const uiBundle = path.resolve(
  projectRoot,
  "../ui/build/kotlin-webpack/js/productionExecutable",
);

const config = getDefaultConfig(projectRoot);

config.watchFolders = [...(config.watchFolders ?? []), uiBundle];
config.resolver.nodeModulesPaths = [path.resolve(projectRoot, "node_modules")];

const kotlinNodeModules = /[\\/]ui[\\/]build[\\/]js[\\/]node_modules[\\/].*/;
const existingBlockList = config.resolver.blockList;
config.resolver.blockList = Array.isArray(existingBlockList)
  ? [...existingBlockList, kotlinNodeModules]
  : [existingBlockList, kotlinNodeModules].filter(Boolean);

module.exports = config;
