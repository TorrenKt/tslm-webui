package io.github.torrenkt.tslmwebui.routers

import io.github.torrenkt.tslmwebui.database.RecognitionRecordTable
import io.github.torrenkt.tslmwebui.database.TokenTable
import io.github.torrenkt.tslmwebui.ktor.requireSuperUser
import io.ktor.server.request.receive
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

fun Route.deleteToken() {
    post<DeleteToken> {
        if (call.requireSuperUser()) {
            return@post
        }

        val ids = call.receive<DeleteToken.Req>().ids.filter { it > 0 }.distinct()
        if (ids.isEmpty()) {
            call.respond(SimpleResp(code = 400, message = "invalid token ids"))
            return@post
        }

        val deleted = suspendTransaction {
            RecognitionRecordTable.deleteWhere { RecognitionRecordTable.tokenId inList ids }
            TokenTable.deleteWhere { TokenTable.id inList ids.map { EntityID(it, TokenTable) } }
        }
        call.respond(DeleteToken.Resp(data = deleted))
    }
}
