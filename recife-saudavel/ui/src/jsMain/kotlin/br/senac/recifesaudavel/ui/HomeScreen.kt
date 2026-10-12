package br.senac.recifesaudavel.ui

import ojaynico.kotlin.react.native.component.Pressable
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import react.FC
import react.Props

internal external interface HomeScreenProps : Props {
    var name: String
    var email: String
    var error: String?
    var loading: Boolean
    var onLogout: () -> Unit
}

internal val HomeScreen = FC<HomeScreenProps> { props ->
    val message = props.error
    View {
        style = Styles.screen
        View {
            style = Styles.card
            Text {
                style = Styles.title
                +"Olá, ${props.name}"
            }
            Text {
                style = Styles.subtitle
                +props.email
            }
            if (message != null) {
                Text {
                    style = Styles.error
                    +message
                }
            }
            Pressable {
                style = if (props.loading) Styles.buttonDisabled else Styles.button
                disabled = props.loading
                onPress = props.onLogout
                Text {
                    style = Styles.buttonText
                    +if (props.loading) "Saindo..." else "Sair"
                }
            }
        }
    }
}
