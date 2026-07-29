package io.github.torrenkt.tslmwebui.core

import kotlinx.serialization.json.Json
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
val GlobalJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
    classDiscriminator = "@type"
}
