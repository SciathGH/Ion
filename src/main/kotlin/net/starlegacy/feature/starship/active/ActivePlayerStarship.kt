package net.starlegacy.feature.starship.active

import co.aikar.commands.ConditionFailedException
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import net.starlegacy.cache.nations.NationCache
import net.starlegacy.cache.nations.PlayerCache
import net.starlegacy.database.Oid
import net.starlegacy.database.schema.starships.PlayerStarship
import net.starlegacy.feature.starship.StarshipType
import net.starlegacy.feature.starship.control.StarshipControl
import net.starlegacy.feature.starship.control.StarshipCruising
import net.starlegacy.feature.starship.event.StarshipMoveEvent
import net.starlegacy.feature.starship.event.StarshipRotateEvent
import net.starlegacy.feature.starship.event.StarshipTranslateEvent
import net.starlegacy.feature.starship.hyperspace.HyperspaceBeacons
import net.starlegacy.feature.starship.movement.RotationMovement
import net.starlegacy.feature.starship.movement.StarshipMovement
import net.starlegacy.feature.starship.movement.TranslateMovement
import net.starlegacy.feature.starship.subsystem.weapon.WeaponSubsystem
import net.starlegacy.util.*
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.lang.Math.cbrt
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class ActivePlayerStarship(
    val data: PlayerStarship,
    blocks: LongOpenHashSet,
    mass: Double,
    centerOfMass: Vec3i,
    hitbox: ActiveStarshipHitbox,
    // map of carried ship to its blocks
    carriedShips: Map<PlayerStarship, LongOpenHashSet>
) : ActiveStarship(data.bukkitWorld(), blocks, mass, centerOfMass, hitbox) {
    val carriedShips: MutableMap<PlayerStarship, LongOpenHashSet> = carriedShips.toMutableMap()
    override val type: StarshipType = data.type
    override val interdictionRange: Int = data.type.interdictionRange
    val autoTurretTargets = mutableMapOf<String, UUID>()

    private var lastUnpilotTime = System.nanoTime()
        private set
    var pilot: Player? = null
        set(value) {
            if (value == null) {
                lastUnpilotTime = System.nanoTime()
            }
            field = value
        }

    val minutesUnpiloted = if (pilot != null) 0 else TimeUnit.NANOSECONDS.toMinutes(System.nanoTime() - lastUnpilotTime)

    var speedLimit = -1

    private data class PendingRotation(val clockwise: Boolean)

    private val pendingRotations = LinkedBlockingQueue<PendingRotation>()
    private val rotationTime get() = TimeUnit.MILLISECONDS.toNanos(250L + blockCount / 40L)

    fun getTargetForward(): BlockFace {
        val rotation = pendingRotations.peek()
        return when {
            rotation == null -> forward
            rotation.clockwise -> forward.rightFace
            else -> forward.leftFace
        }
    }

    fun tryRotate(clockwise: Boolean) {
        pendingRotations.add(PendingRotation(clockwise))

        if (pendingRotations.size > 1) {
            return
        }

        scheduleRotation()
    }

    private fun scheduleRotation() {
        val rotationTimeTicks = TimeUnit.NANOSECONDS.toMillis(rotationTime) / 50L
        Tasks.sync {
            pilot?.setCooldown(StarshipControl.CONTROLLER_TYPE, rotationTimeTicks.toInt())
        }
        Tasks.syncDelay(rotationTimeTicks) {
            if (pendingRotations.none()) {
                return@syncDelay
            }

            val rotation = pendingRotations.poll()

            if (pendingRotations.any()) {
                scheduleRotation()
            }

            moveAsync(RotationMovement(this, rotation.clockwise))
        }
    }

    override fun moveAsync(movement: StarshipMovement): CompletableFuture<Boolean> {
        if (!ActiveStarships.isActive(this)) {
            return CompletableFuture.completedFuture(false)
        }

        val pilot = this.pilot
        if (pilot != null) {
            val event: StarshipMoveEvent = when (movement) {
                is TranslateMovement -> StarshipTranslateEvent(this, pilot, movement)
                is RotationMovement -> StarshipRotateEvent(this, pilot, movement)
                else -> error("Unrecognized movement type ${movement.javaClass.name}")
            }

            if (!event.callEvent()) {
                return CompletableFuture.completedFuture(false)
            }
        }

        val future = CompletableFuture<Boolean>()
        Tasks.async {
            val result = executeMovement(movement, pilot)
            future.complete(result)
        }

        return future
    }

    override fun getPotentialTargets(weapon: WeaponSubsystem): List<Player> {
        return weapon.weaponSets
            .mapNotNull(autoTurretTargets::get)
            .mapNotNull(Bukkit::getPlayer)
            .filter { it.world == world }
    }

    @Synchronized
    private fun executeMovement(movement: StarshipMovement, pilot: Player?): Boolean {
        try {
            movement.execute()
        } catch (e: ConditionFailedException) {
            pilot?.msg("&c" + (e.message ?: "Starship could not move for an unspecified reason!"))
            sneakMovements = 0
            return false
        } catch (e: Exception) {
            e.printStackTrace()
            pilot?.msg("&cAn error occurred while trying to move")
            return false
        }

        return true
    }

    val id: Oid<PlayerStarship> = data._id

    // manual move is sneak/direct control
    val manualMoveCooldownMillis: Long = (cbrt(blockCount.toDouble()) * 40).toLong()
    val directControlCooldown get() = 300L + (blockCount / 700) * 30
    var lastManualMove = System.nanoTime() / 1_000_000
    var sneakMovements = 0
    val shieldBars = mutableMapOf<String, BossBar>()

    var cruiseData = StarshipCruising.CruiseData(this)

    override val weaponColor: Color
        get() = pilot?.let { PlayerCache[it].nationId }?.let { Color.fromRGB(NationCache[it].color) } ?: Color.RED

    fun requirePilot(): Player = requireNotNull(pilot) { "Starship must be piloted!" }

    var nearbyBeacon: HyperspaceBeacons.Beacon? = null

    var isDirectControlEnabled: Boolean = false
        private set
    val directControlPreviousVectors = LinkedBlockingQueue<Vector>(4)
    val directControlVector: Vector = Vector()
    var directControlCenter: Location? = null

    fun setDirectControlEnabled(enabled: Boolean) {
        isDirectControlEnabled = enabled
        if (enabled) {
            sendMessage("&7Direct Control: &aON &e[Use /dc to turn it off - scroll or use hotbar keys to adjust speed - use W/A/S/D to maneuver - hold sneak (Lshift) for a boost]")

            val pilot = this.pilot ?: return
            pilot.walkSpeed = 0.009f
            directControlCenter = pilot.location.toBlockLocation().add(0.5, 0.0, 0.5)
        } else {
            sendMessage("&7Direct Control: &cOFF &e[Use /dc to turn it on]")
            directControlVector.x = 0.0
            directControlVector.y = 0.0
            directControlVector.z = 0.0

            val pilot = this.pilot ?: return
            pilot.walkSpeed = 0.2f // default
        }
    }

    override fun removePassenger(playerID: UUID) {
        super.removePassenger(playerID)
        val player = Bukkit.getPlayer(playerID) ?: return
        for (shieldBar in shieldBars.values) {
            shieldBar.removePlayer(player)
        }
    }

    override fun clearPassengers() {
        for (passenger in onlinePassengers) {
            for (shieldBar in shieldBars.values) {
                shieldBar.removePlayer(passenger)
            }
        }
        super.clearPassengers()
    }
}
