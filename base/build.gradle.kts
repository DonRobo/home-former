plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    implementation(kotlin("reflect"))
    implementation(libs.kotlin.coroutines)
    api(libs.jackson.kotlin)
    api(libs.jackson.yaml)
    implementation(libs.shelly)
    testImplementation(libs.bundles.junit)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.test {
    useJUnitPlatform()
}
