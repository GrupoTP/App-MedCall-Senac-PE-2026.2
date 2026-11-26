// Runtime settings of the portals, read before admin.js. One bundle serves every environment:
// the web image rewrites this file at start from ADMIN_API_BASE_URL (docker/admin-config.sh),
// or a deployment can mount its own copy over it.
window.MEDCALL_ADMIN_CONFIG = {
    // Address of the MedCall API, such as "https://api.example.com". Empty: the portals' own origin,
    // where the Ingress (or the dev server's proxy) sends /api to the server.
    apiBaseUrl: "",
};
