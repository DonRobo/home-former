plugins {
    kotlin("jvm")
    alias(libs.plugins.ktor)
    alias(libs.plugins.jib)
}

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(project(":base"))
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.exposed)
}

ktor {
    docker {
        jreVersion.set(JavaVersion.VERSION_21)
    }
}
