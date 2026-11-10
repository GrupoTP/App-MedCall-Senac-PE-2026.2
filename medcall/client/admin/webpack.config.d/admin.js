// Fonts of bootstrap-icons, referenced from its CSS, become files next to admin.js.
config.module.rules.push({
    test: /\.(woff2?|ttf|eot)$/,
    type: "asset/resource",
    generator: { filename: "fonts/[name][ext]" },
});

// Relative URLs: the same build works at /admin/ in production and at the dev server's root.
config.output = config.output || {};
config.output.publicPath = "auto";

// Dev server on 8082 (the PWA's Metro uses 8081); the API stays on 8080.
if (config.devServer) {
    config.devServer.port = 8082;
    config.devServer.historyApiFallback = true;
    config.devServer.open = false;
}
