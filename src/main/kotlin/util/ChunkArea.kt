package me.geek.tom.slimeforfabric.util

import com.google.common.collect.AbstractIterator
import net.minecraft.util.math.ChunkPos
import kotlin.math.abs

@ExperimentalUnsignedTypes
data class ChunkArea(
    val a: ChunkPos,
    val b: ChunkPos,
) : Iterable<ChunkPos> {

    val lowest get() = a
    val highest get() = b

    val width get() = abs(a.x - b.x).toUShort()
    val depth get() = abs(a.z - b.z).toUShort()

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
            if (currentX >= max.x) {
                currentX = min.x
                currentZ++
            }

            val ret = ChunkPos(currentX, currentZ)

            if (currentX < max.x) {
                currentX++
            }
            if (currentZ >= max.z) return endOfData()

//            println("X: ${ret.x.toString().padStart(2)} Z: ${ret.z.toString().padStart(2)}")
            return ret
        }
    }
}
