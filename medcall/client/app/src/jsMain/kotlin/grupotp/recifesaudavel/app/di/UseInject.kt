package grupotp.recifesaudavel.app.di

import org.koin.mp.KoinPlatform
import react.useMemo

/**
 * The [T] registered in [appModule], looked up once while the component stays mounted. Screens
 * take their use cases from here instead of receiving each one through props.
 */
internal inline fun <reified T : Any> useInject(): T = useMemo { KoinPlatform.getKoin().get<T>() }
