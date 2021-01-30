package me.geek.tom.slimeforfabric.deser

import me.geek.tom.slimeforfabric.io.Bitset
import me.geek.tom.slimeforfabric.io.DataInput
import me.geek.tom.slimeforfabric.readLongArray
import me.geek.tom.slimeforfabric.readNbt
import me.geek.tom.slimeforfabric.util.ChunkArea
import me.geek.tom.slimeforfabric.util.checkBufferStateForNbtRead
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.ChunkSectionPos
import net.minecraft.world.LightType
import net.minecraft.world.chunk.ChunkNibbleArray
import net.minecraft.world.chunk.ChunkSection
import okio.Buffer
import okio.buffer
import okio.sink
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
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

        // This species what chunks are contained in the file, as empty chunks are skiped.
        val chunkBitmaskLength = ceil(((width*depth).toDouble() / (8.0))).toInt()
        val chunkBitmask = input.readBitset(width*depth, chunkBitmaskLength)

        // Raw binary blob of the chunk data, ready for parsing into SlimeChunks (a special form of vanilla ProtoChunk)
        val chunkData = input.decompressAndReadBuffer()

        val chunks: MutableMap<ChunkPos, SlimeChunk> = HashMap()
        for ((index, chunkPos) in area.withIndex()) {
            val chunk = if (chunkBitmask[index] == true) {
                val lightingProvider = world.chunkManager.lightingProvider
                val ch = readBaseChunkData(chunkPos, chunkData, { section, blockLight ->
                    // Set this last value to true, because thats what vanilla does lol.
                    lightingProvider.enqueueSectionData(LightType.BLOCK, section, blockLight, true)
                }, { section, skyLight ->
                    // Set this last value to true, because thats what vanilla does lol.
                    lightingProvider.enqueueSectionData(LightType.SKY, section, skyLight, true)
                })
                println("Chunk read done!")
                ch
            } else {
                println("empty chunk")
                createEmptyChunk(chunkPos)
            }
            // Place the chunk into our temporary storage.
            chunks[chunkPos] = chunk
        }

        // Bundle all the data into a SlimeWorld object
        val slimeWorld = SlimeWorld(chunks)
        // Actually place the loaded data into the world.
        slimeWorld.copyInto(world)
    }

    private fun readBaseChunkData(pos: ChunkPos, data: Buffer,
                                  blockLightHandler: (ChunkSectionPos, ChunkNibbleArray) -> Unit,
                                  skyLightHandler: (ChunkSectionPos, ChunkNibbleArray) -> Unit): SlimeChunk {
        // Debugging.
        checkBufferStateForNbtRead(data.copy())

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

        return SlimeChunk(pos, sections)
    }

    private fun readChunkSection(i: Int, data: Buffer, blockLightHandler: (ChunkNibbleArray) -> Unit, skyLightHandler: (ChunkNibbleArray) -> Unit): ChunkSection {
        val debuggingPath = Paths.get("debugging_after.bin")
        if (!Files.exists(debuggingPath)) {
            debuggingPath.sink().use {
                it.buffer().use { sink ->
                    data.copy().writeTo(sink.outputStream())
                }
            }
        }

        val yOffset = i shl 4 // Multiply by 16
        val section = ChunkSection(yOffset)

        val initialLength = data.size
        val blockLightData = data.readByteArray(2048)
        println("Buffer shrank by ${initialLength - data.size} bytes!")
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