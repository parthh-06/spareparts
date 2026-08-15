    plugins {
        kotlin("jvm") version "1.9.20"
        id("org.jetbrains.compose") version "1.5.10"

    }

    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        implementation(compose.desktop.currentOs)
        implementation("mysql:mysql-connector-java:8.0.33")


    }



    compose.desktop {
        application {
            mainClass = "MainKt"
        }
    }
    tasks.jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        manifest {
            attributes["Main-Class"] = "MainKt"
        }

        from({
            configurations.runtimeClasspath.get().map {
                if (it.isDirectory) it else zipTree(it)
            }
        })
    }