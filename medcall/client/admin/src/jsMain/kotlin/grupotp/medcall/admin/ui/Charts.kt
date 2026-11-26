package grupotp.medcall.admin.ui

/** Builders of Chart.js configs with the MedCall look. */
object Charts {
    init {
        val defaults = chartClass.defaults
        defaults.font.family = "Nunito, system-ui, sans-serif"
        defaults.color = Brand.MUTED
        defaults.plugins.legend.labels.usePointStyle = true
        defaults.maintainAspectRatio = false
    }

    fun ensureDefaults() = Unit

    fun dataset(label: String, data: List<Number?>, color: String, type: String? = null, extra: dynamic = null): dynamic {
        val set: dynamic = js("({})")
        set.label = label
        set.data = data.map { it?.toDouble() }.toTypedArray()
        set.backgroundColor = color
        set.borderColor = color
        set.borderWidth = 2
        set.borderRadius = 4
        set.pointRadius = 2
        set.tension = 0.3
        if (type != null) set.type = type
        if (extra != null) js("Object.assign(set, extra)")
        return set
    }

    fun config(
        type: String,
        labels: List<String>,
        datasets: List<dynamic>,
        stacked: Boolean = false,
        percentAxis: Boolean = false,
        horizontal: Boolean = false,
        legend: Boolean = true,
        yTitle: String? = null,
    ): Any {
        val config: dynamic = js("({ data: {}, options: { plugins: { legend: {}, tooltip: {} }, scales: { x: { grid: {} }, y: { grid: {}, title: {} } } } })")
        config.type = type
        config.data.labels = labels.toTypedArray()
        config.data.datasets = datasets.toTypedArray()
        config.options.responsive = true
        config.options.maintainAspectRatio = false
        config.options.interaction = js("({ mode: 'index', intersect: false })")
        config.options.plugins.legend.display = legend
        config.options.plugins.legend.position = "bottom"
        if (horizontal) config.options.indexAxis = "y"
        val valueAxis = if (horizontal) config.options.scales.x else config.options.scales.y
        config.options.scales.x.stacked = stacked
        config.options.scales.y.stacked = stacked
        config.options.scales.x.grid.display = horizontal
        valueAxis.beginAtZero = true
        if (percentAxis) {
            valueAxis.max = 100
            valueAxis.ticks = js("({ callback: function(v) { return v + '%'; } })")
        }
        if (yTitle != null) {
            config.options.scales.y.title.display = true
            config.options.scales.y.title.text = yTitle
        }
        return config
    }

    fun doughnut(labels: List<String>, values: List<Int>, colors: List<String>): dynamic {
        val config: dynamic = js("({ type: 'doughnut', data: { datasets: [{}] }, options: { plugins: { legend: { position: 'bottom' } }, cutout: '62%' } })")
        config.data.labels = labels.toTypedArray()
        config.data.datasets[0].data = values.toTypedArray()
        config.data.datasets[0].backgroundColor = colors.toTypedArray()
        config.data.datasets[0].borderWidth = 0
        config.options.maintainAspectRatio = false
        return config
    }
}

