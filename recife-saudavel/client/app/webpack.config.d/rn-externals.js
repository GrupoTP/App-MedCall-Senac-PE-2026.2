// Leave React, React Native and Expo modules for Metro to resolve.
// On Android that is the native runtime; on the web Expo aliases react-native to react-native-web.
const metroModules = /^(react|react\/.+|react-native|react-native-.+|expo-.+|@expo-google-fonts\/.+)$/;

config.externals = [
  ({ request }, callback) => {
    if (metroModules.test(request)) {
      return callback(null, `commonjs ${request}`);
    }
    callback();
  },
];

config.output = config.output || {};
config.output.libraryTarget = "commonjs2";

// Hermes cannot parse eval-based source maps, and Terser has broken Kotlin/JS output before.
config.devtool = false;
config.optimization = config.optimization || {};
config.optimization.minimize = false;
