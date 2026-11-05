// Expo configuration, read when Metro starts, when the PWA is exported and when `expo prebuild`
// generates android/. It replaces app.json, which cannot hold comments.
//
// The app reads this object at runtime through expo-constants (Constants.expoConfig), so `extra`
// is how the build passes values to the Kotlin code.
module.exports = () => ({
  name: "Recife Saudável",
  // Project id for Expo's tools. Changing it does not rename the app.
  slug: "recife-saudavel",
  version: "1.0.0",
  orientation: "portrait",
  icon: "./assets/icon.png",
  // The interface has a single, light theme. On the web the accessibility bar offers a dark mode.
  userInterfaceStyle: "light",

  android: {
    // Application id on the device. Changing it installs a separate app instead of an update.
    package: "grupotp.recifesaudavel",
    // Launcher icon on Android 8+: the logo over the brand blue.
    adaptiveIcon: {
      backgroundColor: "#2d7dd2",
      foregroundImage: "./assets/icon.png",
    },
  },

  // public/ is copied as is next to the bundle: index.html, manifest.json, sw.js and the icons.
  // The browser reads public/manifest.json, so a change to the values below goes there too.
  web: {
    bundler: "metro",
    // A single index.html for every address; the Kotlin app does the navigation.
    output: "single",
    favicon: "./assets/favicon.png",
    // Fills %WEB_TITLE% in public/index.html.
    name: "Recife Saudável",
    shortName: "Recife Saudável",
    themeColor: "#2d7dd2",
    backgroundColor: "#eef4fa",
    display: "standalone",
  },

  plugins: [
    // Local database of the signed-in account on Android. The PWA keeps it in the service worker.
    "expo-sqlite",
    // Nunito and Poppins, loaded by the Kotlin app.
    "expo-font",
    [
      "expo-build-properties",
      {
        android: {
          // Allows plain HTTP. The release build calls http://localhost:8080, which `adb reverse`
          // sends to the server on the computer. Development only: in production the API uses
          // HTTPS and this goes away.
          usesCleartextTraffic: true,
        },
      },
    ],
    // Fixes for the generated android/ folder; each one is explained in the plugin.
    "./plugins/withKotlinAndroidBuild",
  ],

  extra: {
    // API_BASE_URL: address of the server, such as https://api.example.com. Unset in development,
    // where the app calls port 8080 on the computer that serves the bundle. Empty, not null: Expo
    // serializes the config and turns null into {}, which the Kotlin side cannot read as a string.
    apiBaseUrl: process.env.API_BASE_URL || "",
  },
});
