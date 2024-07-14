plugins {
    alias(libs.plugins.jvm) apply false
}

allprojects {
    version = "0.1.9"

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
}

task("setVersion") {
    doLast {
        val versionToSet = project.findProperty("newVersion") as String
        val thisFile = File("build.gradle.kts")
        var versionRegex = Regex("(\\s*)version\\s*=\\s*\"\\d+\\.\\d+\\.\\d+\"\\s*")
        var count = 0
        val content = thisFile.readLines()
            .map { line ->
                val match = versionRegex.matchEntire(line)
                if (match != null) {
                    count++
                    "${match.groupValues[1]}version = \"$versionToSet\""
                } else {
                    line
                }
            }
        require(count == 1) {
            "Expected to find exactly one version line in '${thisFile.name}', found $count"
        }
        thisFile.writeText(content.joinToString("\n"))

        versionRegex = Regex("(\\s*)version\\s*:\\s*\"\\d+\\.\\d+\\.\\d+\"\\s*")
        val configYamlFile = File("home-assistant-addon/config.yaml")
        count = 0
        val configYamlContent = configYamlFile.readLines().map {
            val match = versionRegex.matchEntire(it)
            if (match != null) {
                count++
                "${match.groupValues[1]}version: \"$versionToSet\""
            } else {
                it
            }
        }
        require(count == 1) {
            "Expected to find exactly one version line in '${configYamlFile.name}', found $count"
        }
        configYamlFile.writeText(configYamlContent.joinToString("\n"))
    }
}