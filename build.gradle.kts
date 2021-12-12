plugins {
    java
    idea
    id("org.jetbrains.kotlin.jvm") version "1.5.0"
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

targetCompatibility = 1.8
sourceCompatibility = 1.8

group = "net.starlegacy"
version = "SNAPSHOT"

repositories {
    // used for github projects without their own repo, com.github.User:Project:Tag
    maven { url = uri("https://jitpack.io") }

    // Official PaperMC repository for API
    maven { url = uri("https://papermc.io/repo/repository/maven-public/") }

    // used for WorldEdit and WorldGuard
    maven { url = uri("https://maven.sk89q.com/repo/") }

    // used for EventChain
    maven { url = uri("https://www.myget.org/F/egg82-java/maven/") }

    // aikar's repository which mirrors lots of Minecraft things plus hosts his own projects
    maven { url = uri("https://repo.aikar.co/content/groups/aikar/") }

    // used for discordsrv
    maven { url = uri("https://nexus.scarsz.me/content/groups/public/") }

    // used for dependency of discordsrv (MCDiscordReserializer)
    maven { url = uri("https://nexus.vankka.dev/repository/maven-public/") }

    // used for dependency of discordsrv (UltimateChat)
    maven { url = uri("https://raw.githubusercontent.com/FabioZumbi12/UltimateChat/mvn-repo/") }

    // anvilgui repo
    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }

    // Citizens NPCs plugin repo
    maven { url = uri("https://repo.citizensnpcs.co/") }

    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }

    // private repository to host server binaries
    maven { url = uri("https://maven.starlegacy.net/") }

    maven { url = uri("https://repo.mikeprimm.com/") }

    // general maven central repository
    mavenCentral()

    // aikar"s repository which mirrors lots of Minecraft things plus hosts his own projects
    // put twice: once before to quickly pick up his own plugins, once here as a last resort for other stuff
    maven { url = uri("https://repo.aikar.co/content/groups/aikar/") }
}

dependencies {
    // https://papermc.io (full server hosted at https://maven.starlegacy.net/)
    compileOnly("com.destroystokyo.paper:paper-api:1.16.4-R0.1-SNAPSHOT")
    compileOnly("com.destroystokyo.paper:paper:1.16.4-R0.1-SNAPSHOT")

    compileOnly("net.luckperms:api:5.0"

    // https://github.com/MilkBowl/Vault
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")

    // https://github.com/bloodmc/GriefDefender/
    compileOnly("com.github.bloodmc:GriefDefenderAPI:master")

    compileOnly("com.discordsrv:discordsrv:1.20.0")

    // https://github.com/webbukkit/dynmap
    compileOnly("us.dynmap:dynmap-api:3.1")
    compileOnly("us.dynmap:spigot:3.1")

    // https://github.com/EngineHub/WorldEdit
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.0-SNAPSHOT")

    // https://github.com/CitizensDev/Citizens2/
    compileOnly("net.citizensnpcs:citizens:2.0.27-SNAPSHOT")

    // https://kotlinlang.org/
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // https://github.com/davethomas11/MoshiPack
    implementation("com.daveanthonythomas.moshipack:moshipack:1.0.0-beta")

    // https://github.com/Litote/kmongo
    implementation("org.litote.kmongo:kmongo:4.2.3")

    // https://github.com/xetorthio/jedis
    implementation("redis.clients:jedis:2.9.0")

    // https://github.com/config4k/config4k
    implementation("io.github.config4k:config4k:0.4.1")

    // https://github.com/npgall/cqengine
    implementation("com.googlecode.cqengine:cqengine:3.0.0")

    // https://github.com/jkcclemens/khttp
    implementation("khttp:khttp:1.0.0")

    implementation("net.wesjd:anvilgui:1.5.0-SNAPSHOT")

    // https://github.com/stefvanschie/IF
    implementation("com.github.stefvanschie.inventoryframework:IF:0.5.8")

    // https://github.com/aikar/commands
    implementation("co.aikar:acf-paper:0.5.0-SNAPSHOT")

    // https://github.com/egg82/EventChain
    implementation("ninja.egg82:event-chain-bukkit:1.0.7")

    implementation("org.ejml:ejml-all:0.40")

    // https://github.com/MinnDevelopment/discord-webhooks
    implementation("club.minnced:discord-webhooks:0.5.6")

    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.1")
    testImplementation("io.mockk:mockk:1.10.0")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.16:1.0.0")
}

test {
    useJUnitPlatform()
}

compileJava {
    options.compilerArgs += "-parameters"
    options.fork = true
    options.forkOptions.executable = "javac"
}

compileKotlin {
    kotlinOptions.javaParameters = true
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.noReflect = false
    kotlinOptions.noStdlib = false
}

jar {
    archiveFileName = project.name + ".jar"
    destinationDir file(rootProject.projectDir.absolutePath + "/build/raw-libs")
}

shadowJar {
    destinationDir file(rootProject.projectDir.absolutePath + "/build/libs")
    relocate("com.fasterxml.jackson", "net.starlegacy.libs.jackson")
    relocate("co.aikar.commands", "net.starlegacy.libs.acf")
    relocate("org.ejml", "net.starlegacy.libs.ejml")
}

idea {
    module {
        downloadJavadoc = true
        downloadSources = true
    }
}

build.dependsOn shadowJar

test {
    useJUnitPlatform()
}
