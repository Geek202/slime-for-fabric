package me.geek.tom.slimeforfabric.deser

import me.geek.tom.slimeforfabric.io.DataInput
import me.geek.tom.slimeforfabric.readLongArray
import me.geek.tom.slimeforfabric.readNbt
import me.geek.tom.slimeforfabric.util.Bitset
import me.geek.tom.slimeforfabric.util.ChunkArea
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.ChunkSectionPos
import net.minecraft.util.registry.DynamicRegistryManager
import net.minecraft.util.registry.Registry
import net.minecraft.world.LightType
import net.minecraft.world.biome.source.BiomeArray
import net.minecraft.world.chunk.ChunkNibbleArray
import net.minecraft.world.chunk.ChunkSection
import okio.Buffer
import java.io.IOException
import kotlin.math.ceil

@ExperimentalUnsignedTypes
object SlimeDeserialiser {

    /**
     * Loads the data from [input] into the given [ServerWorld], overwriting any previous data.
     */
    fun deserialise(input: DataInput, world: ServerWorld) {
        if (!input.checkSlimeHeader()) {
            throw IOException("Invalid slime header!")
        }

        val version = input.readUByte()
        if (version != (3).toUByte()) {
            throw IOException("Slime format $version is not supported!")
        }

        // This is the 'base' location that the data will be loaded at
        val x = input.readShort()
        val z = input.readShort()

        // The width and depth of the area that this slime file covers
        val width = input.readUShort()
        val depth = input.readUShort()

        // Combine the region metadata into a ChunkArea for easier iteration later
        val area = ChunkArea(x.toInt(), z.toInt(), width.toInt(), depth.toInt())

        // This species what chunks are contained in the file, as empty chunks are skipped.
        val chunkBitmaskLength = ceil(((width*depth).toDouble() / (8.0))).toInt()
        val chunkBitmask = input.readBitset(width*depth, chunkBitmaskLength)

        // Raw binary blob of the chunk data, ready for parsing into SlimeChunks (a special form of vanilla ProtoChunk)
        val chunkData = input.decompressAndReadBuffer()

        val chunks: MutableMap<ChunkPos, SlimeChunk> = HashMap()
        for ((index, chunkPos) in area.withIndex()) {
            if (chunkBitmask[index] == true) {
                val lightingProvider = world.chunkManager.lightingProvider
                val chunk = readBaseChunkData(chunkPos, chunkData, world.registryManager, { section, blockLight ->
                    // Set this last value to false, because it seems to make vanilla correct lighting on neighbouring chunks.
                    lightingProvider.enqueueSectionData(LightType.BLOCK, section, blockLight, false)
                }, { section, skyLight ->
                    // Set this last value to false, because it seems to make vanilla correct lighting on neighbouring chunks.
                    lightingProvider.enqueueSectionData(LightType.SKY, section, skyLight, false)
                })
                // Place the chunk into our temporary storage.
                chunks[chunkPos] = chunk
            }
        }

        // /slime write normal_world_1 -2 -2 5 5 {}
        // Read the BlockEntity data
        val blockEntityData = input.readCompressedNbt()
        val blockEntityTag = blockEntityData.getList("tiles", 10)

        // Read the entity data
        val entityData = input.readCompressedNbt()
        val entityTag = entityData.getList("entities", 10)

        // Bundle all the data into a SlimeWorld object
        val slimeWorld = SlimeWorld(chunks, blockEntityTag, entityTag)
        // Actually place the loaded data into the world.
        slimeWorld.copyInto(world)
    }

    private fun readBaseChunkData(pos: ChunkPos, data: Buffer, registryManager: DynamicRegistryManager,
                                  blockLightHandler: (ChunkSectionPos, ChunkNibbleArray) -> Unit,
                                  skyLightHandler: (ChunkSectionPos, ChunkNibbleArray) -> Unit): SlimeChunk {
        val heightmapData = data.readLongArray(37) // Got this from looking at the length of the array in a vanilla region file.
        val biomeData = IntArray(1024) // Again, got this value from a vanilla file
        for (i in 0 until 1024) biomeData[i] = data.readInt()

        val sectionsBitmask = Bitset.fromBytes(data.readByteArray(2), 16)
        val sections = arrayOfNulls<ChunkSection>(16)
        for (i in 0 until 16) {
            val isSectionPresent = sectionsBitmask[i]!!
            if (!isSectionPresent) {
                sections[i] = null
                continue
            }

            val sectionPos = ChunkSectionPos.from(pos, i)
            val section = readChunkSection(i, data, { lighting ->
                blockLightHandler.invoke(sectionPos, lighting)
            }, { lighting ->
                skyLightHandler.invoke(sectionPos, lighting)
            })
            sections[i] = section
        }

        val chunk = SlimeChunk(pos, sections)
        chunk.setBiomes(BiomeArray(registryManager.get(Registry.BIOME_KEY), biomeData))
        return chunk
    }

    private fun readChunkSection(i: Int, data: Buffer, blockLightHandler: (ChunkNibbleArray) -> Unit, skyLightHandler: (ChunkNibbleArray) -> Unit): ChunkSection {
        val yOffset = i shl 4 // Multiply by 16
        val section = ChunkSection(yOffset)

        val blockLightData = data.readByteArray(2048)
        val blockLight = ChunkNibbleArray(blockLightData)
        blockLightHandler.invoke(blockLight)

        // NBT tags must be serialised with a compound at the root, and so we wrap lists with 'Data'
        val paletteTag = data.readNbt().getList("Data", 10)

        val blockDataLength = data.readInt()
        val blockData = data.readLongArray(blockDataLength) // Size from mc region file
        section.container.read(paletteTag, blockData)

        val skyLightData = data.readByteArray(2048)
        val skyLight = ChunkNibbleArray(skyLightData)
        skyLightHandler.invoke(skyLight)

        return section
    }

    private fun createEmptyChunk(pos: ChunkPos): SlimeChunk {
        return SlimeChunk(pos, null)
    }
}