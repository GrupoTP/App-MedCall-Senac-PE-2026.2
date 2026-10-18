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

// The Kotlin plugin names the library after the output file ("app"), which would nest the exports
// under module.exports.app. client/expo/index.js reads them straight from module.exports.
config.output = config.output || {};
delete config.output.libraryTarget;
config.output.library = { type: "commonjs2" };

// Hermes cannot parse eval-based source maps, and Terser has broken Kotlin/JS output before.
config.devtool = false;
config.optimization = config.optimization || {};
config.optimization.minimize = false;

// ktor-client loads the `ws` package with import(), for WebSockets on Node. Webpack would emit it as
// a separate chunk, and the chunk loader needs document.currentScript: Hermes stops at load with
// "Automatic publicPath is not supported in this browser". Metro also reads app.js only, so
// everything stays in one file.
const webpack = require("webpack");
config.plugins = config.plugins || [];
config.plugins.push(new webpack.optimize.LimitChunkCountPlugin({ maxChunks: 1 }));
