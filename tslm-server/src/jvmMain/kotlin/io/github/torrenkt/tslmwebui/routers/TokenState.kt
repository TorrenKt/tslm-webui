package io.github.torrenkt.tslmwebui.routers

import io.github.torrenkt.tslmwebui.database.TokenTable
import io.github.torrenkt.tslmwebui.ktor.requireSuperUser
import io.ktor.server.request.receive
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Clock

fun Route.tokenState() {
    post<TokenState> {
        if (call.requireSuperUser()) {
            return@post
        }

        val req = call.receive<TokenState.Req>()
        val ids = req.ids.filter { it > 0 }.distinct()
        if (ids.isEmpty()) {
            call.respond(SimpleResp(code = 400, message = "invalid token ids"))
            return@post
        }

        val updated = suspendTransaction {
            TokenTable.update({ TokenTable.id inList ids.map { EntityID(it, TokenTable) } }) {
                it[enabled] = req.enabled
                it[updatedAt] = Clock.System.now()
            }
        }
        call.respond(TokenState.Resp(data = updated))
    }
}
