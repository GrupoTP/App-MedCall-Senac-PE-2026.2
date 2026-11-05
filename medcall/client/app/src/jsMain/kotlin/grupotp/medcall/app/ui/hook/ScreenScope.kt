package grupotp.recifesaudavel.app.ui.hook

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import react.useEffectOnce
import react.useRef

/**
 * Coroutine scope tied to the component. It is cancelled when the component unmounts.
 */
internal fun useScreenScope(): CoroutineScope {
    val scopeHolder = useRef<CoroutineScope>(null)
    val scope = scopeHolder.current ?: CoroutineScope(SupervisorJob()).also { scopeHolder.current = it }
    useEffectOnce {
        try {
            awaitCancellation()
        } finally {
            scope.cancel()
        }
    }
    return scope
}
