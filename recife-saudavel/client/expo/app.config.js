// Settings that change per environment, added to app.json when Metro starts or the app is built.
// API_BASE_URL: address of the server, such as https://api.example.com. Unset in development,
// where the app calls port 8080 on the computer that serves the bundle.
module.exports = ({ config }) => ({
  ...config,
  extra: {
    ...config.extra,
    apiBaseUrl: process.env.API_BASE_URL || null,
  },
});
