plugins {
    alias(libs.plugins.jvm) apply false
}

allprojects {
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
