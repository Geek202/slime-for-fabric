package me.geek.tom.slimeforfabric.ser

import me.geek.tom.slimeforfabric.getChunk
import me.geek.tom.slimeforfabric.io.DataOutput
import me.geek.tom.slimeforfabric.isEmpty
import me.geek.tom.slimeforfabric.toIntArray
import me.geek.tom.slimeforfabric.util.ChunkArea
import net.minecraft.server.world.ServerLightingProvider
import net.minecraft.server.world.ServerWorld
import net.minecraft.world.Heightmap
import net.minecraft.world.LightType
import net.minecraft.world.chunk.ChunkNibbleArray
import net.minecraft.world.chunk.ChunkSection
import okio.Buffer

@ExperimentalUnsignedTypes
object SlimeSerialiser {
    fun serialiseWorld(output: DataOutput, world: ServerWorld, area: ChunkArea) {
        this.writeHeaders(output)
        this.writeMetadata(output, area)
        this.writeChunkBitmask(output, world, area)
    }

    private fun writeChunkBitmask(output: DataOutput, world: ServerWorld, area: ChunkArea) {
        var currentBit = 0
        var currentByte = (0).toUByte()
        for (chunkPos in area) {
            val bit = when (!world.getChunk(chunkPos).isEmpty()) {
                true -> 1
                false -> 0
            } shl currentBit
            currentByte = currentByte or bit.toUByte()
            currentBit++
            if (currentBit >= 8) {
                currentBit = 0
                output.writeUByte(currentByte)
                currentByte = (0).toUByte()
            }
        }
        if (currentBit != 0) {
            output.writeUByte(currentByte)
        }
    }

    private fun buildChunkData(world: ServerWorld, area: ChunkArea): Buffer {
        val buffer = Buffer()

        for (chunkPos in area) {
            val chunk = world.getChunk(chunkPos)
            val heightmap = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE)
            heightmap.toIntArray().forEach(buffer::writeInt)
            // We assert not-null here as a ServerWorld only deals with WorldChunks (afaik at time of writing)
            chunk.biomeArray!!.toIntArray().forEach(buffer::writeInt)

        }

        return buffer
    }

    fun writeChunkSection(buffer: Buffer, section: ChunkSection, blockLight: ChunkNibbleArray, skylight: ChunkNibbleArray): Boolean {
        if (section.isEmpty) return false

        buffer.write(blockLight.asByteArray())


        return true
    }

    private fun writeMetadata(output: DataOutput, area: ChunkArea) {
        val lowest = area.lowest
        output.writeShort(lowest.x.toShort())
        output.writeShort(lowest.z.toShort())
        output.writeShort(area.width)
        output.writeShort(area.depth)
    }

    fun writeHeaders(output: DataOutput) {
        output.writeSlimeHeader()
        output.writeUByte(FORMAT_VERSION)
    }

    val FORMAT_VERSION = (0x03).toUByte()
}
