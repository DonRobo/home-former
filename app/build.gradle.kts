plugins {
    alias(libs.plugins.jvm)
    application
}

repositories {
    mavenCentral()
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/DonRobo/shelly-api")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GH_USER")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GH_TOKEN")
            }
        }
    }
}

dependencies {
    implementation(kotlin("reflect"))
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.yaml)
    implementation(libs.picocli)
    implementation(libs.shelly)
    testImplementation(libs.bundles.junit)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "at.robert.MainKt"
}

tasks.test {
    useJUnitPlatform()
}
