package io.github.torrenkt.tslmwebui.routers

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

@Serializable
enum class Platform {
    MiKanAni,
    MTeam,
}

@Resource("/api/torrent")
data class TorrentListReq(
    val pageSize: Int,
    val pageIndex: Long,
    val sortBy: SortBy = SortBy.PubTime,
    val desc: Boolean = true,
) {
    enum class SortBy {
        Name,
        PubTime,
        ;
    }
}

@Serializable
data class TorrentListResp(
    override val code: Int = 200,
    override val message: String = "success",
    override val data: Data? = null,
): BaseRespObj<TorrentListResp.Data> {
    @Serializable
    data class Data(
        val totalPage: Long,
        val list: List<Item>,
    ) {
        @Serializable
        data class Item(
            val id: String,
            val platform: Platform,
            val name: String,
        )
    }
}
