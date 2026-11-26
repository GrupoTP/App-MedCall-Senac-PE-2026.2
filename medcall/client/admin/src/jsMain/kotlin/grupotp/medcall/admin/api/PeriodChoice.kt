package grupotp.medcall.admin.api

import kotlinx.datetime.LocalDate

/** A dashboard period: the last [days] days in the clinic's calendar, or [from]..[to]. */
data class PeriodChoice(val days: Int? = 30, val from: LocalDate? = null, val to: LocalDate? = null) {
    fun query(): Array<Pair<String, Any?>> =
        if (from != null && to != null) arrayOf("from" to from, "to" to to) else arrayOf("days" to (days ?: 30))
}

