package me.geek.tom.slimeforfabric.ser

import me.geek.tom.slimeforfabric.getChunk
import me.geek.tom.slimeforfabric.io.Bitset
import me.geek.tom.slimeforfabric.io.DataOutput
import me.geek.tom.slimeforfabric.isEmpty
import me.geek.tom.slimeforfabric.util.ChunkArea
import me.geek.tom.slimeforfabric.writeListTag
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtIo
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.ChunkSectionPos
import net.minecraft.world.Heightmap
import net.minecraft.world.LightType
import net.minecraft.world.chunk.ChunkNibbleArray
import net.minecraft.world.chunk.ChunkSection
import net.minecraft.world.chunk.WorldChunk
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.math.ceil

@ExperimentalUnsignedTypes
object SlimeSerialiser {
    fun serialiseWorld(output: DataOutput, world: ServerWorld, area: ChunkArea, custom: CompoundTag = CompoundTag()) {
        this.writeHeaders(output)
        this.writeMetadata(output, area)
        this.writeChunkBitmask(output, world, area)
        this.writeChunkData(output, world, area)
        this.writeCompressedNbt(output, world, area, this::buildBlockEntityData)
        this.writeCompressedNbt(output, world, area, this::buildEntityData)
        this.writeCompressedNbt(output, world, area) { _, _ -> custom }
    }

    private fun writeCompressedNbt(output: DataOutput, world: ServerWorld, area: ChunkArea, generator: (ServerWorld, ChunkArea) -> CompoundTag) {
        val buffer = ByteArrayOutputStream()
        val os = DataOutputStream(buffer)
        NbtIo.write(generator.invoke(world, area), os)
        output.compressAndWriteBuffer(buffer.toByteArray())
    }

    private fun buildEntityData(world: ServerWorld, area: ChunkArea): CompoundTag {
        val entities = ListTag()
        for (chunkPos in area) {
            val chunk = world.getChunk(chunkPos) as WorldChunk
            for (list in chunk.entitySectionArray) {
                for (entity in list.asIterable()) {
                    val tag = CompoundTag()
                    if (entity.saveToTag(tag)) {
                        entities.add(tag)
                    }
                }
            }
        }
        val tag = CompoundTag()
        tag.put("entities", entities)
        return tag
    }

    private fun buildBlockEntityData(world: ServerWorld, area: ChunkArea): CompoundTag {
        val bes = ListTag()
        val rootTag = CompoundTag()
        for (chunkPos in area) {
            val chunk = world.getChunk(chunkPos) as WorldChunk
            for (be in chunk.blockEntities.values) {
                val beData = be.toTag(CompoundTag())
                bes.add(beData)
            }
        }
        rootTag.put("tiles", bes)
        return rootTag
    }

    /**
     * Spec: https://gist.github.com/Geek202/b30aa8362f5dcc635f6703ecbc88f336#file-slime_modified_format-txt-L17
     */
    private fun writeChunkData(output: DataOutput, world: ServerWorld, area: ChunkArea) {
        val chunkData = buildChunkData(world, area)
        val debug = Paths.get("asdasd_pls_just_work_1.bin")
        if (!Files.exists(debug)) Files.write(debug, chunkData)
        output.compressAndWriteBuffer(chunkData)
    }

