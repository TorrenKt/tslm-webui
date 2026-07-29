
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

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
    fun Dockerfile.installZulu21() {
        runCommand(
            "apt-get update && " +
                "apt-get install -y --no-install-recommends ca-certificates curl gnupg && " +
                "curl -fsSL https://repos.azul.com/azul-repo.key | " +
                "gpg --dearmor -o /usr/share/keyrings/azul.gpg && " +
                "echo 'deb [signed-by=/usr/share/keyrings/azul.gpg] https://repos.azul.com/zulu/deb stable main' " +
                "> /etc/apt/sources.list.d/zulu.list && " +
                "apt-get update && " +
                "apt-get install -y --no-install-recommends zulu21-jdk && " +
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
        from(Dockerfile.From("azul/zulu-openjdk:21").withStage("builder"))
        workingDir("/workspace")
        copyFile(".", ".")
        runCommand("./gradlew :tslm-server:installJvmDist --no-daemon")

        from("nvidia/cuda:12.9.0-cudnn-runtime-ubuntu24.04")
        installZulu21()
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

    register<DockerBuildImage>("buildDockerImage") {
        group = "docker"
        dependsOn(createDockerfile)
        inputDir.set(rootProject.layout.projectDirectory)
        dockerFile.set(createDockerfile.flatMap { it.destFile })
        images.add("mhmzx/tslm-webui")
    }

    val createDevDockerfile = register<Dockerfile>("createDevDockerfile") {
        group = "docker"
        dependsOn("installJvmDist")
        destFile.set(layout.buildDirectory.file("docker/Dockerfile.dev"))
        from("nvidia/cuda:12.9.0-cudnn-runtime-ubuntu24.04")
        installZulu21()
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
        images.add("mhmzx/tslm-webui:dev")
    }
}
