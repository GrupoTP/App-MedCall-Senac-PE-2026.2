package grupotp.recifesaudavel.common.pretriage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Answers of "Quando começaram os sintomas?", in the prototype's order. JSON carries the
 * [SerialName] code, so a [label] can be reworded without touching stored pre-triages.
 */
@Serializable
enum class SymptomsStart(val label: String) {
    @SerialName("today")
    Today("Hoje"),

    @SerialName("two_to_three_days")
    TwoToThreeDays("Há 2 a 3 dias"),

    @SerialName("one_week")
    OneWeek("Há uma semana"),

    @SerialName("two_weeks")
    TwoWeeks("Há 2 semanas"),

    @SerialName("one_month")
    OneMonth("Há 1 mês"),

    @SerialName("over_one_month")
    OverOneMonth("Há mais de 1 mês"),

    @SerialName("over_six_months")
    OverSixMonths("Há mais de 6 meses"),

    @SerialName("chronic")
    Chronic("Crônico (mais de 1 ano)"),
}
