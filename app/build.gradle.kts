plugins {
    alias(libs.plugins.jvm)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.yaml)
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
