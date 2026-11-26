package grupotp.medcall.admin

/** The page and the rest of the path, such as a clinic id in `/medcall/clinicas/3`. */
data class Route(val page: Page, val argument: String? = null)

