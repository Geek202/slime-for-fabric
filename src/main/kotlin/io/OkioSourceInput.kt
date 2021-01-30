package me.geek.tom.slimeforfabric.io

import com.github.luben.zstd.Zstd
import okio.Buffer
import okio.BufferedSource
import okio.buffer
import okio.sink
import java.nio.file.Files
import java.nio.file.Paths

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
//        println("Buffer pos before read")

        val compressedSize = source.readInt()
        val uncompressedSize = source.readInt()
        println("Largest: ${if (compressedSize > uncompressedSize) "compressed" else "uncompressed" } Compressed: $compressedSize Uncompressed: $uncompressedSize")
        val compressedData = ByteArray(compressedSize)
        var read = 0
        while (read < compressedData.size) {
            read += source.read(compressedData, read, compressedData.size - read)
        }

        println("Compressed size read: ${compressedData.size}")
        val buffer = Buffer()

        var debug = Paths.get("compressed_data_after.bin")
        if (!Files.exists(debug)) {
            debug.sink().use {
                it.buffer().use { sink ->
                    sink.write(compressedData)
                }
            }
        }

        val decompressedData = Zstd.decompress(compressedData, uncompressedSize)

        debug = Paths.get("decompressed_data_after.bin")
        if (!Files.exists(debug)) {
            debug.sink().use {
                it.buffer().use { sink ->
                    sink.write(decompressedData)
                }
            }
        }

        println("Decompressed data length: ${decompressedData.size}")
        buffer.write(decompressedData)
        return buffer
    }
}
