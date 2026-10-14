package grupotp.recifesaudavel.app.ui

import ojaynico.kotlin.react.native.component.ScrollView
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import react.FC
import react.Key
import react.Props

internal external interface PrivacyPolicyScreenProps : Props {
    var onBack: () -> Unit
}

private val policyItems = listOf(
    "Controlador" to "Projeto Recife Saudável — Polyana Fontes e Thayná Batista da Silva, Faculdade Senac PE, Recife.",
    "Quais dados coletamos" to "Nome, nome social, data de nascimento, e-mail, telefones, informações de saúde da pré-triagem (dados sensíveis conforme LGPD).",
    "Finalidade" to "Viabilizar o agendamento de consultas e organizar o atendimento médico. Não usamos para diagnóstico automático nem para fins diferentes sem novo consentimento.",
    "Compartilhamento" to "Somente com a clínica parceira responsável pelo atendimento, limitado ao necessário.",
    "Retenção" to "Pelo tempo necessário ao atendimento e conforme exigências legais aplicáveis.",
    "Proteção técnica" to "HTTPS, autenticação, RBAC, auditoria de operações e princípio da necessidade de saber.",
    "Seus direitos (LGPD)" to "Acesso, correção, portabilidade, eliminação (quando juridicamente possível), revogação de consentimento e informação sobre compartilhamentos. Solicite em: recifesaudavel@senac.br",
    "RIPD" to "O projeto considera a elaboração de Relatório de Impacto à Proteção de Dados (RIPD) para o tratamento de dados sensíveis previsto.",
    "Dados fictícios no MVP" to "O ambiente acadêmico opera exclusivamente com dados fictícios. Nenhum dado real de saúde é processado neste protótipo.",
)

/**
 * The prototype privacy policy. Static text, so it opens offline too.
 */
internal val PrivacyPolicyScreen = FC<PrivacyPolicyScreenProps> { props ->
    View {
        style = Styles.screen
        ScreenHeader {
            title = "Privacidade e Dados"
            onBack = props.onBack
        }
        ScrollView {
            style = Styles.scroll
            contentContainerStyle = Styles.scrollContent
            View {
                style = Styles.policyHero
                Text {
                    style = Styles.policyHeroTitle
                    accessibilityRole = "header"
                    +"Política de Privacidade e Proteção de Dados"
                }
                Text {
                    style = Styles.policyHeroText
                    +"Dados de saúde são classificados como dados pessoais sensíveis pela LGPD (Lei nº 13.709/2018) e recebem proteção especial."
                }
            }
            for ((title, body) in policyItems) {
                View {
                    key = Key(title)
                    style = Styles.cardCompact
                    Text {
                        style = Styles.policyTitle
                        accessibilityRole = "header"
                        +title
                    }
                    Text {
                        style = Styles.policyText
                        +body
                    }
                }
            }
        }
        AppFooter()
    }
}
