package grupotp.recifesaudavel.app

import grupotp.recifesaudavel.app.di.appModule
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform
import react.create
import react.dom.client.createRoot
import web.dom.ElementId
import web.dom.document
import web.html.HTMLElement

fun main() {
    startKoin {
        modules(appModule)
    }
    val koin = KoinPlatform.getKoin()
    val container = document.getElementById(ElementId("root")) as HTMLElement
    createRoot(container).render(
        App.create {
            signIn = koin.get()
            registerAccount = koin.get()
            requestPasswordRecovery = koin.get()
            listUpcomingAppointments = koin.get()
        },
    )
}
