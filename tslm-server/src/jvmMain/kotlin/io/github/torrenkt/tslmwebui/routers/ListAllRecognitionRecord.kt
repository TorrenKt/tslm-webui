package io.github.torrenkt.tslmwebui.routers

import io.github.torrenkt.tslmwebui.database.RecognitionCache
import io.github.torrenkt.tslmwebui.database.RecognitionCacheTable
import io.github.torrenkt.tslmwebui.database.RecognitionRecord
import io.github.torrenkt.tslmwebui.database.RecognitionRecordTable
import io.github.torrenkt.tslmwebui.database.Token
import io.github.torrenkt.tslmwebui.database.TokenTable
import io.github.torrenkt.tslmwebui.ktor.requireSuperUser
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

fun Route.listAllRecognitionRecord() {
    get<ListAllRecognitionRecord> { req ->
        if (call.requireSuperUser()) {
            return@get
        }
        if (req.pageSize <= 0 || req.pageIndex < 0) {
            call.respond(SimpleResp(code = 400, message = "invalid pagination"))
            return@get
        }

        val pageSize = req.pageSize.toLong()
        val offset = req.pageIndex * pageSize
        val email = req.email.trim()
        val title = req.title.trim()
        val data = suspendTransaction {
            val conditions = buildList<Op<Boolean>> {
                if (email.isNotEmpty()) {
                    val tokenIds = Token.find { TokenTable.email like "%$email%" }.map { it.id.value }
                    add(RecognitionRecordTable.tokenId inList tokenIds)
                }
                if (title.isNotEmpty()) {
                    val cacheIds = RecognitionCache.find { RecognitionCacheTable.title like "%$title%" }
                        .map { it.id.value }
                    add(RecognitionRecordTable.cacheId inList cacheIds)
                }
            }
            val condition = conditions.reduceOrNull { result, next -> result and next }
            val totalCount = if (condition == null) {
                RecognitionRecord.count()
            } else {
                RecognitionRecord.count(condition)
            }
            val totalPage = (totalCount + pageSize - 1) / pageSize
            if (totalCount > 0 && req.pageIndex >= totalPage) {
                return@suspendTransaction null
            }
            val records = if (condition == null) {
                RecognitionRecord.all()
            } else {
                RecognitionRecord.find { condition }
            }
            val list = records
                .orderBy(RecognitionRecordTable.calledAt to SortOrder.DESC)
                .limit(req.pageSize)
                .offset(offset)
                .mapNotNull { record ->
                    val token = Token.findById(record.tokenId)
                    val cache = RecognitionCache.findById(record.cacheId)
                    if (token == null || cache == null) {
                        null
                    } else {
                        ListAllRecognitionRecordResp.Data.Item(
                            tokenId = token.id.value,
                            email = token.email,
                            data = ListRecognitionRecordResp.Data.Item(
                                title = cache.title,
                                calledAt = record.calledAt.toString(),
                                result = cache.result,
                            ),
                        )
                    }
                }
            ListAllRecognitionRecordResp.Data(totalPage = totalPage, list = list)
        }
        if (data == null) {
            call.respond(SimpleResp(code = 400, message = "invalid pagination"))
            return@get
        }
        call.respond(ListAllRecognitionRecordResp(data = data))
    }
}
