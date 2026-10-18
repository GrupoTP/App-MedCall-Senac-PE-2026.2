package grupotp.recifesaudavel.common.security

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * A password or token. Code reads it through [value]; [toString] hides it, so printing a request,
 * an account or a log line that holds one never writes the secret.
 *
 * JSON carries the plain string: only accidental text output is masked.
 */
@Serializable
@JvmInline
value class SecretString(val value: String) {
    fun isBlank(): Boolean = value.isBlank()

    override fun toString(): String = "<secret>"

    companion object {
        val EMPTY = SecretString("")
    }
}
