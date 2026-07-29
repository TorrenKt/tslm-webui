package io.github.torrenkt.tslmwebui.routers

import io.github.torrenkt.tslm.TslmLabel
import kotlinx.serialization.Serializable

@Serializable
data class TslmDisplayEntity(
    val start: Int,
    val end: Int,
    val label: TslmLabel?,
) {
    operator fun invoke(input: String): String {
        return input.substring(start, end)
    }
}
