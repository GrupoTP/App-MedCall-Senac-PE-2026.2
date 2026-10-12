package br.senac.recifesaudavel.ui

import ojaynico.kotlin.react.native.component.Pressable
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.TextInput
import ojaynico.kotlin.react.native.component.View
import react.FC
import react.Props

internal external interface LoginScreenProps : Props {
    var email: String
    var password: String
    var error: String?
    var loading: Boolean
    var serverUrl: String
    var onEmailChange: (String) -> Unit
    var onPasswordChange: (String) -> Unit
    var onSubmit: () -> Unit
}

internal val LoginScreen = FC<LoginScreenProps> { props ->
    val message = props.error
    View {
        style = Styles.screen
        View {
            style = Styles.card
            Text {
                style = Styles.title
                +"Recife Saudável"
            }
            Text {
                style = Styles.subtitle
                +"Entre para continuar"
            }
            Text {
                style = Styles.label
                +"E-mail"
            }
            TextInput {
                style = Styles.input
                value = props.email
                placeholder = "maria@recife.saude"
                onChangeText = props.onEmailChange
                secureTextEntry = false
                autoCapitalize = "none"
                autoCorrect = false
                editable = !props.loading
            }
            Text {
                style = Styles.label
                +"Senha"
            }
            TextInput {
                style = Styles.input
                value = props.password
                placeholder = "senha"
                onChangeText = props.onPasswordChange
                secureTextEntry = true
                autoCapitalize = "none"
                autoCorrect = false
                editable = !props.loading
            }
            Text {
                style = Styles.hint
                +"Usuário de teste: maria@recife.saude / saudavel"
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
                onPress = props.onSubmit
                Text {
                    style = Styles.buttonText
                    +if (props.loading) "Entrando..." else "Entrar"
                }
            }
            Text {
                style = Styles.server
                +"Servidor: ${props.serverUrl}"
            }
        }
    }
}
