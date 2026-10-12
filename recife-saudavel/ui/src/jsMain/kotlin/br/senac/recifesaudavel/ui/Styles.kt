package br.senac.recifesaudavel.ui

import ojaynico.kotlin.react.TextStyle
import ojaynico.kotlin.react.ViewStyle
import ojaynico.kotlin.react.native.component.StyleSheet

internal object Styles {
    val screen = ViewStyle(
        flex = 1,
        justifyContent = "center",
        padding = 24,
        backgroundColor = "#f4f7f5",
    )
    val card = ViewStyle(
        backgroundColor = "#ffffff",
        borderRadius = 16,
        padding = 24,
    )
    val title = TextStyle(
        fontSize = 28,
        fontWeight = "700",
        color = "#14352b",
        marginBottom = 4,
    )
    val subtitle = TextStyle(
        fontSize = 16,
        color = "#3d5c52",
        marginBottom = 20,
    )
    val label = TextStyle(
        fontSize = 14,
        color = "#14352b",
        marginBottom = 6,
    )
    val input = StyleSheet.compose(
        ViewStyle(
            borderWidth = 1,
            borderColor = "#d5e0db",
            borderRadius = 8,
            paddingHorizontal = 12,
            paddingVertical = 10,
            marginBottom = 14,
            backgroundColor = "#ffffff",
        ),
        TextStyle(
            fontSize = 16,
            color = "#14352b",
        ),
    )
    val hint = TextStyle(
        fontSize = 13,
        color = "#3d5c52",
        marginBottom = 16,
    )
    val error = TextStyle(
        fontSize = 14,
        color = "#9b2c2c",
        marginBottom = 12,
    )
    val button = ViewStyle(
        backgroundColor = "#0f6b4c",
        borderRadius = 8,
        paddingVertical = 12,
        alignItems = "center",
    )
    val buttonDisabled = ViewStyle(
        backgroundColor = "#8fb5a6",
        borderRadius = 8,
        paddingVertical = 12,
        alignItems = "center",
    )
    val buttonText = TextStyle(
        color = "#ffffff",
        fontSize = 16,
        fontWeight = "700",
    )
    val server = TextStyle(
        fontSize = 12,
        color = "#6b857c",
        marginTop = 16,
    )
}
