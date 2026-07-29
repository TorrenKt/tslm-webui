package io.github.torrenkt.tslmwebui.routers

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

@Resource("/api/create_token")
class CreateToken {
    @Serializable
    data class Req(
        val email: String,
    )

    @Serializable
    data class Resp(
        override val code: Int = 200,
        override val message: String = "success",
        override val data: String,
    ) : BaseRespObj<String>
}
