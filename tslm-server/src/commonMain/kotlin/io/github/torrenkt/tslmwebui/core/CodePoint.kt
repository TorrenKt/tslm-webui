package io.github.torrenkt.tslmwebui.core

import me.zolotov.kodepoint.forEachCodepoint

val String.codePointCount: Int
    get() {
        var size = 0
        forEachCodepoint { size += 1 }
        return size
    }
