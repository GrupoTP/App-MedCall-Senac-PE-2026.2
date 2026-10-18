package grupotp.recifesaudavel.app.ui.component.form

import grupotp.recifesaudavel.app.ui.theme.Styles
import grupotp.recifesaudavel.common.account.Phone
import ojaynico.kotlin.react.native.component.Text
import react.ChildrenBuilder

internal fun ChildrenBuilder.phoneField(
    caption: String,
    value: Phone,
    whatsapp: Boolean,
    required: Boolean = false,
    onValue: (Phone) -> Unit,
    onWhatsapp: (Boolean) -> Unit,
) {
    formField(caption = caption, required = required) {
        TextField {
            label = caption
            this.value = value.digits
            placeholder = "(81) 99999-9999"
            kind = FieldKind.Phone
            this.onValue = { onValue(Phone(it)) }
        }
        CheckBox {
            checked = whatsapp
            label = "$caption: WhatsApp disponível"
            style = Styles.checkLine
            onToggle = onWhatsapp
            Text {
                style = Styles.checkCopy
                Text {
                    style = Styles.whatsapp
                    +"WhatsApp"
                }
                +" disponível"
            }
        }
    }
}
