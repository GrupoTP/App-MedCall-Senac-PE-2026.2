package grupotp.medcall.admin.ui

import web.dom.document
import web.html.HTMLStyleElement

@JsModule("admin-lte/dist/css/adminlte.min.css")
private external val adminLteCss: dynamic

@JsModule("bootstrap-icons/font/bootstrap-icons.min.css")
private external val bootstrapIconsCss: dynamic

/** The app's palette (client/app Palette.kt), so the portals look like the same product. */
object Brand {
    const val BLUE = "#2d7dd2"
    const val BLUE_DARK = "#1b5ea8"
    const val GREEN = "#236c5d"
    const val INK = "#1c2b3a"
    const val MUTED = "#5a7a94"
    const val CANVAS = "#eef4fa"
    const val AMBER = "#d97706"
    const val RED = "#dc2626"
    const val PURPLE = "#7b5ea7"
    const val TEAL = "#0f9488"
    const val GREY = "#94a3b8"

    /** Series colors of the charts, in order. */
    val SERIES = listOf(BLUE, GREEN, AMBER, PURPLE, TEAL, RED, "#0ea5e9", "#a16207", "#be185d", "#4d7c0f", "#6366f1")
}

/**
 * Loads AdminLTE (which brings Bootstrap 5) and the icons, then the MedCall overrides on top: they
 * are added last so they win over the template's own rules.
 */
fun installTheme() {
    // Reading the modules keeps the bundler from dropping the style imports.
    listOf(adminLteCss, bootstrapIconsCss)
    val style = document.createElement("style").unsafeCast<HTMLStyleElement>()
    style.textContent = CSS
    document.head.appendChild(style)
}

