package io.github.torrenkt.tslmwebui.routers

import io.ktor.resources.*
import kotlinx.serialization.Serializable

@Resource("/api/predict")
class PredictTorrent {
    @Serializable
    data class Req(
        val input: String,
    )

    @Serializable
    data class Resp(
        override val code: Int = 200,
        override val message: String = "success",
        override val data: List<TslmDisplayEntity>? = null,
    ): BaseRespList<TslmDisplayEntity>
}
