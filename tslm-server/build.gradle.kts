
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.docker)
}

group = "io.github.torrenkt.tslmwebui"

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    jvm {
        val main = "io.github.torrenkt.tslmwebui.AppKt"
        mainRun {
            mainClass = main
        }
        binaries {
            executable {
                mainClass = main
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlin.logging)
            implementation(libs.kotlin.codepoints)
            implementation(libs.ktor.resources)
            implementation(libs.bundles.kotlinx.serialization)
            implementation(libs.compose.runtime)
            implementation(libs.compose.components.resources)

            implementation(projects.tslmJava)
        }
        commonTest.dependencies {
            implementation(libs.bundles.kotlin.test)
        }

        wasmJsMain.dependencies {
            implementation(libs.compose.foundation)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.ui.tooling.preview)
            implementation(libs.navigation.compose)
            implementation(libs.bundles.material3.adaptive)
            implementation(libs.bundles.ktor.client)
        }

        jvmMain.dependencies {
            implementation(libs.clikt)
            implementation(libs.koin.ktor)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.bundles.exposed)
            implementation(libs.bundles.exposed.jdbc)
            implementation(libs.bundles.ktor.server)
            implementation(libs.bundles.slf4j)
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "io.github.torrenkt.tslmwebui"
    generateResClass = always
}

ktor {
    development = true
}

