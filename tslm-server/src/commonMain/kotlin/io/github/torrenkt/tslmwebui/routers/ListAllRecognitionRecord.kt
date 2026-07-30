package io.github.torrenkt.tslmwebui.routers

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

@Resource("/api/list_all_recognition_record")
data class ListAllRecognitionRecord(
    val pageSize: Int,
    val pageIndex: Long,
    val email: String = "",
    val title: String = "",
)

@Serializable
data class ListAllRecognitionRecordResp(
    override val code: Int = 200,
    override val message: String = "success",
    override val data: Data? = null,
) : BaseRespObj<ListAllRecognitionRecordResp.Data> {
    @Serializable
    data class Data(
        val totalPage: Long,
        val list: List<Item>,
    ) {
        @Serializable
        data class Item(
            val tokenId: Int,
            val email: String,
            val data: ListRecognitionRecordResp.Data.Item,
        )
    }
}
