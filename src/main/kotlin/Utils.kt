package me.geek.tom.slimeforfabric

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtIo
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.Heightmap
import net.minecraft.world.chunk.Chunk
import okio.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.file.Path
import java.util.function.Consumer
import kotlin.io.use


//@ExperimentalUnsignedTypes
//private fun toHexString(a: ByteArray): String {
//    val builder = StringBuilder()
//    for (byte in a) {
//        builder.append(byte.toUByte().toString(16))
//    }
//    return builder.toString()
//}

fun ServerWorld.getChunk(pos: ChunkPos): Chunk {
    return this.getChunk(pos.x, pos.z)
}

fun Chunk.isEmpty(): Boolean {
    val hm = this.getHeightmap(Heightmap.Type.WORLD_SURFACE)
    return hm.asLongArray().none { it != 0L }
}

fun DataOutputStream.writeListTag(tag: ListTag) {
    val tg = CompoundTag()
    tg.put("Data", tag)
    val baos = ByteArrayOutputStream()
    val output = DataOutputStream(baos)
    NbtIo.write(tg, output)
    val bytes = baos.toByteArray()
    this.writeInt(bytes.size)
    this.write(bytes)
}

fun Buffer.readNbt(): CompoundTag {
    val length = this.readInt()
    val bais = ByteArrayInputStream(this.readByteArray(length.toLong()))
    val input = DataInputStream(bais)
    return NbtIo.read(input)
}

fun Buffer.readLongArray(length: Int): LongArray {
    val longs = LongArray(length)
    for (i in 0 until length) longs[i] = readLong()
    return longs
}

fun write(path: Path, output: Consumer<BufferedSink>) {
    path.sink().use {
        it.buffer().use { buffer ->
            output.accept(buffer)
        }
    }
}

fun read(path: Path, input: Consumer<BufferedSource>) {
    path.source().use {
        it.buffer().use { buffer ->
            input.accept(buffer)
        }
    }
}
