plugins {
    kotlin("jvm")
    application
    `maven-publish`
}

dependencies {
    implementation(libs.kotlin.coroutines)
    implementation(project(":base"))
    implementation(libs.picocli)
    implementation(libs.jackson.kotlin)
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

publishing {
    repositories {
        val gprUser = (project.findProperty("gpr.user") as String? ?: System.getenv("GH_USER"))?.ifBlank { null }
        if (gprUser != null) {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/DonRobo/home-former")
                credentials {
                    username = gprUser
                    password =
                        (project.findProperty("gpr.key") as String? ?: System.getenv("GH_TOKEN"))?.ifBlank { null }
                            ?: error("No GitHub token set")
                }
            }
        }
    }
    publications {
        create<MavenPublication>("distribution") {
            val ghRef = System.getenv("GH_REF")
            val versionToUse = when {
                ghRef == null -> project.version.toString()
                ghRef.startsWith("refs/heads/") -> ghRef.removePrefix("refs/heads/") + "-SNAPSHOT"
                ghRef.startsWith("refs/tags/") -> ghRef.removePrefix("refs/tags/")
                else -> error("Unknown GH_REF: $ghRef")
            }
            groupId = "at.robert.home-former"
            artifactId = "hf-cli"
            version = versionToUse

            artifact(tasks.distZip)
        }
    }
}

distributions {
    main {
        distributionBaseName = "hf-cli"
    }
}

tasks.jar {
    archiveBaseName = "hf-cli"
}

tasks.startScripts {
    applicationName = "hf"
}
