package io.github.torrenkt.tslmwebui.routers

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

@Resource("/api/list_recognition_record")
data class ListRecognitionRecord(
    val pageSize: Int,
    val pageIndex: Long,
)

@Serializable
data class ListRecognitionRecordResp(
    override val code: Int = 200,
    override val message: String = "success",
    override val data: Data? = null,
) : BaseRespObj<ListRecognitionRecordResp.Data> {
    @Serializable
    data class Data(
        val totalPage: Long,
        val list: List<Item>,
    ) {
        @Serializable
        data class Item(
            val title: String,
            val calledAt: String,
            val result: List<TslmDisplayEntity>,
        )
    }
}
