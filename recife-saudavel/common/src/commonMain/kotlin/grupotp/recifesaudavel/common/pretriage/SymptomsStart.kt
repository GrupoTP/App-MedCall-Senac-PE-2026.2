package grupotp.recifesaudavel.common.pretriage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Answers of "Quando começaram os sintomas?", in the prototype's order. JSON carries the
 * [SerialName] code; the text shown for each answer belongs to the client.
 */
@Serializable
enum class SymptomsStart {
    @SerialName("today")
    Today,

    @SerialName("two_to_three_days")
    TwoToThreeDays,

    @SerialName("one_week")
    OneWeek,

    @SerialName("two_weeks")
    TwoWeeks,

    @SerialName("one_month")
    OneMonth,

    @SerialName("over_one_month")
    OverOneMonth,

    @SerialName("over_six_months")
    OverSixMonths,

    @SerialName("chronic")
    Chronic,
}
