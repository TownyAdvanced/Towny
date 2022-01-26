import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

configurations.all {
    // IDEA can have issues resolving Spigot-API if this is not set, unless mavenLocal() is used (a bad practice.)
    resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/releases/")}
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://papermc.io/repo/repository/maven-public/") }

    maven { url = uri("https://repo.citizensnpcs.co/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-public/") }
    maven { url = uri("https://repo.essentialsx.net/releases/") }
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") }
}

dependencies {
    // Duplicate intentional. Jabel fails to find its ByteBuddy dependency otherwise.
    annotationProcessor("com.github.bsideup.jabel:jabel-javac-plugin:0.4.2")
    compileOnly("com.github.bsideup.jabel:jabel-javac-plugin:0.4.2")

    compileOnly("org.spigotmc:spigot-api:1.18.1-R0.1-SNAPSHOT")

    // Relocated by Shadow
    api("io.papermc:paperlib:1.0.6")
    api("org.bstats:bstats-bukkit:2.2.1")
    api("com.zaxxer:HikariCP:5.0.1") {
        exclude("org.slf4j", "slf4j-api")
    }
    api("org.apache.commons:commons-compress:1.21")
    api("net.kyori:adventure-platform-bukkit:4.0.1")

    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude("org.bukkit", "bukkit")
    }
    compileOnly("net.luckperms:api:5.3")
    compileOnly("net.ess3:EssentialsX:2.18.2") {
        exclude("io.papermc", "paperlib")
    }
    compileOnly("net.tnemc:Reserve:0.1.5.0")
    compileOnly("net.tnemc:TheNewChat:1.5.1.0")
    compileOnly("com.github.ElgarL:groupmanager:2.9") {
        exclude("org.jetbrains", "annotations")
        exclude("org.bstats", "bstats-bukkit")
    }
    compileOnly("net.citizensnpcs:citizensapi:2.0.28-SNAPSHOT")
    compileOnly("org.apache.logging.log4j:log4j-core:2.17.1")
    compileOnly("me.clip:placeholderapi:2.10.9")
    compileOnly("org.jetbrains:annotations:22.0.0")
}

group = "com.palmergames.bukkit.towny"
version = "0.97.5.15"
description = "towny"

sourceSets {
    main {
        java {
            setSrcDirs(listOf("src"))
        }
        resources {
            setSrcDirs(listOf("resources"))
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("publishMaven") {
            artifact(tasks["shadowJar"])
            pom {
                name.set("Towny Advanced")
                description.set("Resident-Town-Nation hierarchy combined with a grid-based protection system.")
                url.set("https://townyadvanced.github.io/")
                licenses {
                    license {
                        name.set("CC BY-NC-ND 3.0")
                        url.set("https://creativecommons.org/licenses/by-nc-nd/3.0/legalcode")
                    }
                }
                developers {
                    developer {
                        id.set("LlmDl")
                        name.set("Llama Delio")
                        email.set("LlmDlio@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/TownyAdvanced/Towny.git")
                    developerConnection.set("scm:git:https://github.com/TownyAdvanced/Towny.git")
                    url.set("https://github.com/TownyAdvanced/Towny")
                }
            }
        }
    }
    repositories {
        maven {
            name = "Glare.Repository"
            url = uri("https://repo.glaremasters.me/repository/towny/")
        }
        maven {
            name = "GitHub.Packages"
            url = uri("https://maven.pkg.github.com/TownyAdvanced/Towny")
        }
    }
}

tasks.withType<JavaCompile> {
    // DO NOT REMOVE
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:unchecked")
    sourceCompatibility = "16" // Historical Setting; Used for Jabel IDE support

    options.release.set(8)
    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(16))
    })
}

tasks.named<ShadowJar>("shadowJar").configure {
    relocate("io.papermc.lib","com.palmergames.paperlib")
    relocate("com.zaxxer.hikari", "com.palmergames.hikaricp")
    relocate("org.apache.commons.compress", "com.palmergames.compress")
    relocate("net.kyori.adventure", "com.palmergames.adventure")
    relocate("net.kyori.examination", "com.palmergames.examination")
    relocate("org.bstats", "com.palmergames.bukkit.metrics")
}

tasks {
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to version, "bukkitAPI" to "1.14")
        }
    }

    jar {
        manifest {
            attributes(
                    mapOf("Main-Class" to "com.palmergames.bukkit.towny.Towny", "ImplementationTitle" to project.name, "Implementation-Version" to project.version))
        }
    }

    shadowJar {
        manifest.inheritFrom(jar.get().manifest)
        archiveClassifier.get()
        minimize()
      }
}
