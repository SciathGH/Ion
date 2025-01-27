package net.starlegacy.feature.multiblock

import co.aikar.timings.Timing
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.starlegacy.SLComponent
import net.starlegacy.feature.multiblock.dockingtube.ConnectedDockingTubeMultiblock
import net.starlegacy.feature.multiblock.dockingtube.DisconnectedDockingTubeMultiblock
import net.starlegacy.feature.multiblock.gravitywell.AmplifiedGravityWellMultiblock
import net.starlegacy.feature.multiblock.gravitywell.StandardGravityWellMultiblock
import net.starlegacy.feature.multiblock.hyperdrive.HyperdriveMultiblockClass1
import net.starlegacy.feature.multiblock.hyperdrive.HyperdriveMultiblockClass2
import net.starlegacy.feature.multiblock.hyperdrive.HyperdriveMultiblockClass3
import net.starlegacy.feature.multiblock.hyperdrive.HyperdriveMultiblockClass4
import net.starlegacy.feature.multiblock.misc.AirlockMultiblock
import net.starlegacy.feature.multiblock.misc.CryoPodMultiblock
import net.starlegacy.feature.multiblock.misc.GasCollectorMultiblock
import net.starlegacy.feature.multiblock.misc.MagazineMultiblock
import net.starlegacy.feature.multiblock.misc.MobDefender
import net.starlegacy.feature.multiblock.misc.TractorBeamMultiblock
import net.starlegacy.feature.multiblock.navigationcomputer.NavigationComputerMultiblockAdvanced
import net.starlegacy.feature.multiblock.navigationcomputer.NavigationComputerMultiblockBasic
import net.starlegacy.feature.multiblock.particleshield.BoxShieldMultiblock
import net.starlegacy.feature.multiblock.particleshield.ShieldMultiblockClass08Left
import net.starlegacy.feature.multiblock.particleshield.ShieldMultiblockClass08Right
import net.starlegacy.feature.multiblock.particleshield.ShieldMultiblockClass08i
import net.starlegacy.feature.multiblock.particleshield.ShieldMultiblockClass20
import net.starlegacy.feature.multiblock.particleshield.ShieldMultiblockClass30
import net.starlegacy.feature.multiblock.particleshield.ShieldMultiblockClass65
import net.starlegacy.feature.multiblock.particleshield.ShieldMultiblockClass85
import net.starlegacy.feature.multiblock.starshipweapon.cannon.PlasmaCannonStarshipWeaponMultiblock
import net.starlegacy.feature.multiblock.starshipweapon.heavy.DownwardRocketStarshipWeaponMultiblock
import net.starlegacy.feature.multiblock.starshipweapon.heavy.HeavyLaserStarshipWeaponMultiblock
import net.starlegacy.feature.multiblock.starshipweapon.heavy.HorizontalRocketStarshipWeaponMultiblock
import net.starlegacy.feature.multiblock.starshipweapon.heavy.PhaserStarshipWeaponMultiblock
import net.starlegacy.feature.multiblock.starshipweapon.heavy.TorpedoStarshipWeaponMultiblock
import net.starlegacy.feature.multiblock.starshipweapon.heavy.UpwardRocketStarshipWeaponMultiblock
import net.starlegacy.feature.multiblock.starshipweapon.misc.PointDefenseStarshipWeaponMultiblockBottom
import net.starlegacy.feature.multiblock.starshipweapon.misc.PointDefenseStarshipWeaponMultiblockSide
import net.starlegacy.feature.multiblock.starshipweapon.misc.PointDefenseStarshipWeaponMultiblockTop
import net.starlegacy.feature.multiblock.starshipweapon.turret.BottomHeavyTurretMultiblock
import net.starlegacy.feature.multiblock.starshipweapon.turret.BottomLightTurretMultiblock
import net.starlegacy.feature.multiblock.starshipweapon.turret.BottomClassicTriTurretMultiblock
import net.starlegacy.feature.multiblock.starshipweapon.turret.TopHeavyTurretMultiblock
import net.starlegacy.feature.multiblock.starshipweapon.turret.TopLightTurretMultiblock
import net.starlegacy.feature.multiblock.starshipweapon.turret.TopClassicTriTurretMultiblock
import net.starlegacy.util.msg
import net.starlegacy.util.time
import net.starlegacy.util.timing
import org.bukkit.Location
import org.bukkit.block.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

