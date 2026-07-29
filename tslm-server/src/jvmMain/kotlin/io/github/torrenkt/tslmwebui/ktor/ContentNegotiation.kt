package io.github.torrenkt.tslmwebui.ktor

import io.github.torrenkt.tslmwebui.core.GlobalJson
import io.ktor.serialization.Configuration
import io.ktor.serialization.kotlinx.json.json

fun Configuration.contentNegotiation() {
    json(GlobalJson)
}
