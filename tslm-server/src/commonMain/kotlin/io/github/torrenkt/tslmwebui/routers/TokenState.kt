package io.github.torrenkt.tslmwebui.routers

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

@Resource("/api/token_state")
class TokenState {
    @Serializable
    data class Req(
        val ids: List<Int>,
        val enabled: Boolean,
    )

    @Serializable
    data class Resp(
        override val code: Int = 200,
        override val message: String = "success",
        override val data: Int? = null,
    ) : BaseRespObj<Int>
}
