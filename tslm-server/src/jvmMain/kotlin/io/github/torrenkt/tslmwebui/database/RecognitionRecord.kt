package io.github.torrenkt.tslmwebui.database

import kotlin.time.Instant
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp

object RecognitionRecordTable : LongIdTable("recognition_record") {
    val tokenId = integer("token_id")
    val cacheId = long("cache_id")
    val calledAt = timestamp("called_at")
        .defaultExpression(CurrentTimestamp)
}

class RecognitionRecord(id: EntityID<Long>) : LongEntity(id) {
    var tokenId by RecognitionRecordTable.tokenId
    var cacheId: Long by RecognitionRecordTable.cacheId
    var calledAt: Instant by RecognitionRecordTable.calledAt

    companion object : LongEntityClass<RecognitionRecord>(RecognitionRecordTable)
}
