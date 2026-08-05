package io.github.torrenkt.tslmwebui

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import io.github.torrenkt.tslm.TSLM
import io.github.torrenkt.tslmwebui.core.logger
import io.github.torrenkt.tslmwebui.database.connectPostgreSql
import io.github.torrenkt.tslmwebui.database.setupDatabase
import io.github.torrenkt.tslmwebui.database.RecognitionCacheTable
import io.github.torrenkt.tslmwebui.database.RecognitionRecordTable
import io.github.torrenkt.tslmwebui.database.Token
import io.github.torrenkt.tslmwebui.database.TokenTable
import io.github.torrenkt.tslmwebui.ktor.tokenAuthentication
import io.github.torrenkt.tslmwebui.ktor.contentNegotiation
import io.github.torrenkt.tslmwebui.ktor.routers
import io.github.torrenkt.tslmwebui.token.checkToken
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.Authentication
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.singlePageApplication
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.resources.Resources
import io.ktor.server.routing.routing
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.io.File

object App: SuspendingCliktCommand() {
    val log by logger("TslmWebUI")

    val port: Int by option("--webui-port", envvar = "TSLM_WEBUI_PORT").int().default(2156)

    val publicInstance: Boolean by option("--public-instance", envvar = "TSLM_WEBUI_PUBLIC_INSTANCE")
        .boolean()
        .default(true)

    val superToken: String? by option("--super-token", envvar = "TSLM_WEBUI_SUPER_TOKEN")
        .validate {
            if (!it.checkToken()) {
                fail("super token must contain exactly 128 alphanumeric characters")
            }
        }

    val databaseHost: String by option("--database-host", envvar = "TSLM_WEBUI_DATABASE_HOST").default("127.0.0.1")
    val databasePort: Int by option("--database-port", envvar = "TSLM_WEBUI_DATABASE_PORT").int().default(5432)
    val databaseSchema: String by option("--database-schema", envvar = "TSLM_WEBUI_DATABASE_SCHEMA").default("public")
    val databaseName: String by option("--database-name", envvar = "TSLM_WEBUI_DATABASE_NAME").default("tslm")
    val databaseUsername: String? by option("--database-username", envvar = "TSLM_WEBUI_DATABASE_USERNAME")
    val databasePassword: String? by option("--database-password", envvar = "TSLM_WEBUI_DATABASE_PASSWORD")

    val useCuda: Boolean by option("--use-cuda", envvar = "TSLM_WEBUI_USE_CUDA")
        .boolean()
        .default(true)

    val localModel: File? by option("--model-file", envvar = "TSLM_WEBUI_MODEL_FILE")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
    val modelPath: File by option("--model-path", envvar = "TSLM_WEBUI_MODEL_PATH")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
        .default(TSLM.defaultCacheDir())
    private val onlineModel: File by lazy {
        TSLM.downloadFromHuggingFace(
            useCuda = useCuda,
            cacheDir = modelPath,
        )
    }

    val modelHash: String by lazy {
        (localModel ?: onlineModel).sha256()
    }

    override suspend fun run() {
        if (!publicInstance) {
            checkNotNull(superToken) { "--super-token is required for a private instance" }
            val username = checkNotNull(databaseUsername) { "--database-username is required for a private instance" }
            val password = checkNotNull(databasePassword) { "--database-password is required for a private instance" }
            connectPostgreSql(
                databaseHost = databaseHost,
                databasePort = databasePort,
                databaseName = databaseName,
                databaseSchema = databaseSchema,
                databaseUsername = username,
                databasePassword = password,
            )
            setupDatabase(
                RecognitionCacheTable,
                RecognitionRecordTable,
                TokenTable,
            )
            suspendTransaction {
                if (Token.count() == 0L) {
                    Token.new(0) {
                        email = "SuperUser"
                        token = checkNotNull(superToken)
                    }
                } else {
                    Token.findById(0)?.apply {
                        email = "SuperUser"
                        token = checkNotNull(superToken)
                        enabled = true
                    }
                }
            }
        }

        if (localModel == null) {
            log.info { "model file not exists, downloading..." }
            onlineModel
        }

        log.info { "Loading TSLM model..." }
        val tslm = TSLM(
            useCuda = useCuda,
            modelPath = localModel ?: onlineModel,
        )
        log.info { "Preheating TSLM model..." }
        tslm("test")
        log.info { "TslmWebUI started" }

        embeddedServer(CIO, port = port) {
            if (!publicInstance) {
                install(Authentication) {
                    tokenAuthentication()
                }
            }
            install(Koin) {
                modules(
                    module {
                        single { tslm }
                    },
                )
            }
            install(ContentNegotiation) { contentNegotiation() }
            install(AutoHeadResponse)
            install(Resources)
            routing {
                singlePageApplication {
                    filesPath = "/static"
                    useResources = true
                }
                if (publicInstance) {
                    routers(publicInstance)
                } else {
                    authenticate {
                        routers(publicInstance)
                    }
                }
            }
        }.startSuspend(true)
    }
}

suspend fun main(args: Array<String>) = App.main(args)
