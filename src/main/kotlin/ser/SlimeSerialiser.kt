package me.geek.tom.slimeforfabric.ser

import me.geek.tom.slimeforfabric.getChunk
import me.geek.tom.slimeforfabric.io.DataOutput
import me.geek.tom.slimeforfabric.isEmpty
import me.geek.tom.slimeforfabric.toIntArray
import me.geek.tom.slimeforfabric.util.ChunkArea
import me.geek.tom.slimeforfabric.writeListTag
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.ChunkSectionPos
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
        this.writeChunkData(output, world, area)
    }

    /**
     * Spec: https://gist.github.com/Geek202/b30aa8362f5dcc635f6703ecbc88f336#file-slime_modified_format-txt-L17
     */
    private fun writeChunkData(output: DataOutput, world: ServerWorld, area: ChunkArea) {
        val chunkData = buildChunkData(world, area)
        output.compressAndWriteBuffer(chunkData)
    }

    /**
     * Spec: https://gist.github.com/Geek202/b30aa8362f5dcc635f6703ecbc88f336#file-slime_modified_format-txt-L19
     */
    private fun buildChunkData(world: ServerWorld, area: ChunkArea): Buffer {
        val buffer = Buffer()
        val blockLighting = world.lightingProvider[LightType.BLOCK]
        val skyLighting = world.lightingProvider[LightType.SKY]

        for (chunkPos in area) {
            // Spec for chunk data: https://gist.github.com/Geek202/b30aa8362f5dcc635f6703ecbc88f336#file-slime_modified_format-txt-L47

            val chunk = world.getChunk(chunkPos)
            val heightmap = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE)
            heightmap.toIntArray().forEach(buffer::writeInt)
            // We assert not-null here as a ServerWorld only deals with WorldChunks (afaik at time of writing)
            chunk.biomeArray!!.toIntArray().forEach(buffer::writeInt)

            // Write the chunk sections
            val sectionsBuffer = Buffer()
            var sectionMask = (0).toUShort()

            for (chunkSection in chunk.sectionArray) {
                if (chunkSection == null) {
                    println("Null chunk section!")
                    continue
                }
                val sectionIndex = chunkSection.yOffset shr 16
                val sectionPos = ChunkSectionPos.from(chunkPos.x, sectionIndex, chunkPos.z)
                val empty = writeChunkSection(sectionsBuffer, chunkSection, blockLighting.getLightSection(sectionPos)!!, skyLighting.getLightSection(sectionPos)!!)
                if (!empty) {
                    val bit = (1) shl sectionIndex
                    sectionMask = sectionMask or bit.toUShort()
                }
           }

            buffer.writeShort(sectionMask.toInt())
            buffer.write(sectionsBuffer, sectionsBuffer.size)
        }

        return buffer
    }

    /**
     * Spec: https://gist.github.com/Geek202/b30aa8362f5dcc635f6703ecbc88f336#file-slime_modified_format-txt-L11
     */
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

    /**
     * Spec: https://gist.github.com/Geek202/b30aa8362f5dcc635f6703ecbc88f336#file-slime_modified_format-txt-L47
     */
    fun writeChunkSection(buffer: Buffer, section: ChunkSection, blockLight: ChunkNibbleArray, skylight: ChunkNibbleArray): Boolean {
        if (section.isEmpty) return false

        buffer.write(blockLight.asByteArray())

        val containerTag = CompoundTag()
        section.container.write(containerTag, "palette", "blocks")
        val paletteTag = containerTag.getList("palette", 10)!! // assert non-null here, as we know it was written above
        val blocks = containerTag.getLongArray("blocks")
        buffer.writeListTag(paletteTag)
        blocks.forEach(buffer::writeLong)

        buffer.write(skylight.asByteArray())

        return true
    }

    /**
     * Spec: https://gist.github.com/Geek202/b30aa8362f5dcc635f6703ecbc88f336#file-slime_modified_format-txt-L7
     */
    private fun writeMetadata(output: DataOutput, area: ChunkArea) {
        val lowest = area.lowest
        output.writeShort(lowest.x.toShort())
        output.writeShort(lowest.z.toShort())
        output.writeShort(area.width)
        output.writeShort(area.depth)
    }

    /**
     * Spec: https://gist.github.com/Geek202/b30aa8362f5dcc635f6703ecbc88f336#file-slime_modified_format-txt-L5
     */
    fun writeHeaders(output: DataOutput) {
        output.writeSlimeHeader()
        output.writeUByte(FORMAT_VERSION)
    }

    val FORMAT_VERSION = (0x03).toUByte()
}
