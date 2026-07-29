package io.github.torrenkt.tslmwebui.core

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json

val TslmApiClient: HttpClient by lazy {
    HttpClient {
        defaultRequest {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            AuthStorage.token?.let {
                header(HttpHeaders.Authorization, "Bearer $it")
            }
        }
        install(ContentNegotiation) {
            json(GlobalJson)
        }
        install(Resources)
    }
}
