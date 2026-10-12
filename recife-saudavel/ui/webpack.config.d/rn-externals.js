// Leave React and React Native for Metro to resolve.
// On Android that is the native runtime; on web Expo aliases react-native to react-native-web.
config.externals = {
  react: "commonjs react",
  "react/jsx-runtime": "commonjs react/jsx-runtime",
  "react/jsx-dev-runtime": "commonjs react/jsx-dev-runtime",
  "react-native": "commonjs react-native",
};

config.output = config.output || {};
config.output.libraryTarget = "commonjs2";

// Hermes cannot parse eval-based source maps, and Terser has broken Kotlin/JS output before.
config.devtool = false;
config.optimization = config.optimization || {};
config.optimization.minimize = false;
