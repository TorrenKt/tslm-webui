package io.github.torrenkt.tslmwebui.routers

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

@Resource("/api/list_token")
data class ListToken(
    val pageSize: Int,
    val pageIndex: Long,
    val query: String = "",
)

@Serializable
data class ListTokenResp(
    override val code: Int = 200,
    override val message: String = "success",
    override val data: Data? = null,
) : BaseRespObj<ListTokenResp.Data> {
    @Serializable
    data class Data(
        val totalPage: Long,
        val list: List<Item>,
    ) {
        @Serializable
        data class Item(
            val id: Int,
            val email: String,
            val token: String,
            val createdAt: String,
            val updatedAt: String,
            val enabled: Boolean,
        )
    }
}
