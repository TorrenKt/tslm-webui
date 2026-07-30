package io.github.torrenkt.tslmwebui.ktor

import io.github.torrenkt.tslmwebui.routers.predictTorrent
import io.github.torrenkt.tslmwebui.routers.authState
import io.github.torrenkt.tslmwebui.routers.createToken
import io.github.torrenkt.tslmwebui.routers.deleteToken
import io.github.torrenkt.tslmwebui.routers.listToken
import io.github.torrenkt.tslmwebui.routers.listAllRecognitionRecord
import io.github.torrenkt.tslmwebui.routers.listRecognitionRecord
import io.github.torrenkt.tslmwebui.routers.refreshToken
import io.github.torrenkt.tslmwebui.routers.tokenState
import io.ktor.server.routing.Route
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route

// resource 404: https://youtrack.jetbrains.com/issue/KTOR-6773
fun Route.routers(publicInstance: Boolean) {
    predictTorrent()

    if (publicInstance) {
        return
    }

    authState()
    createToken()
    deleteToken()
    refreshToken()
    listToken()
    listAllRecognitionRecord()
    listRecognitionRecord()
    tokenState()
}
