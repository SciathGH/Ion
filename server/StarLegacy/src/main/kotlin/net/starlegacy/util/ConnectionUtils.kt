package net.starlegacy.util

import java.lang.reflect.Field
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.RelativeArgument.X
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.RelativeArgument.X_ROT
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.RelativeArgument.Y
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.RelativeArgument.Y_ROT
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.RelativeArgument.Z
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.phys.Vec3
import org.bukkit.Location
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.util.Vector

// Based on a modernised version from https://www.spigotmc.org/threads/teleport-player-smoothly.317416/page-2
// Thanks "andrew121410", if only I found this sooner.

object ConnectionUtils {
	private val OFFSET_DIRECTION = setOf(X_ROT, Y_ROT)
	private val OFFSET_ALL = setOf(X_ROT, Y_ROT, X, Y, Z)

	private var justTeleportedField = getField("justTeleported")
	private var awaitingPositionFromClientField = getField("y")
	private var lastPosXField = getField("lastPosX")
	private var lastPosYField = getField("lastPosY")
	private var lastPosZField = getField("lastPosZ")
	private var awaitingTeleportField = getField("z")
	private var awaitingTeleportTimeField = getField("A")
	private var aboveGroundVehicleTickCountField = getField("E")

	@Throws(NoSuchFieldException::class)
	private fun getField(name: String): Field {
		val field = ServerGamePacketListenerImpl::class.java.getDeclaredField(name)
		field.isAccessible = true
		return field
	}

	fun move(player: Player, loc: Location, theta: Float = 0.0f, offsetPos: Vector? = null) {
		val handle = (player as CraftPlayer).handle
		val x = loc.x
		val y = loc.y
		val z = loc.z

		if (handle.containerMenu !== handle.inventoryMenu) handle.closeContainer()

		handle.absMoveTo(x, y, z, handle.yRot + theta, handle.xRot)

		val connection = handle.connection

		var teleportAwait = 0

		justTeleportedField.set(connection, true)
		awaitingPositionFromClientField.set(connection, Vec3(x, y, z))
		lastPosXField.set(connection, x)
		lastPosYField.set(connection, y)
		lastPosZField.set(connection, z)

		teleportAwait = awaitingTeleportField.getInt(connection) + 1
		if (teleportAwait == 2147483647) teleportAwait = 0
		awaitingTeleportField.set(connection, teleportAwait);

		awaitingTeleportTimeField.set(connection, aboveGroundVehicleTickCountField.get(connection))

		val px: Double
		val py: Double
		val pz: Double

		if (offsetPos == null) {
			px = x
			py = y
			pz = z
		} else {
			px = offsetPos.x
			py = offsetPos.y
			pz = offsetPos.z
		}

		val flags = if (offsetPos != null) OFFSET_ALL else OFFSET_DIRECTION
		val packet = ClientboundPlayerPositionPacket(px, py, pz, theta, 0f, flags, teleportAwait, false)
		connection.send(packet)
	}

	fun teleport(player: Player, loc: Location) {
		move(player, loc, 0.0f, null)
	}

	fun teleportRotate(player: Player, loc: Location, theta: Float) {
		move(player, loc, theta, null)
	}

	fun move(player: Player, loc: Location, dx: Double, dy: Double, dz: Double) {
		move(player, loc, 0.0f, Vector(dx, dy, dz))
	}
}
