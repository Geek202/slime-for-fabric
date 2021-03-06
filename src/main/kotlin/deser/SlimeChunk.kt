package me.geek.tom.slimeforfabric.deser

import net.minecraft.fluid.Fluids
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.ChunkTickScheduler
import net.minecraft.world.chunk.*

class SlimeChunk(chunkPos: ChunkPos, sections: Array<ChunkSection?>?) :
    ProtoChunk(chunkPos, UpgradeData.NO_UPGRADE_DATA, sections, ChunkTickScheduler({ block ->
        block == null || block.defaultState.isAir
    }, chunkPos), ChunkTickScheduler({ fluid ->
        fluid == null || fluid === Fluids.EMPTY
    }, chunkPos)) {

    /**
     * This is very cursed
     */
    fun copyInto(other: Chunk) {
        System.arraycopy(this.sectionArray, 0, other.sectionArray, 0, 16)
        if (other is WorldChunk) {
            other.blockEntities.clear()
            other.markDirty()
            for (i in 0 until 16) {
                val entities = other.entitySectionArray[i].method_29903()
                for (entity in entities) {
                    entity.remove()
                }
            }
        }

        System.arraycopy(this.biomeArray!!.data, 0, other.biomeArray!!.data, 0, this.biomeArray!!.data.size)
    }
}
