package net.horizonsend.ion.proxy

import co.aikar.commands.VelocityCommandManager
import com.google.inject.Inject
import com.velocitypowered.api.event.EventTask
import com.velocitypowered.api.event.ResultedEvent.ComponentResult
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.connection.PreLoginEvent
import com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerPing
import com.velocitypowered.api.proxy.server.ServerPing.Players
import com.velocitypowered.api.proxy.server.ServerPing.SamplePlayer
import com.velocitypowered.api.proxy.server.ServerPing.Version
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent.DIRECT_MESSAGES
import net.dv8tion.jda.api.utils.cache.CacheFlag.ACTIVITY
import net.dv8tion.jda.api.utils.cache.CacheFlag.CLIENT_STATUS
import net.dv8tion.jda.api.utils.cache.CacheFlag.EMOTE
import net.dv8tion.jda.api.utils.cache.CacheFlag.ONLINE_STATUS
import net.dv8tion.jda.api.utils.cache.CacheFlag.VOICE_STATE
import net.horizonsend.ion.proxy.commands.Link
import net.horizonsend.ion.proxy.commands.Move
import net.horizonsend.ion.proxy.commands.Server
import net.horizonsend.ion.proxy.commands.Unlink
import net.horizonsend.ion.proxy.database.MongoManager
import net.kyori.adventure.text.minimessage.MiniMessage.miniMessage
import org.slf4j.Logger

@Plugin(id = "ion", name = "Ion (Proxy)", version = "1.0.0", description = "Ion (Proxy)", authors = ["PeterCrawley"], url = "https://horizonsend.net")
class Ion @Inject constructor(val server: ProxyServer, private val logger: Logger, @DataDirectory val dataDirectory: Path) {
	companion object {
		lateinit var ionInstance: Ion
			private set

		val server get() = ionInstance.server

		var ionConfig: Config = Config()
			private set

		var jda: JDA? = null
			private set
	}

	@Subscribe
	fun onPreLoginEvent(event: PreLoginEvent): EventTask = EventTask.async {
		if (event.connection.protocolVersion.protocol != 757) event.result =
			PreLoginComponentResult.denied(miniMessage().deserialize("<red><bold>Sorry, only 1.18(.1) clients can play on Horizon's End!"))
	}

	@Subscribe
	fun onLoginEvent(event: LoginEvent): EventTask = EventTask.async {
		if (server.playerCount > 30 && !event.player.hasPermission("ion.maxPlayerBypass")) event.result =
			ComponentResult.denied(miniMessage().deserialize("<red><bold>Sorry, the server is full!"))
	}

	@Subscribe
	fun onProxyPingEvent(event: ProxyPingEvent): EventTask = EventTask.async {
		event.ping = ServerPing(
			Version(757, "1.18.1"),
			Players(server.playerCount, 30, server.allPlayers.map { SamplePlayer(it.username, it.uniqueId) }),
			if (event.connection.protocolVersion.protocol == 757) {
				miniMessage().deserialize("<gold><bold>Horizon's End</bold><gray> - <red><bold>Spaceships</bold><gray>, <green><bold>Planets</bold><gray>, <blue><bold>Combat</bold><gray>\n<italic>Community ran continuation of Star Legacy")
			} else {
				miniMessage().deserialize("<red><bold>Sorry, only 1.18(.1) clients can play on Horizon's End!")
			},
			null
		)
	}

	@Subscribe
	@Suppress("UNUSED_PARAMETER") // Parameter is required to indicate what event to subscribe to
	fun onStart(event: ProxyInitializeEvent): EventTask = EventTask.async {
		ionInstance = this

		dataDirectory.createDirectories() // Ensure the directories exist

		// Loading of config
		dataDirectory.resolve("config.json").apply {
			if (!exists()) {
				logger.warn("Failed to find the config file, creating a new one.")
				writeText(Json.encodeToString(ionConfig))
			}

			else ionConfig = Json.decodeFromString(readText())
		}

		if (ionConfig.discordToken == "")
			logger.error("Unable to start JDA, the bot token is likely invalid. The plugin will continue with reduced functionality.")

		// Connect to discord
		else try {
			jda = JDABuilder.create(ionConfig.discordToken, DIRECT_MESSAGES).apply {
				disableCache(ACTIVITY, VOICE_STATE, EMOTE, CLIENT_STATUS, ONLINE_STATUS)
			}.build().apply {
				addEventListener(JDAListener)
			}
		} catch (e: Exception) {
			logger.error("Unable to start JDA, the bot token is likely invalid. The plugin will continue with reduced functionality.")
		}

		// Init MongoDB
		MongoManager

		VelocityCommandManager(server, this).apply {
			setOf(Link, Move, Server, Unlink).forEach { registerCommand(it) }

			commandCompletions.apply {
				registerCompletion("multiTargets") {
					server.allPlayers.map { it.username }.toMutableList().apply {
						add("*")
						addAll(server.allServers.map { "@${it.serverInfo.name}" })
					}
				}

				registerCompletion("players") {
					server.allPlayers.map { it.username }.toMutableList()
				}

				registerCompletion("servers") { context ->
					server.allServers.map { it.serverInfo.name }.filter { context.sender.hasPermission("ion.server.$it") }
				}
			}

			@Suppress("DEPRECATION") // To quote Micle (Regions.kt L209) "our standards are very low"
			enableUnstableAPI("help")
		}
	}
}