package io.github.torrenkt.tslmwebui.routers

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

@Resource("/api/auth_state")
class AuthState {
    @Serializable
    data class Resp(
        override val code: Int = 200,
        override val message: String = "success",
        override val data: Info,
    ): BaseRespObj<Resp.Info> {
        @Serializable
        data class Info(
            val isAdmin: Boolean,
            val email: String?,
        )
    }
}
