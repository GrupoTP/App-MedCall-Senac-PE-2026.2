package grupotp.recifesaudavel.app.ui

import react.ChildrenBuilder
import react.dom.svg.ReactSVG.circle
import react.dom.svg.ReactSVG.line
import react.dom.svg.ReactSVG.path
import react.dom.svg.ReactSVG.svg
import react.dom.svg.StrokeLinecap
import react.dom.svg.StrokeLinejoin

internal fun ChildrenBuilder.strokeIcon(
    size: Int,
    strokeWidth: Double = 2.0,
    draw: ChildrenBuilder.() -> Unit,
) {
    svg {
        width = size.toDouble()
        height = size.toDouble()
        viewBox = "0 0 24 24"
        fill = "none"
        stroke = "currentColor"
        this.strokeWidth = strokeWidth
        strokeLinecap = StrokeLinecap.round
        strokeLinejoin = StrokeLinejoin.round
        ariaHidden = true
        draw()
    }
}

internal fun ChildrenBuilder.heartIcon() {
    strokeIcon(size = 46, strokeWidth = 1.7) {
        path {
            d = "M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
        }
    }
}

internal fun ChildrenBuilder.triageIcon() {
    strokeIcon(size = 22) {
        path {
            d = "M3 3h18v18H3z"
        }
        path {
            d = "M9 12l2 2 4-4"
        }
    }
}

internal fun ChildrenBuilder.calendarIcon() {
    strokeIcon(size = 22) {
        path {
            d = "M3 4h18v18H3z"
        }
        line {
            x1 = 16.0
            y1 = 2.0
            x2 = 16.0
            y2 = 6.0
        }
        line {
            x1 = 8.0
            y1 = 2.0
            x2 = 8.0
            y2 = 6.0
        }
        line {
            x1 = 3.0
            y1 = 10.0
            x2 = 21.0
            y2 = 10.0
        }
    }
}

internal fun ChildrenBuilder.journeyIcon() {
    strokeIcon(size = 22) {
        circle {
            cx = 12.0
            cy = 12.0
            r = 10.0
        }
        path {
            d = "M9 12l2 2 4-4"
        }
    }
}

internal fun ChildrenBuilder.backIcon() {
    strokeIcon(size = 18, strokeWidth = 2.5) {
        path {
            d = "M15 18l-6-6 6-6"
        }
    }
}

internal fun ChildrenBuilder.eyeIcon() {
    strokeIcon(size = 17) {
        path {
            d = "M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"
        }
        circle {
            cx = 12.0
            cy = 12.0
            r = 3.0
        }
    }
}

internal fun ChildrenBuilder.eyeOffIcon() {
    strokeIcon(size = 17) {
        path {
            d = "M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"
        }
        line {
            x1 = 1.0
            y1 = 1.0
            x2 = 23.0
            y2 = 23.0
        }
    }
}

internal fun ChildrenBuilder.alertIcon() {
    strokeIcon(size = 16, strokeWidth = 2.5) {
        circle {
            cx = 12.0
            cy = 12.0
            r = 10.0
        }
        line {
            x1 = 12.0
            y1 = 8.0
            x2 = 12.0
            y2 = 12.0
        }
        line {
            x1 = 12.0
            y1 = 16.0
            x2 = 12.01
            y2 = 16.0
        }
    }
}
