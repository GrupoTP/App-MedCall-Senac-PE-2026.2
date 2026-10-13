// Config plugin that keeps the Android build working after `expo prebuild` regenerates android/.
//
// 1. The React Native bundle task only tracks .js files inside this client folder (and skips any
//    build/ folder), so it never sees the Kotlin UI bundle and reuses a stale JS bundle in the APK.
//    We add the Kotlin bundle as an extra input of every bundle task.
// 2. On Windows the native code of react-native-svg and react-native-safe-area-context needs
//    CMake 4.1.2+ (ninja 1.12 accepts paths over 260 characters). We point `cmake.dir` in
//    android/local.properties at the newest CMake found in the Android SDK.
const fs = require("fs");
const os = require("os");
const path = require("path");
const { withAppBuildGradle, withDangerousMod, CodeGenerator } = require("expo/config-plugins");

const MIN_CMAKE = [4, 1, 2];

const KOTLIN_BUNDLE_GRADLE = `// The Kotlin UI bundle lives outside the React Native root; track it so release bundles rebuild.
def kotlinUiBundleDir = new File(projectRoot, "../recife-saudavel-app/build/kotlin-webpack/js/productionExecutable")
tasks.withType(com.facebook.react.tasks.BundleHermesCTask).configureEach {
    inputs.files(fileTree(kotlinUiBundleDir) { include "*.js" })
        .withPropertyName("kotlinUiBundle")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}`;

const withKotlinBundleInput = (config) =>
  withAppBuildGradle(config, (config) => {
    if (config.modResults.language !== "groovy") {
      throw new Error("withKotlinAndroidBuild: expected android/app/build.gradle in Groovy.");
    }
    config.modResults.contents = CodeGenerator.mergeContents({
      src: config.modResults.contents,
      newSrc: KOTLIN_BUNDLE_GRADLE,
      tag: "recife-saudavel-kotlin-bundle",
      anchor: /^android \{/,
      offset: 0,
      comment: "//",
    }).contents;
    return config;
  });

function androidSdkDir() {
  const fromEnv = process.env.ANDROID_HOME || process.env.ANDROID_SDK_ROOT;
  if (fromEnv) return fromEnv;
  const localAppData = process.env.LOCALAPPDATA || path.join(os.homedir(), "AppData", "Local");
  return path.join(localAppData, "Android", "Sdk");
}

function parseVersion(name) {
  const match = /^(\d+)\.(\d+)\.(\d+)/.exec(name);
  return match ? match.slice(1).map(Number) : null;
}

function compareVersions(a, b) {
  for (let i = 0; i < 3; i++) {
    if (a[i] !== b[i]) return a[i] - b[i];
  }
  return 0;
}

function newestCmake(sdkDir) {
  const cmakeRoot = path.join(sdkDir, "cmake");
  if (!fs.existsSync(cmakeRoot)) return null;
  return fs
    .readdirSync(cmakeRoot)
    .map((name) => ({ name, version: parseVersion(name) }))
    .filter(({ name, version }) => version && compareVersions(version, MIN_CMAKE) >= 0 && fs.existsSync(path.join(cmakeRoot, name, "bin")))
    .sort((a, b) => compareVersions(b.version, a.version))
    .map(({ name }) => path.join(cmakeRoot, name))[0] ?? null;
}

// local.properties is a Java properties file: use forward slashes and escape the drive colon.
const toPropertiesPath = (p) => p.replace(/\\/g, "/").replace(/:/g, "\\:");

const withWindowsCmake = (config) =>
  withDangerousMod(config, [
    "android",
    (config) => {
      if (process.platform !== "win32") return config;

      const file = path.join(config.modRequest.platformProjectRoot, "local.properties");
      const contents = fs.existsSync(file) ? fs.readFileSync(file, "utf8") : "";
      if (/^\s*cmake\.dir\s*=/m.test(contents)) return config;

      const sdkDir = androidSdkDir();
      const cmakeDir = newestCmake(sdkDir);
      if (!cmakeDir) {
        console.warn(
          `withKotlinAndroidBuild: no CMake ${MIN_CMAKE.join(".")}+ in ${path.join(sdkDir, "cmake")}. ` +
            "Install it with the Android SDK Manager, or the native build may fail with long paths.",
        );
        return config;
      }

      const lines = contents.split(/\r?\n/).filter((line) => line.trim() !== "");
      if (!lines.some((line) => /^\s*sdk\.dir\s*=/.test(line))) {
        lines.push(`sdk.dir=${toPropertiesPath(sdkDir)}`);
      }
      lines.push(`cmake.dir=${toPropertiesPath(cmakeDir)}`);
      fs.writeFileSync(file, lines.join("\n") + "\n");
      return config;
    },
  ]);

module.exports = (config) => withWindowsCmake(withKotlinBundleInput(config));