object Multiblocks : SLComponent() {
	private lateinit var multiblocks: List<Multiblock>

	private fun initMultiblocks() {
		multiblocks = listOf(
			GasCollectorMultiblock,

			HyperdriveMultiblockClass1,
			HyperdriveMultiblockClass2,
			HyperdriveMultiblockClass3,
			HyperdriveMultiblockClass4,

			NavigationComputerMultiblockBasic,
			NavigationComputerMultiblockAdvanced,

			ShieldMultiblockClass08Right,
			ShieldMultiblockClass08Left,
			ShieldMultiblockClass20,
			ShieldMultiblockClass30,
			ShieldMultiblockClass65,
			ShieldMultiblockClass85,
			ShieldMultiblockClass08i,
			BoxShieldMultiblock,

			DisconnectedDockingTubeMultiblock,
			ConnectedDockingTubeMultiblock,

			CryoPodMultiblock,
			MagazineMultiblock,
			AirlockMultiblock,
			TractorBeamMultiblock,

			StandardGravityWellMultiblock,
			AmplifiedGravityWellMultiblock,

			MobDefender,

			PlasmaCannonStarshipWeaponMultiblock,
			HeavyLaserStarshipWeaponMultiblock,
			TorpedoStarshipWeaponMultiblock,
			PointDefenseStarshipWeaponMultiblockTop,
			PointDefenseStarshipWeaponMultiblockSide,
			PointDefenseStarshipWeaponMultiblockBottom,
			TopLightTurretMultiblock,
			BottomLightTurretMultiblock,
			TopHeavyTurretMultiblock,
			BottomHeavyTurretMultiblock,
			TopClassicTriTurretMultiblock,
			BottomClassicTriTurretMultiblock,
			HorizontalRocketStarshipWeaponMultiblock,
			UpwardRocketStarshipWeaponMultiblock,
			DownwardRocketStarshipWeaponMultiblock,
			PhaserStarshipWeaponMultiblock
		)
	}

	private val multiblockCache: MutableMap<Location, Multiblock> = Object2ObjectOpenHashMap()

	private lateinit var gettingTiming: Timing
	private lateinit var detectionTiming: Timing

	override fun onEnable() {
		initMultiblocks()

		gettingTiming = timing("Multiblock Getting")
		detectionTiming = timing("Multiblock Detection")

		log.info("Loaded ${multiblocks.size} multiblocks")
	}

	fun all(): List<Multiblock> = multiblocks

	@JvmStatic
	@JvmOverloads
	operator fun get(
		sign: Sign, checkStructure: Boolean = true, loadChunks: Boolean = true
	): Multiblock? = gettingTiming.time {
		val location: Location = sign.location
		val lines: Array<String> = sign.lines

		val cached: Multiblock? = multiblockCache[location]
		if (cached != null) {
			// one was already cached before
			if (cached.matchesSign(lines) && (!checkStructure || cached.signMatchesStructure(sign, loadChunks))) {
				// it still matches so returned the cached one
				return@time cached
			} else {
				// it no longer matches so remove it, and re-detect it afterwards
				multiblockCache.remove(location)
			}
		}

		for (multiblock in multiblocks) {
			val matchesSign = multiblock.matchesSign(lines)
			if (matchesSign && (!checkStructure || multiblock.signMatchesStructure(sign, loadChunks))) {
				if (checkStructure) {
					multiblockCache[location] = multiblock
				}
				return@time multiblock
			}
		}

		return@time null
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	fun onInteractMultiblockSign(event: PlayerInteractEvent) {
		if (event.hand != EquipmentSlot.HAND || event.action != Action.RIGHT_CLICK_BLOCK) {
			return
		}

		val sign = event.clickedBlock?.state as? Sign ?: return
		var lastMatch: Multiblock? = null
		val player = event.player

		if (!player.hasPermission("starlegacy.multiblock.detect")) {
			player msg "&cYou don't have permission to detect multiblocks!"
			return
		}

		for (multiblock in multiblocks) {
			if (multiblock.matchesUndetectedSign(sign)) {
				if (multiblock.signMatchesStructure(sign, particles = true)) {
					return multiblock.setupSign(player, sign)
				} else {
					lastMatch = multiblock
				}
			}
		}

		if (lastMatch != null) {
			player msg "&4Improperly built &c${lastMatch.name}&4. Make sure every block is correctly placed!"
		}
	}
}
