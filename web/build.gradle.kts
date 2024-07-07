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

jib {
    from {
        platforms {
            platform {
                os = "linux"
                architecture = "amd64"
            }
//            platform {
//                os = "linux"
//                architecture = "arm64"
//            }
        }
    }
    to {
        image = "ghcr.io/DonRobo/home-former"
        auth {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GH_USER")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GH_TOKEN")
        }
    }
}
