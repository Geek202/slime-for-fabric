package me.geek.tom.slimeforfabric.io

import com.github.luben.zstd.Zstd
import okio.BufferedSink
import okio.buffer
import okio.sink
import java.nio.file.Files
import java.nio.file.Paths

@ExperimentalUnsignedTypes
class OkioSinkOutput(
    private val buffer: BufferedSink,
) : DataOutput {
    override fun writeHeader(a: UByte, b: UByte) {
        println("Write header: $a $b")
        buffer.writeByte(a.toInt()).writeByte(b.toInt())
    }

    override fun writeUByte(b: UByte) {
        println("Write UByte: $b")
        buffer.writeByte(b.toInt())
    }

    override fun writeUShort(s: UShort) {
        println("Write UShort: $s")
        buffer.writeShort(s.toInt())
    }

    override fun writeShort(s: Short) {
        println("Write short: $s")
        buffer.writeShort(s.toInt())
    }

    override fun writeInt(i: Int) {
        println("Write int: $i")
        buffer.writeInt(i)
    }

    override fun writeBitSet(bitSet: Bitset, padToLength: Int) {
        var writtenBytes = 0
        val bytes = bitSet.packToBytes()
        this.writeBytes(bytes)
        writtenBytes += bytes.size
        val padding = padToLength - bytes.size
        for (i in 1..padding) {
            this.writeUByte((0).toUByte())
            writtenBytes++
        }
        println("Actually wrote $writtenBytes of bitset")
    }

    override fun writeBytes(bytes: ByteArray) {
        println("Write bytes: ${bytes.size}")
        this.buffer.write(bytes)
    }

    override fun compressAndWriteBuffer(buffer: ByteArray) {
        val uncompressedSize = buffer.size
//        val baos = ByteArrayOutputStream()
//        buffer.writeTo(baos)
//        val bytes = buffer.readByteArray()
//        val uncompressedData = baos.toByteArray()

        var debug = Paths.get("uncompressed_data_before.bin")
        if (!Files.exists(debug)) {
            debug.sink().use {
                it.buffer().use { sink ->
                    sink.write(buffer)
                }
            }
        }

        val compressed = Zstd.compress(buffer)

        debug = Paths.get("compressed_data_org.bin")
        if (!Files.exists(debug)) {
            debug.sink().use {
                it.buffer().use { sink ->
                    sink.write(compressed)
                }
            }
        }

        println("Writing ${compressed.size} compressed bytes ($uncompressedSize uncompressed)")
        this.buffer.writeInt(compressed.size)
        this.buffer.writeInt(uncompressedSize)
        this.buffer.write(compressed)
    }
}
