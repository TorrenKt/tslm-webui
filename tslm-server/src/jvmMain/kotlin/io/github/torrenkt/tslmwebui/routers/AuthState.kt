package io.github.torrenkt.tslmwebui.routers

import io.github.torrenkt.tslmwebui.ktor.TokenPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Route

fun Route.authState() {
    get<AuthState> {
        val tokenPrincipal = call.principal<TokenPrincipal>()
        val isAdmin = tokenPrincipal?.id == 0
        call.respond(
            AuthState.Resp(
                data = AuthState.Resp.Info(
                    isAdmin = isAdmin,
                    email = tokenPrincipal?.email,
                ),
            ),
        )
    }
}
