package io.github.torrenkt.tslmwebui.routers

import io.github.torrenkt.tslmwebui.database.RecognitionCache
import io.github.torrenkt.tslmwebui.database.RecognitionRecord
import io.github.torrenkt.tslmwebui.database.RecognitionRecordTable
import io.github.torrenkt.tslmwebui.ktor.TokenPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

fun Route.listRecognitionRecord() {
    get<ListRecognitionRecord> { req ->
        if (req.pageSize <= 0 || req.pageIndex < 0) {
            call.respond(SimpleResp(code = 400, message = "invalid pagination"))
            return@get
        }
        val tokenId = call.principal<TokenPrincipal>()?.id
        if (tokenId == null) {
            call.respond(HttpStatusCode.Forbidden)
            return@get
        }

        val pageSize = req.pageSize.toLong()
        val offset = req.pageIndex * pageSize
        val data = suspendTransaction {
            val totalCount = RecognitionRecord.count(RecognitionRecordTable.tokenId eq tokenId)
            val totalPage = (totalCount + pageSize - 1) / pageSize
            if (totalCount > 0 && req.pageIndex >= totalPage) {
                return@suspendTransaction null
            }
            val list = RecognitionRecord.find { RecognitionRecordTable.tokenId eq tokenId }
                .orderBy(RecognitionRecordTable.calledAt to SortOrder.DESC)
                .limit(req.pageSize)
                .offset(offset)
                .mapNotNull { record ->
                    RecognitionCache.findById(record.cacheId)?.let { cache ->
                        ListRecognitionRecordResp.Data.Item(
                            title = cache.title,
                            calledAt = record.calledAt.toString(),
                            result = cache.result,
                        )
                    }
                }
            ListRecognitionRecordResp.Data(totalPage = totalPage, list = list)
        }
        if (data == null) {
            call.respond(SimpleResp(code = 400, message = "invalid pagination"))
            return@get
        }
        call.respond(ListRecognitionRecordResp(data = data))
    }
}
