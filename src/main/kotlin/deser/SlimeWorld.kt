package me.geek.tom.slimeforfabric.deser

import me.geek.tom.slimeforfabric.getChunk
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos

class SlimeWorld(
    private val chunks: Map<ChunkPos, SlimeChunk>,
    private val blockEntities: ListTag,
    private val entities: ListTag,
) {

    fun copyInto(world: ServerWorld) {
        this.copyChunkData(world)
        this.copyBlockEntities(world)
        this.copyEntities(world)
    }

    private fun copyChunkData(world: ServerWorld) {
        for ((pos, chunk) in this.chunks.entries) {
            chunk.copyInto(world.getChunk(pos))
        }
    }

    private fun copyBlockEntities(world: ServerWorld) {
        val pos = BlockPos.Mutable()
        for (be in this.blockEntities) {
            val tag = be as CompoundTag
            pos.set(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"))
            val chunk = world.getChunk(pos)
            chunk.addPendingBlockEntityTag(tag)
        }
    }

    private fun copyEntities(world: ServerWorld) {
        val pos = BlockPos.Mutable()
        for (tag in this.entities) {
            val entityTag = tag as CompoundTag
            EntityType.loadEntityWithPassengers(entityTag, world) { entity: Entity ->
                pos.set(entity.x, entity.y, entity.z)
                world.getChunk(pos).addEntity(entity)
                entity
            }
        }
    }
}
