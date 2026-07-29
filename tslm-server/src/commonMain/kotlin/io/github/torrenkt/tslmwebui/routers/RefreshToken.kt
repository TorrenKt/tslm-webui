package io.github.torrenkt.tslmwebui.routers

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

@Resource("/api/refresh_token")
class RefreshToken {
    @Serializable
    data class Req(
        val id: Int,
    )

    @Serializable
    data class Resp(
        override val code: Int = 200,
        override val message: String = "success",
        override val data: String? = null,
    ) : BaseRespObj<String>
}
