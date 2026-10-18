package grupotp.recifesaudavel.worker.shell

/** HTML of the page exported by Expo. */
class ExportedPage(private val html: String) {
    /**
     * Build files the page loads. The worker installs after the page fetched them, so they are
     * stored on install instead of on first use.
     */
    fun bundlePaths(): List<String> =
        BUNDLE_REFERENCE.findAll(html).map { it.groupValues[1] }.distinct().toList()

    private companion object {
        val BUNDLE_REFERENCE = Regex("""(?:src|href)="(/_expo/static/[^"]+)"""")
    }
}
