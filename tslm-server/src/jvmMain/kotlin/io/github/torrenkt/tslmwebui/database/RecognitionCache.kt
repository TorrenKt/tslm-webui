package io.github.torrenkt.tslmwebui.database

import io.github.torrenkt.tslmwebui.routers.TslmDisplayEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

object RecognitionCacheTable : LongIdTable("recognition_cache") {
    val title = varchar("title", 1000)
    val modelHash = varchar("model_hash", 64)
    val result = jsonb<List<TslmDisplayEntity>>("result")

    init {
        uniqueIndex("recognition_cache_title_model_hash_index", title, modelHash)
    }
}

class RecognitionCache(id: EntityID<Long>) : LongEntity(id) {
    var title by RecognitionCacheTable.title
    var modelHash by RecognitionCacheTable.modelHash
    var result by RecognitionCacheTable.result

    companion object : LongEntityClass<RecognitionCache>(RecognitionCacheTable)
}
