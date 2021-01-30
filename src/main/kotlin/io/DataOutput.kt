package me.geek.tom.slimeforfabric.io

import me.geek.tom.slimeforfabric.util.Bitset

@ExperimentalUnsignedTypes
interface DataOutput {
    fun writeSlimeHeader() {
        writeHeader((0xB1).toUByte(), (0x0C).toUByte())
    }

    fun writeHeader(a: UByte, b: UByte)
    fun writeUByte(b: UByte)
    fun writeUShort(s: UShort)
    fun writeShort(s: Short)
    fun writeInt(i: Int)
    fun writeBitSet(bitSet: Bitset, padToLength: Int)
    fun writeBytes(bytes: ByteArray)

    fun compressAndWriteBuffer(buffer: ByteArray)
}
