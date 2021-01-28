package me.geek.tom.slimeforfabric.io

@ExperimentalUnsignedTypes
interface DataOutput {
    fun writeSlimeHeader() {
        writeHeader((0xB1).toUByte(), (0x0C).toUByte())
    }

    fun writeHeader(a: UByte, b: UByte)
    fun writeUByte(b: UByte)
    fun writeShort(s: Short)
}
