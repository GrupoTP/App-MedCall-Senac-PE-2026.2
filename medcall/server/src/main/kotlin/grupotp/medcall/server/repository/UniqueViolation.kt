package grupotp.recifesaudavel.server.repository

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import java.sql.SQLException

/** SQLSTATE of a unique or primary key violation on H2 (and PostgreSQL). */
private const val UNIQUE_VIOLATION_STATE = "23505"

/** MySQL reports every integrity error as SQLSTATE 23000; error 1062 is the duplicate key one. */
private const val MYSQL_DUPLICATE_ENTRY = 1062

/**
 * True when the statement failed because a unique index already holds the value. Other
 * integrity errors, such as a missing foreign key, share the same exception class and
 * must not be taken for it.
 */
internal fun ExposedSQLException.isUniqueViolation(): Boolean =
    generateSequence<Throwable>(this) { it.cause }
        .filterIsInstance<SQLException>()
        .any { it.sqlState == UNIQUE_VIOLATION_STATE || it.errorCode == MYSQL_DUPLICATE_ENTRY }
