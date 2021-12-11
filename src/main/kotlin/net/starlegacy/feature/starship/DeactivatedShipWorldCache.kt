package net.starlegacy.feature.starship

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.starlegacy.database.schema.starships.PlayerStarship
import net.starlegacy.util.Vec3i
import net.starlegacy.util.blockKey
import net.starlegacy.util.chunkKey
import net.starlegacy.util.orNull
import org.bukkit.Chunk
import org.bukkit.World
import org.litote.kmongo.json
import org.litote.kmongo.setValue
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Optional
import java.util.concurrent.TimeUnit

/**
 * For keeping track of all deactivated ships
 */
class DeactivatedShipWorldCache(world: World) {
    private val worldName = world.name
    val dataFolder = File(world.worldFolder, "data/starlegacy/starship_saves").also { it.mkdirs() }

    private val blockKeyMap: MutableMap<Long, PlayerStarship> = Long2ObjectOpenHashMap()
    private val chunkKeyMap: Multimap<Long, PlayerStarship> = HashMultimap.create()

    private val mutex = Any()

    fun add(data: PlayerStarship): Unit = synchronized(mutex) {
        val blockKey: Long = data.blockKey

        check(!blockKeyMap.containsKey(blockKey)) {
            "$worldName already has starship data at ${Vec3i(blockKey)} (existing: ${blockKeyMap[blockKey]}, tried adding: $data)"
        }

        blockKeyMap[blockKey] = data

        data.containedChunks?.forEach { chunkKey ->
            chunkKeyMap[chunkKey].add(data)
        }
    }

    fun remove(data: PlayerStarship): Unit = synchronized(mutex) {
        val blockKey: Long = data.blockKey
        val existing: PlayerStarship? = blockKeyMap[blockKey]

        requireNotNull(existing) {
            "$worldName does not have starship data at ${Vec3i(blockKey)}, " +
                    "but ${data._id} was attempted to be removed. " +
                    "Full json: ${data.json}"
        }

        require(existing._id == data._id) {
            "$worldName does have starship data at ${Vec3i(blockKey)}, " +
                    "but it's a different ID! " +
                    "Tried removing ${data._id} but found ${existing._id}"
        }

        blockKeyMap.remove(data.blockKey)

        data.containedChunks?.forEach { chunkKey ->
            chunkKeyMap[chunkKey].remove(data)
        }

        savedStateCache.invalidate(data)
    }

    val savedStateCache: LoadingCache<PlayerStarship, Optional<PlayerStarshipState>> = CacheBuilder.newBuilder()
        .weakKeys()
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build(CacheLoader.from { data: PlayerStarship? ->
            if (data != null) {
                val saveFile = DeactivatedPlayerStarships.getSaveFile(world, data)
                if (saveFile.exists()) {
                    val result = FileInputStream(saveFile).use {
                        PlayerStarshipState.readFromStream(it)
                    }
                    Optional.of(result)
                } else {
                    Optional.empty()
                }
            } else {
                Optional.empty()
            }
        })

    fun removeState(data: PlayerStarship): Unit = synchronized(mutex) {
        data.containedChunks?.forEach { chunkKeyMap[it].remove(data) }
        PlayerStarship.updateById(data._id, setValue(PlayerStarship::containedChunks, null))
        DeactivatedPlayerStarships.getSaveFile(data.bukkitWorld(), data).delete()
        savedStateCache.put(data, Optional.empty())
    }

    fun updateState(data: PlayerStarship, state: PlayerStarshipState): Unit = synchronized(mutex) {
        data.containedChunks?.forEach { chunkKeyMap[it].remove(data) }
        data.containedChunks = state.coveredChunks
        data.containedChunks?.forEach { chunkKeyMap[it].add(data) }

        PlayerStarship.updateById(data._id, setValue(PlayerStarship::containedChunks, state.coveredChunks))

        val saveFile = DeactivatedPlayerStarships.getSaveFile(data.bukkitWorld(), data)
        FileOutputStream(saveFile).use {
            state.writeToStream(it)
        }

        savedStateCache.put(data, Optional.of(state))
    }

    operator fun get(blockKey: Long): PlayerStarship? = blockKeyMap[blockKey]

    operator fun get(x: Int, y: Int, z: Int): PlayerStarship? = this[blockKey(x, y, z)]

    fun getInChunk(chunk: Chunk): List<PlayerStarship> {
        val chunkKey = chunk.chunkKey

        if (!chunkKeyMap.containsKey(chunkKey)) {
            return listOf()
        }

        return chunkKeyMap[chunkKey].toList()
    }

    fun getLockedContaining(x: Int, y: Int, z: Int): PlayerStarship? {
        val blockKey = blockKey(x, y, z)
        val chunkKey = chunkKey(x shr 4, z shr 4)

        for (data: PlayerStarship in chunkKeyMap.get(chunkKey)) {
            if (!data.isLockActive()) {
                continue
            }

            val state = savedStateCache[data].orNull() ?: continue

            if (!state.blockMap.containsKey(blockKey)) {
                continue
            }

            return data
        }

        return null
    }
}
