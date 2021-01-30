package me.geek.tom.slimeforfabric.io

import com.github.luben.zstd.Zstd
import me.geek.tom.slimeforfabric.util.Bitset
import okio.BufferedSink

@ExperimentalUnsignedTypes
class OkioSinkOutput(
    private val buffer: BufferedSink,
) : DataOutput {
    override fun writeHeader(a: UByte, b: UByte) {
        buffer.writeByte(a.toInt()).writeByte(b.toInt())
    }

    override fun writeUByte(b: UByte) {
        buffer.writeByte(b.toInt())
    }

    override fun writeUShort(s: UShort) {
        buffer.writeShort(s.toInt())
    }

    override fun writeShort(s: Short) {
        buffer.writeShort(s.toInt())
    }

    override fun writeInt(i: Int) {
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
    }

    override fun writeBytes(bytes: ByteArray) {
        this.buffer.write(bytes)
    }

    override fun compressAndWriteBuffer(buffer: ByteArray) {
        val uncompressedSize = buffer.size

        val compressed = Zstd.compress(buffer)

        this.buffer.writeInt(compressed.size)
        this.buffer.writeInt(uncompressedSize)
        this.buffer.write(compressed)
    }
}
