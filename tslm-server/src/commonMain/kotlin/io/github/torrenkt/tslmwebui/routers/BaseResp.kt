package io.github.torrenkt.tslmwebui.routers

import kotlinx.serialization.Serializable

@Serializable
sealed interface BaseResp {
    val code: Int
    val message: String
}

@Serializable
sealed interface BaseRespObj<T: Any>: BaseResp {
    override val code: Int
    override val message: String
    val data: T?
}

@Serializable
sealed interface BaseRespList<T: Any>: BaseResp {
    override val code: Int
    override val message: String
    val data: List<T>?
}

@Serializable
data class SimpleResp(
    override val code: Int = 200,
    override val message: String = "success",
): BaseResp
