package me.geek.tom.slimeforfabric.io

import com.github.luben.zstd.Zstd
import com.github.luben.zstd.ZstdOutputStream
import okio.Buffer
import okio.BufferedSink
import java.io.ByteArrayOutputStream

@ExperimentalUnsignedTypes
class OkioBufferOutput(
    private val buffer: BufferedSink,
) : DataOutput {
    override fun writeHeader(a: UByte, b: UByte) {
        buffer.writeByte(a.toInt()).writeByte(b.toInt())
    }

    override fun writeUByte(b: UByte) {
        buffer.writeByte(b.toInt())
    }

    override fun writeShort(s: Short) {
        buffer.writeShort(s.toInt())
    }

    override fun writeInt(i: Int) {
        buffer.writeInt(i)
    }

    override fun compressAndWriteBuffer(buffer: Buffer) {
        val uncompressedSize = buffer.size.toInt()
        val baos = ByteArrayOutputStream()
        buffer.writeTo(baos)
        val compressed = Zstd.compress(baos.toByteArray())
        this.buffer.writeInt(compressed.size)
        this.buffer.writeInt(uncompressedSize)
        this.buffer.write(compressed)
    }
}
