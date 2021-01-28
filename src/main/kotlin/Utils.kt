package me.geek.tom.slimeforfabric

import me.geek.tom.slimeforfabric.io.OkioBufferOutput
import me.geek.tom.slimeforfabric.ser.SlimeSerialiser
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.Heightmap
import net.minecraft.world.biome.source.BiomeArray
import net.minecraft.world.chunk.Chunk
import okio.BufferedSink
import okio.buffer
import okio.sink
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Consumer

@ExperimentalUnsignedTypes
fun main() {
    val serialiser = SlimeSerialiser
    val path = Paths.get("test.slime")
    path.sink().use {
        it.buffer().use { sink ->
            val output = OkioBufferOutput(sink)
            serialiser.writeHeaders(output)
        }
    }
}

fun ServerWorld.getChunk(pos: ChunkPos): Chunk {
    return this.getChunk(pos.x, pos.z)
}

fun Chunk.isEmpty(): Boolean {
    val hm = this.getHeightmap(Heightmap.Type.WORLD_SURFACE)
    return hm.asLongArray().none { it != 0L }
}

fun Heightmap.toIntArray(): IntArray {
    val longs = this.asLongArray()
    val arr = IntArray(longs.size)
    longs.forEachIndexed { i, height ->
        arr[i] = height.toInt()
    }
    return arr
}

fun write(path: Path, output: Consumer<BufferedSink>) {
    path.sink().use {
        it.buffer().use { buffer ->
            output.accept(buffer)
        }
    }
}
