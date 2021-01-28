package me.geek.tom.slimeforfabric.io

import okio.Buffer

@ExperimentalUnsignedTypes
interface DataOutput {
    fun writeSlimeHeader() {
        writeHeader((0xB1).toUByte(), (0x0C).toUByte())
    }

    fun writeHeader(a: UByte, b: UByte)
    fun writeUByte(b: UByte)
    fun writeShort(s: Short)
    fun writeInt(i: Int)

    fun compressAndWriteBuffer(buffer: Buffer)
}
