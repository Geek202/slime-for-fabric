package me.geek.tom.slimeforfabric.util

import me.geek.tom.slimeforfabric.readLongArray
import okio.Buffer
import okio.buffer
import okio.source
import java.nio.file.Paths

fun main() {
    val buffer = Buffer()
    Paths.get("run").resolve("debugging_after.bin").source().use {
        it.buffer().use { source ->
            source.readAll(buffer)
        }
    }
    checkBufferStateForNbtRead(buffer)
}

fun checkBufferStateForNbtRead(buffer: Buffer) {
    val initialSize = buffer.size
    val heightmap = buffer.readLongArray(37)
    val sizeAfterHeightmap = buffer.size
    println("Heightmap read ${initialSize - sizeAfterHeightmap} bytes!")

    val biomes = IntArray(1024)
    for ((i, _) in biomes.withIndex()) biomes[i] = buffer.readInt()
    println("biomes correct: ${biomes.all { it == 6 }}")
    val sizeAfterBiomes = buffer.size
    println("Biomes read ${sizeAfterHeightmap - sizeAfterBiomes} bytes!")

    val bitmask = buffer.readByteArray(2)
    val sizeAfterBitmask = buffer.size
    println("Bitmask read ${sizeAfterBiomes - sizeAfterBitmask} bytes!")

    val data = buffer.readByteArray(2048)
    val sizeAfterBlockLight = buffer.size
    println("Block light read ${sizeAfterBitmask - sizeAfterBlockLight} bytes!")

    println("All zero: ${data.all { it == (0).toByte() }}, data size: ${data.size}")
    val nbtSize = buffer.readInt()
    println("Got NBT size: $nbtSize!")
}
