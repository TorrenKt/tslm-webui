package io.github.torrenkt.tslmwebui.routers

import io.github.torrenkt.tslm.TSLM
import io.github.torrenkt.tslm.normalizeWith
import io.github.torrenkt.tslmwebui.App
import io.github.torrenkt.tslmwebui.core.logger
import io.github.torrenkt.tslmwebui.database.RecognitionCache
import io.github.torrenkt.tslmwebui.database.RecognitionCacheTable
import io.github.torrenkt.tslmwebui.database.RecognitionRecord
import io.github.torrenkt.tslmwebui.ktor.TokenPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

private val log by logger("PredictTorrent")

fun Route.predictTorrent() {
    val tslm by inject<TSLM>()
    post<PredictTorrent> {
        val req = try {
            call.receive<PredictTorrent.Req>()
        } catch (e: Exception) {
            call.respond(SimpleResp(code = -1, e.message ?: "Unknown"))
            return@post
        }
        val tokenId = call.principal<TokenPrincipal>()?.id
        val result = if (tokenId == null) {
            tslm(req.input).toDisplayEntities()
        } else {
            val cachedResult = suspendTransaction {
                RecognitionCache.find {
                    (RecognitionCacheTable.title eq req.input) and
                        (RecognitionCacheTable.modelHash eq App.modelHash)
                }.singleOrNull()?.result
            }
            val result = cachedResult ?: tslm(req.input).toDisplayEntities()
            suspendTransaction {
                val cache = RecognitionCache.find {
                    (RecognitionCacheTable.title eq req.input) and
                        (RecognitionCacheTable.modelHash eq App.modelHash)
                }.singleOrNull() ?: RecognitionCache.new {
                    title = req.input
                    modelHash = App.modelHash
                    this.result = result
                }
                RecognitionRecord.new {
                    this.tokenId = tokenId
                    cacheId = cache.id.value
                }
                cache.result
            }
        }
        log.info { "predict ${result.size} entities: ${req.input}" }

        call.respond(PredictTorrent.Resp(
            data = result
        ))
    }
}

private fun List<io.github.torrenkt.tslm.TslmEntity>.toDisplayEntities(): List<TslmDisplayEntity> {
    return normalizeWith().map { entity ->
        TslmDisplayEntity(entity.start, entity.end, entity.label)
    }
}
