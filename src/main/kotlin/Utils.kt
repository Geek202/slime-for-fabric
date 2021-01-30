package me.geek.tom.slimeforfabric

import com.github.luben.zstd.Zstd
import me.geek.tom.slimeforfabric.io.OkioSourceInput
import me.geek.tom.slimeforfabric.ser.SlimeSerialiser
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
import java.lang.StringBuilder
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Consumer
import kotlin.io.use
import kotlin.math.ceil

//@ExperimentalUnsignedTypes
//fun main() {
////    val serialiser = SlimeSerialiser
//    val path = Paths.get("slime_worlds/real_world_1.slimem")
//    path.source().use {
//        it.buffer().use { buffer ->
//            val input = OkioSourceInput(buffer)
//            if (!input.checkSlimeHeader()) {
//                prprintln("Invalid slime header!")
//                return
//            }
//            val version = input.readUByte()
//            println("Got slime version $version")
//            val x = input.readShort()
//            val z = input.readShort()
//            println("Region starts at $x,$z")
//            val width = input.readUShort()
//            val depth = input.readUShort()
//            println("Region is ${width}x${depth}")
//            val chunkBitmaskLength = ceil(((width*depth).toDouble() / (8.0))).toInt()
//            println("Reading $chunkBitmaskLength byte(s) for the chunk bitmask")
//            for (i in 1..chunkBitmaskLength) input.readUByte()
//            val chunkData = input.decompressAndReadBuffer()
//            println("Read ${chunkData.size} byte(s) of chunk data")
//            val blockEntityData = input.decompressAndReadBuffer()
//            val entityData = input.decompressAndReadBuffer()
//            println("Read ${blockEntityData.size} byte(s) of block entity data and ${entityData.size} byte(s) of entity data")
//            val blockEntitiesTag = blockEntityData.readNbt()
//            val entitiesTag = entityData.readNbt()
//            println("Block entities: $blockEntitiesTag")
//            println("Entities: $entitiesTag")
//            val customData = input.decompressAndReadBuffer()
//            val customTag = customData.readNbt()
//            println("Read custom data: $customTag")
//        }
//    }
//}

@ExperimentalUnsignedTypes
fun main() {
    val input = "Hello, World!".toByteArray()
    println("Input:        0x${toHexString(input)}")
    val compressed = Zstd.compress(input)
    println("Compressed:   0x${toHexString(compressed)}")
    val uncompressed = Zstd.decompress(compressed, input.size)
    println("Decompressed: 0x${toHexString(uncompressed)}")
}

@ExperimentalUnsignedTypes
private fun toHexString(a: ByteArray): String {
    val builder = StringBuilder()
    for (byte in a) {
        builder.append(byte.toUByte().toString(16))
    }
    return builder.toString()
}

fun ServerWorld.getChunk(pos: ChunkPos): Chunk {
    return this.getChunk(pos.x, pos.z)
}

fun Chunk.isEmpty(): Boolean {
    val hm = this.getHeightmap(Heightmap.Type.WORLD_SURFACE)
    return hm.asLongArray().none { it != 0L }
}

fun Buffer.writeListTag(tag: ListTag) {
    val tg = CompoundTag()
    tg.put("Data", tag)
    this.writeNbt(tg)
}

fun DataOutputStream.writeListTag(tag: ListTag) {
    val tg = CompoundTag()
    tg.put("Data", tag)
    val baos = ByteArrayOutputStream()
    val output = DataOutputStream(baos)
    NbtIo.write(tg, output)
    val bytes = baos.toByteArray()
//    println("Write ${bytes.size} bytes of NBT")
    this.writeInt(bytes.size)
    this.write(bytes)
}

fun Buffer.writeNbt(tag: CompoundTag) {
    val baos = ByteArrayOutputStream()
    val output = DataOutputStream(baos)
    NbtIo.write(tag, output)
    val bytes = baos.toByteArray()
//    println("Write ${bytes.size} bytes of NBT")
    this.writeInt(bytes.size)
    this.write(bytes)
}

fun Buffer.readNbt(): CompoundTag {
    val length = this.readInt()
    println("NBT data length $length")
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