tasks {
    val currentReleaseTag = providers.provider {
        val gitDescribe = providers.exec {
            commandLine("git", "describe", "--tags", "--exact-match", "HEAD")
            isIgnoreExitValue = true
        }
        if (gitDescribe.result.get().exitValue != 0) {
            error("A GitHub release requires HEAD to have a tag in vMAJOR.MINOR.PATCH format.")
        }
        val releaseTag = gitDescribe.standardOutput.asText.get().trim()
        Regex("^v\\d+\\.\\d+\\.\\d+$").matchEntire(releaseTag)
            ?: error("A GitHub release requires HEAD to have a tag in vMAJOR.MINOR.PATCH format.")
        releaseTag
    }

    val releaseImageTags = providers.provider {
        val gitDescribe = providers.exec {
            commandLine("git", "describe", "--tags", "--exact-match", "HEAD")
            isIgnoreExitValue = true
        }
        val releaseTag = (if (gitDescribe.result.get().exitValue == 0) {
            gitDescribe.standardOutput.asText.get().trim()
        } else {
            providers.gradleProperty("docker.releaseVersion").orNull
        }) ?: error("A release image requires HEAD or docker.releaseVersion to use vMAJOR.MINOR.PATCH format.")
        val match = Regex("^v(\\d+)\\.(\\d+)\\.(\\d+)$").matchEntire(releaseTag)
            ?: error("A release image requires HEAD or docker.releaseVersion to use vMAJOR.MINOR.PATCH format.")

        listOf(
            "ghcr.io/torrenkt/tslm-webui:$releaseTag",
            "ghcr.io/torrenkt/tslm-webui:latest",
            "ghcr.io/torrenkt/tslm-webui:v${match.groupValues[1]}",
        )
    }

    fun Dockerfile.installZulu25() {
        runCommand(
            "apt-get update && " +
                "apt-get install -y --no-install-recommends ca-certificates curl gnupg && " +
                "curl -fsSL https://repos.azul.com/azul-repo.key | " +
                "gpg --dearmor -o /usr/share/keyrings/azul.gpg && " +
                "echo 'deb [signed-by=/usr/share/keyrings/azul.gpg] https://repos.azul.com/zulu/deb stable main' " +
                "> /etc/apt/sources.list.d/zulu.list && " +
                "apt-get update && " +
                "apt-get install -y --no-install-recommends zulu25-jdk && " +
                "rm -rf /var/lib/apt/lists/*",
        )
    }

    named<ProcessResources>("jvmProcessResources") {
        dependsOn("wasmJsBrowserDistribution")
        from(layout.buildDirectory.dir("dist/wasmJs/productionExecutable")) {
            into("static")
            exclude("**/*.map")
        }
    }

    val createDockerfile = register<Dockerfile>("createDockerfile") {
        group = "docker"
        from(Dockerfile.From("azul/zulu-openjdk:25").withStage("builder"))
        workingDir("/workspace")
        runCommand("apt-get update && apt-get install libatomic1")
        copyFile(".", ".")
        runCommand("./gradlew :tslm-server:installJvmDist --no-daemon")

        from("nvidia/cuda:12.9.0-cudnn-runtime-ubuntu24.04")
        installZulu25()
        workingDir("/app")
        copyFile(
            Dockerfile.CopyFile(
                "/workspace/tslm-server/build/install/tslm-server-jvm",
                "/app",
            ).withStage("builder"),
        )
        exposePort(2156)
        entryPoint("/app/bin/tslm-server")
    }

    val buildDockerImage = register<DockerBuildImage>("buildDockerImage") {
        group = "docker"
        dependsOn(createDockerfile)
        inputDir.set(rootProject.layout.projectDirectory)
        dockerFile.set(createDockerfile.flatMap { it.destFile })
        images.addAll(releaseImageTags)
    }

    val pushDockerImage = register<DockerPushImage>("pushDockerImage") {
        group = "docker"
        description = "Builds and pushes the current release to GitHub Container Registry."
        dependsOn(buildDockerImage)
        images.addAll(releaseImageTags)
    }

    val verifyGitHubReleaseTag = register("verifyGitHubReleaseTag") {
        group = "release"
        description = "Verifies that the current release tag exists on GitHub."

        doLast {
            val releaseTag = currentReleaseTag.get()
            val remoteTag = providers.exec {
                commandLine(
                    "git",
                    "ls-remote",
                    "--exit-code",
                    "--tags",
                    "https://github.com/TorrenKt/tslm-webui.git",
                    "refs/tags/$releaseTag",
                )
                isIgnoreExitValue = true
            }
            if (remoteTag.result.get().exitValue != 0) {
                error("GitHub tag $releaseTag does not exist. Push it before creating a release.")
            }
        }
    }

    named("installJvmDist") {
        mustRunAfter(verifyGitHubReleaseTag)
    }

    val releaseJvmTarGz = register<Tar>("releaseJvmTarGz") {
        group = "release"
        description = "Packages the JVM distribution for the current release tag."
        dependsOn(verifyGitHubReleaseTag, "installJvmDist")
        compression = Compression.GZIP
        archiveExtension.set("tar.gz")
        archiveFileName.set(currentReleaseTag.map { "tslm-webui-$it-jvm.tar.gz" })
        from(layout.buildDirectory.dir("install/tslm-server-jvm")) {
            into("tslm-server-jvm")
        }
    }

    register("createGitHubRelease") {
        group = "release"
        description = "Creates the current GitHub release and uploads the JVM distribution."
        dependsOn(releaseJvmTarGz)

        doLast {
            val releaseTag = currentReleaseTag.get()
            val token = providers.environmentVariable("GITHUB_TOKEN").orNull
                ?: error("createGitHubRelease requires the GITHUB_TOKEN environment variable.")
            val archive = releaseJvmTarGz.flatMap { it.archiveFile }.get().asFile
            val client = HttpClient.newHttpClient()

            fun request(uri: String, method: String = "GET", body: String? = null): HttpResponse<String> {
                val builder = HttpRequest.newBuilder(URI(uri))
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", "Bearer $token")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                return client.send(
                    if (body == null) builder.method(method, HttpRequest.BodyPublishers.noBody()).build()
                    else builder.method(method, HttpRequest.BodyPublishers.ofString(body)).build(),
                    HttpResponse.BodyHandlers.ofString(),
                )
            }

            val tagResponse = request("https://api.github.com/repos/TorrenKt/tslm-webui/git/ref/tags/$releaseTag")
            if (tagResponse.statusCode() == 404) {
                error("GitHub tag $releaseTag does not exist. Push it before creating a release.")
            }
            check(tagResponse.statusCode() == 200) {
                "Unable to verify GitHub tag $releaseTag: HTTP ${tagResponse.statusCode()}: ${tagResponse.body()}"
            }

            val releaseResponse = request(
                uri = "https://api.github.com/repos/TorrenKt/tslm-webui/releases",
                method = "POST",
                body = """{"tag_name":"$releaseTag","name":"$releaseTag","generate_release_notes":true}""",
            )
            check(releaseResponse.statusCode() == 201) {
                "Unable to create GitHub release $releaseTag: HTTP ${releaseResponse.statusCode()}: ${releaseResponse.body()}"
            }
            val uploadUrl = Regex("\"upload_url\":\"([^\"]+)\"")
                .find(releaseResponse.body())
                ?.groupValues
                ?.get(1)
                ?.substringBefore("{")
                ?: error("GitHub release response did not include an upload URL.")
            val uploadRequest = HttpRequest.newBuilder(URI("$uploadUrl?name=${archive.name}"))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/gzip")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .POST(HttpRequest.BodyPublishers.ofFile(archive.toPath()))
                .build()
            val uploadResponse = client.send(uploadRequest, HttpResponse.BodyHandlers.ofString())
            check(uploadResponse.statusCode() == 201) {
                "Unable to upload ${archive.name}: HTTP ${uploadResponse.statusCode()}: ${uploadResponse.body()}"
            }
        }
    }

    val createDevDockerfile = register<Dockerfile>("createDevDockerfile") {
        group = "docker"
        dependsOn("installJvmDist")
        destFile.set(layout.buildDirectory.file("docker/Dockerfile.dev"))
        from("nvidia/cuda:12.9.0-cudnn-runtime-ubuntu24.04")
        installZulu25()
        workingDir("/app")
        copyFile("tslm-server/build/install/tslm-server-jvm", "/app")
        exposePort(2156)
        entryPoint("/app/bin/tslm-server")
    }

    register<DockerBuildImage>("buildDevDockerImage") {
        group = "docker"
        dependsOn(createDevDockerfile)
        inputDir.set(rootProject.layout.projectDirectory)
        dockerFile.set(createDevDockerfile.flatMap { it.destFile })
        images.add("ghcr.io/torrenkt/tslm-webui:v1-dev")
    }
}
