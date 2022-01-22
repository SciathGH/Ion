plugins {
	java
	kotlin("jvm") version "1.6.10"
	id("io.papermc.paperweight.userdev") version "1.3.3"
	id("com.github.johnrengelman.shadow") version "7.1.2"
}

repositories {
	maven("https://repo.mikeprimm.com/") // Has to be first because of Dynmap

	maven("https://repo.codemc.io/repository/maven-snapshots/")
	maven("https://papermc.io/repo/repository/maven-public/")
	maven("https://nexus.scarsz.me/content/groups/public/")
	maven("https://repo.dmulloy2.net/repository/public/")
	maven("https://repo.aikar.co/content/groups/aikar/")
	maven("https://www.myget.org/F/egg82-java/maven/")
	maven("https://maven.enginehub.org/repo/")
	maven("https://repo.citizensnpcs.co/")
	maven("https://jitpack.io")

	mavenCentral()
}

dependencies {
	compileOnly("io.papermc.paper:paper-api:1.18.1-R0.1-SNAPSHOT") // This has to be here otherwise it tries to use Bukkit 1.7.10... thanks Dynmap.
	paperDevBundle("1.18.1-R0.1-SNAPSHOT")

	compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.8")
	compileOnly("net.citizensnpcs:citizens:2.0.27-SNAPSHOT")
	compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
	compileOnly("net.luckperms:api:5.3")

	compileOnly("us.dynmap:dynmap-api:3.1") {
		exclude("org.bukkit:bukkit:1.7.10")
	}

	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
	implementation("org.litote.kmongo:kmongo:4.4.0")
	implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.0")
	implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.10")
	implementation("com.github.stefvanschie.inventoryframework:IF:0.10.3")
	implementation("com.daveanthonythomas.moshipack:moshipack:1.0.1")
	implementation("com.googlecode.cqengine:cqengine:3.6.0")
	implementation("com.github.jkcclemens:khttp:0.1.0")
	implementation("net.wesjd:anvilgui:1.5.2-SNAPSHOT") // 1.5.3-SNAPSHOT is broken
	implementation("net.wesjd:anvilgui-1_18_R1:1.5.3-SNAPSHOT")
	implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
	implementation("redis.clients:jedis:3.7.1") // Newer version changed stuff, I am too lazy to fix it.
}

sourceSets {
	main {
		java {
			srcDir("Ion/src/main/kotlin")
			srcDir("StarLegacy/src/main/java")
			srcDir("StarLegacy/src/main/kotlin")
		}
		resources {
			srcDir("Ion/src/main/resources")
		}
	}
}

tasks {
	compileJava {
		options.compilerArgs.add("-parameters")
		options.isFork = true
	}

	compileKotlin {
		kotlinOptions.javaParameters = true
	}

	reobfJar {
		outputJar.set(file(rootProject.projectDir.absolutePath + "/build/IonServer.jar"))
	}

	create("ionBuild") {
		dependsOn("reobfJar")
	}
}

java {
	toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}