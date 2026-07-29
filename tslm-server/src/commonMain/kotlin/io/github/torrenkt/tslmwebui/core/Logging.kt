package io.github.torrenkt.tslmwebui.core

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging

fun logger(name: String): Lazy<KLogger> = lazy {
    KotlinLogging.logger(name)
}
