@file:JsModule("react-native-svg")

package grupotp.medcall.app.ui.icon

import react.FC
import react.PropsWithChildren

/** Named `Mask` export. The shared wrapper does not declare it. */
external interface MaskProps : PropsWithChildren {
    var id: String
    var maskUnits: String?
    var x: Number?
    var y: Number?
    var width: dynamic
    var height: dynamic
}

external val Mask: FC<MaskProps>

/** `G` with the `mask` attribute the shared wrapper leaves off [ojaynico.kotlin.react.native.svg.G]. */
external interface MaskedGroupProps : PropsWithChildren {
    var fill: String?
    var stroke: String?
    var strokeWidth: Number?
    var strokeLinecap: String?
    var strokeLinejoin: String?
    var mask: String?
}

@JsName("G")
external val MaskedGroup: FC<MaskedGroupProps>