private val CSS = """
:root, [data-bs-theme=light] {
  --bs-primary: ${Brand.BLUE};
  --bs-primary-rgb: 45, 125, 210;
  --bs-link-color: ${Brand.BLUE_DARK};
  --bs-link-color-rgb: 27, 94, 168;
  --bs-body-font-family: "Nunito", system-ui, sans-serif;
  --bs-body-color: ${Brand.INK};
  --bs-tertiary-bg: ${Brand.CANVAS};
}
body { font-family: "Nunito", system-ui, sans-serif; }
h1, h2, h3, h4, h5, .brand-text, .info-box-number, .kpi-value { font-family: "Poppins", system-ui, sans-serif; }
.btn-primary { --bs-btn-bg: ${Brand.BLUE}; --bs-btn-border-color: ${Brand.BLUE}; --bs-btn-hover-bg: ${Brand.BLUE_DARK}; --bs-btn-hover-border-color: ${Brand.BLUE_DARK}; --bs-btn-active-bg: ${Brand.BLUE_DARK}; }
.btn-outline-primary { --bs-btn-color: ${Brand.BLUE}; --bs-btn-border-color: ${Brand.BLUE}; --bs-btn-hover-bg: ${Brand.BLUE}; --bs-btn-hover-border-color: ${Brand.BLUE}; }
.text-bg-primary { background-color: ${Brand.BLUE} !important; }
.app-sidebar { background: linear-gradient(180deg, #15324f 0%, #1b4e7e 100%) !important; }
.sidebar-brand { border-bottom: 1px solid rgba(255,255,255,.12); }
.sidebar-brand .brand-image { background: #fff; border-radius: 10px; padding: 3px; opacity: 1 !important; box-shadow: none !important; }
.sidebar-menu .nav-link.active { background-color: rgba(255,255,255,.16) !important; color: #fff !important; }
.sidebar-menu .nav-header { color: rgba(255,255,255,.55); font-size: .72rem; letter-spacing: .06em; }
.app-content-header h3 { font-weight: 600; }
.app-content-header .subtitle { color: ${Brand.MUTED}; font-size: .95rem; }
.kpi-box { display: flex; gap: .9rem; align-items: flex-start; background: #fff; border-radius: .75rem; padding: 1rem 1.1rem; box-shadow: 0 1px 2px rgba(16,24,40,.06), 0 1px 3px rgba(16,24,40,.08); height: 100%; border-left: 4px solid var(--kpi-color, ${Brand.BLUE}); }
.kpi-box .kpi-icon { width: 44px; height: 44px; border-radius: 12px; display: grid; place-items: center; font-size: 1.35rem; color: var(--kpi-color, ${Brand.BLUE}); background: color-mix(in srgb, var(--kpi-color, ${Brand.BLUE}) 12%, white); flex: none; }
.kpi-box .kpi-label { color: ${Brand.MUTED}; font-size: .82rem; font-weight: 600; text-transform: uppercase; letter-spacing: .03em; }
.kpi-box .kpi-value { font-size: 1.65rem; font-weight: 600; line-height: 1.2; color: ${Brand.INK}; }
.kpi-box .kpi-detail { font-size: .82rem; color: ${Brand.MUTED}; }
.kpi-box .kpi-code { font-size: .68rem; color: #94a3b8; font-weight: 700; margin-left: .25rem; }
.kpi-blue { --kpi-color: ${Brand.BLUE}; }
.kpi-green { --kpi-color: ${Brand.GREEN}; }
.kpi-amber { --kpi-color: ${Brand.AMBER}; }
.kpi-red { --kpi-color: ${Brand.RED}; }
.kpi-purple { --kpi-color: ${Brand.PURPLE}; }
.kpi-teal { --kpi-color: ${Brand.TEAL}; }
.kpi-box.alert-state { --kpi-color: ${Brand.RED}; background: #fff7f7; }
.kpi-trend-up { color: #15803d; font-weight: 700; }
.kpi-trend-down { color: ${Brand.RED}; font-weight: 700; }
.card { border: 0; border-radius: .75rem; box-shadow: 0 1px 2px rgba(16,24,40,.06), 0 1px 3px rgba(16,24,40,.08); }
.card-header { background: transparent; border-bottom: 1px solid #e8eef5; }
.card-title { font-weight: 600; font-size: 1.02rem; }
.card-question { color: ${Brand.MUTED}; font-size: .85rem; margin-top: .15rem; }
.chart-box { position: relative; width: 100%; }
.filter-bar { background: #fff; border-radius: .75rem; padding: .85rem 1rem; box-shadow: 0 1px 2px rgba(16,24,40,.06); }
.filter-bar .form-label { font-size: .78rem; font-weight: 700; color: ${Brand.MUTED}; text-transform: uppercase; margin-bottom: .2rem; }
.status-badge { font-weight: 600; font-size: .78rem; padding: .35em .65em; border-radius: 999px; }
.status-pending { background: #fef9c3; color: #a16207; }
.status-confirmed { background: #dcfce7; color: #15803d; }
.status-declined, .status-cancelled { background: #fee2e2; color: #b91c1c; }
.status-attended { background: #e0f2fe; color: #0369a1; }
.status-no_show { background: #f3e8ff; color: #7e22ce; }
.table > :not(caption) > * > * { padding: .65rem .75rem; }
.table thead th { font-size: .78rem; text-transform: uppercase; letter-spacing: .03em; color: ${Brand.MUTED}; font-weight: 700; }
.row-overdue { background: #fff7ed !important; }
.heat-table { border-collapse: separate; border-spacing: 2px; font-size: .74rem; }
.heat-table td { min-width: 26px; height: 26px; text-align: center; border-radius: 4px; color: ${Brand.INK}; }
.heat-table th { font-weight: 600; color: ${Brand.MUTED}; white-space: nowrap; padding-right: .5rem; text-align: right; }
.heat-table thead th { text-align: center; padding: 0 0 .25rem 0; }
.limits { background: #fffbeb; border: 1px solid #fde68a; border-radius: .75rem; padding: .9rem 1.1rem; font-size: .9rem; }
.login-page { min-height: 100vh; display: grid; place-items: center; background: radial-gradient(circle at 20% 20%, #dbeafe 0, ${Brand.CANVAS} 45%, #e6f2ee 100%); padding: 1rem; }
.login-card { width: 100%; max-width: 420px; border-radius: 1rem; }
.login-card .logo { height: 54px; }
.secret-box { font-family: ui-monospace, Menlo, Consolas, monospace; background: #f1f5f9; border: 1px dashed #94a3b8; border-radius: .5rem; padding: .6rem .8rem; word-break: break-all; }
.modal.d-block { background: rgba(15, 23, 42, .45); overflow-y: auto; }
.empty-state { color: ${Brand.MUTED}; text-align: center; padding: 2rem 1rem; }
.empty-state i { font-size: 2rem; display: block; margin-bottom: .5rem; color: #94a3b8; }
.toast-area { position: fixed; right: 1rem; bottom: 1rem; z-index: 1100; display: flex; flex-direction: column; gap: .5rem; max-width: 360px; }
.nav-badge { margin-left: auto; }
@media (max-width: 575.98px) { .kpi-box .kpi-value { font-size: 1.35rem; } .app-content-header h3 { font-size: 1.25rem; } }
"""
