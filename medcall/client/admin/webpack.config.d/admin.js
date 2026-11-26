// Fonts of bootstrap-icons, referenced from its CSS, become files next to admin.js.
config.module.rules.push({
    test: /\.(woff2?|ttf|eot)$/,
    type: "asset/resource",
    generator: { filename: "fonts/[name][ext]" },
});

// Relative URLs: the same build works at /admin/ in production and at the dev server's root.
config.output = config.output || {};
config.output.publicPath = "auto";

// Dev server only. The portals call their own origin (config.js leaves apiBaseUrl empty) and the
// proxy sends the API routes to the server, so no address is compiled into the bundle.
//   ADMIN_DEV_PORT     port of this dev server (8081 is taken by the patient PWA's Metro)
//   ADMIN_DEV_API_URL  where :server:run listens
if (config.devServer) {
    config.devServer.port = Number(process.env.ADMIN_DEV_PORT || 8082);
    config.devServer.historyApiFallback = true;
    config.devServer.open = false;
    config.devServer.proxy = [
        {
            context: ["/api", "/healthz", "/readyz"],
            target: process.env.ADMIN_DEV_API_URL || "http://localhost:8080",
            changeOrigin: true,
        },
    ];
}
