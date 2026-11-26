package grupotp.medcall.common.kpi

import kotlinx.serialization.Serializable

/**
 * A ratio kept as its two counts, so the screen can show "12 de 40" next to the percentage and
 * a reviewer can recompute it.
 *
 * @property masked true when the MedCall panel hides a group under [MIN_GROUP] cases (LGPD); the
 * counts are then zero and [value] is null.
 */
@Serializable
data class Rate(
    val numerator: Int,
    val denominator: Int,
    val masked: Boolean = false,
) {
    /** 0.0 to 1.0, or null when there is nothing to divide by. */
    val value: Double? get() = if (masked || denominator == 0) null else numerator.toDouble() / denominator

    companion object {
        val EMPTY = Rate(0, 0)
        val MASKED = Rate(0, 0, masked = true)

        /** Smallest group the MedCall panel shows (docs/kpis.md, "Mascaramento"). */
        const val MIN_GROUP = 5
    }
}

