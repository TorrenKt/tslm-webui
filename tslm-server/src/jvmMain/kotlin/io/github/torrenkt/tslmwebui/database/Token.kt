package io.github.torrenkt.tslmwebui.database

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Instant

object TokenTable : IntIdTable("token") {
    val email = varchar("email", 320)
    val createdAt = timestamp("created_at")
        .defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at")
        .defaultExpression(CurrentTimestamp)
    val token = varchar("token", 128).uniqueIndex()
    val enabled = bool("enabled").default(true)
}

class Token(id: EntityID<Int>) : IntEntity(id) {
    var email by TokenTable.email
    var createdAt: Instant by TokenTable.createdAt
    var updatedAt: Instant by TokenTable.updatedAt
    var token by TokenTable.token
    var enabled by TokenTable.enabled

    companion object : IntEntityClass<Token>(TokenTable)
}
