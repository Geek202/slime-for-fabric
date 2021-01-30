package me.geek.tom.slimeforfabric.deser

import me.geek.tom.slimeforfabric.getChunk
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.ChunkPos

class SlimeWorld(
    val chunks: Map<ChunkPos, SlimeChunk>,
) {

    fun copyInto(world: ServerWorld) {
        for ((pos, chunk) in this.chunks.entries) {
            chunk.copyInto(world.getChunk(pos))
        }
    }
}
