package grupotp.medcall.admin

import grupotp.medcall.common.staff.StaffRole
import web.location.location

object Router {
    fun current(role: StaffRole): Route {
        val hash = location.hash.removePrefix("#")
        val page = Page.entries.filter { hash == it.path || hash.startsWith(it.path + "/") }
            .maxByOrNull { it.path.length }
        if (page == null || role !in page.roles) {
            return Route(Page.home(role))
        }
        val argument = hash.removePrefix(page.path).removePrefix("/").ifEmpty { null }
        return Route(page, argument)
    }

    fun go(page: Page, argument: String? = null) {
        location.hash = "#" + page.path + (argument?.let { "/$it" } ?: "")
    }

    fun href(page: Page, argument: String? = null): String = "#" + page.path + (argument?.let { "/$it" } ?: "")
}

