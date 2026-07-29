package io.github.torrenkt.tslmwebui.routers

import io.github.torrenkt.tslmwebui.database.Token
import io.github.torrenkt.tslmwebui.database.TokenTable
import io.github.torrenkt.tslmwebui.ktor.requireSuperUser
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

fun Route.listToken() {
    get<ListToken> { req ->
        if (call.requireSuperUser()) {
            return@get
        }
        if (req.pageSize <= 0 || req.pageIndex < 0) {
            call.respond(SimpleResp(code = 400, message = "invalid pagination"))
            return@get
        }

        val pageSize = req.pageSize.toLong()
        val offset = req.pageIndex * pageSize
        val query = req.query.trim()
        val data = suspendTransaction {
            val nonSuperUser = TokenTable.id neq EntityID(0, TokenTable)
            val tokens = if (query.isEmpty()) {
                Token.find { nonSuperUser }
            } else {
                Token.find { nonSuperUser and (TokenTable.email like "%$query%") }
            }
            val totalCount = if (query.isEmpty()) {
                Token.count(nonSuperUser)
            } else {
                Token.count(nonSuperUser and (TokenTable.email like "%$query%"))
            }
            val totalPage = (totalCount + pageSize - 1) / pageSize
            if (totalCount > 0 && req.pageIndex >= totalPage) {
                return@suspendTransaction null
            }
            val list = tokens
                .orderBy(TokenTable.id to SortOrder.ASC)
                .limit(req.pageSize)
                .offset(offset)
                .map { token ->
                    ListTokenResp.Data.Item(
                        id = token.id.value,
                        email = token.email,
                        token = token.token.take(2) + "****" + token.token.takeLast(2),
                        createdAt = token.createdAt.toString(),
                        updatedAt = token.updatedAt.toString(),
                        enabled = token.enabled,
                    )
                }
            ListTokenResp.Data(
                totalPage = totalPage,
                list = list,
            )
        }
        if (data == null) {
            call.respond(SimpleResp(code = 400, message = "invalid pagination"))
            return@get
        }
        call.respond(ListTokenResp(data = data))
    }
}
