package net.horizonsend.ion.server

import net.kyori.adventure.text.minimessage.MiniMessage.miniMessage
import org.bukkit.command.CommandSender

fun niceTimeFormatting(time: Long): String {
	val seconds = time / 1000
	val minutes = seconds / 60
	val hours = minutes / 60
	val days = hours / 24
	val years = days / 365

	return when {
		years   > 0 -> "$years years ago"
		days    > 0 -> "$days days ago"
		hours   > 0 -> "$hours hours ago"
		minutes > 0 -> "$minutes minutes ago"
		seconds > 0 -> "$seconds seconds ago"
		else -> "now"
	}
}

fun CommandSender.sendMiniMessage(message: String) = sendMessage(miniMessage().deserialize(message.trimIndent()))