package io.github.torrenkt.tslmwebui.ktor

import io.github.torrenkt.tslmwebui.database.Token
import io.github.torrenkt.tslmwebui.database.TokenTable
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.ForbiddenResponse
import io.ktor.server.auth.bearer
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

data class TokenPrincipal(
    val id: Int,
    val email: String,
)

fun AuthenticationConfig.tokenAuthentication() {
    bearer {
        authenticate { credentials ->
            return@authenticate suspendTransaction {
                Token.find {
                    TokenTable.token eq credentials.token
                }.singleOrNull()?.takeIf {
                    it.enabled
                }?.let {
                    TokenPrincipal(it.id.value, it.email)
                }
            }
        }
    }
}

suspend fun ApplicationCall.requireSuperUser(): Boolean {
    if (principal<TokenPrincipal>()?.id == 0) {
        return false
    }
    respond(HttpStatusCode.Forbidden)
    return true
}

fun ApplicationCall.isSuperUser(): Boolean {
    return principal<TokenPrincipal>()?.id == 0
}
