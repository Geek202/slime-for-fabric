package me.geek.tom.slimeforfabric.io

import com.github.luben.zstd.Zstd
import me.geek.tom.slimeforfabric.util.Bitset
import okio.Buffer
import okio.BufferedSource

@ExperimentalUnsignedTypes
class OkioSourceInput(
    private val source: BufferedSource
) : DataInput {
    override fun readHeader(): Pair<UByte, UByte> {
        return Pair(readUByte(), readUByte())
    }

    override fun readUByte(): UByte {
        return source.readByte().toUByte()
    }

    override fun readUShort(): UShort {
        return source.readShort().toUShort()
    }

    override fun readShort(): Short {
        return source.readShort()
    }

    override fun readInt(): Int {
        return source.readInt()
    }

    override fun readBitset(length: UInt, bytesLength: Int): Bitset {
        val bytes = this.readBytes(bytesLength)
        return Bitset.fromBytes(bytes, length.toInt())
    }

    override fun readBytes(length: Int): ByteArray {
        return this.source.readByteArray(length.toLong())
    }

    override fun decompressAndReadBuffer(): Buffer {
        val compressedSize = source.readInt()
        val uncompressedSize = source.readInt()
        val compressedData = ByteArray(compressedSize)
        var read = 0
        while (read < compressedData.size) {
            read += source.read(compressedData, read, compressedData.size - read)
        }

        val decompressedData = Zstd.decompress(compressedData, uncompressedSize)

        val buffer = Buffer()
        buffer.write(decompressedData)
        return buffer
    }
}
