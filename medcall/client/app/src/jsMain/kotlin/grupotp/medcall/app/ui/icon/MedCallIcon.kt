package grupotp.medcall.app.ui.icon

import ojaynico.kotlin.react.native.svg.Circle
import ojaynico.kotlin.react.native.svg.Defs
import ojaynico.kotlin.react.native.svg.Path
import ojaynico.kotlin.react.native.svg.Rect
import ojaynico.kotlin.react.native.svg.Svg
import react.ChildrenBuilder

private const val HEART =
    "M430.0,674.0 C356.0,614.0 254.0,530.0 254.0,456.0 C254.0,401.0 296.0,363.0 342.0,363.0 " +
        "C385.0,363.0 413.0,393.0 430.0,424.0 C447.0,393.0 475.0,363.0 518.0,363.0 " +
        "C564.0,363.0 606.0,401.0 606.0,456.0 C606.0,530.0 504.0,614.0 430.0,674.0 Z"

private const val HEART_BLUE =
    "M430.0,674.0 C356.0,614.0 254.0,530.0 254.0,456.0 C254.0,401.0 296.0,363.0 342.0,363.0 " +
        "C385.0,363.0 413.0,393.0 430.0,424.0 C447.0,393.0 475.0,363.0 518.0,363.0 " +
        "C537.3,363.0 555.9,369.7 571.0,381.6"

private const val GREEN = "#236C5D"
private const val BLUE = "#185890"

/**
 * Official mark from SPM-72 (`medcall-icon.svg`): white tile, heart and the checked clipboard.
 * The title next to it carries the name, so this drawing stays decorative.
 */
internal fun ChildrenBuilder.medCallIcon(size: Int) {
    Svg {
        width = size
        height = size
        viewBox = "143.1 142.6 737.8 737.8"
        decorative()
        Rect {
            x = 143.1
            y = 142.6
            width = 737.8
            height = 737.8
            rx = 162.3
            fill = "#FFFFFF"
        }
        Defs {
            Mask {
                id = "medcall-icon-gap"
                maskUnits = "userSpaceOnUse"
                x = 0
                y = 0
                width = 1024
                height = 1024
                Rect {
                    width = 1024
                    height = 1024
                    fill = "#ffffff"
                }
                Path {
                    d = HEART
                    fill = "#000000"
                    stroke = "#000000"
                    strokeWidth = 62
                    strokeLinejoin = "round"
                }
            }
        }
        MaskedGroup {
            fill = "none"
            strokeWidth = 30
            strokeLinecap = "round"
            strokeLinejoin = "round"
            MaskedGroup {
                stroke = GREEN
                mask = "url(#medcall-icon-gap)"
                Path { d = "M588,377 H726 A44,44 0 0 1 770,421 V632 A44,44 0 0 1 726,676 H520" }
                Path {
                    d = "M641,465 H770"
                    strokeLinecap = "butt"
                }
                Path { d = "M684,347 V404" }
                Path {
                    d = "M623,585 L647,609 L700,556"
                    strokeWidth = 28
                }
            }
            Path {
                d = HEART
                stroke = GREEN
            }
            Path {
                d = HEART_BLUE
                stroke = BLUE
                strokeLinecap = "butt"
            }
            Circle {
                cx = 430
                cy = 674
                r = 15
                fill = BLUE
            }
        }
    }
}