    /**
     * Spec: https://gist.github.com/Geek202/b30aa8362f5dcc635f6703ecbc88f336#file-slime_modified_format-txt-L19
     */
    private fun buildChunkData(world: ServerWorld, area: ChunkArea): ByteArray {
        val baos = ByteArrayOutputStream()
        val buffer = DataOutputStream(baos)
        val blockLighting = world.lightingProvider[LightType.BLOCK]
        val skyLighting = world.lightingProvider[LightType.SKY]

        for (chunkPos in area) {
            // Spec for chunk data: https://gist.github.com/Geek202/b30aa8362f5dcc635f6703ecbc88f336#file-slime_modified_format-txt-L47

            val chunk = world.getChunk(chunkPos)
            if (chunk.isEmpty()) continue

            val heightmap = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE)
            val heightmapLongs = heightmap.asLongArray()
//            println("Heightmap data length: ${heightmapLongs.size}")
            heightmapLongs.forEach(buffer::writeLong)
            // We assert not-null here as a ServerWorld only deals with WorldChunks (afaik at time of writing)
            val biomeData = chunk.biomeArray!!.toIntArray()
//            println("Biome data length: ${biomeData.size}")
            biomeData.forEach(buffer::writeInt)

            // Write the chunk sections
            val sectionsBaos = ByteArrayOutputStream()
            val sectionsBuffer = DataOutputStream(sectionsBaos)
            var sectionMask = (0).toUShort()
            val sectionBitset = Bitset()

            for ((sectionIndex, chunkSection) in chunk.sectionArray.withIndex()) {
                if (chunkSection == null) {
                    sectionBitset[sectionIndex] = false
                    continue
                }
                val sectionPos = ChunkSectionPos.from(chunkPos.x, sectionIndex, chunkPos.z)
                val hasData = writeChunkSection(sectionsBuffer, chunkSection, blockLighting.getLightSection(sectionPos)!!, skyLighting.getLightSection(sectionPos)!!)
                if (hasData) {
                    val bit = (1) shl sectionIndex
                    sectionMask = sectionMask or bit.toUShort()
                }
                sectionBitset[sectionIndex] = hasData
            }

            buffer.write(sectionBitset.packToBytes())
            buffer.write(sectionsBaos.toByteArray())
        }

        return baos.toByteArray()
    }

    /**
     * Spec: https://gist.github.com/Geek202/b30aa8362f5dcc635f6703ecbc88f336#file-slime_modified_format-txt-L11
     */
    private fun writeChunkBitmask(output: DataOutput, world: ServerWorld, area: ChunkArea) {
        val bitSet = Bitset()
        var bitCount = 0
        for ((currentBit, chunkPos) in area.withIndex()) {
            val bit = !world.getChunk(chunkPos).isEmpty()
            bitSet[currentBit] = bit
            bitCount++
        }
        println("Bitcount: $bitCount")
        val computedMaskLength = ceil((area.width * area.depth).toDouble() / 8.0).toInt()
        output.writeBitSet(bitSet, computedMaskLength)
        println("Chunk count: ${area.depth * area.width}, bit count: ${bitSet.size()}")
        println("Wrote $computedMaskLength bytes for chunk bitmask!")
    }

    /**
     * Spec: https://gist.github.com/Geek202/b30aa8362f5dcc635f6703ecbc88f336#file-slime_modified_format-txt-L47
     */
    private fun writeChunkSection(buffer: DataOutputStream, section: ChunkSection, blockLight: ChunkNibbleArray, skylight: ChunkNibbleArray): Boolean {
        if (section.isEmpty) return false

        val blockLightBytes = blockLight.asByteArray()
//        println("Block light length: ${blockLightBytes.size}")
//        val initialSize = buffer.size
        buffer.write(blockLightBytes)
//        println("Buffer grew by ${buffer.size - initialSize} bytes!")

        val containerTag = CompoundTag()
        section.container.write(containerTag, "palette", "blocks")
        val paletteTag = containerTag.getList("palette", 10)!! // assert non-null here, as we know it was written above
        val blocks = containerTag.getLongArray("blocks")
//        println("Blocks are ${blocks.size * 8} bytes!")
        buffer.writeListTag(paletteTag)
        buffer.writeInt(blocks.size)
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
        output.writeUShort(area.width)
        output.writeUShort(area.depth)
    }

    /**
     * Spec: https://gist.github.com/Geek202/b30aa8362f5dcc635f6703ecbc88f336#file-slime_modified_format-txt-L5
     */
    private fun writeHeaders(output: DataOutput) {
        output.writeSlimeHeader()
        output.writeUByte(FORMAT_VERSION)
    }

    private val FORMAT_VERSION = (0x03).toUByte()
}
