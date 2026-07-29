package io.github.torrenkt.tslmwebui.routers

import io.github.torrenkt.tslmwebui.database.Token
import io.github.torrenkt.tslmwebui.core.isValidEmail
import io.github.torrenkt.tslmwebui.ktor.requireSuperUser
import io.github.torrenkt.tslmwebui.token.newToken
import io.ktor.server.request.receive
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

fun Route.createToken() {
    post<CreateToken> {
        if (call.requireSuperUser()) {
            return@post
        }

        val email = call.receive<CreateToken.Req>().email.trim()
        if (!email.isValidEmail()) {
            call.respond(SimpleResp(code = 400, message = "invalid email"))
            return@post
        }

        val token = suspendTransaction {
            Token.new {
                this.email = email
                this.token = newToken()
            }.token
        }
        call.respond(CreateToken.Resp(data = token))
    }
}
