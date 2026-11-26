package grupotp.medcall.admin.ui

@JsModule("chart.js/auto")
private external val chartModule: dynamic


/** The Chart class: the CommonJS build exports it as the module itself, the ES one as `default`. */
internal val chartClass: dynamic get() = chartModule.default ?: chartModule

