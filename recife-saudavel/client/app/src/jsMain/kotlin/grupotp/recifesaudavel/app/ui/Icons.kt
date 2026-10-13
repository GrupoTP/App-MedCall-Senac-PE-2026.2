package grupotp.recifesaudavel.app.ui

import ojaynico.kotlin.react.native.svg.Circle
import ojaynico.kotlin.react.native.svg.Defs
import ojaynico.kotlin.react.native.svg.Line
import ojaynico.kotlin.react.native.svg.LinearGradient
import ojaynico.kotlin.react.native.svg.Path
import ojaynico.kotlin.react.native.svg.Rect
import ojaynico.kotlin.react.native.svg.Stop
import ojaynico.kotlin.react.native.svg.Svg
import react.ChildrenBuilder

internal fun ChildrenBuilder.strokeIcon(
    size: Int,
    color: String,
    strokeWidth: Double = 2.0,
    draw: ChildrenBuilder.() -> Unit,
) {
    Svg {
        width = size
        height = size
        viewBox = "0 0 24 24"
        fill = "none"
        stroke = color
        this.strokeWidth = strokeWidth
        strokeLinecap = "round"
        strokeLinejoin = "round"
        importantForAccessibility = "no-hide-descendants"
        draw()
    }
}

private fun ChildrenBuilder.line(x1: Double, y1: Double, x2: Double, y2: Double) {
    Line {
        this.x1 = x1
        this.y1 = y1
        this.x2 = x2
        this.y2 = y2
    }
}

private fun ChildrenBuilder.path(d: String) {
    Path {
        this.d = d
    }
}

private fun ChildrenBuilder.circle(cx: Double, cy: Double, r: Double) {
    Circle {
        this.cx = cx
        this.cy = cy
        this.r = r
    }
}

/**
 * Top-to-bottom gradient behind its siblings. React Native has no CSS gradients.
 */
internal fun ChildrenBuilder.verticalGradient(id: String, from: String, to: String) {
    Svg {
        style = Styles.fill
        width = "100%"
        height = "100%"
        preserveAspectRatio = "none"
        importantForAccessibility = "no-hide-descendants"
        Defs {
            LinearGradient {
                this.id = id
                x1 = 0
                y1 = 0
                x2 = 0
                y2 = 1
                Stop {
                    offset = 0
                    stopColor = from
                }
                Stop {
                    offset = 1
                    stopColor = to
                }
            }
        }
        Rect {
            x = 0
            y = 0
            width = "100%"
            height = "100%"
            fill = "url(#$id)"
        }
    }
}

internal fun ChildrenBuilder.heartIcon(color: String) {
    strokeIcon(size = 46, color = color, strokeWidth = 1.7) {
        path("M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z")
    }
}

internal fun ChildrenBuilder.triageIcon(color: String) {
    strokeIcon(size = 22, color = color) {
        path("M3 3h18v18H3z")
        path("M9 12l2 2 4-4")
    }
}

internal fun ChildrenBuilder.calendarIcon(color: String) {
    strokeIcon(size = 22, color = color) {
        path("M3 4h18v18H3z")
        line(16.0, 2.0, 16.0, 6.0)
        line(8.0, 2.0, 8.0, 6.0)
        line(3.0, 10.0, 21.0, 10.0)
    }
}

internal fun ChildrenBuilder.journeyIcon(color: String) {
    strokeIcon(size = 22, color = color) {
        circle(12.0, 12.0, 10.0)
        path("M9 12l2 2 4-4")
    }
}

internal fun ChildrenBuilder.backIcon(color: String) {
    strokeIcon(size = 18, color = color, strokeWidth = 2.5) {
        path("M15 18l-6-6 6-6")
    }
}

internal fun ChildrenBuilder.eyeIcon(color: String) {
    strokeIcon(size = 17, color = color) {
        path("M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z")
        circle(12.0, 12.0, 3.0)
    }
}

internal fun ChildrenBuilder.eyeOffIcon(color: String) {
    strokeIcon(size = 17, color = color) {
        path("M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24")
        line(1.0, 1.0, 23.0, 23.0)
    }
}

internal fun ChildrenBuilder.clipboardIcon(color: String) {
    strokeIcon(size = 20, color = color) {
        path("M9 11l3 3L22 4")
        path("M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11")
    }
}

internal fun ChildrenBuilder.listIcon(color: String) {
    strokeIcon(size = 20, color = color) {
        line(8.0, 6.0, 21.0, 6.0)
        line(8.0, 12.0, 21.0, 12.0)
        line(8.0, 18.0, 21.0, 18.0)
        line(3.0, 6.0, 3.01, 6.0)
        line(3.0, 12.0, 3.01, 12.0)
        line(3.0, 18.0, 3.01, 18.0)
    }
}

internal fun ChildrenBuilder.chevronIcon(color: String) {
    strokeIcon(size = 16, color = color, strokeWidth = 2.5) {
        path("M9 18l6-6-6-6")
    }
}

internal fun ChildrenBuilder.checkIcon(color: String) {
    strokeIcon(size = 12, color = color, strokeWidth = 3.5) {
        path("M20 6L9 17l-5-5")
    }
}

internal fun ChildrenBuilder.settingsIcon(color: String) {
    strokeIcon(size = 18, color = color) {
        circle(12.0, 12.0, 3.0)
        path("M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z")
    }
}

internal fun ChildrenBuilder.alertIcon(color: String) {
    strokeIcon(size = 16, color = color, strokeWidth = 2.5) {
        circle(12.0, 12.0, 10.0)
        line(12.0, 8.0, 12.0, 12.0)
        line(12.0, 16.0, 12.01, 16.0)
    }
}
