package me.geek.tom.slimeforfabric.util

import com.google.common.collect.AbstractIterator
import net.minecraft.util.math.ChunkPos
import kotlin.math.abs

data class ChunkArea(
    val a: ChunkPos,
    val b: ChunkPos,
) : Iterable<ChunkPos> {

    val lowest get() = a
    val highest get() = b

    val width get() = abs(a.x - b.x).toShort()
    val depth get() = abs(a.z - b.z).toShort()

    constructor(x: Int, z: Int, width: Int, depth: Int): this(ChunkPos(x, z), ChunkPos(x + width, z + depth))

    override fun iterator(): kotlin.collections.Iterator<ChunkPos> {
        return Iterator(a, b)
    }

    private class Iterator(
        private val min: ChunkPos,
        private val max: ChunkPos,
    ) : AbstractIterator<ChunkPos>() {

        private var currentX = min.x
        private var currentZ = min.z

        override fun computeNext(): ChunkPos? {
            if (currentZ >= max.z) {
                currentZ = min.z
                currentX++
            } else {
                currentZ++
            }
            if (currentX > max.x) return endOfData()
            return ChunkPos(currentX, currentZ)
        }
    }
}
