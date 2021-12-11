package net.starlegacy.feature.starship

import net.starlegacy.SLComponent
import net.starlegacy.cache.nations.PlayerCache
import net.starlegacy.cache.nations.RelationCache
import net.starlegacy.database.schema.nations.NationRelation
import net.starlegacy.feature.misc.CustomItems
import net.starlegacy.feature.multiblock.Multiblocks
import net.starlegacy.feature.multiblock.gravitywell.GravityWellMultiblock
import net.starlegacy.feature.space.WorldFlags.Flag
import net.starlegacy.feature.space.WorldFlags.isFlagSet
import net.starlegacy.feature.starship.active.ActivePlayerStarship
import net.starlegacy.feature.starship.active.ActiveStarship
import net.starlegacy.feature.starship.active.ActiveStarships
import net.starlegacy.feature.starship.control.StarshipCruising
import net.starlegacy.util.LegacyItemUtils
import net.starlegacy.util.isWallSign
import net.starlegacy.util.msg
import org.bukkit.World
import org.bukkit.block.Sign
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

object Interdiction : SLComponent() {
    override fun onEnable() {
        subscribe<PlayerInteractEvent>() { event ->
            val player = event.player
            val block = event.clickedBlock ?: return@subscribe
            if (!block.type.isWallSign) {
                return@subscribe
            }
            val sign = block.state as Sign
            val multiblock = Multiblocks[sign]
            if (multiblock !is GravityWellMultiblock) {
                return@subscribe
            }
            val starship = ActiveStarships.findByPassenger(player)
                ?: return@subscribe player msg "&cYou're not riding the starship"
            if (!starship.contains(block.x, block.y, block.z)) {
                return@subscribe
            }
            if (!isFlagSet(starship.world, Flag.SPACE)) {
                player msg "&cYou cannot cast a mass shadow within another mass shadow, so you can only use gravity wells in space"
                return@subscribe
            }
            when (event.action) {
                Action.RIGHT_CLICK_BLOCK -> {
                    starship.toggleInterdiction()
                }
                Action.LEFT_CLICK_BLOCK -> {
                    pulseGravityWell(player, starship, sign)
                }
                else -> return@subscribe
            }
        }
    }

    private fun pulseGravityWell(player: Player, starship: ActiveStarship, sign: Sign) {
        val world = sign.world

        if (!isFlagSet(world, Flag.SPACE)) {
            player msg "&cYou cannot use gravity wells within other gravity wells."
            return
        }

        if (world.environment == World.Environment.NETHER) {
            player msg "&cYou can't use gravity wells in hyperspace."
            return
        }

        if (!starship.isInterdicting) {
            player msg "&cGravity well is disabled."
            return
        }

        val input = GravityWellMultiblock.getInput(sign)

        if (LegacyItemUtils.getTotalItems(input, CustomItems.MINERAL_CHETHERITE.singleItem()) < 2) {
            player msg "&cNot enough hypermatter in the dropper. Two chetherite shards are required!"
            return
        }

        for (cruisingShip in ActiveStarships.getInWorld(world)) {
            if (cruisingShip !is ActivePlayerStarship) {
                continue
            }

            if (cruisingShip == starship) {
                continue
            }

            if (!StarshipCruising.isCruising(cruisingShip)) {
                continue
            }

            val pilot = cruisingShip.pilot ?: continue

            if (pilot.location.distance(sign.location) > GravityWellMultiblock.PULSE_RADIUS) {
                continue
            }

            if (isAllied(pilot, player)) {
                continue
            }

            cruisingShip.cruiseData.velocity.multiply(0.9)
            cruisingShip.sendMessage("&cQuantum fluctuations detected - velocity has been reduced by 10%.")
        }

        input.removeItem(CustomItems.MINERAL_CHETHERITE.itemStack(2))
        starship.sendMessage("&5Gravity pulse has been invoked by ${player.name}.")
    }

    private fun isAllied(pilot: Player, player: Player): Boolean {
        val pilotNation = PlayerCache[pilot].nationId ?: return false
        val playerNation = PlayerCache[player].nationId ?: return false
        return RelationCache[pilotNation, playerNation] >= NationRelation.Level.ALLY
    }
}
