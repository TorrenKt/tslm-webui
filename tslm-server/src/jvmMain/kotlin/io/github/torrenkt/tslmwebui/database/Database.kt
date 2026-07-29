package io.github.torrenkt.tslmwebui.database

import io.github.torrenkt.tslmwebui.core.GlobalJson
import io.github.torrenkt.tslmwebui.core.logger
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.json.jsonb
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils

private val log by logger("Database")

fun connectPostgreSql(
    databaseHost: String,
    databasePort: Int,
    databaseName: String,
    databaseSchema: String,
    databaseUsername: String,
    databasePassword: String,
) {
    log.info { "connecting database..." }
    Database.connect(
        url = "jdbc:postgresql://${databaseHost}:${databasePort}/${databaseName}",
        user = databaseUsername,
        password = databasePassword,
        setupConnection = { it.schema = databaseSchema },
    )
}

fun setupDatabase(vararg tables: Table) {
    transaction {
        val statements = MigrationUtils.statementsRequiredForDatabaseMigration(*tables)
        if (statements.isNotEmpty()) {
            log.info { "executing database schema migrations..." }
            execInBatch(statements)
        } else {
            log.info { "database schema is up to date" }
        }
    }
}

inline fun <reified T : Any> Table.jsonb(name: String) = jsonb<T>(name, GlobalJson)
