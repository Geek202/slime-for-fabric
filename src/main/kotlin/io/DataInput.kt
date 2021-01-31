package me.geek.tom.slimeforfabric.io

import me.geek.tom.slimeforfabric.util.Bitset
import net.minecraft.nbt.CompoundTag
import okio.Buffer

@ExperimentalUnsignedTypes
interface DataInput {
    fun checkSlimeHeader(): Boolean {
        val header = readHeader()
        return header.first == (0xB1).toUByte() && header.second == (0x0C).toUByte()
    }

    fun readHeader(): Pair<UByte, UByte>
    fun readUByte(): UByte
    fun readUShort(): UShort
    fun readShort(): Short
    fun readInt(): Int
    fun readBitset(length: UInt, bytesLength: Int): Bitset
    fun readBytes(length: Int): ByteArray

    fun decompressAndReadBuffer(): Buffer
    fun readCompressedNbt(): CompoundTag
}