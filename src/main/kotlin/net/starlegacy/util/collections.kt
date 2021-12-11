package net.starlegacy.util

import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import com.google.common.collect.Table
import java.util.AbstractMap
import java.util.concurrent.ThreadLocalRandom

fun <T> List<T>.randomEntry(): T = when {
    this.isEmpty() -> error("No entries in list to pick from!")
    this.size == 1 -> single()
    else -> this[randomInt(0, size)]
}

fun <K, V> Pair<K, V>.toEntry() = AbstractMap.SimpleEntry<K, V>(this.first, this.second)
fun <K, V> Iterable<Pair<K, V>>.toEntries() = map { it.toEntry() }

@Suppress("UnstableApiUsage")
fun <K, V> multimapOf(): Multimap<K, V> = MultimapBuilder.hashKeys().arrayListValues().build<K, V>()

fun <K, V> multimapOf(vararg pairs: Pair<K, V>): ImmutableMultimap<K, V> = ImmutableMultimap
    .copyOf(pairs.toList().toEntries())

operator fun <R, C, V> Table<R, C, V>.set(row: R, column: C, value: V): V? = put(row, column, value)

fun <T> Set<T>.randomEntry(): T = when {
    this.isEmpty() -> error("No entries in list to pick from!")
    this.size == 1 -> single()
    else -> {
        val index = ThreadLocalRandom.current().nextInt(this.size)
        val iter = this.iterator()
        for (i in 0 until index) {
            iter.next()
        }
        /*(return)*/ iter.next()
    }
}
