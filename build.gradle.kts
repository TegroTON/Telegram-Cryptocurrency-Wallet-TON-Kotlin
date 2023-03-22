import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
    application
}

group = "su.spy_me"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

kotlin {
    jvm()

    sourceSets {
        val jvmMain by getting {
            dependencies {
                val vkSdkKotlinVersion = "0.0.8"
                val ydbVersion = "2.0.1"
                val ktorVersion = "2.2.4"

                // Module "core" is required.
                // `project(":core")` only available if your project and the SDK are in the same Gradle project.
                // Prefer to use "com.petersamokhin.vksdk:core:${vkSdkKotlinVersion}"
                implementation("com.petersamokhin.vksdk:core:$vkSdkKotlinVersion")

                // If your project is not JVM-based, or you simply want to use ktor.
                implementation("com.petersamokhin.vksdk:http-client-jvm-okhttp:$vkSdkKotlinVersion")

                // In this case, `ktor-client` is required. You can use any.
                implementation("io.ktor:ktor-client-cio:$ktorVersion")
                implementation("io.ktor:ktor-client-logging-jvm:$ktorVersion")
                implementation("org.jetbrains.kotlinx:atomicfu:0.19.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")
                implementation("org.jetbrains.exposed:exposed-core:0.41.1")
                implementation("org.jetbrains.exposed:exposed-jdbc:0.41.1")
                implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.41.1")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
                implementation("io.github.g0dkar:qrcode-kotlin-jvm:3.3.0")
                implementation("net.dzikoysk:exposed-upsert:1.1.0")
                implementation("org.postgresql:postgresql:42.5.4")
                implementation("org.telegram:telegrambots:6.5.0")
                implementation("tech.ydb:ydb-sdk-core:$ydbVersion")
                implementation("tech.ydb:ydb-sdk-table:$ydbVersion")
                implementation("tech.ydb.auth:yc-auth-provider:$ydbVersion")
                implementation("org.ton:ton-kotlin:0.2.15")

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-properties:1.4.1")
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}