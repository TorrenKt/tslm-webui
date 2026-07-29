package io.github.torrenkt.tslmwebui.routers

import io.github.torrenkt.tslmwebui.database.Token
import io.github.torrenkt.tslmwebui.ktor.requireSuperUser
import io.github.torrenkt.tslmwebui.token.newToken
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import kotlin.time.Clock

fun Route.refreshToken() {
    post<RefreshToken> {
        if (call.requireSuperUser()) {
            return@post
        }

        val id = call.receive<RefreshToken.Req>().id
        if (id <= 0) {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }
        val token = suspendTransaction {
            Token.findById(id)?.let {
                it.token = newToken()
                it.updatedAt = Clock.System.now()
                it.token
            }
        }
        if (token == null) {
            call.respond(HttpStatusCode.NotFound)
            return@post
        }
        call.respond(RefreshToken.Resp(data = token))
    }
}
